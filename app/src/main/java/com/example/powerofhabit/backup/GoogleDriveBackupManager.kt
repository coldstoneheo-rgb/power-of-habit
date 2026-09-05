package com.example.powerofhabit.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.example.powerofhabit.data.local.AppDatabase
import com.example.powerofhabit.data.local.SqliteFileHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 사용자가 고른 Drive 동작. 화면·ViewModel이 "무엇을 하려다 로그인이 필요해졌는지" 기억하는 데 쓴다. */
enum class DriveAction { BACKUP, RESTORE, /** Google 계정 연결 해제 */ SIGN_OUT }

/** 백업/복원 결과. [NEEDS_SIGN_IN]이면 호출 측이 Google 로그인을 띄우고 성공 후 같은 동작을 다시 부른다. */
enum class DriveOutcome { SUCCESS, NEEDS_SIGN_IN, FAILED, /** 백업 DB가 이 앱보다 새 스키마 — 앱 업데이트 필요 */ BACKUP_TOO_NEW }

/**
 * Google Drive appDataFolder에 SQLite DB(zip)를 백업/복원한다.
 * - 로그인 판정은 한 곳([signedInAccount]): 계정이 있고 `drive.appdata` 권한까지 승인된 경우만 "로그인됨"이다.
 * - 백업·복원·자동 백업은 프로세스 전역 [ioMutex]로 직렬화한다(수동 버튼과 5초 디바운스 자동 백업이 겹쳐도 zip·업로드가 섞이지 않는다).
 * - 복원은 zip을 내려받아 **검증(엔트리·SQLite 헤더)까지 끝낸 뒤**에만 현재 DB를 교체한다.
 */
class GoogleDriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveBackup"
        private const val BACKUP_FILE_NAME = "power_of_habit_backup.zip"
        private const val DB_NAME = "power_of_habit.db"
        private val DB_ENTRY_NAMES = setOf(DB_NAME, "$DB_NAME-shm", "$DB_NAME-wal")
        /** 앱 전용 숨김 폴더(appDataFolder)만 접근하는 최소 권한 스코프. */
        private val DRIVE_APPDATA_SCOPE = Scope(DriveScopes.DRIVE_APPDATA)

        /** 백업/복원 직렬화. 인스턴스가 여러 곳에서 새로 만들어지므로 companion에 둔다. */
        private val ioMutex = Mutex()

        // Global debounced backup scheduler
        private val backupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val backupTrigger = MutableSharedFlow<GoogleDriveBackupManager>(replay = 0, extraBufferCapacity = 1)

        init {
            backupScope.launch {
                @OptIn(kotlinx.coroutines.FlowPreview::class)
                backupTrigger
                    .debounce(5000) // 5 seconds debounce
                    .collect { manager ->
                        Log.d(TAG, "Triggering debounced backup...")
                        manager.backupDatabase()
                    }
            }
        }
    }

    // ---------------------------------------------------------------- sign-in

    /** 로그인돼 있고 Drive appDataFolder 권한까지 승인된 계정. 없으면 null. 모든 "로그인 여부" 판단은 이 함수를 거친다. */
    private fun signedInAccount(): GoogleSignInAccount? = try {
        GoogleSignIn.getLastSignedInAccount(context)?.takeIf { GoogleSignIn.hasPermissions(it, DRIVE_APPDATA_SCOPE) }
    } catch (e: Exception) {
        null
    }

    fun isGoogleSignedIn(): Boolean = signedInAccount() != null

    /** 연결된 Google 계정 이메일(표시용). 없으면 null. */
    fun signedInEmail(): String? = signedInAccount()?.email

    /**
     * Drive appDataFolder 권한을 요청하는 로그인 클라이언트. 호출 측은 `signInClient().signInIntent`를
     * `StartActivityForResult`로 띄우고 결과를 [accountFromSignInResult]로 해석한다.
     * 실제로 동작하려면 Cloud Console에 이 앱의 패키지+서명 SHA-1로 Android OAuth 클라이언트가 등록돼 있어야 한다(docs/RELEASE.md §0-4).
     */
    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_APPDATA_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /**
     * Google 계정 연결 해제(signOut). Drive의 백업 파일은 그대로 남고, 다음 백업/복원 버튼이 다시 로그인을 요청한다.
     * 권한이 서버에서 회수됐거나 다른 계정으로 바꾸고 싶을 때의 유일한 탈출구(#32 자체 리뷰 #3).
     */
    suspend fun signOut(): Result<Unit> = ioMutex.withLock { // 진행 중인 백업/복원·자동 백업이 끝난 뒤에 끊는다
        try {
            signInClient().signOut().await()
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-out failed", e)
            Result.failure(e)
        }
    }

    /** GMS Task → 코루틴. 취소 가능하고 스레드를 막지 않는다(Tasks.await는 블로킹·비취소). */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                val e = task.exception
                when {
                    e != null -> cont.resumeWith(Result.failure(e))
                    task.isCanceled -> cont.cancel()
                    else -> cont.resumeWith(Result.success(task.result))
                }
            }
        }

    /** 로그인 액티비티 결과 인텐트 → 계정. 실패는 [describeSignInError]로 사용자 문구를 만든다. */
    fun accountFromSignInResult(data: Intent?): Result<GoogleSignInAccount> = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        if (GoogleSignIn.hasPermissions(account, DRIVE_APPDATA_SCOPE)) Result.success(account)
        else Result.failure(IllegalStateException("Drive 권한이 거부되었습니다"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** 로그인 실패 이유를 한 줄로. 개발자 설정 오류(코드 10)는 OAuth 클라이언트 미등록이라 따로 짚어 준다. */
    fun describeSignInError(e: Throwable): String = when {
        e is ApiException && e.statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
            "Google 로그인 설정 오류(코드 10): 이 앱의 OAuth 클라이언트가 등록되지 않았습니다"
        e is ApiException && e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google 로그인이 취소되었습니다"
        e is ApiException && e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google 로그인이 이미 진행 중입니다"
        e is ApiException && e.statusCode == GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요"
        e is ApiException && e.statusCode == CommonStatusCodes.NETWORK_ERROR -> "네트워크 오류로 Google 로그인에 실패했습니다"
        e is ApiException -> "Google 로그인 실패(코드 ${e.statusCode})"
        else -> e.message ?: "Google 로그인에 실패했습니다"
    }

    /** 기록 변경 뒤 5초 디바운스 자동 백업. 로그인돼 있지 않으면 아무것도 하지 않는다. */
    fun scheduleAutoBackup() {
        try {
            if (signedInAccount() == null) {
                Log.d(TAG, "Not signed in, skipping auto backup schedule")
                return
            }
            backupTrigger.tryEmit(this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule auto backup: ${e.message}")
        }
    }

    // ---------------------------------------------------------------- backup / restore

    private fun driveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(account.account)
        return Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("Power Of Habit")
            .build()
    }

    /** 토큰 만료·권한 회수·401/403은 "다시 로그인"으로 안내한다. 그 외는 실패. */
    private fun outcomeForError(e: Exception, what: String): DriveOutcome {
        val authProblem = e is UserRecoverableAuthIOException ||
            (e is GoogleJsonResponseException && (e.statusCode == 401 || e.statusCode == 403))
        Log.e(TAG, "Google Drive $what failed", e)
        return if (authProblem) DriveOutcome.NEEDS_SIGN_IN else DriveOutcome.FAILED
    }

    suspend fun backupDatabase(): DriveOutcome = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val account = signedInAccount() ?: return@withLock DriveOutcome.NEEDS_SIGN_IN
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file does not exist")
                return@withLock DriveOutcome.FAILED
            }
            val zipFile = File(context.cacheDir, BACKUP_FILE_NAME)
            try {
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    addFileToZip(zos, dbFile, DB_NAME)
                    File(dbFile.path + "-shm").takeIf { it.exists() }?.let { addFileToZip(zos, it, "$DB_NAME-shm") }
                    File(dbFile.path + "-wal").takeIf { it.exists() }?.let { addFileToZip(zos, it, "$DB_NAME-wal") }
                }

                val driveService = driveService(account)
                val files = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_FILE_NAME'")
                    .execute()
                    .files
                val mediaContent = com.google.api.client.http.FileContent("application/zip", zipFile)
                if (files.isNullOrEmpty()) {
                    val metadata = com.google.api.services.drive.model.File().apply {
                        name = BACKUP_FILE_NAME
                        parents = Collections.singletonList("appDataFolder")
                    }
                    driveService.files().create(metadata, mediaContent).execute()
                    Log.d(TAG, "Backup created successfully on Google Drive")
                } else {
                    driveService.files().update(files[0].id, null, mediaContent).execute()
                    Log.d(TAG, "Backup updated successfully on Google Drive")
                }
                DriveOutcome.SUCCESS
            } catch (e: Exception) {
                outcomeForError(e, "backup")
            } finally {
                zipFile.delete()
            }
        }
    }

    /**
     * Drive 백업을 내려받아 현재 DB를 교체하고 앱을 재시작한다.
     * 순서: 다운로드 → 임시 폴더에 풀기(zip slip 방어) → `power_of_habit.db`가 있고 SQLite 헤더가 맞는지 검증 → 그때서야 Room 닫고 교체.
     * 검증 전에는 현재 DB를 건드리지 않으므로, 깨진 파일을 받아도 데이터가 사라지지 않는다.
     * 성공 시 프로세스를 종료·재시작하므로 [DriveOutcome.SUCCESS]는 사실상 돌아오지 않는다.
     */
    suspend fun restoreDatabase(): DriveOutcome = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val account = signedInAccount() ?: return@withLock DriveOutcome.NEEDS_SIGN_IN
            val zipFile = File(context.cacheDir, "restore_$BACKUP_FILE_NAME")
            val stagingDir = File(context.cacheDir, "restore_staging")
            try {
                val driveService = driveService(account)
                val files = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_FILE_NAME'")
                    .execute()
                    .files
                if (files.isNullOrEmpty()) {
                    Log.e(TAG, "No backup file found on Google Drive")
                    return@withLock DriveOutcome.FAILED
                }
                FileOutputStream(zipFile).use { fos ->
                    driveService.files().get(files[0].id).executeMediaAndDownloadTo(fos)
                }
                Log.d(TAG, "Backup zip downloaded from Google Drive")

                // 1. 임시 폴더에 풀고 검증한다 — 여기서 실패하면 현재 DB는 그대로다.
                stagingDir.deleteRecursively()
                if (!stagingDir.mkdirs()) return@withLock DriveOutcome.FAILED
                val extracted = extractZip(zipFile, stagingDir)
                val stagedDb = extracted.firstOrNull { it.name == DB_NAME }
                if (stagedDb == null || !SqliteFileHeader.isSqlite(stagedDb)) {
                    Log.e(TAG, "Backup zip has no valid $DB_NAME (entries=${extracted.map { it.name }})")
                    return@withLock DriveOutcome.FAILED
                }
                // 더 새 앱이 만든 백업(스키마 버전이 높음)을 옛 앱에 넣으면 Room이 "다운그레이드 마이그레이션 없음"으로 매 실행마다 죽는다.
                val backupSchema = SqliteFileHeader.userVersion(stagedDb)
                if (backupSchema > AppDatabase.SCHEMA_VERSION) {
                    Log.e(TAG, "Backup schema v$backupSchema is newer than app v${AppDatabase.SCHEMA_VERSION}")
                    return@withLock DriveOutcome.BACKUP_TOO_NEW
                }

                // 2. Room을 닫고 현재 파일을 교체한다.
                val dbFile = context.getDatabasePath(DB_NAME)
                val dbDir = dbFile.parentFile ?: return@withLock DriveOutcome.FAILED
                try {
                    dagger.hilt.EntryPoints.get(
                        context.applicationContext,
                        com.example.powerofhabit.di.DatabaseEntryPoint::class.java
                    ).appDatabase().close()
                    Log.d(TAG, "Database connection closed cleanly before restore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to close database before restore", e)
                }
                DB_ENTRY_NAMES.forEach { File(dbDir, it).delete() }
                extracted.forEach { staged ->
                    val dest = File(dbDir, staged.name)
                    if (!staged.renameTo(dest)) staged.copyTo(dest, overwrite = true)
                }
                Log.d(TAG, "Database restored successfully. Triggering app restart.")

                // 3. Room이 새 파일을 깨끗하게 열도록 프로세스를 재시작한다.
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent == null) {
                    Log.e(TAG, "Launch intent was null during restore restart")
                    return@withLock DriveOutcome.FAILED
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Runtime.getRuntime().exit(0)
                DriveOutcome.SUCCESS
            } catch (e: Exception) {
                outcomeForError(e, "restore")
            } finally {
                zipFile.delete()
                stagingDir.deleteRecursively()
            }
        }
    }

    /** zip을 [dir]에 푼다. DB 파일 이름 3종만 받고(zip slip·잡파일 방어) 푼 파일 목록을 돌려준다. */
    private fun extractZip(zipFile: File, dir: File): List<File> {
        val out = ArrayList<File>()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name in DB_ENTRY_NAMES) {
                    val destFile = File(dir, name)
                    if (!destFile.canonicalPath.startsWith(dir.canonicalPath + File.separator)) {
                        throw SecurityException("Illegal zip entry path: $name")
                    }
                    FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                    out.add(destFile)
                } else {
                    Log.w(TAG, "Skipping unexpected zip entry: $name")
                }
                entry = zis.nextEntry
            }
        }
        return out
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, zipEntryName: String) {
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(zipEntryName))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }
}
