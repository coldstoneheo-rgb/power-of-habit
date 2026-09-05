package com.example.powerofhabit.badges

import android.content.Context
import android.util.Log
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 기록이 바뀐 습관 하나에 대해 뱃지를 판정·수여한다. 규칙은 [BadgeRules](순수, 테스트), 여기는 DB 읽기/쓰기만.
 * 이미 받은 뱃지는 회수하지 않는다(규칙이 바뀌어도 과거 수여는 유지).
 */
class BadgeManager(
    private val repository: DataRepository,
    @Suppress("unused") private val context: Context
) {

    suspend fun checkAndAwardBadges(habit: HabitEntity, records: List<HabitRecordEntity>) = withContext(Dispatchers.IO) {
        try {
            val totalCompleted = records.count { it.status == "COMPLETED" }
            val maxStreak = BadgeRules.maxStreak(habit, records)
            val earned = repository.getAllBadges().first().mapTo(HashSet()) { it.badgeId }
            for (spec in BadgeRules.due(totalCompleted, maxStreak, earned)) {
                repository.insertBadge(
                    BadgeEntity(
                        badgeId = spec.id,
                        badgeName = spec.name,
                        description = spec.description,
                        earnedAt = System.currentTimeMillis(),
                        badgeIconType = spec.iconType
                    )
                )
                Log.d("BadgeManager", "New achievement unlocked: ${spec.name} (${spec.iconType}) habit=${habit.habitId} streak=$maxStreak")
            }
        } catch (e: Exception) {
            Log.e("BadgeManager", "Failed to check and award badges", e)
        }
    }
}
