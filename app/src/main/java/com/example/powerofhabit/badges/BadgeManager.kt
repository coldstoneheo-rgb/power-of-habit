package com.example.powerofhabit.badges

import android.content.Context
import android.util.Log
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class BadgeManager(
    private val repository: DataRepository,
    private val context: Context
) {

    suspend fun checkAndAwardBadges(records: List<HabitRecordEntity>) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val completedRecords = records.filter { it.status == "COMPLETED" }
            val totalCompleted = completedRecords.size
            val earnedBadges = repository.getAllBadges().first().map { it.badgeId }.toSet()

            // 1. "START_FIRST" Badge (First success)
            if (totalCompleted >= 1 && !earnedBadges.contains("START_FIRST")) {
                awardBadge(
                    id = "START_FIRST",
                    name = "습관 여행의 시작",
                    desc = "첫 번째 습관 실천을 완료했습니다!",
                    type = "BRONZE"
                )
            }

            // 2. Cumulative Completion Badges
            if (totalCompleted >= 10 && !earnedBadges.contains("HABIT_COMPLETE_10")) {
                awardBadge(
                    id = "HABIT_COMPLETE_10",
                    name = "첫 10회의 발걸음",
                    desc = "습관 완수 횟수 10회를 달성했습니다!",
                    type = "BRONZE"
                )
            }
            if (totalCompleted >= 50 && !earnedBadges.contains("HABIT_COMPLETE_50")) {
                awardBadge(
                    id = "HABIT_COMPLETE_50",
                    name = "반백의 열정",
                    desc = "습관 완수 횟수 50회를 달성했습니다!",
                    type = "SILVER"
                )
            }
            if (totalCompleted >= 100 && !earnedBadges.contains("HABIT_COMPLETE_100")) {
                awardBadge(
                    id = "HABIT_COMPLETE_100",
                    name = "백일의 기적",
                    desc = "습관 완수 횟수 100회를 돌파했습니다!",
                    type = "GOLD"
                )
            }

            // 3. Streak-based Badges
            if (records.isNotEmpty()) {
                val sortedDates = records.filter { it.status == "COMPLETED" }
                    .mapNotNull {
                        try { LocalDate.parse(it.date) } catch (e: Exception) { null }
                    }
                    .sorted()

                var currentStreak = 0
                var maxStreak = 0
                var prevDate: LocalDate? = null

                for (date in sortedDates) {
                    if (prevDate == null) {
                        currentStreak = 1
                    } else {
                        if (date == prevDate.plusDays(1)) {
                            currentStreak++
                        } else if (date != prevDate) {
                            currentStreak = 1
                        }
                    }
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                    prevDate = date
                }

                if (maxStreak >= 3 && !earnedBadges.contains("STREAK_3")) {
                    awardBadge(
                        id = "STREAK_3",
                        name = "작심삼일 탈출",
                        desc = "습관 3일 연속 달성에 성공했습니다!",
                        type = "BRONZE"
                    )
                }
                if (maxStreak >= 5 && !earnedBadges.contains("STREAK_5")) {
                    awardBadge(
                        id = "STREAK_5",
                        name = "꾸준한 실행가",
                        desc = "습관 5일 연속 달성에 성공했습니다!",
                        type = "SILVER"
                    )
                }
                if (maxStreak >= 7 && !earnedBadges.contains("STREAK_7")) {
                    awardBadge(
                        id = "STREAK_7",
                        name = "빛나는 일주일",
                        desc = "일주일 7일 연속 완벽 달성에 성공했습니다!",
                        type = "SILVER"
                    )
                }
                if (maxStreak >= 21 && !earnedBadges.contains("STREAK_21")) {
                    awardBadge(
                        id = "STREAK_21",
                        name = "21일의 습관화",
                        desc = "습관 형성 21일의 벽을 돌파했습니다!",
                        type = "SILVER"
                    )
                }
                if (maxStreak >= 30 && !earnedBadges.contains("STREAK_30")) {
                    awardBadge(
                        id = "STREAK_30",
                        name = "습관 마스터",
                        desc = "지속 가능한 성장! 습관 30일 연속 달성 완료!",
                        type = "GOLD"
                    )
                }
                if (maxStreak >= 66 && !earnedBadges.contains("STREAK_66")) {
                    awardBadge(
                        id = "STREAK_66",
                        name = "체화된 습관",
                        desc = "평균 습관 형성 주기 66일을 완전히 정복했습니다!",
                        type = "GOLD"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("BadgeManager", "Failed to check and award badges", e)
        }
    }

    private suspend fun awardBadge(id: String, name: String, desc: String, type: String) {
        val earnedBadge = BadgeEntity(
            badgeId = id,
            badgeName = name,
            description = desc,
            earnedAt = System.currentTimeMillis(),
            badgeIconType = type
        )
        repository.insertBadge(earnedBadge)
        Log.d("BadgeManager", "New achievement unlocked: $name ($type)")
    }
}
