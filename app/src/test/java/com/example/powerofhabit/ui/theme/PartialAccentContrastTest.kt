package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.powerofhabit.widget.HabitWidgets
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 결정 기록 2026-09-05 결정 1·2: 기준미달 색은 바탕 위에서 읽혀야 한다.
 * 앱(다크/라이트)은 AA 4.5:1, 위젯(85% 웜 블랙 배경 ≈ bg.base)은 3:1을 팔레트 36색 전부에 대해 고정한다.
 * 캘린더의 기준미달 칸(35% 틴트 위 text.primary 글자)도 AA를 지켜야 한다.
 */
class PartialAccentContrastTest {

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
            assertTrue("same as accent: $accent", partial != accent)
            assertTrue("same as disabled: $accent", partial != DarkTokens.textDisabled)
        }
    }

    @Test
    fun widgetPartial_isReadableOnWidgetBackground() {
        // 실제로 위젯이 그리는 함수(HabitWidgets.Colors.partial)를 검사한다
        for ((_, accent) in PremiumMatteColors) {
            val partial = HabitWidgets.Colors.partial(accent)
            val ratio = HabitColorTokens.contrastRatio(partial, DarkTokens.bgBase)
            assertTrue("accent=$accent ratio=$ratio", ratio >= 3f)
        }
    }

    @Test
    fun calendarPartialCell_digitIsReadableOverTint() {
        // HistoryCalendarWidget: 기준미달 칸 = partialAccent @35% over bgLayer2, 글자 = text.primary
        for ((_, accent) in PremiumMatteColors) {
            for (tokens in listOf(DarkTokens, LightTokens)) {
                val disc = tokens.partialAccent(accent).copy(alpha = 0.35f).compositeOver(tokens.bgLayer2)
                val ratio = HabitColorTokens.contrastRatio(tokens.textPrimary, disc)
                assertTrue("accent=$accent dark=${tokens.isDark} ratio=$ratio", ratio >= 4.5f)
            }
        }
    }
}
