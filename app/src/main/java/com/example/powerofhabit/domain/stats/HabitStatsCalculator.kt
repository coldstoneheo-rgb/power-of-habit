package com.example.powerofhabit.domain.stats

import com.example.powerofhabit.data.local.HabitRecordEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** 하나의 평가 기간(일/주/월/창)에 대한 결과. */
data class PeriodResult(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val required: Int,
    val completed: Int,
    val status: PeriodStatus
)

enum class PeriodStatus {
    /** 목표 횟수 달성. 스트릭 +1, 점수 목표 100. */
    MET,
    /** 미달 확정(기간 종료, 또는 남은 날로는 목표 도달 불가). 스트릭 0으로 리셋, 점수 목표 = 달성비율. */
    FAILED,
    /** 건너뜀으로만 채워진 기간. 스트릭·점수에 영향 없음. */
    NEUTRAL,
    /** 오늘을 포함하고 아직 판정할 수 없는 진행 중 기간. */
    PENDING
}

/** 진행 중 기간의 달성 현황 (예: 이번 주 2/3회). */
data class CurrentPeriodProgress(
    val label: String,
    val completed: Int,
    val required: Int
) {
    val fraction: Float get() = if (required > 0) (completed.toFloat() / required).coerceIn(0f, 1f) else 0f
}

data class HabitStats(
    val frequency: HabitFrequency,
    val currentStreak: Int,
    val maxStreak: Int,
    /** 기준일부터 오늘까지 매일의 EMA 점수(0~100). 기간형 주기는 기간이 끝나는 날(또는 조기 달성일)에 계단식으로 갱신된다. */
    val dailyScores: List<Pair<LocalDate, Float>>,
    /** 이번 달 달성률(0~1). 주기별 기대 횟수(경과일 페이스) 대비 완료 횟수. */
    val monthProgress: Float,
    val currentPeriod: CurrentPeriodProgress,
    val periods: List<PeriodResult>
) {
    val latestScore: Float get() = dailyScores.lastOrNull()?.second ?: 0f
    val streakUnit: String get() = frequency.streakUnit
}

/**
 * 빈도(주기)를 인지하는 통계 엔진. 순수 Kotlin — Android 의존 없음, `today` 주입으로 테스트 가능.
 *
 * 규칙 요약
 * - 기간 판정: 완료 ≥ 목표 → MET / 남은 열린 날로 목표 도달 불가 또는 기간 종료 → FAILED /
 *   기록이 건너뜀뿐(1회 목표 기간, 또는 기간 전체가 건너뜀) → NEUTRAL / 그 외 오늘 포함 → PENDING
 * - 스트릭: MET 연속 개수. FAILED에서 리셋, NEUTRAL·PENDING은 무시. 단위는 주기에 따름(일/주/개월/회).
 * - 점수(EMA α=0.15): 기간이 판정되는 날(종료일, 또는 조기 달성/조기 실패 확정일)에 목표값(min(완료/목표,1)×100)으로 갱신.
 *   아직 판정된 기간이 없으면 진행 중 기간의 달성비율을 임시 점수로 보여준다.
 * - 기간 기준일(anchor): 습관 생성일과 첫 기록일 중 이른 날. 과거 날짜를 나중에 편집해도 창이 다시 잘리지 않도록 생성일을 넘기는 것을 권장.
 */
object HabitStatsCalculator {

    const val EMA_ALPHA = 0.15f
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_FAILED = "FAILED"
    const val STATUS_SKIPPED = "SKIPPED"

    /** 앱 달력이 일요일 시작이므로 주 단위 기간도 일요일 시작으로 맞춘다. */
    val WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

    /** `HabitEntity.createdAt`(epoch millis) → 기준일. */
    fun anchorFromEpochMillis(createdAtMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(createdAtMillis).atZone(zone).toLocalDate()

    fun compute(
        records: List<HabitRecordEntity>,
        frequency: HabitFrequency,
        today: LocalDate = LocalDate.now(),
        anchorDate: LocalDate? = null
    ): HabitStats {
        val byDate: Map<LocalDate, HabitRecordEntity> = records
            .mapNotNull { r -> parseDate(r.date)?.let { it to r } }
            .toMap()
        val firstRecordDate: LocalDate? = byDate.keys.minOrNull()
        val anchor: LocalDate? = resolveAnchor(frequency, firstRecordDate, anchorDate, today)

        if (anchor == null) {
            return HabitStats(
                frequency = frequency,
                currentStreak = 0,
                maxStreak = 0,
                dailyScores = emptyList(),
                monthProgress = 0f,
                currentPeriod = currentPeriodOf(frequency, emptyList(), today),
                periods = emptyList()
            )
        }

        val periods = buildPeriods(frequency, anchor, today).map { (start, end, required) ->
            evaluatePeriod(start, end, required, byDate, today)
        }

        // 스트릭: 단일 전방 패스. PENDING/NEUTRAL은 건너뛰므로 루프 종료 시 temp가 곧 현재 스트릭.
        var temp = 0
        var max = 0
        for (p in periods) {
            when (p.status) {
                PeriodStatus.MET -> { temp++; if (temp > max) max = temp }
                PeriodStatus.FAILED -> temp = 0
                PeriodStatus.NEUTRAL, PeriodStatus.PENDING -> Unit
            }
        }

        // EMA 일별 시리즈: 기간이 판정되는 날에 갱신. 판정일 = 종료일, 단 진행 중 기간이 이미 확정(MET/FAILED)이면 오늘.
        val scores = ArrayList<Pair<LocalDate, Float>>()
        var ema: Float? = null
        var idx = 0
        var day: LocalDate = anchor
        while (!day.isAfter(today)) {
            while (idx < periods.size && periods[idx].endInclusive.isBefore(day)) idx++
            val p = periods.getOrNull(idx)
            if (p != null) {
                val decisionDay = if (p.endInclusive.isAfter(today)) today else p.endInclusive
                if (day == decisionDay) {
                    val target = when (p.status) {
                        PeriodStatus.MET -> 100f
                        PeriodStatus.FAILED -> ratio(p) * 100f
                        PeriodStatus.NEUTRAL, PeriodStatus.PENDING -> null
                    }
                    if (target != null) {
                        ema = ema?.let { it * (1 - EMA_ALPHA) + target * EMA_ALPHA } ?: target
                    }
                }
            }
            scores.add(day to (ema ?: 0f))
            day = day.plusDays(1)
        }
        // 판정된 기간이 하나도 없으면 진행 중 기간의 달성비율을 임시 점수로.
        if (ema == null) {
            val pending = periods.lastOrNull { it.status == PeriodStatus.PENDING }
            if (pending != null && pending.completed > 0 && scores.isNotEmpty()) {
                val last = scores.size - 1
                scores[last] = scores[last].first to ratio(pending) * 100f
            }
        }

        return HabitStats(
            frequency = frequency,
            currentStreak = temp,
            maxStreak = max,
            dailyScores = scores,
            monthProgress = monthProgress(frequency, byDate, today),
            currentPeriod = currentPeriodOf(frequency, periods, today),
            periods = periods
        )
    }

    /** 점수 추이 차트용 그룹핑. filter: 일/주/월/분기/년. */
    fun groupScores(daily: List<Pair<LocalDate, Float>>, filter: String): Pair<List<Float>, List<String>> {
        if (daily.isEmpty()) return listOf(0f) to listOf("오늘")
        val entries = daily.sortedBy { it.first }
        fun grouped(keyOf: (LocalDate) -> String, labelOf: (LocalDate) -> String, take: Int): Pair<List<Float>, List<String>> {
            val g = entries.groupBy { keyOf(it.first) }
            val scores = g.map { it.value.last().second }
            val labels = g.map { labelOf(it.value.first().first) }
            return scores.takeLast(take) to labels.takeLast(take)
        }
        return when (filter) {
            "주" -> grouped(
                keyOf = { d -> val s = d.with(TemporalAdjusters.previousOrSame(WEEK_START)); "${s.year}-${s.dayOfYear}" },
                labelOf = { d -> "${d.monthValue}월 ${d.dayOfMonth}일" },
                take = 8
            )
            "월" -> grouped({ d -> "${d.year}-${d.monthValue}" }, { d -> "${d.monthValue}월" }, 12)
            "분기" -> grouped({ d -> "${d.year}-Q${(d.monthValue - 1) / 3 + 1}" }, { d -> "${d.monthValue}월" }, 8)
            "년" -> grouped({ d -> "${d.year}" }, { d -> "${d.year}년" }, 5)
            else -> {
                val recent = entries.takeLast(12)
                recent.map { it.second } to recent.map { "${it.first.dayOfMonth}일" }
            }
        }
    }

    // ---- internals ----

    private data class Window(val start: LocalDate, val endInclusive: LocalDate, val required: Int)

    /**
     * 기간 기준일. 달력 정렬 주기(매일/주/월)는 첫 기록일이면 충분하다. 창(window)을 자르는 주기(N일마다/D일내 N회)는
     * 생성일을 위상(phase) 기준으로 삼고, 그보다 이른 기록이 있으면 창 크기 배수만큼 뒤로 물러나 위상을 유지한다.
     * 이렇게 해야 과거 날짜를 나중에 편집해도 이미 판정된 창이 다시 잘리지 않는다.
     */
    private fun resolveAnchor(frequency: HabitFrequency, firstRecord: LocalDate?, anchorDate: LocalDate?, today: LocalDate): LocalDate? {
        val windowDays = when (frequency) {
            is HabitFrequency.EveryNDays -> frequency.days.toLong()
            is HabitFrequency.TimesInDays -> frequency.days.toLong()
            else -> null
        }
        val phase = anchorDate?.takeIf { !it.isAfter(today) }
        if (windowDays == null || phase == null) return firstRecord ?: phase
        if (firstRecord == null || !firstRecord.isBefore(phase)) return phase
        val stepsBack = (ChronoUnit.DAYS.between(firstRecord, phase) + windowDays - 1) / windowDays
        return phase.minusDays(stepsBack * windowDays)
    }

    private fun ratio(p: PeriodResult): Float =
        if (p.required > 0) (p.completed.toFloat() / p.required).coerceIn(0f, 1f) else 0f

    private fun buildPeriods(frequency: HabitFrequency, anchor: LocalDate, today: LocalDate): List<Window> {
        val out = ArrayList<Window>()
        when (frequency) {
            HabitFrequency.Daily -> {
                var d = anchor
                while (!d.isAfter(today)) { out.add(Window(d, d, 1)); d = d.plusDays(1) }
            }
            is HabitFrequency.EveryNDays -> {
                val n = frequency.days.toLong()
                var s = anchor
                while (!s.isAfter(today)) { out.add(Window(s, s.plusDays(n - 1), 1)); s = s.plusDays(n) }
            }
            is HabitFrequency.TimesInDays -> {
                val n = frequency.days.toLong()
                var s = anchor
                while (!s.isAfter(today)) { out.add(Window(s, s.plusDays(n - 1), frequency.times)); s = s.plusDays(n) }
            }
            is HabitFrequency.TimesPerWeek -> {
                var s = anchor.with(TemporalAdjusters.previousOrSame(WEEK_START))
                while (!s.isAfter(today)) { out.add(Window(s, s.plusDays(6), frequency.times)); s = s.plusWeeks(1) }
            }
            is HabitFrequency.TimesPerMonth -> {
                var ym = YearMonth.from(anchor)
                val last = YearMonth.from(today)
                while (!ym.isAfter(last)) { out.add(Window(ym.atDay(1), ym.atEndOfMonth(), frequency.times)); ym = ym.plusMonths(1) }
            }
        }
        return out
    }

    private fun evaluatePeriod(
        start: LocalDate,
        end: LocalDate,
        required: Int,
        byDate: Map<LocalDate, HabitRecordEntity>,
        today: LocalDate
    ): PeriodResult {
        var completed = 0
        var failed = 0
        var skipped = 0
        var d = start
        while (!d.isAfter(end)) {
            when (byDate[d]?.status) {
                STATUS_COMPLETED -> completed++
                STATUS_FAILED -> failed++
                STATUS_SKIPPED -> skipped++
            }
            d = d.plusDays(1)
        }
        val periodLength = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val containsToday = !end.isBefore(today)
        // 아직 기록이 없는 '열린' 날: 오늘(미기록 시)과 내일 이후 기간 종료일까지.
        val openDays = if (!containsToday) 0 else {
            val future = ChronoUnit.DAYS.between(today, end).toInt() // 내일~종료일
            future + (if (byDate[today] == null) 1 else 0)
        }
        val onlySkips = completed == 0 && failed == 0 && skipped > 0
        val status = when {
            completed >= required -> PeriodStatus.MET
            onlySkips && (required == 1 || skipped == periodLength) -> PeriodStatus.NEUTRAL
            completed + openDays < required -> PeriodStatus.FAILED
            containsToday -> PeriodStatus.PENDING
            else -> PeriodStatus.FAILED
        }
        return PeriodResult(start, end, required, completed, status)
    }

    private fun currentPeriodOf(frequency: HabitFrequency, periods: List<PeriodResult>, today: LocalDate): CurrentPeriodProgress {
        val current = periods.lastOrNull { !it.start.isAfter(today) && !it.endInclusive.isBefore(today) }
        val required = current?.required ?: when (frequency) {
            is HabitFrequency.TimesPerWeek -> frequency.times
            is HabitFrequency.TimesPerMonth -> frequency.times
            is HabitFrequency.TimesInDays -> frequency.times
            else -> 1
        }
        val label = when (frequency) {
            HabitFrequency.Daily -> "오늘"
            is HabitFrequency.EveryNDays -> "이번 ${frequency.days}일"
            is HabitFrequency.TimesPerWeek -> "이번 주"
            is HabitFrequency.TimesPerMonth -> "이번 달"
            is HabitFrequency.TimesInDays -> "이번 ${frequency.days}일"
        }
        return CurrentPeriodProgress(label, current?.completed ?: 0, required)
    }

    /** 이번 달 달성률 = 이번 달 완료 횟수 / 주기별 기대 횟수(경과일 기준 페이스). */
    private fun monthProgress(frequency: HabitFrequency, byDate: Map<LocalDate, HabitRecordEntity>, today: LocalDate): Float {
        val ym = YearMonth.from(today)
        val completed = byDate.count { (d, r) -> YearMonth.from(d) == ym && r.status == STATUS_COMPLETED }
        val elapsed = today.dayOfMonth.toFloat()
        val expected = when (frequency) {
            HabitFrequency.Daily -> elapsed
            is HabitFrequency.EveryNDays -> kotlin.math.ceil(elapsed / frequency.days)
            is HabitFrequency.TimesPerWeek -> frequency.times * elapsed / 7f
            is HabitFrequency.TimesPerMonth -> frequency.times * elapsed / ym.lengthOfMonth()
            is HabitFrequency.TimesInDays -> frequency.times * elapsed / frequency.days
        }
        return if (expected > 0f) (completed / expected).coerceIn(0f, 1f) else 0f
    }

    private fun parseDate(s: String): LocalDate? = try { LocalDate.parse(s) } catch (e: Exception) { null }
}
