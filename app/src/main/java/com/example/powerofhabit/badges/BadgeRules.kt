package com.example.powerofhabit.badges

import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.stats.HabitFrequency
import com.example.powerofhabit.domain.stats.HabitStatsCalculator
import java.time.LocalDate

/** 수여할 뱃지 한 장의 정의. */
data class BadgeSpec(val id: String, val name: String, val description: String, val iconType: String)

/**
 * 뱃지 판정 규칙(순수 Kotlin). [BadgeManager]는 DB 읽기/쓰기만 하고 "무엇을 줄지"는 여기서 정한다.
 *
 * 스트릭(결정 기록 2026-09-06 뱃지 스트릭): 통계 엔진과 **같은** 기간 판정([HabitStatsCalculator])의 연속 MET 개수다.
 * 예전에는 완료 날짜가 하루씩 이어져야 했기 때문에 "주 3회" 같은 습관은 정의상 스트릭 뱃지를 받을 수 없었고,
 * 건너뜀(SKIPPED) 하루가 스트릭을 끊었다. 이제 매일 습관은 예전과 같이 "일"이 단위이고, 주기 습관은 기간(주·개월·창)이 단위다.
 * 임계값 3·5·7·21·30·66은 그대로 두되 설명 문구는 단위 중립("연속 N회 달성")으로 쓴다.
 */
object BadgeRules {

    val START_FIRST = BadgeSpec("START_FIRST", "습관 여행의 시작", "첫 번째 습관 실천을 완료했습니다!", "BRONZE")

    /** 누적 완료 횟수(해당 습관) 임계값. */
    val CUMULATIVE: List<Pair<Int, BadgeSpec>> = listOf(
        10 to BadgeSpec("HABIT_COMPLETE_10", "첫 10회의 발걸음", "습관 완수 횟수 10회를 달성했습니다!", "BRONZE"),
        50 to BadgeSpec("HABIT_COMPLETE_50", "반백의 열정", "습관 완수 횟수 50회를 달성했습니다!", "SILVER"),
        100 to BadgeSpec("HABIT_COMPLETE_100", "백일의 기적", "습관 완수 횟수 100회를 돌파했습니다!", "GOLD")
    )

    /** 연속 달성(기간 단위) 임계값. */
    val STREAK: List<Pair<Int, BadgeSpec>> = listOf(
        3 to BadgeSpec("STREAK_3", "작심삼일 탈출", "연속 3회 달성에 성공했습니다! (매일 습관은 3일, 주간 습관은 3주)", "BRONZE"),
        5 to BadgeSpec("STREAK_5", "꾸준한 실행가", "연속 5회 달성에 성공했습니다!", "SILVER"),
        7 to BadgeSpec("STREAK_7", "빛나는 일주일", "연속 7회 완벽 달성에 성공했습니다!", "SILVER"),
        21 to BadgeSpec("STREAK_21", "21일의 습관화", "연속 21회 달성 — 습관 형성의 벽을 돌파했습니다!", "SILVER"),
        30 to BadgeSpec("STREAK_30", "습관 마스터", "지속 가능한 성장! 연속 30회 달성 완료!", "GOLD"),
        66 to BadgeSpec("STREAK_66", "체화된 습관", "평균 습관 형성 주기 66회 연속 달성을 완전히 정복했습니다!", "GOLD")
    )

    /** 이 습관의 최대 스트릭(기간 단위). 통계 화면의 "최고 스트릭"과 같은 숫자다. */
    fun maxStreak(habit: HabitEntity, records: List<HabitRecordEntity>, today: LocalDate = LocalDate.now()): Int {
        if (records.isEmpty()) return 0
        val frequency = HabitFrequency.parse(habit.frequencyType, habit.frequencyValue)
        return HabitStatsCalculator.compute(
            records = records,
            frequency = frequency,
            today = today,
            anchorDate = HabitStatsCalculator.anchorFromEpochMillis(habit.createdAt)
        ).maxStreak
    }

    /** 아직 받지 않았고 조건을 넘은 뱃지. 순서는 시작 → 누적 → 스트릭. */
    fun due(totalCompleted: Int, maxStreak: Int, earned: Set<String>): List<BadgeSpec> {
        val out = ArrayList<BadgeSpec>()
        if (totalCompleted >= 1 && START_FIRST.id !in earned) out += START_FIRST
        CUMULATIVE.forEach { (threshold, spec) -> if (totalCompleted >= threshold && spec.id !in earned) out += spec }
        STREAK.forEach { (threshold, spec) -> if (maxStreak >= threshold && spec.id !in earned) out += spec }
        return out
    }
}
