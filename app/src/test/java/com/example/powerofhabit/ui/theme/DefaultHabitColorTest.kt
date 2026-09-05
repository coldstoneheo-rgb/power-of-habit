package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.powerofhabit.data.local.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 데이터 계층의 기본 습관색 문자열과 테마의 HabitOrange는 같은 색이어야 한다(옛 DB 가져오기 폴백이 이 문자열을 쓴다). */
class DefaultHabitColorTest {
    @Test
    fun defaultThemeColorHex_matchesHabitOrange() {
        // android.graphics.Color.parseColor는 JVM 테스트에서 스텁이라 직접 파싱한다
        val rgb = HabitEntity.DEFAULT_THEME_COLOR_HEX.removePrefix("#").toLong(16)
        assertEquals(HabitOrange, Color(0xFF000000L or rgb))
    }

    @Test
    fun primaryToken_isBrandOrange_inBothThemes_andReadableOnItself() {
        assertEquals(HabitOrange, DarkTokens.primary)
        assertEquals(HabitOrange, LightTokens.primary)
        for (tokens in listOf(DarkTokens, LightTokens)) {
            // 버튼 글자(onAccent) 대 primary 배경: AA 본문 기준 4.5:1
            val ratio = HabitColorTokens.contrastRatio(tokens.onAccent(tokens.primary), tokens.primary)
            assertTrue("dark=${tokens.isDark} ratio=$ratio", ratio >= 4.5f)
        }
    }
}
