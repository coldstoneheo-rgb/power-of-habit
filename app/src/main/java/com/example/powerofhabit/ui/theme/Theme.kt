package com.example.powerofhabit.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * @param applyWindowChrome 상태바 색·아이콘 톤을 이 액티비티 창에 적용할지. 런처 위에 떠 있는 투명 액티비티(위젯 입력)는 false.
 */
@Composable
fun PowerOfHabitTheme(
    darkTheme: Boolean = true,
    applyWindowChrome: Boolean = true,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    // primary는 토큰(tokens.primary) — 버튼·스위치·로딩 등 습관 컨텍스트 밖 강조에만 쓴다.
    // 토큰화는 끝났고, 색 정책(DESIGN_GUIDE B7~B9: 채움 버튼 layer3·primary 중립화)은 별도 디자인 결정으로 남아 있다.
    val colorScheme = remember(tokens) { tokens.toColorScheme() }
    val view = LocalView.current
    if (applyWindowChrome && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = tokens.bgBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalHabitTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HabitShapes,
            content = content
        )
    }
}
