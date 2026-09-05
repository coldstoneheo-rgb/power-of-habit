package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.powerofhabit.data.local.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 브랜드 primary 한 출처 고정: 데이터 계층의 기본 습관색 문자열(옛 DB 가져오기 폴백) == 두 테마의 `primary` 토큰 == HabitOrange,
 * primary 위 잉크(onAccent)가 읽히는지, 그리고 습관색 파싱 폴백 규칙(parseHabitColorOr).
 */
class DefaultHabitColorTest {
    @Test
    fun defaultThemeColorHex_matchesPrimaryToken_inBothThemes() {
        // android.graphics.Color.parseColor는 JVM 테스트에서 스텁이라 직접 파싱한다
        val rgb = HabitEntity.DEFAULT_THEME_COLOR_HEX.removePrefix("#").toLong(16)
        val fromHex = Color(0xFF000000L or rgb)
        assertEquals(DarkTokens.primary, fromHex)
        assertEquals(LightTokens.primary, fromHex)
    }

    // parseHabitColorOr는 android.graphics.Color.parseColor에 의존하는데 JVM 단위 테스트에서는 스텁(항상 0, 예외 없음)이라
    // 여기서 검증할 수 없다 — 파싱·폴백 동작은 androidTest 또는 실기기 몫. 규칙이 한 함수에 있다는 것만 코드 구조로 보장한다.

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
