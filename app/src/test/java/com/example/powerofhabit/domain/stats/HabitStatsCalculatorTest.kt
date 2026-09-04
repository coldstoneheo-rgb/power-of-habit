package com.example.powerofhabit.domain.stats

import com.example.powerofhabit.data.local.HabitRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsCalculatorTest {

    // 2026-09-05 is a Saturday; the app week runs Sunday..Saturday.
    private val today: LocalDate = LocalDate.of(2026, 9, 5)

    private fun rec(date: String, status: String = "COMPLETED", value: Float? = null) =
        HabitRecordEntity(habitId = 1, date = date, status = status, inputValue = value)

    private fun done(vararg dates: String) = dates.map { rec(it) }

    // ---------- empty ----------

    @Test
    fun emptyRecords_returnZeros() {
        val s = HabitStatsCalculator.compute(emptyList(), HabitFrequency.Daily, today)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.maxStreak)
        assertEquals(0f, s.latestScore, 0f)
        assertEquals(0f, s.monthProgress, 0f)
        assertTrue(s.dailyScores.isEmpty())
        assertEquals("오늘", s.currentPeriod.label)
    }

    // ---------- Daily ----------

    @Test
    fun daily_missingPastDayBreaksStreak_todayMissingDoesNot() {
        // 9/1, 9/2 완료, 9/3 기록 없음(실패), 9/4 완료, 9/5(오늘) 기록 없음(보류)
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-02", "2026-09-04"), HabitFrequency.Daily, today)
        assertEquals(2, s.maxStreak)
        assertEquals(1, s.currentStreak)
        assertEquals("일", s.streakUnit)
    }

    @Test
    fun daily_skippedDayIsNeutral() {
        val records = done("2026-09-01", "2026-09-02") + rec("2026-09-03", "SKIPPED") + done("2026-09-04", "2026-09-05")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(4, s.currentStreak)
        assertEquals(4, s.maxStreak)
    }

    @Test
    fun daily_failedResetsStreak() {
        val records = done("2026-09-01", "2026-09-02", "2026-09-03") + rec("2026-09-04", "FAILED") + done("2026-09-05")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(3, s.maxStreak)
        assertEquals(1, s.currentStreak)
    }

    @Test
    fun daily_emaSeries_startsAtFirstTargetThenSmooths() {
        val s = HabitStatsCalculator.compute(done("2026-09-03", "2026-09-04"), HabitFrequency.Daily, today)
        // 9/3 → 100 (초기화), 9/4 → 100, 9/5 오늘 미기록 → 보류(유지)
        assertEquals(3, s.dailyScores.size)
        assertEquals(100f, s.dailyScores[0].second, 0.001f)
        assertEquals(100f, s.dailyScores[2].second, 0.001f)

        val withFail = done("2026-09-03") + rec("2026-09-04", "FAILED")
        val s2 = HabitStatsCalculator.compute(withFail, HabitFrequency.Daily, today)
        assertEquals(100f * (1 - HabitStatsCalculator.EMA_ALPHA), s2.dailyScores[1].second, 0.001f)
    }

    @Test
    fun daily_monthProgress_isCompletedOverElapsedDays() {
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03", "2026-09-04"), HabitFrequency.Daily, today)
        assertEquals(3f / 5f, s.monthProgress, 0.001f)
    }

    // ---------- TimesPerWeek ----------

    @Test
    fun weekly3_streakCountsWeeksNotDays() {
        // 주1: 8/16(일)~8/22(토) 3회 완료 → MET
        // 주2: 8/23~8/29 2회 → FAILED
        // 주3: 8/30~9/5(오늘 포함) 3회 → MET
        val records = done(
            "2026-08-17", "2026-08-19", "2026-08-21",
            "2026-08-24", "2026-08-26",
            "2026-08-31", "2026-09-02", "2026-09-04"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals("주", s.streakUnit)
        assertEquals(1, s.maxStreak)
        assertEquals(1, s.currentStreak)
        assertEquals(3, s.periods.size)
        assertEquals(PeriodStatus.MET, s.periods[0].status)
        assertEquals(PeriodStatus.FAILED, s.periods[1].status)
        assertEquals(PeriodStatus.MET, s.periods[2].status)
    }

    @Test
    fun weekly3_currentWeekInProgressDoesNotBreakStreak() {
        // 주1 MET, 주2 MET, 이번 주 1회(보류)
        val records = done(
            "2026-08-17", "2026-08-19", "2026-08-21",
            "2026-08-24", "2026-08-26", "2026-08-28",
            "2026-09-01"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.maxStreak)
        assertEquals(PeriodStatus.PENDING, s.periods.last().status)
        assertEquals("이번 주", s.currentPeriod.label)
        assertEquals(1, s.currentPeriod.completed)
        assertEquals(3, s.currentPeriod.required)
        assertEquals(1f / 3f, s.currentPeriod.fraction, 0.001f)
    }

    @Test
    fun weekly3_failedWeekGetsPartialCreditInScore() {
        // 주1 3/3 → 100, 주2 2/3 → 목표 66.7로 EMA 갱신
        val records = done(
            "2026-08-17", "2026-08-19", "2026-08-21",
            "2026-08-24", "2026-08-26"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        val expected = 100f * (1 - HabitStatsCalculator.EMA_ALPHA) + (2f / 3f * 100f) * HabitStatsCalculator.EMA_ALPHA
        // 8/29(주2 종료일) 이후 값은 expected로 유지된다 (이번 주는 보류)
        assertEquals(expected, s.latestScore, 0.01f)
        // 주1 종료일(8/22) 전까지는 초기값 0, 8/22에 100
        assertEquals(0f, s.dailyScores.first { it.first == LocalDate.of(2026, 8, 21) }.second, 0f)
        assertEquals(100f, s.dailyScores.first { it.first == LocalDate.of(2026, 8, 22) }.second, 0.001f)
    }

    @Test
    fun weekly3_monthProgress_usesPace() {
        // 9월 1~5일(5일 경과) 기대 = 3 * 5/7 ≈ 2.14, 완료 2회 → 0.933
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03"), HabitFrequency.TimesPerWeek(3), today)
        assertEquals(2f / (3f * 5f / 7f), s.monthProgress, 0.001f)
    }

    // ---------- TimesPerMonth ----------

    @Test
    fun monthly4_streakInMonths_andProgressAgainstTarget() {
        // 7월 4회 MET, 8월 3회 FAILED, 9월 2회 PENDING
        val records = done(
            "2026-07-02", "2026-07-09", "2026-07-16", "2026-07-23",
            "2026-08-03", "2026-08-10", "2026-08-17",
            "2026-09-01", "2026-09-03"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerMonth(4), today)
        assertEquals("개월", s.streakUnit)
        assertEquals(1, s.maxStreak)
        assertEquals(0, s.currentStreak)
        assertEquals(0.5f, s.monthProgress, 0.001f)
        assertEquals("이번 달", s.currentPeriod.label)
        assertEquals(2, s.currentPeriod.completed)
        assertEquals(4, s.currentPeriod.required)
    }

    // ---------- EveryNDays ----------

    @Test
    fun every3Days_oneCompletionPerWindowKeepsStreak() {
        // 창: 8/28~8/30, 8/31~9/2, 9/3~9/5(오늘 포함)
        val records = done("2026-08-28", "2026-09-01", "2026-09-05")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.EveryNDays(3), today)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.maxStreak)
        assertEquals("회", s.streakUnit)
    }

    @Test
    fun every3Days_emptyWindowBreaksStreak() {
        // 창: 8/25~8/27 MET, 8/28~8/30 FAILED, 8/31~9/2 MET, 9/3~9/5 PENDING
        val records = done("2026-08-25", "2026-09-02")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.EveryNDays(3), today)
        assertEquals(1, s.maxStreak)
        assertEquals(1, s.currentStreak)
        assertEquals(PeriodStatus.FAILED, s.periods[1].status)
    }

    // ---------- TimesInDays ----------

    @Test
    fun threeInSevenDays_windowsFromFirstRecord() {
        // 창1: 8/23~8/29 3회 MET, 창2: 8/30~9/5 1회 PENDING
        val records = done("2026-08-23", "2026-08-25", "2026-08-27", "2026-09-01")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesInDays(3, 7), today)
        assertEquals(1, s.currentStreak)
        assertEquals(PeriodStatus.PENDING, s.periods.last().status)
        assertEquals("이번 7일", s.currentPeriod.label)
    }

    // ---------- robustness ----------

    @Test
    fun malformedDates_areIgnored() {
        val records = done("2026-09-04") + rec("not-a-date")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(1, s.currentStreak)
    }

    // ---------- groupScores ----------

    @Test
    fun groupScores_weeklyTakesLastValuePerWeek_maxEight() {
        val daily = (0 until 70).map { i -> today.minusDays(69L - i) to i.toFloat() }
        val (scores, labels) = HabitStatsCalculator.groupScores(daily, "주")
        assertEquals(8, scores.size)
        assertEquals(8, labels.size)
        assertEquals(69f, scores.last(), 0f)
    }

    @Test
    fun groupScores_dailyTakesLastTwelve() {
        val daily = (0 until 20).map { i -> today.minusDays(19L - i) to i.toFloat() }
        val (scores, labels) = HabitStatsCalculator.groupScores(daily, "일")
        assertEquals(12, scores.size)
        assertEquals("5일", labels.last())
    }

    @Test
    fun groupScores_empty() {
        val (scores, labels) = HabitStatsCalculator.groupScores(emptyList(), "월")
        assertEquals(listOf(0f), scores)
        assertEquals(listOf("오늘"), labels)
    }
}
