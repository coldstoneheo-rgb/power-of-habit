package com.example.powerofhabit.domain.stats

import com.example.powerofhabit.data.local.HabitRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsCalculatorTest {

    // 2026-09-05 is a Saturday; the app week runs Sunday..Saturday, so 8/30~9/5 is the current week.
    private val today: LocalDate = LocalDate.of(2026, 9, 5)
    private val alpha = HabitStatsCalculator.EMA_ALPHA

    private fun rec(date: String, status: String = "COMPLETED", value: Float? = null) =
        HabitRecordEntity(habitId = 1, date = date, status = status, inputValue = value)

    private fun done(vararg dates: String) = dates.map { rec(it) }

    private fun scoreOn(s: HabitStats, date: String): Float =
        s.dailyScores.first { it.first == LocalDate.parse(date) }.second

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
        assertEquals(PeriodStatus.PENDING, s.periods.last().status)
    }

    @Test
    fun daily_missingPastDayLowersScore() {
        // 점수도 스트릭과 같은 규칙: 기록 없는 과거 날은 실패(목표 0)
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-02", "2026-09-04"), HabitFrequency.Daily, today)
        assertEquals(100f, scoreOn(s, "2026-09-02"), 0.001f)
        assertEquals(100f * (1 - alpha), scoreOn(s, "2026-09-03"), 0.001f)
        val after94 = 100f * (1 - alpha) * (1 - alpha) + 100f * alpha
        assertEquals(after94, scoreOn(s, "2026-09-04"), 0.001f)
        assertEquals(after94, s.latestScore, 0.001f) // 오늘은 보류 → 유지
    }

    @Test
    fun daily_skippedDayIsNeutral() {
        val records = done("2026-09-01", "2026-09-02") + rec("2026-09-03", "SKIPPED") + done("2026-09-04", "2026-09-05")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(4, s.currentStreak)
        assertEquals(4, s.maxStreak)
        assertEquals(100f, s.latestScore, 0.001f)
    }

    @Test
    fun daily_failedResetsStreak() {
        val records = done("2026-09-01", "2026-09-02", "2026-09-03") + rec("2026-09-04", "FAILED") + done("2026-09-05")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(3, s.maxStreak)
        assertEquals(1, s.currentStreak)
    }

    @Test
    fun daily_failedTodayCountsImmediately() {
        // 오늘을 실패로 표시하면 당장 스트릭이 끊기고 점수도 내려간다 (보류 아님)
        val records = done("2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04") + rec("2026-09-05", "FAILED")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.Daily, today)
        assertEquals(PeriodStatus.FAILED, s.periods.last().status)
        assertEquals(0, s.currentStreak)
        assertEquals(4, s.maxStreak)
        assertEquals(100f * (1 - alpha), s.latestScore, 0.001f)
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
        assertEquals(100f * (1 - alpha), s2.dailyScores[1].second, 0.001f)
    }

    @Test
    fun daily_monthProgress_isCompletedOverElapsedDays() {
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03", "2026-09-04"), HabitFrequency.Daily, today)
        assertEquals(3f / 5f, s.monthProgress, 0.001f)
    }

    @Test
    fun daily_anchorDateDoesNotChangeStart() {
        // 매일 주기는 달력 정렬이라 생성일이 첫 기록보다 앞서도 첫 기록일부터 센다
        val s = HabitStatsCalculator.compute(done("2026-09-03", "2026-09-04"), HabitFrequency.Daily, today, anchorDate = LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 9, 3), s.dailyScores.first().first)
        assertEquals(2, s.currentStreak)
    }

    // ---------- TimesPerWeek ----------

    @Test
    fun weekly3_streakCountsWeeksNotDays() {
        // 주1: 8/16(일)~8/22(토) 3회 → MET, 주2: 8/23~8/29 2회 → FAILED, 주3: 8/30~9/5 3회 → MET
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
    fun weekly3_currentWeekStillReachableIsPending() {
        // 주1 MET, 주2 MET, 이번 주 2회 + 오늘 미기록(남은 열린 날 1) → 3회 도달 가능 → PENDING
        val records = done(
            "2026-08-17", "2026-08-19", "2026-08-21",
            "2026-08-24", "2026-08-26", "2026-08-28",
            "2026-09-01", "2026-09-03"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.maxStreak)
        assertEquals(PeriodStatus.PENDING, s.periods.last().status)
        assertEquals("이번 주", s.currentPeriod.label)
        assertEquals(2, s.currentPeriod.completed)
        assertEquals(3, s.currentPeriod.required)
        assertEquals(2f / 3f, s.currentPeriod.fraction, 0.001f)
    }

    @Test
    fun weekly3_currentWeekUnreachableFailsEarly() {
        // 이번 주 1회, 오늘(토) 미기록 → 최대 2회 → 도달 불가 → 오늘 FAILED 확정, 스트릭 리셋
        val records = done(
            "2026-08-24", "2026-08-26", "2026-08-28",
            "2026-09-01"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(PeriodStatus.FAILED, s.periods.last().status)
        assertEquals(0, s.currentStreak)
        assertEquals(1, s.maxStreak)
        // 점수: 8/29 100 → 오늘 목표 33.3
        assertEquals(100f * (1 - alpha) + (1f / 3f * 100f) * alpha, s.latestScore, 0.01f)
    }

    @Test
    fun weekly3_metWeekInProgressUpdatesScoreToday() {
        // 이번 주 이미 3회 → MET, 점수 갱신은 오늘 반영
        val records = done("2026-08-30", "2026-09-01", "2026-09-03")
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(PeriodStatus.MET, s.periods.last().status)
        assertEquals(1, s.currentStreak)
        assertEquals(0f, scoreOn(s, "2026-09-04"), 0f)
        assertEquals(100f, s.latestScore, 0.001f)
    }

    @Test
    fun weekly3_failedWeekGetsPartialCredit() {
        // 주1 3/3 → 100 (8/22), 주2 2/3 → 목표 66.7 (8/29), 이번 주 0회 + 오늘 하루 남음 → 도달 불가 → 오늘 0
        val records = done(
            "2026-08-17", "2026-08-19", "2026-08-21",
            "2026-08-24", "2026-08-26"
        )
        val s = HabitStatsCalculator.compute(records, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(0f, scoreOn(s, "2026-08-21"), 0f)
        assertEquals(100f, scoreOn(s, "2026-08-22"), 0.001f)
        val afterWeek2 = 100f * (1 - alpha) + (2f / 3f * 100f) * alpha
        assertEquals(afterWeek2, scoreOn(s, "2026-08-29"), 0.01f)
        assertEquals(afterWeek2, scoreOn(s, "2026-09-04"), 0.01f)
        assertEquals(afterWeek2 * (1 - alpha), s.latestScore, 0.01f)
    }

    @Test
    fun weekly3_singleSkipDoesNotProtectWeek_butFullySkippedWeekIsNeutral() {
        // 주1 MET, 주2에 SKIPPED 하루만 → FAILED(중립 아님), 주3 MET
        val one = done("2026-08-17", "2026-08-19", "2026-08-21") + rec("2026-08-24", "SKIPPED") +
            done("2026-08-31", "2026-09-02", "2026-09-04")
        val s1 = HabitStatsCalculator.compute(one, HabitFrequency.TimesPerWeek(3), today)
        assertEquals(PeriodStatus.FAILED, s1.periods[1].status)
        assertEquals(1, s1.currentStreak)

        // 주2 7일 전부 SKIPPED → NEUTRAL, 스트릭 이어짐
        val allSkipped = (23..29).map { rec("2026-08-$it", "SKIPPED") }
        val s2 = HabitStatsCalculator.compute(
            done("2026-08-17", "2026-08-19", "2026-08-21") + allSkipped + done("2026-08-31", "2026-09-02", "2026-09-04"),
            HabitFrequency.TimesPerWeek(3), today
        )
        assertEquals(PeriodStatus.NEUTRAL, s2.periods[1].status)
        assertEquals(2, s2.currentStreak)
    }

    @Test
    fun weekly3_monthProgress_usesPace() {
        // 9월 1~5일(5일 경과) 기대 = 3 * 5/7 ≈ 2.14, 완료 2회 → 0.933
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03"), HabitFrequency.TimesPerWeek(3), today)
        assertEquals(2f / (3f * 5f / 7f), s.monthProgress, 0.001f)
    }

    // ---------- TimesPerMonth ----------

    @Test
    fun monthly4_streakInMonths_andPaceProgress() {
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
        assertEquals(PeriodStatus.PENDING, s.periods.last().status)
        // 페이스: 기대 4 * 5/30 = 0.667, 완료 2 → 1.0(캡)
        assertEquals(1f, s.monthProgress, 0.001f)
        assertEquals("이번 달", s.currentPeriod.label)
        assertEquals(2, s.currentPeriod.completed)
        assertEquals(4, s.currentPeriod.required)
        assertEquals(0.5f, s.currentPeriod.fraction, 0.001f)
    }

    @Test
    fun monthly4_pendingOnly_showsProvisionalRatioScore() {
        // 새 습관: 판정된 기간이 없으면 진행 중 달성비율을 임시 점수로 (0점으로 보이지 않게)
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03"), HabitFrequency.TimesPerMonth(4), today)
        assertEquals(50f, s.latestScore, 0.001f)
        assertEquals(0f, scoreOn(s, "2026-09-04"), 0f)
    }

    @Test
    fun monthly_earlyMonthProgressNotPenalised() {
        // 월 10회, 9/5까지 2회: 기대 10*5/30 = 1.67 → 1.0 (기존 방식 2/10 = 0.2 아님)
        val s = HabitStatsCalculator.compute(done("2026-09-01", "2026-09-03"), HabitFrequency.TimesPerMonth(10), today)
        assertEquals(1f, s.monthProgress, 0.001f)
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
        assertEquals(PeriodStatus.PENDING, s.periods[3].status)
    }

    @Test
    fun every3Days_anchorKeepsWindowPhaseWhenEarlierRecordAdded() {
        val anchor = LocalDate.of(2026, 8, 28)
        val base = done("2026-08-28", "2026-09-01", "2026-09-05")
        val before = HabitStatsCalculator.compute(base, HabitFrequency.EveryNDays(3), today, anchor)
        assertEquals(3, before.currentStreak)

        // 생성일 하루 전에 SKIPPED를 추가해도 창의 위상은 유지 → 기존 판정 불변
        val after = HabitStatsCalculator.compute(base + rec("2026-08-27", "SKIPPED"), HabitFrequency.EveryNDays(3), today, anchor)
        assertEquals(3, after.currentStreak)
        assertEquals(3, after.maxStreak)
        assertEquals(LocalDate.of(2026, 8, 25), after.periods.first().start)
        assertEquals(PeriodStatus.NEUTRAL, after.periods.first().status)
        assertEquals(LocalDate.of(2026, 8, 28), after.periods[1].start)
    }

    // ---------- TimesInDays ----------

    @Test
    fun threeInSevenDays_windowsFromFirstRecord() {
        // 창1: 8/23~8/29 3회 MET, 창2: 8/30~9/5 2회 + 오늘 열림 → PENDING
        val records = done("2026-08-23", "2026-08-25", "2026-08-27", "2026-09-01", "2026-09-03")
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

    @Test
    fun anchorInFuture_isIgnored() {
        val s = HabitStatsCalculator.compute(done("2026-09-04"), HabitFrequency.EveryNDays(3), today, anchorDate = LocalDate.of(2026, 12, 1))
        assertEquals(LocalDate.of(2026, 9, 4), s.periods.first().start)
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
