package com.example.powerofhabit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 디자인 토큰 (docs/DESIGN_GUIDE.md Part B의 코드 표현).
 *
 * 규칙 요약
 * - 화면은 무채 85% / 텍스트 12% / 액센트 3%. 액센트(습관 색)는 이름·완료 마크·차트·섹션 제목·도넛에만 쓴다.
 * - 깊이는 그림자 대신 bgBase → bgLayer1 → bgLayer2 → bgLayer3 톤 계단으로만. 경계가 꼭 필요할 때만 lineHair.
 * - 모서리는 2단 곡률 차(카드 md 12dp / 내부 요소 sm 8dp)로 스퀘어클 인상을 만든다.
 */
@Immutable
data class HabitColorTokens(
    val isDark: Boolean,
    /** 화면 바닥. */
    val bgBase: Color,
    /** 앱바·하단 영역. */
    val bgLayer1: Color,
    /** 카드·다이얼로그. */
    val bgLayer2: Color,
    /** 카드 내부 강조 블록·입력 필드·바 트랙·칩. */
    val bgLayer3: Color,
    /** 구분선. 항상 알파. */
    val lineHair: Color,
    /** 포커스/선택 테두리. */
    val lineFocus: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** 미완료 마크·비활성. */
    val textDisabled: Color,
    /** 건너뜀 `–`. */
    val statusSkip: Color,
    /** 시스템 오류(삭제·복원 실패 등)에만. 습관 실패에는 쓰지 않는다. */
    val statusError: Color
) {
    /** 바 트랙 위 비강조 구간, 이력 막대. */
    fun accentDim(accent: Color): Color = accent.copy(alpha = 0.45f)

    /** 선택 셀 배경, 캘린더 오늘 표시. */
    fun accentGlow(accent: Color): Color = accent.copy(alpha = 0.12f)

    /** 라이트 모드에서 밝은 액센트(옐로·라임·민트)가 텍스트로 쓰일 때 명도 18% 보정. */
    fun accentForText(accent: Color): Color = if (isDark) accent else lerp(accent, Color.Black, 0.18f)
}

val DarkTokens = HabitColorTokens(
    isDark = true,
    bgBase = Color(0xFF101012),
    bgLayer1 = Color(0xFF17171A),
    bgLayer2 = Color(0xFF1E1E22),
    bgLayer3 = Color(0xFF26262B),
    lineHair = Color(0xFFFFFFFF).copy(alpha = 0.08f),
    lineFocus = Color(0xFFFFFFFF).copy(alpha = 0.16f),
    textPrimary = Color(0xFFF2F2F7),
    textSecondary = Color(0xFF9A9AA3),
    textDisabled = Color(0xFF55555E),
    statusSkip = Color(0xFF9A9AA3),
    statusError = Color(0xFFFF6B6B)
)

val LightTokens = HabitColorTokens(
    isDark = false,
    bgBase = Color(0xFFF4F4F6),
    bgLayer1 = Color(0xFFFFFFFF),
    bgLayer2 = Color(0xFFFFFFFF),
    bgLayer3 = Color(0xFFECECF0),
    lineHair = Color(0xFF000000).copy(alpha = 0.08f),
    lineFocus = Color(0xFF000000).copy(alpha = 0.16f),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6B6B75),
    textDisabled = Color(0xFFB5B5BD),
    statusSkip = Color(0xFF6B6B75),
    statusError = Color(0xFFD64545)
)

val LocalHabitTokens = staticCompositionLocalOf { DarkTokens }

/** `HabitTheme.colors.bgLayer2` 처럼 접근한다. `PowerOfHabitTheme` 안에서만 유효. */
object HabitTheme {
    val colors: HabitColorTokens
        @Composable @ReadOnlyComposable get() = LocalHabitTokens.current
}

/** 간격: 4dp 베이스, 8dp 리듬. */
object Space {
    val s1: Dp = 4.dp
    val s2: Dp = 8.dp
    val s3: Dp = 12.dp
    val s4: Dp = 16.dp
    val s5: Dp = 20.dp
    val s6: Dp = 24.dp
    val s8: Dp = 32.dp

    /** 화면 좌우 패딩. */
    val screenH: Dp = s5
    /** 카드 내부 패딩. */
    val card: Dp = s4
    /** 카드 사이. */
    val cardGap: Dp = s3
}

/** 곡률. xs 셀/칩 · sm 입력·내부 요소 · md 카드(기본) · lg 다이얼로그 · pill 바/필터칩. */
object Radius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 20.dp
    val pill: Dp = 999.dp
}

val HabitShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(28.dp)
)
