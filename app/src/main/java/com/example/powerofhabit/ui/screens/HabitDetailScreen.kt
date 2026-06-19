package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.SolidColor
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
import com.example.powerofhabit.ui.theme.MetalBorderBrush
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
                    onNavigateToEdit = onNavigateToEdit,
                    onUpdateRecordForDate = { date, status, value ->
                        viewModel.updateRecordForDate(date, status, value)
                    }
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
    onNavigateToEdit: (Int) -> Unit,
    onUpdateRecordForDate: (String, String, Float?) -> Unit
) {
    val scrollState = rememberScrollState()
    val themeColor = remember(habit.themeColor) {
        try {
            Color(android.graphics.Color.parseColor(habit.themeColor))
        } catch (e: Exception) {
            HabitOrange
        }
    }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDateForEdit by remember { mutableStateOf<LocalDate?>(null) }
    
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
        if (records.isEmpty()) return@remember 0 to 0
        
        val recordsMap = records.associateBy { it.date }
        val sortedDates = records.mapNotNull { 
            try { LocalDate.parse(it.date) } catch (e: Exception) { null }
        }.sorted()
        
        if (sortedDates.isEmpty()) return@remember 0 to 0
        
        val startDate = sortedDates.first()
        val today = LocalDate.now()
        
        var maxStreak = 0
        var tempStreak = 0
        var currentDate = startDate
        
        // Calculate max streak by iterating from startDate to today
        while (!currentDate.isAfter(today)) {
            val dateStr = currentDate.toString()
            val record = recordsMap[dateStr]
            
            if (record != null) {
                when (record.status) {
                    "COMPLETED" -> {
                        tempStreak++
                        if (tempStreak > maxStreak) {
                            maxStreak = tempStreak
                        }
                    }
                    "FAILED" -> {
                        tempStreak = 0
                    }
                    "SKIPPED" -> {
                        // Skipped days do not break or increment the streak
                    }
                }
            } else {
                // Missing record for a past day breaks the streak
                if (currentDate != today) {
                    tempStreak = 0
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        
        // Calculate current streak going backwards from today
        var currentStreak = 0
        var checkDate = today
        while (true) {
            val dateStr = checkDate.toString()
            val record = recordsMap[dateStr]
            if (record != null) {
                when (record.status) {
                    "COMPLETED" -> {
                        currentStreak++
                    }
                    "FAILED" -> {
                        break
                    }
                    "SKIPPED" -> {
                        // Skipped days are ignored, continue backwards
                    }
                }
            } else {
                if (checkDate != today) {
                    break
                }
            }
            checkDate = checkDate.minusDays(1)
            if (checkDate.isBefore(startDate)) {
                break
            }
        }
        
        currentStreak to maxStreak
    }
    val (currentStreak, maxStreak) = streaks
    
    // 3) Progress calculation (Monthly goal completion)
    val progress = remember(records) {
        val today = LocalDate.now()
        val currentMonthPrefix = today.toString().substring(0, 7) // "YYYY-MM"
        val currentMonthRecords = records.filter { it.date.startsWith(currentMonthPrefix) }
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
            if (!habit.memo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = habit.memo,
                    color = LightGrayText.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        
        // Analytics Widgets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. Quick Summary (Donut Chart & 2x2 Stats Dashboard)
            CardSection(title = "한눈에 보기 (Quick Summary)") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 도넛 차트는 이번 달 목표 달성 비율을 나타냅니다. EMA 점수는 최근 완료 여부에 더 가중치를 둔 습관 강도 점수(0~100점)입니다.",
                            color = LightGrayText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            letterSpacing = -0.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    color = themeColor,
                                    strokeWidth = 8.dp,
                                    modifier = Modifier.fillMaxSize(),
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "이번 달 달성률",
                                color = LightGrayText,
                                fontSize = 11.sp,
                                letterSpacing = -0.5.sp
                            )
                        }

                        // 2x2 Grid
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(label = "EMA 점수", value = "${emaScores.last().toInt()}점", color = themeColor, modifier = Modifier.weight(1f))
                                StatCard(label = "이번 달 완료", value = "${records.count { it.date.startsWith(LocalDate.now().toString().substring(0, 7)) && it.status == "COMPLETED" }}회", color = Color.White, modifier = Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(label = "올해 누적", value = "${records.count { it.date.startsWith(LocalDate.now().toString().substring(0, 4)) && it.status == "COMPLETED" }}회", color = Color.White, modifier = Modifier.weight(1f))
                                StatCard(label = "전체 완료", value = "${records.count { it.status == "COMPLETED" }}회", color = Color.White, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 2. Habit Score Trend
            CardSection(title = "점수 추이 (Habit Score Trend)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "시간 경과에 따른 습관 지수 변화 그래프입니다.",
                        color = LightGrayText,
                        fontSize = 12.sp,
                        letterSpacing = -0.5.sp
                    )
                    HabitScoreWidget(
                        scores = emaScores,
                        themeColor = themeColor
                    )
                }
            }
            
            // 3. Streak
            CardSection(title = "연속 기록 (Streak Tracker)") {
                StreakWidget(
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    themeColor = themeColor
                )
            }
            
            // 4. Target Goal Progress
            CardSection(title = "목표 달성률 (Monthly Progress)") {
                TargetGoalWidget(
                    title = "목표 대비 달성도",
                    progress = progress,
                    themeColor = themeColor
                )
            }
            
            // 5. History Calendar with Edit function
            CardSection(title = "실행 이력 (History Calendar)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "💡 날짜를 누르면 과거 실행 기록을 수정할 수 있습니다.",
                        color = LightGrayText,
                        fontSize = 12.sp,
                        letterSpacing = -0.5.sp
                    )
                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Text("<", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = -0.5.sp
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Text(">", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    
                    HistoryCalendarWidget(
                        yearMonth = currentMonth,
                        records = calendarRecords,
                        themeColor = themeColor,
                        onDateClick = { date -> selectedDateForEdit = date }
                    )
                }
            }
            
            // 6. Heatmap Frequencies
            CardSection(title = "연간 빈도 매트릭스 (Yearly Heatmap)") {
                HeatmapWidget(
                    frequencies = heatmapFrequencies,
                    themeColor = themeColor
                )
            }
        }
    }

    // Edit Record Dialog
    selectedDateForEdit?.let { date ->
        val record = records.find { it.date == date.toString() }
        var status by remember { mutableStateOf(record?.status ?: "NONE") }
        var inputValue by remember { mutableStateOf(record?.inputValue?.toString() ?: "") }
        
        AlertDialog(
            onDismissRequest = { selectedDateForEdit = null },
            title = {
                Text(
                    text = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 기록 수정",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "달성 상태를 선택해 주세요.",
                        color = LightGrayText,
                        fontSize = 14.sp,
                        letterSpacing = -0.5.sp
                    )
                    
                    // Status Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "COMPLETED" to "성공",
                            "FAILED" to "실패",
                            "SKIPPED" to "건너뜀",
                            "NONE" to "삭제"
                        ).forEach { (statKey, statLabel) ->
                            val isSelected = status == statKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) themeColor else DarkGrayBackground)
                                    .border(1.dp, if (isSelected) SolidColor(Color.White) else MetalBorderBrush, RoundedCornerShape(8.dp))
                                    .clickable { status = statKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statLabel,
                                    color = if (isSelected) Color.White else LightGrayText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Numeric value input if VALUE type habit
                    if (habit.habitType == "VALUE") {
                        OutlinedTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            label = { Text("수치 입력 (${habit.unit ?: ""})") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = inputValue.toFloatOrNull()
                        onUpdateRecordForDate(date.toString(), status, value)
                        selectedDateForEdit = null
                    }
                ) {
                    Text("저장", color = themeColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDateForEdit = null }) {
                    Text("취소", color = LightGrayText)
                }
            },
            containerColor = DarkGrayBackground,
            titleContentColor = Color.White
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BlackBackground)
            .border(1.dp, MetalBorderBrush, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = LightGrayText,
            fontSize = 11.sp,
            letterSpacing = -0.5.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = -0.5.sp
        )
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = -0.5.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGrayBackground)
                .border(1.dp, MetalBorderBrush, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}
