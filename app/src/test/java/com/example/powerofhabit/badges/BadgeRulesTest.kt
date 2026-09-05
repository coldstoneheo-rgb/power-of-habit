package com.example.powerofhabit.badges

import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 뱃지 스트릭은 통계 엔진의 기간 판정과 같은 숫자여야 한다(결정 기록 2026-09-06 뱃지 스트릭). */
class BadgeRulesTest {

    private val zone = ZoneId.systemDefault()

    private fun habit(frequencyType: String = "DAILY", frequencyValue: String = "", createdOn: String = "2026-08-01") = HabitEntity(
        habitId = 1, title = "t", question = "q", frequencyType = frequencyType, frequencyValue = frequencyValue,
        reminderTime = null, themeColor = "#FF9800", habitType = "CHECK", unit = null,
        createdAt = LocalDate.parse(createdOn).atStartOfDay(zone).toInstant().toEpochMilli()
    )

    private fun rec(date: String, status: String = "COMPLETED") =
        HabitRecordEntity(recordId = 0, habitId = 1, date = date, status = status, inputValue = null)

    @Test
    fun daily_streakIsConsecutiveDays_asBefore() {
        val records = listOf(rec("2026-08-01"), rec("2026-08-02"), rec("2026-08-03"), rec("2026-08-05"))
        assertEquals(3, BadgeRules.maxStreak(habit(), records, today = LocalDate.parse("2026-08-05")))
    }

    @Test
    fun daily_skippedDayDoesNotBreakStreak_unlikeOldDateChain() {
        // 옛 규칙(완료 날짜가 하루씩 이어져야 함)에서는 8/3 건너뜀이 스트릭을 2로 끊었다. 통계 엔진은 NEUTRAL로 무시한다.
        val records = listOf(rec("2026-08-01"), rec("2026-08-02"), rec("2026-08-03", "SKIPPED"), rec("2026-08-04"))
        assertEquals(3, BadgeRules.maxStreak(habit(), records, today = LocalDate.parse("2026-08-04")))
    }

    @Test
    fun weekly3x_streakCountsConsecutiveMetWeeks_notDays() {
        // 일요일 시작 주. 8/2(일)~8/8, 8/9~8/15, 8/16~8/22 세 주 각각 3회 완료 → 스트릭 3 (옛 규칙은 날짜가 안 이어져 1)
        val records = listOf(
            rec("2026-08-03"), rec("2026-08-05"), rec("2026-08-07"),
            rec("2026-08-10"), rec("2026-08-12"), rec("2026-08-14"),
            rec("2026-08-17"), rec("2026-08-19"), rec("2026-08-21")
        )
        val h = habit("WEEKLY_COUNT", "3", createdOn = "2026-08-02")
        assertEquals(3, BadgeRules.maxStreak(h, records, today = LocalDate.parse("2026-08-22")))
    }

    @Test
    fun weekly3x_weekWithTwoCompletions_breaksStreak() {
        val records = listOf(
            rec("2026-08-03"), rec("2026-08-05"), rec("2026-08-07"),
            rec("2026-08-10"), rec("2026-08-12"),                    // 2회 — 실패
            rec("2026-08-17"), rec("2026-08-19"), rec("2026-08-21")
        )
        val h = habit("WEEKLY_COUNT", "3", createdOn = "2026-08-02")
        assertEquals(1, BadgeRules.maxStreak(h, records, today = LocalDate.parse("2026-08-22")))
    }

    @Test
    fun emptyRecords_zeroStreak() {
        assertEquals(0, BadgeRules.maxStreak(habit(), emptyList()))
    }

    @Test
    fun due_awardsOnlyUnearnedAboveThreshold_inOrder() {
        val due = BadgeRules.due(totalCompleted = 12, maxStreak = 5, earned = setOf("START_FIRST", "STREAK_3"))
        assertEquals(listOf("HABIT_COMPLETE_10", "STREAK_5"), due.map { it.id })
    }

    @Test
    fun due_nothingWhenBelowThresholds_orAllEarned() {
        assertEquals(emptyList<BadgeSpec>(), BadgeRules.due(0, 0, emptySet()))
        val all = (listOf(BadgeRules.START_FIRST) + BadgeRules.CUMULATIVE.map { it.second } + BadgeRules.STREAK.map { it.second }).map { it.id }.toSet()
        assertEquals(emptyList<BadgeSpec>(), BadgeRules.due(1000, 100, all))
    }

    @Test
    fun due_firstSuccessGivesStartBadge() {
        assertEquals(listOf("START_FIRST"), BadgeRules.due(1, 1, emptySet()).map { it.id })
    }
}
