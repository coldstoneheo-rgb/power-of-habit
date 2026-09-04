package com.example.powerofhabit.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetCalendarModelTest {

    @Test
    fun monthGrid_september2026_startsOnTuesday_hasFiveRows() {
        // 2026-09-01 is a Tuesday → Sunday-start grid has 2 leading blanks; 30 days → 32 cells → 5 rows of 7.
        val grid = WidgetCalendarModel.monthGrid(YearMonth.of(2026, 9), emptyMap())
        assertEquals(5, grid.size)
        assertTrue(grid.all { it.size == 7 })
        assertNull(grid[0][0])
        assertNull(grid[0][1])
        assertEquals(1, grid[0][2]?.day)
        assertEquals(29, grid[4][2]?.day)
        assertEquals(30, grid[4][3]?.day)
        assertNull(grid[4][4])
        assertNull(grid[4][6])
    }

    @Test
    fun monthGrid_februaryStartingSunday_hasNoLeadingBlanks() {
        // 2026-02-01 is a Sunday, 28 days → exactly 4 full rows.
        val grid = WidgetCalendarModel.monthGrid(YearMonth.of(2026, 2), emptyMap())
        assertEquals(4, grid.size)
        assertEquals(1, grid[0][0]?.day)
        assertEquals(28, grid[3][6]?.day)
    }

    @Test
    fun monthGrid_mapsStatusesByDate() {
        val statuses = mapOf(
            LocalDate.of(2026, 9, 3) to "COMPLETED",
            LocalDate.of(2026, 9, 4) to "SKIPPED"
        )
        val grid = WidgetCalendarModel.monthGrid(YearMonth.of(2026, 9), statuses)
        assertEquals("COMPLETED", grid[0][4]?.status)
        assertEquals("SKIPPED", grid[0][5]?.status)
        assertNull(grid[0][6]?.status)
    }

    @Test
    fun statusByDate_ignoresMalformedDates() {
        val map = WidgetCalendarModel.statusByDate(listOf("2026-09-03" to "COMPLETED", "bad" to "FAILED"))
        assertEquals(1, map.size)
        assertEquals("COMPLETED", map[LocalDate.of(2026, 9, 3)])
    }
}
