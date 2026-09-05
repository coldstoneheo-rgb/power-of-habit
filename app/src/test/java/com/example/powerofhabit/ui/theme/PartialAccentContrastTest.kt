package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 결정 기록 2026-09-05 결정 1·2: 기준미달 색은 바탕 위에서 읽혀야 한다.
 * 앱(다크/라이트)은 AA 4.5:1, 위젯(85% 웜 블랙 배경, 최악의 경우 검정 배경)은 3:1을 팔레트 36색 전부에 대해 고정한다.
 */
class PartialAccentContrastTest {

    private val widgetBg = Color(0xFF101012)

    @Test
    fun partialAccent_meetsAA_onBothThemes() {
        for ((_, accent) in PremiumMatteColors) {
            for (tokens in listOf(DarkTokens, LightTokens)) {
                val c = tokens.partialAccent(accent)
                val ratio = HabitColorTokens.contrastRatio(c, tokens.bgBase)
                assertTrue("accent=$accent theme dark=${tokens.isDark} ratio=$ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun partialAccent_isDistinctFromSuccessAndDisabled() {
        for ((_, accent) in PremiumMatteColors) {
            val partial = DarkTokens.partialAccent(accent)
            // 성공(액센트 원색)과는 색이 달라야 하고, 미수행(text.disabled)과도 달라야 한다
            assertTrue("same as accent: $accent", partial != accent)
            assertTrue("same as disabled: $accent", partial != DarkTokens.textDisabled)
        }
    }

    @Test
    fun widgetPartial_isReadableOnWidgetBackground() {
        for ((_, accent) in PremiumMatteColors) {
            val partial = lerp(accent, DarkTokens.textSecondary, HabitColorTokens.PARTIAL_MIX)
            val ratio = HabitColorTokens.contrastRatio(partial, widgetBg)
            assertTrue("accent=$accent ratio=$ratio", ratio >= 3f)
        }
    }
}
