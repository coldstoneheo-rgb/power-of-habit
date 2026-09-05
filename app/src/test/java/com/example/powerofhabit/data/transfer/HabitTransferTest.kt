package com.example.powerofhabit.data.transfer

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class HabitTransferTest {

    private fun habit(id: Int, title: String, createdAt: Long, type: String = "CHECK") = HabitEntity(
        habitId = id,
        title = title,
        question = "$title?",
        frequencyType = "DAILY",
        frequencyValue = "",
        reminderTime = if (id % 2 == 0) "09:00" else null,
        themeColor = "#FFCC00",
        habitType = type,
        unit = if (type == "VALUE") "잔" else null,
        isReminderEnabled = id % 2 == 0,
        memo = if (id == 1) "메모" else null,
        targetValue = if (type == "VALUE") 8f else null,
        createdAt = createdAt
    )

    private fun record(id: Int, habitId: Int, date: String, status: String = "COMPLETED", value: Float? = null) =
        HabitRecordEntity(recordId = id, habitId = habitId, date = date, status = status, inputValue = value)

    private fun badge(id: String, earnedAt: Long = 1_000L) =
        BadgeEntity(badgeId = id, badgeName = id, description = "desc $id", earnedAt = earnedAt, badgeIconType = "GOLD")

    private fun export(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity> = emptyList(),
        badges: List<BadgeEntity> = emptyList()
    ) = HabitTransfer.buildExport(habits, records, badges, appVersionName = "1.0-test", exportedAt = Instant.EPOCH)

    // ---- 인코딩/디코딩 ----

    @Test
    fun roundTrip_encodeThenDecode_isIdentical() {
        val original = export(
            habits = listOf(habit(1, "물 마시기", 100L, type = "VALUE"), habit(2, "독서", 200L)),
            records = listOf(record(1, 1, "2026-09-01", value = 5f), record(2, 2, "2026-09-02", status = "FAILED")),
            badges = listOf(badge("START_FIRST"))
        )

        val decoded = HabitTransfer.decode(HabitTransfer.encode(original))

        assertEquals(original, decoded)
        assertEquals(1, decoded.formatVersion)
        assertEquals("1970-01-01T00:00:00Z", decoded.exportedAt)
        assertEquals("1.0-test", decoded.appVersionName)
    }

    @Test
    fun decode_ignoresUnknownKeys() {
        val json = """
            {
              "formatVersion": 1,
              "exportedAt": "2026-09-05T00:00:00Z",
              "futureTopLevelField": {"a": 1},
              "habits": [
                {"habitId": 7, "title": "명상", "question": "했나?", "frequencyType": "DAILY", "frequencyValue": "",
                 "themeColor": "#000000", "habitType": "CHECK", "createdAt": 5, "someNewHabitField": true}
              ],
              "records": [
                {"recordId": 3, "habitId": 7, "date": "2026-09-01", "status": "COMPLETED", "extra": "x"}
              ],
              "badges": []
            }
        """.trimIndent()

        val decoded = HabitTransfer.decode(json)

        assertEquals(1, decoded.habits.size)
        assertEquals("명상", decoded.habits[0].title)
        assertNull(decoded.habits[0].reminderTime)
        assertEquals(1, decoded.records.size)
        assertNull(decoded.records[0].inputValue)
    }

    @Test
    fun decode_rejectsNewerFormatVersion() {
        val json = """{"formatVersion": 2, "exportedAt": "2026-09-05T00:00:00Z"}"""
        try {
            HabitTransfer.decode(json)
            fail("formatVersion 2 must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("지원하지 않는 형식 버전"))
        }
    }

    @Test
    fun decode_rejectsNonFormatJson_asIllegalArgument() {
        try {
            HabitTransfer.decode("""{"hello": "world"}""")
            fail("missing required field must be rejected")
        } catch (e: IllegalArgumentException) {
            // exportedAt 누락 → 형식 오류. Result.failure 메시지로 노출된다.
        }
    }

    // ---- plan ----

    @Test
    fun plan_emptyDatabase_importsEverything() {
        val import = export(
            habits = listOf(habit(10, "물 마시기", 100L), habit(11, "독서", 200L)),
            records = listOf(record(1, 10, "2026-09-01"), record(2, 10, "2026-09-02"), record(3, 11, "2026-09-01")),
            badges = listOf(badge("START_FIRST"), badge("STREAK_3"))
        )

        val plan = HabitTransfer.plan(emptyList(), emptyList(), emptyList(), import)

        assertEquals(ImportSummary(habitsAdded = 2, habitsMatched = 0, recordsAdded = 3, recordsSkipped = 0, badgesAdded = 2), plan.summary)
        assertEquals(listOf(10, 11), plan.habitsToInsert.map { it.sourceHabitId })
        assertTrue("새 습관은 id 자동 생성", plan.habitsToInsert.all { it.habit.habitId == 0 })
        assertTrue(plan.matchedHabitIds.isEmpty())

        // 삽입 결과(10→1, 11→2)로 기록 habitId가 재매핑된다.
        val resolved = plan.resolveRecords(mapOf(10 to 1, 11 to 2))
        assertEquals(listOf(1, 1, 2), resolved.map { it.habitId })
        assertTrue("recordId는 자동 생성", resolved.all { it.recordId == 0 })
    }

    @Test
    fun plan_matchesHabitByCreatedAt_evenIfRenamed() {
        // 한쪽 기기에서 제목을 바꿔도 createdAt이 같으면 같은 습관이다 — 중복 생성 없이 매칭
        val existing = listOf(habit(5, "물 2L 마시기", 100L))
        val import = export(habits = listOf(habit(1, "물 마시기", 100L)), records = listOf(record(1, 1, "2026-09-01")))

        val plan = HabitTransfer.plan(existing, emptyList(), emptyList(), import)

        assertEquals(mapOf(1 to 5), plan.matchedHabitIds)
        assertTrue(plan.habitsToInsert.isEmpty())
        assertEquals(listOf(5), plan.resolveRecords(emptyMap()).map { it.habitId })
    }

    @Test
    fun plan_twoFileHabitsMatchingSameExisting_dedupeRecordsAfterRemap() {
        // 파일의 습관 1·2가 모두 기존 5에 매칭되면 같은 날짜 기록은 한 번만(recordId 큰 것) 들어간다
        val existing = listOf(habit(5, "물", 100L))
        val import = export(
            habits = listOf(habit(1, "물", 100L), habit(2, "물(복사)", 100L)),
            records = listOf(record(10, 1, "2026-09-01", value = 3f), record(11, 2, "2026-09-01", value = 7f))
        )

        val plan = HabitTransfer.plan(existing, emptyList(), emptyList(), import)

        val resolved = plan.resolveRecords(emptyMap())
        assertEquals(1, resolved.size)
        assertEquals(7f, resolved.single().inputValue)
        assertEquals(1, plan.summary.recordsSkipped)
    }

    @Test
    fun decode_stripsLeadingBom() {
        val text = "﻿" + HabitTransfer.encode(export(habits = listOf(habit(1, "물", 100L))))
        assertEquals(1, HabitTransfer.decode(text).habits.size)
    }

    @Test
    fun decode_newerVersionWithDifferentShape_saysUpdateNotWrongFormat() {
        // 미래 형식: formatVersion 2 + 우리가 모르는 구조(records가 객체). 버전을 먼저 읽어 "업데이트" 안내를 해야 한다
        val text = """{"formatVersion":2,"exportedAt":"x","habits":[],"records":{"v2":true},"badges":[]}"""
        try {
            HabitTransfer.decode(text)
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("업데이트"))
        }
    }

    @Test
    fun buildExport_sanitizesNonFiniteFloats() {
        val habits = listOf(habit(1, "물", 100L, type = "VALUE").copy(targetValue = Float.NaN))
        val records = listOf(record(1, 1, "2026-09-01", value = Float.POSITIVE_INFINITY), record(2, 1, "2026-09-02", value = 5f))

        val export = export(habits, records)
        val decoded = HabitTransfer.decode(HabitTransfer.encode(export)) // NaN이 있었다면 encode에서 예외

        assertNull(decoded.habits.single().targetValue)
        assertNull(decoded.records.first { it.date == "2026-09-01" }.inputValue)
        assertEquals(5f, decoded.records.first { it.date == "2026-09-02" }.inputValue)
    }

    @Test
    fun plan_matchesHabitByCreatedAt_andRemapsIds() {
        val existing = listOf(habit(5, "물 마시기", 100L), habit(6, "다른 습관", 999L))
        val import = export(
            habits = listOf(
                habit(1, "물 마시기", 100L),  // 같은 createdAt → 기존 id 5 재사용
                habit(2, "물 마시기", 101L),  // createdAt 다름 → 새 습관
                habit(3, "운동", 300L)        // 새 습관
            ),
            records = listOf(record(1, 1, "2026-09-01"), record(2, 2, "2026-09-01"), record(3, 3, "2026-09-01"))
        )

        val plan = HabitTransfer.plan(existing, emptyList(), emptyList(), import)

        assertEquals(mapOf(1 to 5), plan.matchedHabitIds)
        assertEquals(listOf(2, 3), plan.habitsToInsert.map { it.sourceHabitId })
        assertEquals(1, plan.summary.habitsMatched)
        assertEquals(2, plan.summary.habitsAdded)

        // 매칭(1→5) + 삽입 결과(2→7, 3→8)가 합쳐져 기록에 적용된다. 순서는 파일 순서.
        val resolved = plan.resolveRecords(mapOf(2 to 7, 3 to 8))
        assertEquals(listOf(1, 2, 3), plan.recordsToInsert.map { it.sourceHabitId })
        assertEquals(listOf(5, 7, 8), resolved.map { it.habitId })
    }

    @Test
    fun plan_duplicateHabitDateInFile_keepsLargestRecordId() {
        val import = export(
            habits = listOf(habit(1, "물 마시기", 100L)),
            records = listOf(
                record(10, 1, "2026-09-01", status = "FAILED"),
                record(30, 1, "2026-09-01", status = "COMPLETED"), // 가장 최근 쓰기
                record(20, 1, "2026-09-01", status = "SKIPPED"),
                record(40, 1, "2026-09-02", status = "COMPLETED")
            )
        )

        val plan = HabitTransfer.plan(emptyList(), emptyList(), emptyList(), import)

        assertEquals(2, plan.summary.recordsAdded)
        assertEquals(2, plan.summary.recordsSkipped)
        val sept1 = plan.recordsToInsert.single { it.record.date == "2026-09-01" }
        assertEquals("COMPLETED", sept1.record.status)
    }

    @Test
    fun plan_existingRecordForSameHabitDate_isKeptNotOverwritten() {
        val existing = listOf(habit(5, "물 마시기", 100L))
        val existingRecords = listOf(record(99, 5, "2026-09-01", status = "FAILED"))
        val import = export(
            habits = listOf(habit(1, "물 마시기", 100L)),
            records = listOf(record(1, 1, "2026-09-01", status = "COMPLETED"), record(2, 1, "2026-09-02"))
        )

        val plan = HabitTransfer.plan(existing, existingRecords, emptyList(), import)

        assertEquals(1, plan.summary.recordsAdded)
        assertEquals(1, plan.summary.recordsSkipped)
        val resolved = plan.resolveRecords(emptyMap())
        assertEquals(listOf("2026-09-02"), resolved.map { it.date })
        assertEquals(5, resolved.single().habitId)
    }

    @Test
    fun plan_preservesUnknownStatusString() {
        val import = export(
            habits = listOf(habit(1, "물 마시기", 100L, type = "VALUE")),
            records = listOf(record(1, 1, "2026-09-01", status = "PARTIAL", value = 3.5f))
        )

        val plan = HabitTransfer.plan(emptyList(), emptyList(), emptyList(), import)

        val r = plan.resolveRecords(mapOf(1 to 1)).single()
        assertEquals("PARTIAL", r.status)
        assertEquals(3.5f, r.inputValue)
    }

    @Test
    fun plan_skipsBadgesAlreadyPresent() {
        val existingBadges = listOf(badge("START_FIRST", earnedAt = 1L))
        val import = export(
            habits = emptyList(),
            badges = listOf(badge("START_FIRST", earnedAt = 2L), badge("STREAK_3"), badge("STREAK_3"))
        )

        val plan = HabitTransfer.plan(emptyList(), emptyList(), existingBadges, import)

        assertEquals(listOf("STREAK_3"), plan.badgesToInsert.map { it.badgeId })
        assertEquals(1, plan.summary.badgesAdded)
    }

    @Test
    fun plan_orphanRecordWithoutHabitInFile_isSkipped() {
        val import = export(
            habits = listOf(habit(1, "물 마시기", 100L)),
            records = listOf(record(1, 1, "2026-09-01"), record(2, 42, "2026-09-01"))
        )

        val plan = HabitTransfer.plan(emptyList(), emptyList(), emptyList(), import)

        assertEquals(1, plan.summary.recordsAdded)
        assertEquals(1, plan.summary.recordsSkipped)
        assertEquals(1, plan.resolveRecords(mapOf(1 to 1)).size)
    }
}
