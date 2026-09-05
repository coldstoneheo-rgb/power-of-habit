package com.example.powerofhabit.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 옛 앱 DB 행 → DTO 변환. 스키마 v1(컬럼 없음)부터 v5까지, SQLite의 느슨한 타입(Long/Double/0-1)을 모두 받아야 한다. */
class LegacyDbRowsTest {

    @Test
    fun habit_v1Row_withoutLaterColumns_getsDefaults() {
        val row = mapOf<String, Any?>(
            "habitId" to 3L, "title" to "물", "question" to "마셨나?", "frequencyType" to "DAILY", "frequencyValue" to "",
            "reminderTime" to null, "themeColor" to "#4FC3F7", "habitType" to "VALUE", "unit" to "잔", "createdAt" to 1_700_000_000_000L
        )
        val dto = LegacyDbRows.habit(row)!!
        assertEquals(3, dto.habitId)
        assertEquals(false, dto.isReminderEnabled)
        assertNull(dto.memo)
        assertNull(dto.targetValue)
        assertEquals("AT_LEAST", dto.targetType)
        assertEquals(1_700_000_000_000L, dto.createdAt)
        assertEquals("잔", dto.unit)
    }

    @Test
    fun habit_v5Row_readsEverything_withSqliteTypes() {
        val row = mapOf<String, Any?>(
            "habitId" to 7L, "title" to "담배", "question" to "q", "frequencyType" to "WEEKLY_COUNT", "frequencyValue" to "3",
            "reminderTime" to "09:00", "themeColor" to "#E57373", "habitType" to "VALUE", "unit" to "개비",
            "isReminderEnabled" to 1L, "memo" to "메모", "targetValue" to 5.0, "targetType" to "AT_MOST", "createdAt" to 42L
        )
        val dto = LegacyDbRows.habit(row)!!
        assertEquals(true, dto.isReminderEnabled)
        assertEquals("메모", dto.memo)
        assertEquals(5f, dto.targetValue!!, 0f)
        assertEquals("AT_MOST", dto.targetType)
        assertEquals("09:00", dto.reminderTime)
    }

    @Test
    fun habit_missingRequired_orBlankTitle_isSkipped() {
        assertNull(LegacyDbRows.habit(mapOf("title" to "x")))
        assertNull(LegacyDbRows.habit(mapOf("habitId" to 1L, "title" to "   ")))
    }

    @Test
    fun habit_nonFiniteTarget_andBlankColor_fallBack() {
        val dto = LegacyDbRows.habit(mapOf("habitId" to 1L, "title" to "t", "targetValue" to Double.NaN, "themeColor" to ""))!!
        assertNull(dto.targetValue)
        assertEquals("#FF9800", dto.themeColor)
        assertEquals("CHECK", dto.habitType)
        assertEquals(0L, dto.createdAt)
    }

    @Test
    fun record_readsLooseTypes_andKeepsUnknownStatus() {
        val dto = LegacyDbRows.record(mapOf("recordId" to 9L, "habitId" to 3L, "date" to "2026-09-01", "status" to "WHATEVER", "inputValue" to 2.5))!!
        assertEquals(9, dto.recordId)
        assertEquals("WHATEVER", dto.status)
        assertEquals(2.5f, dto.inputValue!!, 0f)
        assertNull(LegacyDbRows.record(mapOf("habitId" to 3L, "date" to "2026-09-01")))          // status 없음
        assertNull(LegacyDbRows.record(mapOf("habitId" to 3L, "status" to "COMPLETED")))          // date 없음
        assertNull(LegacyDbRows.record(mapOf("habitId" to 1L, "date" to "d", "status" to "s"))!!.inputValue) // inputValue 없음 → null
    }

    @Test
    fun badge_defaultsAndRequiredId() {
        val dto = LegacyDbRows.badge(mapOf("badgeId" to "STREAK_3", "earnedAt" to 10L))!!
        assertEquals("STREAK_3", dto.badgeName)
        assertEquals("DEFAULT", dto.badgeIconType)
        assertEquals(10L, dto.earnedAt)
        assertNull(LegacyDbRows.badge(mapOf("badgeName" to "x")))
    }

    @Test
    fun resolveFiles_findsDbAndSidecars_caseInsensitive() {
        val files = LegacyDbRows.resolveFiles(listOf("power_of_habit.db-shm", "power_of_habit.db", "POWER_OF_HABIT.DB-WAL"))!!
        assertEquals("power_of_habit.db", files.db)
        assertEquals("POWER_OF_HABIT.DB-WAL", files.wal)
        assertEquals("power_of_habit.db-shm", files.shm)
        assertEquals(LegacyDbFiles("a.db", null, null), LegacyDbRows.resolveFiles(listOf("a.db", "notes.txt")))
    }

    @Test
    fun resolveFiles_rejectsZeroOrTwoDbFiles() {
        assertNull(LegacyDbRows.resolveFiles(listOf("a.db-wal")))
        assertNull(LegacyDbRows.resolveFiles(listOf("a.db", "b.db")))
        assertNull(LegacyDbRows.resolveFiles(emptyList()))
    }
}
