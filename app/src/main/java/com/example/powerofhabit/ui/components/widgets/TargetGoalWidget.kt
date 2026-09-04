package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Radius
import com.example.powerofhabit.ui.theme.Space

/**
 * 목표 달성률 위젯 (가이드 B7 "프로그레스 바"): 높이 8 · pill · 트랙 layer3 · 채움 액센트 · 우측 % bodyStrong.
 */
@Composable
fun TargetGoalWidget(
    title: String,
    progress: Float, // 0f to 1f
    themeColor: Color
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = HabitTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${(safeProgress * 100).toInt()}%",
                color = HabitTheme.colors.accentForText(themeColor),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(HabitTheme.colors.bgLayer3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(themeColor)
            )
        }
    }
}
