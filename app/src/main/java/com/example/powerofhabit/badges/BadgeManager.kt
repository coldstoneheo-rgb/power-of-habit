package com.example.powerofhabit.badges

import android.util.Log
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.BadgeEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * 기록이 바뀐 습관 하나에 대해 뱃지를 판정·수여한다. 규칙은 [BadgeRules](순수, 테스트), 여기는 DB 읽기/쓰기만.
 * 이미 받은 뱃지는 회수하지 않는다(규칙이 바뀌어도 과거 수여는 유지). DAO의 insertBadge는 IGNORE라 같은 뱃지가 겹쳐 들어와도
 * 첫 수여 시각이 남는다. 읽기 실패는 여기서 삼킨다 — 기록 저장은 이미 끝났으므로 호출자가 "저장 실패"로 오해하면 안 된다.
 */
class BadgeManager(private val repository: DataRepository) {

    // Room suspend DAO는 스스로 IO로 넘어가므로 여기서 디스패처를 바꾸지 않는다(테스트 디스패처 안에서도 결정적으로 끝난다).
    suspend fun checkAndAwardBadges(habitId: Int) {
        try {
            val habit = repository.getHabitById(habitId).first() ?: return // 지워진 습관
            val records = repository.getRecordsForHabit(habitId).first()
            val earned = repository.getAllBadges().first().mapTo(HashSet()) { it.badgeId }
            for (spec in BadgeRules.due(habit, records, earned)) {
                repository.insertBadge(
                    BadgeEntity(
                        badgeId = spec.id,
                        badgeName = spec.name,
                        description = spec.description,
                        earnedAt = System.currentTimeMillis(),
                        badgeIconType = spec.iconType
                    )
                )
                Log.d("BadgeManager", "New achievement unlocked: ${spec.name} (${spec.iconType}) habit=$habitId")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("BadgeManager", "Failed to check and award badges", e)
        }
    }
}
