package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Radius
import com.example.powerofhabit.ui.theme.Space

/**
 * 연속 수행 위젯 (가이드 B7 "스트릭 바"): 높이 24 · pill · 트랙 layer3 · 최고 기록은 액센트, 현재는 accentDim.
 */
@Composable
fun StreakWidget(
    currentStreak: Int,
    maxStreak: Int,
    themeColor: Color,
    unitLabel: String = "일"
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
        StreakBar("현재 연속", currentStreak, maxStreak, unitLabel, HabitTheme.colors.accentDim(themeColor))
        StreakBar("최고 연속", maxStreak, maxStreak, unitLabel, themeColor)
    }
}

@Composable
private fun StreakBar(label: String, days: Int, maxDays: Int, unitLabel: String, color: Color) {
    val fraction = if (maxDays > 0) days.toFloat() / maxDays else 0f

    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = HabitTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$days$unitLabel",
                color = HabitTheme.colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(HabitTheme.colors.bgLayer3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
            }
        }
    }
}
