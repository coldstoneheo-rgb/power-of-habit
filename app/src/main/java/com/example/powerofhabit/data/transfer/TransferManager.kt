package com.example.powerofhabit.data.transfer

import android.content.Context
import android.net.Uri
import com.example.powerofhabit.data.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * 로컬 JSON 파일로 내보내기/가져오기 (SAF Uri). 병합 규칙과 형식은 [HabitTransfer] 참조.
 *
 * 가져오기는 트랜잭션 없이 습관 → 기록 → 뱃지 순서로 삽입한다. 중간에 실패하면 이미 들어간 습관/기록은 남지만
 * 병합 규칙이 멱등이라(같은 습관은 매칭, 같은 (habit,date)는 스킵) 같은 파일을 다시 가져오면 나머지만 채워진다.
 * 뱃지 재판정·Drive 자동 백업([com.example.powerofhabit.data.RecordSideEffects])은 호출하지 않는다 —
 * 위젯은 DB 옵저버가 갱신하고, 뱃지는 다음 체크 때 재판정된다.
 */
class TransferManager(private val repository: DataRepository) {

    /** 성공 시 내보낸 습관 수. */
    suspend fun exportTo(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val habits = repository.getAllHabits().first()
            val records = repository.getAllRecords().first()
            val badges = repository.getAllBadges().first()
            val export = HabitTransfer.buildExport(habits, records, badges, appVersionName(context))
            val text = HabitTransfer.encode(export)
            val resolver = context.contentResolver
            // "wt"로 열어 기존 파일을 잘라낸다 — 일부 문서 제공자는 "w"만 주면 이전 내용 뒤가 남는다.
            val stream = try {
                resolver.openOutputStream(uri, "wt")
            } catch (e: FileNotFoundException) {
                resolver.openOutputStream(uri, "w")
            } ?: throw IllegalStateException("파일을 열 수 없습니다")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            habits.size
        }
    }

    suspend fun importFrom(context: Context, uri: Uri): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("파일을 열 수 없습니다")
            val export = HabitTransfer.decode(text)

            val plan = HabitTransfer.plan(
                existingHabits = repository.getAllHabits().first(),
                existingRecords = repository.getAllRecords().first(),
                existingBadges = repository.getAllBadges().first(),
                import = export
            )

            val insertedIds = HashMap<Int, Int>()
            for (pending in plan.habitsToInsert) {
                insertedIds[pending.sourceHabitId] = repository.insertHabit(pending.habit).toInt()
            }
            val records = plan.resolveRecords(insertedIds)
            if (records.isNotEmpty()) repository.insertRecords(records)
            for (badge in plan.badgesToInsert) repository.insertBadge(badge)

            plan.summary
        }
    }

    private fun appVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()
}
