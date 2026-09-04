package com.example.powerofhabit.domain.stats

/**
 * 습관 주기(빈도)의 단일 정의.
 *
 * DB에는 `frequencyType`(문자열 코드) + `frequencyValue`(세부 값)로 저장된다.
 * 파싱·라벨링·평가 단위를 이 한 곳에서 관리해 화면/통계/뱃지가 서로 다른 해석을 하지 않게 한다.
 */
sealed class HabitFrequency {

    /** 사용자에게 보여줄 한글 라벨 (예: "주 3회"). */
    abstract val label: String

    /** 스트릭 숫자 뒤에 붙는 단위 (예: "일", "주"). */
    abstract val streakUnit: String

    /** 매일 1회. */
    object Daily : HabitFrequency() {
        override val label = "매일"
        override val streakUnit = "일"
    }

    /** N일마다 1회 (N ≥ 1). N == 1이면 사실상 매일. */
    data class EveryNDays(val days: Int) : HabitFrequency() {
        override val label = "${days}일마다"
        override val streakUnit = "회"
    }

    /** 주(일~토) 단위로 N회. */
    data class TimesPerWeek(val times: Int) : HabitFrequency() {
        override val label = "주 ${times}회"
        override val streakUnit = "주"
    }

    /** 달력 월 단위로 N회. */
    data class TimesPerMonth(val times: Int) : HabitFrequency() {
        override val label = "월 ${times}회"
        override val streakUnit = "개월"
    }

    /** D일 창(window) 안에 N회. 창은 첫 기록일부터 연속으로 자른다. */
    data class TimesInDays(val times: Int, val days: Int) : HabitFrequency() {
        override val label = "${days}일내 ${times}회"
        override val streakUnit = "회"
    }

    companion object {
        const val TYPE_DAILY = "DAILY"
        const val TYPE_INTERVAL = "INTERVAL"
        const val TYPE_WEEKLY_COUNT = "WEEKLY_COUNT"
        const val TYPE_MONTHLY_COUNT = "MONTHLY_COUNT"
        const val TYPE_COUNT_IN_DAYS = "COUNT_IN_DAYS"

        /**
         * DB 문자열 → [HabitFrequency]. 알 수 없는 타입이나 손상된 값은 [Daily]로 안전 폴백한다
         * (과거 데이터가 화면을 깨뜨리지 않도록).
         */
        fun parse(type: String?, value: String?): HabitFrequency {
            val v = value?.trim().orEmpty()
            return when (type) {
                TYPE_INTERVAL -> v.toIntOrNull()?.takeIf { it >= 1 }?.let { EveryNDays(it) } ?: Daily
                TYPE_WEEKLY_COUNT -> v.toIntOrNull()?.takeIf { it in 1..7 }?.let { TimesPerWeek(it) } ?: Daily
                TYPE_MONTHLY_COUNT -> v.toIntOrNull()?.takeIf { it in 1..31 }?.let { TimesPerMonth(it) } ?: Daily
                TYPE_COUNT_IN_DAYS -> {
                    val parts = v.split("/")
                    val times = parts.getOrNull(0)?.trim()?.toIntOrNull()
                    val days = parts.getOrNull(1)?.trim()?.toIntOrNull()
                    if (times != null && days != null && times >= 1 && days >= 1 && times <= days) {
                        TimesInDays(times, days)
                    } else Daily
                }
                else -> Daily
            }
        }
    }
}
