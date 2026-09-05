package com.example.powerofhabit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordOutcomesTest {

    @Test
    fun check_habit_followsStatus() {
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("CHECK", "COMPLETED", null, null, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("CHECK", "FAILED", null, null, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("CHECK", null, null, null, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.SKIPPED, RecordOutcomes.of("CHECK", "SKIPPED", null, null, RecordOutcomes.TARGET_AT_LEAST))
    }

    @Test
    fun value_habit_isJudgedByValueNotStatus() {
        // 목표 8: 8 이상 성공, 0 < v < 8 기준미달, 0 미수행
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "FAILED", 8f, 8f, RecordOutcomes.TARGET_AT_LEAST))   // stale FAILED라도 값이 목표 이상이면 성공
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "COMPLETED", 9f, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.PARTIAL, RecordOutcomes.of("VALUE", "FAILED", 5f, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.PARTIAL, RecordOutcomes.of("VALUE", "COMPLETED", 5f, 8f, RecordOutcomes.TARGET_AT_LEAST)) // 목표를 올린 뒤 남은 stale COMPLETED
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", "FAILED", 0f, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", "FAILED", -1f, 8f, RecordOutcomes.TARGET_AT_LEAST))
    }

    @Test
    fun value_habit_withoutTarget_anyPositiveIsSuccess() {
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "FAILED", 0.5f, null, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", null, 3f, 0f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, 0f, null, RecordOutcomes.TARGET_AT_LEAST))
    }

    @Test
    fun value_habit_withoutValue_respectsExplicitSuccessOnly() {
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "COMPLETED", null, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", "FAILED", null, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, null, 8f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals(RecordOutcome.SKIPPED, RecordOutcomes.of("VALUE", "SKIPPED", 5f, 8f, RecordOutcomes.TARGET_AT_LEAST))
    }

    @Test
    fun statusForValue_isCacheOfOutcome() {
        val atLeast = RecordOutcomes.TARGET_AT_LEAST
        assertEquals("COMPLETED", RecordOutcomes.statusForValue(8f, 8f, atLeast))
        assertEquals("FAILED", RecordOutcomes.statusForValue(5f, 8f, atLeast))
        assertEquals("FAILED", RecordOutcomes.statusForValue(0f, 8f, atLeast))
        assertEquals("COMPLETED", RecordOutcomes.statusForValue(1f, null, atLeast))
        assertEquals("FAILED", RecordOutcomes.statusForValue(null, 8f, atLeast))
    }
}
