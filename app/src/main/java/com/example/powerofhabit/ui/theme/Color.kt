package com.example.powerofhabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val BlackBackground = Color(0xFF101012) // Slightly warm black for high-end feel
val DarkGrayBackground = Color(0xFF1C1C1E) // Matte metallic charcoal background
val LightGrayText = Color(0xFF8E8E93)
val WhiteText = Color(0xFFF2F2F7)

// Brushed metal edge brushes and highlights

val MetalBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.15f),
        Color(0xFFFFFFFF).copy(alpha = 0.02f),
        Color(0xFFFFFFFF).copy(alpha = 0.10f)
    )
)

val GoldenMetalBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFDF00),
        Color(0xFFD4AF37),
        Color(0xFFA67C00)
    )
)

val SilverMetalBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE6E6E6),
        Color(0xFFC0C0C0),
        Color(0xFF8C8C8C)
    )
)

val BronzeMetalBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFCD7F32),
        Color(0xFFB87333),
        Color(0xFF804A00)
    )
)

// Point Colors for Habits
val HabitOrange = Color(0xFFFF9800)
val HabitSkyBlue = Color(0xFF03A9F4)
val HabitPurple = Color(0xFF9C27B0)
val HabitGreen = Color(0xFF4CAF50)
val HabitRed = Color(0xFFF44336)
val HabitYellow = Color(0xFFFFEB3B)

// 32 Premium Matte Colors (Hex + Color mapping)
val PremiumMatteColors = listOf(
    "#E57373" to Color(0xFFE57373), // Matte Red
    "#F06292" to Color(0xFFF06292), // Matte Rose
    "#BA68C8" to Color(0xFFBA68C8), // Matte Purple
    "#9575CD" to Color(0xFF9575CD), // Matte Deep Purple
    "#7986CB" to Color(0xFF7986CB), // Matte Indigo
    "#64B5F6" to Color(0xFF64B5F6), // Matte Blue
    "#4FC3F7" to Color(0xFF4FC3F7), // Matte Light Blue
    "#4DD0E1" to Color(0xFF4DD0E1), // Matte Cyan
    "#4DB6AC" to Color(0xFF4DB6AC), // Matte Teal
    "#81C784" to Color(0xFF81C784), // Matte Green
    "#AED581" to Color(0xFFAED581), // Matte Light Green
    "#DCE775" to Color(0xFFDCE775), // Matte Lime
    "#FFF176" to Color(0xFFFFF176), // Matte Yellow
    "#FFD54F" to Color(0xFFFFD54F), // Matte Amber
    "#FFB74D" to Color(0xFFFFB74D), // Matte Orange
    "#FF8A65" to Color(0xFFFF8A65), // Matte Deep Orange
    "#A1887F" to Color(0xFFA1887F), // Matte Brown
    "#E0E0E0" to Color(0xFFE0E0E0), // Matte Silver Grey
    "#90A4AE" to Color(0xFF90A4AE), // Matte Blue Grey
    "#C2B280" to Color(0xFFC2B280), // Matte Sand
    "#8E9F80" to Color(0xFF8E9F80), // Matte Sage
    "#CD7F32" to Color(0xFFCD7F32), // Matte Bronze
    "#B5A642" to Color(0xFFB5A642), // Matte Brass
    "#B87333" to Color(0xFFB87333), // Matte Copper
    "#D4AF37" to Color(0xFFD4AF37), // Matte Gold
    "#808000" to Color(0xFF808000), // Matte Olive
    "#3F51B5" to Color(0xFF3F51B5), // Matte Classic Blue
    "#E040FB" to Color(0xFFE040FB), // Matte Neon Purple
    "#00E676" to Color(0xFF00E676), // Matte Neon Green
    "#26A69A" to Color(0xFF26A69A), // Matte Persian Green
    "#FF7043" to Color(0xFFFF7043), // Matte Coral
    "#A7FFEB" to Color(0xFFA7FFEB)  // Matte Mint
)

val LightBackground = Color(0xFFF2F2F7) // System background light gray
val LightSurface = Color(0xFFFFFFFF) // Surface white
val DarkText = Color(0xFF1C1C1E) // Dark charcoal text

val LightColorScheme = lightColorScheme(
    primary = HabitOrange,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText
)

val DarkColorScheme = darkColorScheme(
    primary = HabitOrange,
    background = BlackBackground,
    surface = DarkGrayBackground,
    onPrimary = WhiteText,
    onBackground = WhiteText,
    onSurface = WhiteText
)
