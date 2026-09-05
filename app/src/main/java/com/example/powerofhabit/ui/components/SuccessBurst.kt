package com.example.powerofhabit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * 성공(목표 충족) 순간의 "폭죽" (가이드 B7·결정 기록 2026-09-05 결정 2).
 * 절제 규칙: 입자 8개, 색은 액센트와 accentDim 두 톤만(무지개 컨페티 금지), 450ms, EaseOutCubic, 반경은 셀의 1.5배.
 * Glance 위젯에서는 쓸 수 없고(애니메이션 불가) 앱 셀과 위젯 입력 액티비티에서만 쓴다.
 */
@Composable
fun SuccessBurst(
    accent: Color,
    modifier: Modifier = Modifier,
    radiusScale: Float = 1.5f,
    durationMillis: Int = 450,
    onFinished: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)))
        onFinished()
    }
    val dim = accent.copy(alpha = 0.45f)
    Canvas(modifier = modifier) {
        val p = progress.value
        if (p <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * radiusScale
        val particleRadius = (size.minDimension * 0.06f) * (1f - p * 0.6f)
        val alpha = (1f - p).coerceIn(0f, 1f)
        for (i in 0 until PARTICLES) {
            val angle = (Math.PI * 2 / PARTICLES) * i + Math.PI / PARTICLES
            val dist = maxRadius * p
            val pos = Offset(center.x + (cos(angle) * dist).toFloat(), center.y + (sin(angle) * dist).toFloat())
            val color = if (i % 2 == 0) accent else dim
            drawCircle(color = color.copy(alpha = color.alpha * alpha), radius = particleRadius, center = pos)
        }
    }
}

private const val PARTICLES = 8
