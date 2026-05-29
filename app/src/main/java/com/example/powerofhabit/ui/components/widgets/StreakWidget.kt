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
    themeColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StreakBar("Current Streak", currentStreak, maxStreak, themeColor.copy(alpha = 0.8f))
        StreakBar("Max Streak", maxStreak, maxStreak, themeColor)
    }
}

@Composable
private fun StreakBar(label: String, days: Int, maxDays: Int, color: Color) {
    val fraction = if (maxDays > 0) days.toFloat() / maxDays else 0f
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.LightGray, fontSize = 14.sp)
            Text("$days Days", color = Color.White, fontWeight = FontWeight.Bold)
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
                    .fillMaxWidth(fraction.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
            }
        }
    }
}
