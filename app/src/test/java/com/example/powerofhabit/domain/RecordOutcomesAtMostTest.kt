package com.example.powerofhabit.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 이하 목표(AT_MOST) 판정 — 결정 기록 2026-09-06. */
class RecordOutcomesAtMostTest {

    private val atMost = RecordOutcomes.TARGET_AT_MOST

    @Test
    fun atMost_valueAtOrBelowTarget_isSuccess_includingZero() {
        // 담배 5개비 이하: 0·3·5는 성공, 6은 한도 초과(PARTIAL), 미기록은 미수행
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "FAILED", 0f, 5f, atMost))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", null, 3f, 5f, atMost))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "FAILED", 5f, 5f, atMost))
        assertEquals(RecordOutcome.PARTIAL, RecordOutcomes.of("VALUE", "COMPLETED", 6f, 5f, atMost))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, null, 5f, atMost))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", "FAILED", null, 5f, atMost))
    }

    @Test
    fun atMost_explicitSuccessWithoutValue_isRespected_likeAtLeast() {
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", "COMPLETED", null, 5f, atMost))
    }

    @Test
    fun atMost_negativeOrNonFinite_isNone() {
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, -1f, 5f, atMost))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, Float.NaN, 5f, atMost))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, Float.POSITIVE_INFINITY, 5f, atMost))
    }

    @Test
    fun atMost_withoutTarget_anyRecordedValueIsSuccess() {
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", null, 0f, null, atMost))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", null, 12f, 0f, atMost))
    }

    @Test
    fun atMost_skippedAndCheckHabits_unaffected() {
        assertEquals(RecordOutcome.SKIPPED, RecordOutcomes.of("VALUE", "SKIPPED", 9f, 5f, atMost))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("CHECK", "COMPLETED", null, null, atMost))
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("CHECK", "FAILED", null, null, atMost))
    }

    @Test
    fun statusForValue_followsDirection() {
        assertEquals("COMPLETED", RecordOutcomes.statusForValue(0f, 5f, atMost))
        assertEquals("COMPLETED", RecordOutcomes.statusForValue(5f, 5f, atMost))
        assertEquals("FAILED", RecordOutcomes.statusForValue(6f, 5f, atMost))
        // 기본(이상)은 예전과 같다
        assertEquals("FAILED", RecordOutcomes.statusForValue(0f, 5f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals("COMPLETED", RecordOutcomes.statusForValue(5f, 5f, RecordOutcomes.TARGET_AT_LEAST))
        assertEquals("FAILED", RecordOutcomes.statusForValue(4f, 5f, RecordOutcomes.TARGET_AT_LEAST))
    }

    @Test
    fun unknownDirection_behavesAsAtLeast() {
        assertEquals(RecordOutcome.NONE, RecordOutcomes.of("VALUE", null, 0f, 5f, "WHATEVER"))
        assertEquals(RecordOutcome.SUCCESS, RecordOutcomes.of("VALUE", null, 5f, 5f, "WHATEVER"))
        assertEquals(RecordOutcome.PARTIAL, RecordOutcomes.of("VALUE", null, 4f, 5f, "WHATEVER"))
    }

    @Test
    fun restatusAfterTargetChange_rewritesOnlyValueRecordsWhoseCacheFlipped() {
        // 이상 5 → 이하 5로 바꾼 뒤: 0(FAILED)→COMPLETED, 3(FAILED)→COMPLETED, 8(COMPLETED)→FAILED, 5(COMPLETED)는 그대로
        assertEquals("COMPLETED", RecordOutcomes.restatusAfterTargetChange("FAILED", 0f, 5f, atMost))
        assertEquals("COMPLETED", RecordOutcomes.restatusAfterTargetChange("FAILED", 3f, 5f, atMost))
        assertEquals("FAILED", RecordOutcomes.restatusAfterTargetChange("COMPLETED", 8f, 5f, atMost))
        assertEquals(null, RecordOutcomes.restatusAfterTargetChange("COMPLETED", 5f, 5f, atMost))
        // 건너뜀·값 없는 기록은 건드리지 않는다
        assertEquals(null, RecordOutcomes.restatusAfterTargetChange("SKIPPED", 9f, 5f, atMost))
        assertEquals(null, RecordOutcomes.restatusAfterTargetChange("COMPLETED", null, 5f, atMost))
        assertEquals(null, RecordOutcomes.restatusAfterTargetChange("FAILED", null, 5f, atMost))
    }

    @Test
    fun targetLabel_formatsNumberUnitAndDirection() {
        assertEquals("5 개비 이하", RecordOutcomes.targetLabel(5f, "개비", atMost))
        assertEquals("2.5 km", RecordOutcomes.targetLabel(2.5f, "km", RecordOutcomes.TARGET_AT_LEAST))
        assertEquals("5 이하", RecordOutcomes.targetLabel(5f, "", atMost))
        assertEquals(null, RecordOutcomes.targetLabel(null, "km", atMost))
        assertEquals(null, RecordOutcomes.targetLabel(Float.NaN, "km", atMost))
    }
}
