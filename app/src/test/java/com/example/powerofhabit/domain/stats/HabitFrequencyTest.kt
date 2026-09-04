package com.example.powerofhabit.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitFrequencyTest {

    @Test
    fun parse_daily() {
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("DAILY", ""))
        assertEquals("매일", HabitFrequency.parse("DAILY", "").label)
    }

    @Test
    fun parse_interval_weekly_monthly() {
        assertEquals(HabitFrequency.EveryNDays(3), HabitFrequency.parse("INTERVAL", "3"))
        assertEquals(HabitFrequency.TimesPerWeek(3), HabitFrequency.parse("WEEKLY_COUNT", "3"))
        assertEquals(HabitFrequency.TimesPerMonth(10), HabitFrequency.parse("MONTHLY_COUNT", "10"))
        assertEquals("주 3회", HabitFrequency.TimesPerWeek(3).label)
        assertEquals("주", HabitFrequency.TimesPerWeek(3).streakUnit)
    }

    @Test
    fun parse_countInDays() {
        assertEquals(HabitFrequency.TimesInDays(3, 7), HabitFrequency.parse("COUNT_IN_DAYS", "3/7"))
        assertEquals("7일내 3회", HabitFrequency.TimesInDays(3, 7).label)
    }

    @Test
    fun parse_interval1_isDaily() {
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("INTERVAL", "1"))
    }

    @Test
    fun parse_invalidValues_fallBackToDaily() {
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("INTERVAL", "0"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("INTERVAL", "abc"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("WEEKLY_COUNT", "8"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("MONTHLY_COUNT", "32"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("COUNT_IN_DAYS", "9/7"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("COUNT_IN_DAYS", "3"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse("UNKNOWN", "3"))
        assertEquals(HabitFrequency.Daily, HabitFrequency.parse(null, null))
    }
}
