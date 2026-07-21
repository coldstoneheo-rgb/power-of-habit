package com.example.powerofhabit.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                    },
                    onDeleteHabit = {
                        viewModel.deleteHabit(successState.habit, onBack)
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
    onUpdateRecordForDate: (String, String, Float?) -> Unit,
    onDeleteHabit: () -> Unit
) {
    val context = LocalContext.current
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("주") }
    
    // 1) EMA score list & dates calculation with Filter
    val (filteredScores, filteredDates) = remember(records, selectedFilter) {
        val today = LocalDate.now()
        val startDate = if (records.isNotEmpty()) {
            records.mapNotNull { try { LocalDate.parse(it.date) } catch (e: Exception) { null } }.minOrNull() ?: today.minusDays(14)
        } else today.minusDays(14)

        val recordsMap = records.associateBy { it.date }
        val dateScoreMap = mutableMapOf<LocalDate, Float>()

        var currentEma = 0f
        val alpha = 0.15f
        var d = startDate
        while (!d.isAfter(today)) {
            val rec = recordsMap[d.toString()]
            if (rec != null) {
                val target = when (rec.status) {
                    "COMPLETED" -> 100f
                    "FAILED" -> 0f
                    else -> currentEma
                }
                currentEma = if (currentEma == 0f) target else currentEma * (1 - alpha) + target * alpha
            }
            dateScoreMap[d] = currentEma
            d = d.plusDays(1)
        }

        val entries = dateScoreMap.entries.sortedBy { it.key }
        if (entries.isEmpty()) listOf(0f) to listOf("오늘") else {
            when (selectedFilter) {
                "일" -> {
                    val recent = entries.takeLast(12)
                    recent.map { it.value } to recent.map { "${it.key.dayOfMonth}일" }
                }
                "주" -> {
                    val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())
                    val grouped = entries.groupBy { "${it.key.year}-W${it.key.get(weekFields.weekOfWeekBasedYear())}" }
                    val scores = grouped.map { it.value.last().value }
                    val dates = grouped.map { "${it.value.first().key.monthValue}월 ${it.value.first().key.dayOfMonth}일" }
                    scores.takeLast(8) to dates.takeLast(8)
                }
                "월" -> {
                    val grouped = entries.groupBy { "${it.key.year}-${it.key.monthValue}" }
                    val scores = grouped.map { it.value.last().value }
                    val dates = grouped.map { "${it.value.first().key.monthValue}월" }
                    scores.takeLast(12) to dates.takeLast(12)
                }
                "분기" -> {
                    val grouped = entries.groupBy {
                        val q = (it.key.monthValue - 1) / 3 + 1
                        "${it.key.year}-Q$q"
                    }
                    val scores = grouped.map { it.value.last().value }
                    val dates = grouped.map { "${it.value.first().key.monthValue}월" }
                    scores.takeLast(8) to dates.takeLast(8)
                }
                "년" -> {
                    val grouped = entries.groupBy { "${it.key.year}" }
                    val scores = grouped.map { it.value.last().value }
                    val dates = grouped.map { "${it.key}년" }
                    scores.takeLast(5) to dates.takeLast(5)
                }
                else -> {
                    val recent = entries.takeLast(12)
                    recent.map { it.value } to recent.map { "${it.key.dayOfMonth}일" }
                }
            }
        }
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
    
    // 3) Progress calculation (Monthly goal completion rate)
    val progress = remember(records) {
        val today = LocalDate.now()
        val currentMonthPrefix = today.toString().substring(0, 7) // "YYYY-MM"
        val completedCount = records.count { it.date.startsWith(currentMonthPrefix) && it.status == "COMPLETED" }
        val elapsedDaysInMonth = today.dayOfMonth
        if (elapsedDaysInMonth > 0) (completedCount.toFloat() / elapsedDaysInMonth).coerceIn(0f, 1f) else 0f
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
        // Navigation header (Colorized with themeColor)
        Surface(
            color = themeColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = habit.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { onNavigateToEdit(habit.habitId) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("CSV 내보내기", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                exportHabitRecordsToCsv(context, habit, records)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Habit Header Info (Sub Header Chips)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = habit.question,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            val targetText = remember(habit) {
                if (habit.habitType == "VALUE" && habit.targetValue != null) {
                    val valStr = if (habit.targetValue % 1f == 0f) "${habit.targetValue.toInt()}" else "${habit.targetValue}"
                    "$valStr ${habit.unit ?: ""}".trim()
                } else null
            }

            val freqText = remember(habit) {
                when (habit.frequencyType) {
                    "DAILY" -> "매일"
                    "INTERVAL" -> "${habit.frequencyValue}일마다"
                    "WEEKLY_COUNT" -> "주 ${habit.frequencyValue}회"
                    "MONTHLY_COUNT" -> "월 ${habit.frequencyValue}회"
                    "COUNT_IN_DAYS" -> {
                        val parts = habit.frequencyValue.split("/")
                        if (parts.size == 2) "${parts[1]}일내 ${parts[0]}회" else "매일"
                    }
                    else -> "매일"
                }
            }

            val reminderText = remember(habit) {
                if (habit.isReminderEnabled) {
                    habit.reminderTime ?: "09:00"
                } else "OFF"
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (targetText != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↑ ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(targetText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅 ", fontSize = 13.sp)
                        Text(freqText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (reminderText == "OFF") "🔕 " else "🔔 ", fontSize = 13.sp)
                        Text(reminderText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
            
            if (!habit.memo.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = habit.memo,
                    color = LightGrayText.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
        
        // Analytics Widgets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Quick Summary (Donut Chart & 2x2 Stats Dashboard)
            CardSection(title = "한눈에 보기") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 EMA 점수는 최근 완료 여부에 더 가중치를 둔 습관 형성 정도(0~100점)를 나타냅니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            letterSpacing = -0.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart - Shows EMA Score
                        val currentEmaScore = filteredScores.last()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(68.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { currentEmaScore / 100f },
                                    color = themeColor,
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.fillMaxSize(),
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                Text(
                                    text = "${currentEmaScore.toInt()}점",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "EMA 점수",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                letterSpacing = -0.5.sp
                            )
                        }

                        // 2x2 Grid - Switch EMA with Monthly Progress %
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatCard(label = "이번 달 달성률", value = "${(progress * 100).toInt()}%", color = themeColor, modifier = Modifier.weight(1f))
                                StatCard(label = "이번 달 완료", value = "${records.count { it.date.startsWith(LocalDate.now().toString().substring(0, 7)) && it.status == "COMPLETED" }}회", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatCard(label = "올해 누적", value = "${records.count { it.date.startsWith(LocalDate.now().toString().substring(0, 4)) && it.status == "COMPLETED" }}회", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                StatCard(label = "전체 완료", value = "${records.count { it.status == "COMPLETED" }}회", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 2. Habit Score Trend
            CardSection(title = "점수 추이") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시간 경과에 따른 습관 지수 변화 그래프입니다.",
                            color = LightGrayText,
                            fontSize = 12.sp,
                            letterSpacing = -0.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Filter Dropdown
                        var showFilterMenu by remember { mutableStateOf(false) }
                        Box {
                            TextButton(
                                onClick = { showFilterMenu = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = themeColor)
                            ) {
                                Text(text = "$selectedFilter ▾", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("일", "주", "월", "분기", "년").forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            selectedFilter = filter
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HabitScoreWidget(
                        scores = filteredScores,
                        dates = filteredDates,
                        selectedFilter = selectedFilter,
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
            CardSection(title = "연간 빈도 매트릭스") {
                HeatmapWidget(
                    frequencies = heatmapFrequencies,
                    themeColor = themeColor
                )
            }

            // 하단 스마트폰 네비게이션 바 및 메뉴 버튼 영역 겹침 방지 패딩
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
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
                        val computedStatus = if (habit.habitType == "VALUE" && status != "NONE" && status != "SKIPPED") {
                            val targetVal = habit.targetValue
                            if (targetVal != null) {
                                if (value != null && value >= targetVal) "COMPLETED" else "FAILED"
                            } else {
                                status
                            }
                        } else {
                            status
                        }
                        onUpdateRecordForDate(date.toString(), computedStatus, value)
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

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "습관 삭제",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Text(
                    text = "정말로 이 습관을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
                    color = LightGrayText,
                    fontSize = 14.sp,
                    letterSpacing = -0.5.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteHabit()
                    }
                ) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = -0.5.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
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

private fun exportHabitRecordsToCsv(context: Context, habit: HabitEntity, records: List<HabitRecordEntity>) {
    try {
        val csvContent = java.lang.StringBuilder().apply {
            append('\uFEFF') // Excel 한글 깨짐 방지를 위한 UTF-8 BOM 추가
            append("Date,Status,Value (${habit.unit ?: ""})\n")
            records.sortedBy { it.date }.forEach { record ->
                append("${record.date},${record.status},${record.inputValue ?: ""}\n")
            }
        }.toString()
        
        // 파일명에 안전한 문자(영어, 숫자, 한글)만 허용하고 나머지는 언더스코어(_)로 치환
        val sanitizedTitle = habit.title.replace(Regex("[^a-zA-Z0-9가-힣]"), "_")
        val fileName = "habit_${sanitizedTitle}_records.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvContent)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${habit.title} Habit Records")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "CSV 내보내기"))
    } catch (e: Exception) {
        Toast.makeText(context, "CSV 내보내기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
