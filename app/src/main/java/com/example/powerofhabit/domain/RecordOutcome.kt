package com.example.powerofhabit.domain

/**
 * 하루 기록의 표시 상태. 저장된 status 문자열이 아니라 **값과 목표**로 판정한다 (결정 기록 2026-09-05 결정 1).
 * - SUCCESS  : 체크형 COMPLETED, 또는 수치형 값이 목표 이상(목표 없으면 값 > 0)
 * - PARTIAL  : 수치형 — 수행했지만 목표 미달 (0 < 값 < 목표)
 * - NONE     : 미수행 (기록 없음, FAILED, 값 0/없음)
 * - SKIPPED  : 건너뜀
 * 통계 엔진은 여전히 status(COMPLETED/FAILED/SKIPPED)를 쓴다. 여기는 화면·위젯 표현 전용이다.
 */
enum class RecordOutcome { SUCCESS, PARTIAL, NONE, SKIPPED }

object RecordOutcomes {

    const val TYPE_VALUE = "VALUE"

    /** 표시 상태 판정. 4개 렌더러(메인 셀·상세 캘린더·1x1·2x2 위젯)가 모두 이 함수만 쓴다. */
    fun of(habitType: String, status: String?, inputValue: Float?, targetValue: Float?): RecordOutcome {
        if (status == "SKIPPED") return RecordOutcome.SKIPPED
        if (habitType != TYPE_VALUE) {
            return if (status == "COMPLETED") RecordOutcome.SUCCESS else RecordOutcome.NONE
        }
        val value = inputValue
        if (value == null) {
            // 값 없이 상세 화면에서 "성공"으로 표시한 경우만 존중한다.
            return if (status == "COMPLETED") RecordOutcome.SUCCESS else RecordOutcome.NONE
        }
        if (value <= 0f) return RecordOutcome.NONE
        val target = targetValue
        return when {
            target == null || target <= 0f -> RecordOutcome.SUCCESS
            value >= target -> RecordOutcome.SUCCESS
            else -> RecordOutcome.PARTIAL
        }
    }

    /**
     * 수치형 저장 시 기록할 status. 메인 다이얼로그·상세 다이얼로그·위젯 입력이 모두 이 함수로 저장해
     * "status는 값의 캐시"라는 불변식을 유지한다. 0 이하는 미수행(FAILED)이다.
     */
    fun statusForValue(inputValue: Float?, targetValue: Float?): String =
        when (of(TYPE_VALUE, null, inputValue, targetValue)) {
            RecordOutcome.SUCCESS -> "COMPLETED"
            else -> "FAILED"
        }
}
