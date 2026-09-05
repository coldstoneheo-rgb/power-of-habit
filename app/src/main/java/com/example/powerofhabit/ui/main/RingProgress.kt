package com.example.powerofhabit.ui.main

/**
 * 홈 목록 습관 이름 앞의 링 게이지 채움 비율(0~1).
 *
 * 점수는 [com.example.powerofhabit.domain.stats.HabitStatsCalculator]의 EMA(0~100)이며, 기록이 없거나 아직 판정된 기간이 없는
 * 습관은 0이다. 예전에는 "시작 진행도"라며 0.1~0.2를 바닥으로 깔았는데, 방금 만든 습관의 게이지가 이미 차 있어 보여
 * (실기기 피드백 2026-09-05) 실제 점수만 그린다 — 0점은 트랙만 보인다.
 */
internal fun scoreToRingProgress(score: Float?): Float {
    if (score == null || score.isNaN()) return 0f
    return (score / 100f).coerceIn(0f, 1f)
}
