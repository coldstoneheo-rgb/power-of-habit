package com.example.powerofhabit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 디자인 토큰 (docs/DESIGN_GUIDE.md Part B의 코드 표현).
 *
 * 규칙 요약
 * - 화면은 무채 85% / 텍스트 12% / 액센트 3%. 액센트(습관 색)는 이름·완료 마크·차트·섹션 제목·도넛에만 쓴다.
 * - 깊이는 그림자 대신 bgBase → bgLayer1 → bgLayer2 → bgLayer3 톤 계단으로만. 경계가 꼭 필요할 때만 lineHair.
 * - 모서리는 2단 곡률 차(카드 md 12dp / 내부 요소 sm 8dp)로 스퀘어클 인상을 만든다.
 * - 액센트 위에 올리는 글자는 [onAccent], 바탕 위에 액센트 글자는 [accentForText]로 대비를 보정한다.
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
    /** 입력 필드 등 "있어야 보이는" 테두리. M3 outlineVariant에 매핑. */
    val lineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    /** 미완료 마크·비활성. M3 outline에 매핑. */
    val textDisabled: Color,
    /** 건너뜀 `–`. */
    val statusSkip: Color,
    /** 사용자가 "실패"로 표시한 날. 저채도 적색 — 시스템 오류색과 구분. */
    val statusFail: Color,
    /** 시스템 오류(삭제·복원 실패 등)에만. */
    val statusError: Color,
    /**
     * 브랜드 강조(primary): 버튼·스위치·로딩·오늘 날짜 등 **습관 컨텍스트 밖** 강조와, 습관색을 파싱할 수 없을 때의 폴백.
     * 습관 하나의 색은 항상 `habit.themeColor`(accent)를 쓴다. M3 colorScheme.primary에 매핑. 두 테마 모두 브랜드 오렌지.
     */
    val primary: Color
) {
    /** 바 트랙 위 비강조 구간, 이력 막대. */
    fun accentDim(accent: Color): Color = accent.copy(alpha = 0.45f)

    /** 선택 셀 배경, 캘린더 오늘 표시. */
    fun accentGlow(accent: Color): Color = accent.copy(alpha = 0.12f)

    /**
     * "수행했지만 기준 미달" 색: 액센트를 text.secondary 쪽으로 60% 당겨 채도·명도를 낮춘 습관색 계열(결정 기록 2026-09-05 결정 1).
     * 두 테마 모두 채도가 약 40% 남아 회색 X(미수행)와 구분되고, 바탕 대비는 [accentForText]로 AA를 맞춘다.
     */
    fun partialAccent(accent: Color, background: Color = bgBase): Color =
        accentForText(lerp(accent, textSecondary, PARTIAL_MIX), background)

    /** 액센트로 채운 면 위의 글자/아이콘 색. 액센트 명도에 따라 어두운/밝은 잉크를 고른다. */
    fun onAccent(accent: Color): Color = if (accent.luminance() > 0.4f) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)

    /**
     * 바탕(bgBase) 위에 액센트 글자를 쓸 때 4.5:1(AA)이 될 때까지 다크는 밝게, 라이트는 어둡게 당긴다.
     * 파스텔·옐로 계열이 흰 바탕에서 사라지거나, 진한 인디고가 검은 바탕에서 묻히는 것을 막는다.
     */
    fun accentForText(accent: Color, background: Color = bgBase): Color {
        val toward = if (isDark) Color.White else Color.Black
        var c = accent
        var i = 0
        while (contrastRatio(c, background) < 4.5f && i < 12) {
            c = lerp(c, toward, 0.12f)
            i++
        }
        return c
    }

    /** M3 colorScheme. surfaceContainer* 계열까지 톤 계단에 매핑해 기본 컴포넌트가 팔레트 밖 회색을 쓰지 않게 한다. */
    fun toColorScheme(primary: Color = this.primary, onPrimary: Color = onAccent(primary)): ColorScheme {
        val base = if (isDark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primary,
            onPrimary = onPrimary,
            background = bgBase,
            onBackground = textPrimary,
            surface = bgLayer2,
            onSurface = textPrimary,
            surfaceVariant = bgLayer3,
            onSurfaceVariant = textSecondary,
            surfaceContainerLowest = bgBase,
            surfaceContainerLow = bgLayer1,
            surfaceContainer = bgLayer2,
            surfaceContainerHigh = bgLayer3,
            surfaceContainerHighest = bgLayer3,
            surfaceTint = Color.Transparent,
            outline = textDisabled,
            outlineVariant = lineStrong,
            error = statusError,
            onError = onAccent(statusError)
        )
    }

    companion object {
        /** 기준미달 색을 만들 때 text.secondary 쪽으로 당기는 비율. */
        const val PARTIAL_MIX = 0.6f

        /** WCAG 대비비. 알파가 있는 색은 배경 위에 합성해 계산한다. */
        fun contrastRatio(fg: Color, bg: Color): Float {
            val f = fg.compositeOver(bg).luminance()
            val b = bg.luminance()
            val (hi, lo) = if (f > b) f to b else b to f
            return (hi + 0.05f) / (lo + 0.05f)
        }
    }
}

val DarkTokens = HabitColorTokens(
    isDark = true,
    bgBase = Color(0xFF101012),
    bgLayer1 = Color(0xFF17171A),
    bgLayer2 = Color(0xFF1E1E22),
    bgLayer3 = Color(0xFF26262B),
    lineHair = Color(0xFFFFFFFF).copy(alpha = 0.08f),
    lineFocus = Color(0xFFFFFFFF).copy(alpha = 0.16f),
    lineStrong = Color(0xFFFFFFFF).copy(alpha = 0.28f),
    textPrimary = Color(0xFFF2F2F7),
    textSecondary = Color(0xFF9A9AA3),
    textDisabled = Color(0xFF55555E),
    statusSkip = Color(0xFF9A9AA3),
    statusFail = Color(0xFFD26A6A),
    statusError = Color(0xFFFF6B6B),
    primary = HabitOrange
)

val LightTokens = HabitColorTokens(
    isDark = false,
    bgBase = Color(0xFFF4F4F6),
    bgLayer1 = Color(0xFFFAFAFB),
    bgLayer2 = Color(0xFFFFFFFF),
    bgLayer3 = Color(0xFFECECF0),
    lineHair = Color(0xFF000000).copy(alpha = 0.08f),
    lineFocus = Color(0xFF000000).copy(alpha = 0.16f),
    lineStrong = Color(0xFF000000).copy(alpha = 0.24f),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF6B6B75),
    textDisabled = Color(0xFFB5B5BD),
    statusSkip = Color(0xFF6B6B75),
    statusFail = Color(0xFFB94A4A),
    statusError = Color(0xFFD64545),
    primary = HabitOrange
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
}

/** 곡률. 모양(Shape)이 필요하면 `MaterialTheme.shapes`를 쓰고, 여기는 값이 필요한 곳(pill 등)에만 쓴다. */
object Radius {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 20.dp
    val pill: Dp = 999.dp
}

/** M3 shapes: extraSmall 셀/칩 · small 입력·내부 요소 · medium 카드 · large 다이얼로그. */
val HabitShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * 습관색 hex → Color. 파싱할 수 없으면 테마의 primary(브랜드 오렌지)로 폴백한다. 메인 목록·상세·입력 다이얼로그가 공용.
 * (위젯은 컴포지션 밖이라 `HabitWidgets.parseThemeColor`가 `DarkTokens.primary`로 같은 규칙을 쓴다.)
 */
@Composable
fun habitAccent(hex: String?): Color {
    val fallback = HabitTheme.colors.primary
    return remember(hex, fallback) {
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { fallback }
    }
}
