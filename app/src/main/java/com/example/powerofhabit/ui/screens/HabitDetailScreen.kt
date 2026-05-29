package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.components.widgets.*
import com.example.powerofhabit.ui.theme.BlackBackground
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme
import java.time.YearMonth

@Composable
fun HabitDetailScreen() {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text("Habit Detail", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        
        HabitScoreWidget(
            scores = listOf(10f, 30f, 25f, 60f, 55f, 80f, 90f),
            themeColor = HabitOrange,
        )
        
        StreakWidget(
            currentStreak = 5,
            maxStreak = 12,
            themeColor = HabitOrange,
        )
        
        TargetGoalWidget(
            title = "Monthly Goal",
            progress = 0.75f,
            themeColor = HabitOrange,
        )
        
        HistoryCalendarWidget(
            yearMonth = YearMonth.now(),
            records = emptyMap(), // Dummy
            themeColor = HabitOrange,
        )
        
        // Dummy 7x12 heatmap for a year
        val dummyHeatmap = List(7) { List(12) { (0..10).random() } }
        HeatmapWidget(
            frequencies = dummyHeatmap,
            themeColor = HabitOrange,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HabitDetailScreenPreview() {
    PowerOfHabitTheme {
        HabitDetailScreen()
    }
}
