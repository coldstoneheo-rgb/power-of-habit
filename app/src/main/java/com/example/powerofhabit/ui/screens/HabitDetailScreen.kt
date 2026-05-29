package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.ui.components.widgets.*
import com.example.powerofhabit.ui.theme.BlackBackground
import com.example.powerofhabit.ui.theme.DarkGrayBackground
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.LightGrayText
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HabitDetailScreen(
    habitId: Int,
    onBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(habitId) {
        viewModel.setHabitId(habitId)
    }
    
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
    ) {
        when (state) {
            HabitDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HabitOrange)
                }
            }
            is HabitDetailUiState.Success -> {
                val successState = state as HabitDetailUiState.Success
                HabitDetailContent(
                    habit = successState.habit,
                    records = successState.records,
                    onBack = onBack,
                    onNavigateToEdit = onNavigateToEdit
                )
            }
            is HabitDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${(state as HabitDetailUiState.Error).throwable.localizedMessage}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DarkGrayBackground)) {
                            Text("Back", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitDetailContent(
    habit: HabitEntity,
    records: List<HabitRecordEntity>,
    onBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val themeColor = remember(habit.themeColor) {
        try {
            Color(android.graphics.Color.parseColor(habit.themeColor))
        } catch (e: Exception) {
            HabitOrange
        }
    }
    
    // 1) EMA score list calculation
    val emaScores = remember(records) {
        val scores = mutableListOf<Float>()
        var currentEma = 0f
        var first = true
        records.sortedBy { it.date }.forEach { record ->
            if (record.status == "SKIPPED") {
                if (!first) scores.add(currentEma)
                return@forEach
            }
            val value = if (record.status == "COMPLETED") 100f else 0f
            if (first) {
                currentEma = value
                first = false
            } else {
                currentEma = 0.2f * value + 0.8f * currentEma
            }
            scores.add(currentEma)
        }
        if (scores.isEmpty()) listOf(0f) else scores
    }
    
    // 2) Streak calculation
    val streaks = remember(records) {
        var maxStreak = 0
        var tempStreak = 0
        records.sortedBy { it.date }.forEach { record ->
            if (record.status == "COMPLETED") {
                tempStreak++
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
            } else if (record.status == "FAILED") {
                tempStreak = 0
            }
        }
        val sortedRecords = records.sortedByDescending { it.date }
        var currentStreak = 0
        for (record in sortedRecords) {
            if (record.status == "COMPLETED") {
                currentStreak++
            } else if (record.status == "FAILED") {
                break
            }
        }
        currentStreak to maxStreak
    }
    val (currentStreak, maxStreak) = streaks
    
    // 3) Progress calculation (Monthly goal completion)
    val progress = remember(records) {
        val today = LocalDate.now()
        val currentMonthRecords = records.filter {
            try {
                val date = LocalDate.parse(it.date)
                date.monthValue == today.monthValue && date.year == today.year
            } catch (e: Exception) {
                false
            }
        }
        val completedCount = currentMonthRecords.count { it.status == "COMPLETED" }
        val totalCount = currentMonthRecords.count { it.status != "SKIPPED" }
        if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    }
    
    // 4) Calendar records map
    val calendarRecords = remember(records) {
        records.associate {
            try {
                LocalDate.parse(it.date) to it.status
            } catch (e: Exception) {
                LocalDate.now() to "NONE"
            }
        }
    }
    
    // 5) Heatmap calculation
    val heatmapFrequencies = remember(records) {
        val today = LocalDate.now()
        val oneYearAgo = today.minusWeeks(51).with(java.time.DayOfWeek.SUNDAY)
        val matrix = List(7) { MutableList(52) { 0 } }
        records.forEach { record ->
            try {
                val date = LocalDate.parse(record.date)
                if (!date.isBefore(oneYearAgo) && !date.isAfter(today)) {
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(oneYearAgo, date).toInt()
                    if (daysBetween >= 0) {
                        val week = daysBetween / 7
                        val dayOfWeek = date.dayOfWeek.value % 7
                        if (week in 0..51 && dayOfWeek in 0..6) {
                            if (record.status == "COMPLETED") {
                                matrix[dayOfWeek][week] = 10
                            } else if (record.status == "SKIPPED") {
                                matrix[dayOfWeek][week] = 3
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        matrix
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = { onNavigateToEdit(habit.habitId) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
            }
        }
        
        // Habit Header Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = habit.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = habit.question,
                color = LightGrayText,
                fontSize = 16.sp
            )
        }
        
        // Analytics Widgets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Habit Score Trend
            CardSection(title = "Habit Score (EMA Trend)") {
                HabitScoreWidget(
                    scores = emaScores,
                    themeColor = themeColor
                )
            }
            
            // Streak
            CardSection(title = "Streak Tracker") {
                StreakWidget(
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    themeColor = themeColor
                )
            }
            
            // Target Goal Progress
            CardSection(title = "Monthly Progress") {
                TargetGoalWidget(
                    title = "Target Completion",
                    progress = progress,
                    themeColor = themeColor
                )
            }
            
            // History Calendar
            CardSection(title = "Completion History") {
                HistoryCalendarWidget(
                    yearMonth = YearMonth.now(),
                    records = calendarRecords,
                    themeColor = themeColor
                )
            }
            
            // Heatmap Frequencies
            CardSection(title = "Yearly Frequency Matrix") {
                HeatmapWidget(
                    frequencies = heatmapFrequencies,
                    themeColor = themeColor
                )
            }
        }
    }
}

@Composable
private fun CardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGrayBackground)
                .padding(16.dp)
        ) {
            content()
        }
    }
}
