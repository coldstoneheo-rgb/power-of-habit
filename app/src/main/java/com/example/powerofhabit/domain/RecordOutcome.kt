package com.example.powerofhabit.domain

/**
 * 하루 기록의 표시 상태. 저장된 status 문자열이 아니라 **값과 목표**로 판정한다 (결정 기록 2026-09-05 결정 1).
 * - SUCCESS  : 체크형 COMPLETED, 또는 수치형 값이 목표를 충족(이상 목표: 값 ≥ 목표 / 이하 목표: 값 ≤ 목표. 목표 없으면 값 > 0)
 * - PARTIAL  : 수치형 — 기록은 했지만 목표 미달성 (이상 목표: 0 < 값 < 목표 / 이하 목표: 값 > 목표, 즉 한도 초과)
 * - NONE     : 미수행 (기록 없음, FAILED, 이상 목표에서 값 0/없음)
 * - SKIPPED  : 건너뜀
 * 통계 엔진은 여전히 status(COMPLETED/FAILED/SKIPPED)를 쓴다. 여기는 화면·위젯 표현 전용이다.
 *
 * 이하 목표(AT_MOST, 결정 기록 2026-09-06): 기록한 값 0은 성공이고(담배 0개비), **기록하지 않은 날은 성공이 아니라 미수행**이다 —
 * "안 적으면 성공"으로 두면 통계가 조용히 부풀기 때문에 사용자가 0을 직접 적어야 한다.
 */
enum class RecordOutcome { SUCCESS, PARTIAL, NONE, SKIPPED }

object RecordOutcomes {

    const val TYPE_VALUE = "VALUE"

    /** 목표 방향: 값이 목표 **이상**이면 성공(기본, 예: 달리기 5km). */
    const val TARGET_AT_LEAST = "AT_LEAST"
    /** 목표 방향: 값이 목표 **이하**면 성공(예: 담배 5개비 이하, 체중). */
    const val TARGET_AT_MOST = "AT_MOST"

    fun isAtMost(targetType: String?): Boolean = targetType == TARGET_AT_MOST

    /** 표시 상태 판정. 4개 렌더러(메인 셀·상세 캘린더·1x1·2x2 위젯)가 모두 이 함수만 쓴다. */
    fun of(
        habitType: String,
        status: String?,
        inputValue: Float?,
        targetValue: Float?,
        targetType: String? = TARGET_AT_LEAST
    ): RecordOutcome {
        if (status == "SKIPPED") return RecordOutcome.SKIPPED
        if (habitType != TYPE_VALUE) {
            return if (status == "COMPLETED") RecordOutcome.SUCCESS else RecordOutcome.NONE
        }
        val value = inputValue
        if (value == null) {
            // 값 없이 상세 화면에서 "성공"으로 표시한 경우만 존중한다.
            return if (status == "COMPLETED") RecordOutcome.SUCCESS else RecordOutcome.NONE
        }
        if (!value.isFinite()) return RecordOutcome.NONE // NaN·Infinity는 미수행 취급
        val target = targetValue?.takeIf { it.isFinite() && it > 0f }
        return if (isAtMost(targetType)) {
            when {
                value < 0f -> RecordOutcome.NONE
                target == null -> RecordOutcome.SUCCESS // 한도가 없으면 적은 것 자체가 성공
                value <= target -> RecordOutcome.SUCCESS
                else -> RecordOutcome.PARTIAL // 한도 초과 — 기록은 했으니 미수행과 구분한다
            }
        } else {
            when {
                value <= 0f -> RecordOutcome.NONE
                target == null -> RecordOutcome.SUCCESS
                value >= target -> RecordOutcome.SUCCESS
                else -> RecordOutcome.PARTIAL
            }
        }
    }

    /**
     * 수치형 저장 시 기록할 status. 메인 다이얼로그·상세 다이얼로그·위젯 입력이 모두 이 함수로 저장해
     * "status는 값의 캐시"라는 불변식을 유지한다. 목표 미달성(기준미달·한도 초과)과 미수행은 FAILED다.
     */
    fun statusForValue(inputValue: Float?, targetValue: Float?, targetType: String? = TARGET_AT_LEAST): String =
        when (of(TYPE_VALUE, null, inputValue, targetValue, targetType)) {
            RecordOutcome.SUCCESS -> "COMPLETED"
            else -> "FAILED"
        }
}
