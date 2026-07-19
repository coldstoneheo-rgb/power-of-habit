package com.example.powerofhabit.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import android.content.Intent

class GoogleDriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveBackup"
        private const val BACKUP_FILE_NAME = "power_of_habit_backup.zip"

        // Global debounced backup scheduler
        private val backupScope = CoroutineScope(
            Dispatchers.IO + SupervisorJob()
        )
        private val backupTrigger = MutableSharedFlow<GoogleDriveBackupManager>(
            replay = 0,
            extraBufferCapacity = 1
        )

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

    fun isGoogleSignedIn(): Boolean {
        return try {
            GoogleSignIn.getLastSignedInAccount(context) != null
        } catch (e: Exception) {
            false
        }
    }

    fun scheduleAutoBackup() {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.d(TAG, "Not signed in, skipping auto backup schedule")
                return
            }
            backupTrigger.tryEmit(this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule auto backup: ${e.message}")
        }
    }

    suspend fun backupDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("power_of_habit.db")
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file does not exist")
                return@withContext false
            }

            val dbShm = File(dbFile.path + "-shm")
            val dbWal = File(dbFile.path + "-wal")
            val zipFile = File(context.cacheDir, BACKUP_FILE_NAME)

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                addFileToZip(zos, dbFile, "power_of_habit.db")
                if (dbShm.exists()) addFileToZip(zos, dbShm, "power_of_habit.db-shm")
                if (dbWal.exists()) addFileToZip(zos, dbWal, "power_of_habit.db-wal")
            }

            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.w(TAG, "Google accounts not signed in.")
                return@withContext false
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
            ).setSelectedAccount(account.account)

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Power Of Habit").build()

            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .execute()

            val files = result.files
            val metadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = Collections.singletonList("appDataFolder")
            }

            val mediaContent = com.google.api.client.http.FileContent("application/zip", zipFile)

            if (files.isNullOrEmpty()) {
                driveService.files().create(metadata, mediaContent).execute()
                Log.d(TAG, "Backup created successfully on Google Drive")
            } else {
                val existingFileId = files[0].id
                driveService.files().update(existingFileId, null, mediaContent).execute()
                Log.d(TAG, "Backup updated successfully on Google Drive")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive backup failed", e)
            false
        }
    }

    suspend fun restoreDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(context.cacheDir, BACKUP_FILE_NAME)
            val account = GoogleSignIn.getLastSignedInAccount(context)

            if (account != null) {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
                ).setSelectedAccount(account.account)

                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("Power Of Habit").build()

                val result = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$BACKUP_FILE_NAME'")
                    .execute()

                val files = result.files
                if (files.isNullOrEmpty()) {
                    Log.e(TAG, "No backup file found on Google Drive")
                    return@withContext false
                }

                val fileId = files[0].id
                FileOutputStream(zipFile).use { fos ->
                    driveService.files().get(fileId).executeMediaAndDownloadTo(fos)
                }
                Log.d(TAG, "Backup zip downloaded from Google Drive")
            } else {
                if (!zipFile.exists()) {
                    Log.e(TAG, "No local backup zip found")
                    return@withContext false
                }
            }

            val dbFile = context.getDatabasePath("power_of_habit.db")
            val dbDir = dbFile.parentFile ?: return@withContext false

            // 1. Close database cleanly to prevent corruption
            try {
                val dbEntryPoint = dagger.hilt.EntryPoints.get(
                    context.applicationContext,
                    com.example.powerofhabit.di.DatabaseEntryPoint::class.java
                )
                dbEntryPoint.appDatabase().close()
                Log.d(TAG, "Database connection closed cleanly before restore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close database before restore", e)
            }
            
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-wal").delete()
            dbFile.delete()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val destFile = File(dbDir, entry.name)
                    // Zip Slip protection
                    if (!destFile.canonicalPath.startsWith(dbDir.canonicalPath + File.separator)) {
                        throw SecurityException("Illegal zip entry path: ${entry.name}")
                    }
                    FileOutputStream(destFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    entry = zis.nextEntry
                }
            }
            Log.d(TAG, "Database restored successfully. Triggering app restart.")

            // Trigger app restart to cleanly reload Room database
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                Runtime.getRuntime().exit(0)
                true
            } else {
                Log.w(TAG, "Launch intent was null during restore restart")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Drive restore failed: ${e.localizedMessage}", e)
            false
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, zipEntryName: String) {
        val entry = ZipEntry(zipEntryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
