package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

val BlackBackground = Color(0xFF000000)
val DarkGrayBackground = Color(0xFF1E1E1E)
val LightGrayText = Color(0xFF888888)
val WhiteText = Color(0xFFFFFFFF)

// Point Colors for Habits
val HabitOrange = Color(0xFFFF9800)
val HabitSkyBlue = Color(0xFF03A9F4)
val HabitPurple = Color(0xFF9C27B0)
val HabitGreen = Color(0xFF4CAF50)
val HabitRed = Color(0xFFF44336)
val HabitYellow = Color(0xFFFFEB3B)

val DarkColorScheme = darkColorScheme(
    primary = HabitOrange,
    background = BlackBackground,
    surface = DarkGrayBackground,
    onPrimary = WhiteText,
    onBackground = WhiteText,
    onSurface = WhiteText
)
