package com.example.powerofhabit.data.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.reminder.HabitReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * 로컬 JSON 파일로 내보내기/가져오기 (SAF Uri). 병합 규칙과 형식은 [HabitTransfer] 참조.
 *
 * 가져오기는 **하나의 DB 트랜잭션** 안에서 습관 → 기록 → 뱃지 순서로 삽입한다. 계획(plan)은 스냅샷이므로
 * 삽입 직전에 (habitId, date) 존재 여부와 대상 습관의 존재를 다시 확인해, 가져오는 동안 사용자가 위젯을 누르거나
 * 습관을 지워도 중복 행·FK 실패가 생기지 않게 한다. 요약 수치는 실제로 들어간 행 기준이다.
 * 가져온 습관 중 알림이 켜진 것은 다른 생성 경로(등록 화면)와 같이 알람을 예약한다.
 * 뱃지 재판정·Drive 자동 백업은 호출하지 않는다 — 위젯은 DB 옵저버가 갱신하고, 뱃지는 다음 체크 때 재판정된다.
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

    /** JSON 파일(§4-1)을 병합한다. */
    suspend fun importFrom(context: Context, uri: Uri): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("파일을 열 수 없습니다")
            importExport(context, HabitTransfer.decode(text))
        }
    }

    /** 옛 앱의 SQLite 파일(.db + 선택 -wal/-shm, §4-2)을 읽어 JSON 가져오기와 같은 규칙으로 병합한다. */
    suspend fun importLegacyDb(context: Context, uris: List<Uri>): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val read = LegacyDbImporter(context).read(uris)
            importExport(context, read.export).let { it.copy(warnings = it.warnings + read.warnings) }
        }
    }

    /** 병합 본체. JSON·옛 DB 두 입구가 같은 계획·트랜잭션·알림 예약을 탄다. */
    private suspend fun importExport(context: Context, export: HabitExport): ImportSummary {
            val plan = HabitTransfer.plan(
                existingHabits = repository.getAllHabits().first(),
                existingRecords = repository.getAllRecords().first(),
                existingBadges = repository.getAllBadges().first(),
                import = export
            )

            val insertedHabits = ArrayList<HabitEntity>()
            var recordsAdded = 0
            var recordsSkipped = plan.summary.recordsSkipped
            var badgesAdded = 0

            repository.inTransaction {
                val insertedIds = HashMap<Int, Int>()
                for (pending in plan.habitsToInsert) {
                    val newId = repository.insertHabit(pending.habit).toInt()
                    insertedIds[pending.sourceHabitId] = newId
                    insertedHabits += pending.habit.copy(habitId = newId)
                }
                // 매칭된 습관이 계획 이후 삭제됐으면 그 기록은 버린다(FK 실패로 전체가 롤백되는 것을 막는다).
                val liveMatched = plan.matchedHabitIds.filterValues { id -> repository.getHabitById(id).first() != null }
                val idMap = liveMatched + insertedIds
                for (pending in plan.recordsToInsert) {
                    val habitId = idMap[pending.sourceHabitId]
                    if (habitId == null) { recordsSkipped++; continue }
                    if (repository.getRecord(habitId, pending.record.date) != null) { recordsSkipped++; continue }
                    repository.insertRecord(pending.record.copy(habitId = habitId))
                    recordsAdded++
                }
                val existingBadgeIds = repository.getAllBadges().first().mapTo(HashSet()) { it.badgeId }
                for (badge in plan.badgesToInsert) {
                    if (badge.badgeId in existingBadgeIds) continue
                    repository.insertBadge(badge)
                    badgesAdded++
                }
            }

            // 알림이 켜진 채 들어온 습관은 등록 화면과 같은 방식으로 알람을 예약한다.
            val reminderManager = HabitReminderManager(context)
            for (habit in insertedHabits) {
                if (habit.isReminderEnabled) {
                    try { reminderManager.scheduleReminder(habit) } catch (e: Exception) {
                        Log.w("TransferManager", "reminder schedule failed for ${habit.habitId}", e)
                    }
                }
            }

            return plan.summary.copy(recordsAdded = recordsAdded, recordsSkipped = recordsSkipped, badgesAdded = badgesAdded)
    }

    private fun appVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()
}
