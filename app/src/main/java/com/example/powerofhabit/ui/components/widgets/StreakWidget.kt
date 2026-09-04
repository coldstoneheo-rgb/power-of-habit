package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.theme.DarkGrayBackground

@Composable
fun StreakWidget(
    currentStreak: Int,
    maxStreak: Int,
    themeColor: Color,
    unitLabel: String = "일"
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StreakBar("현재 연속", currentStreak, maxStreak, unitLabel, themeColor.copy(alpha = 0.8f))
        StreakBar("최고 연속", maxStreak, maxStreak, unitLabel, themeColor)
    }
}

@Composable
private fun StreakBar(label: String, days: Int, maxDays: Int, unitLabel: String, color: Color) {
    val fraction = if (maxDays > 0) days.toFloat() / maxDays else 0f
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.LightGray, fontSize = 14.sp)
            Text("$days$unitLabel", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkGrayBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
            }
        }
    }
}
