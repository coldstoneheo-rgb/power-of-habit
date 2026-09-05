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
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.domain.stats.HabitFrequency
import com.example.powerofhabit.domain.stats.HabitStatsCalculator
import com.example.powerofhabit.ui.components.ValueInput
import com.example.powerofhabit.ui.components.ValueInputField
import com.example.powerofhabit.ui.components.widgets.*
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Space
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
            .background(HabitTheme.colors.bgBase)
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
            HabitDetailUiState.NotFound -> {
                // 위젯 딥링크로 삭제된 습관에 들어온 경우. 짧게 알리고 자동으로 돌아간다 — 단 한 번만 pop(이중 pop 방지).
                var navigated by remember { mutableStateOf(false) }
                val goBackOnce = {
                    if (!navigated) { navigated = true; onBack() }
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    goBackOnce()
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "삭제되었거나 찾을 수 없는 습관입니다",
                            color = HabitTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Space.s2))
                        Text(
                            text = "목록으로 돌아갑니다",
                            color = HabitTheme.colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Space.s4))
                        Button(onClick = goBackOnce, colors = ButtonDefaults.buttonColors(containerColor = HabitTheme.colors.bgLayer3)) {
                            Text("돌아가기", color = HabitTheme.colors.textPrimary)
                        }
                    }
                }
            }
            is HabitDetailUiState.Error -> {
                // 실제 DB/IO 예외: 삭제와 구분해 원인을 보여주고 자동 이동은 하지 않는다(ViewModel이 로그 남김).
                val message = (state as HabitDetailUiState.Error).throwable.localizedMessage ?: "알 수 없는 오류"
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "기록을 불러오지 못했습니다",
                            color = HabitTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Space.s2))
                        Text(
                            text = message,
                            color = HabitTheme.colors.statusError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Space.s4))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = HabitTheme.colors.bgLayer3)) {
                            Text("돌아가기", color = HabitTheme.colors.textPrimary)
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
    
    // 1~3) 빈도 인지형 통계 (EMA 점수·스트릭·달성률) — 계산은 HabitStatsCalculator가 담당
    val frequency = remember(habit.frequencyType, habit.frequencyValue) {
        HabitFrequency.parse(habit.frequencyType, habit.frequencyValue)
    }
    val today = LocalDate.now() // remember 키에 포함해 자정이 지나면 재계산되게 한다
    val stats = remember(records, habit, today) {
        HabitStatsCalculator.compute(habit, records, today) // 홈 점수·뱃지와 같은 오버로드
    }
    val (filteredScores, filteredDates) = remember(stats, selectedFilter) {
        HabitStatsCalculator.groupScores(stats.dailyScores, selectedFilter)
    }
    val currentStreak = stats.currentStreak
    val maxStreak = stats.maxStreak
    val progress = stats.monthProgress

    // 4) Calendar records map — 표시 상태(RecordOutcome 이름). 기록이 있는데 NONE이면 캘린더는 "실패"로 그린다.
    val calendarRecords = remember(records, habit) { // habit 전체를 키로 — 방향만 바뀐 편집도 다시 판정
        records.mapNotNull { r ->
            try {
                LocalDate.parse(r.date) to RecordOutcomes.of(habit.habitType, r.status, r.inputValue, habit.targetValue, habit.targetType).name
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }
    
    // 5) Heatmap calculation
    val heatmapFrequencies = remember(records, habit) {
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
                            // 캘린더·위젯과 같은 판정(RecordOutcomes)을 쓴다: 성공 10 / 기준미달 6 / 건너뜀 3 / 미수행 0
                            matrix[dayOfWeek][week] = when (RecordOutcomes.of(habit.habitType, record.status, record.inputValue, habit.targetValue, habit.targetType)) {
                                com.example.powerofhabit.domain.RecordOutcome.SUCCESS -> 10
                                com.example.powerofhabit.domain.RecordOutcome.PARTIAL -> 6
                                com.example.powerofhabit.domain.RecordOutcome.SKIPPED -> 3
                                com.example.powerofhabit.domain.RecordOutcome.NONE -> 0
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
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = HabitTheme.colors.onAccent(themeColor))
                }
                Text(
                    text = habit.title,
                    color = HabitTheme.colors.onAccent(themeColor),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { onNavigateToEdit(habit.habitId) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = HabitTheme.colors.onAccent(themeColor))
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = HabitTheme.colors.onAccent(themeColor))
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
                            text = { Text("삭제", color = HabitTheme.colors.statusError) },
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
                if (habit.habitType == "VALUE") RecordOutcomes.targetLabel(habit.targetValue, habit.unit, habit.targetType) else null
            }

            val freqText = remember(frequency) { frequency.label }

            val reminderText = remember(habit) {
                if (habit.isReminderEnabled) {
                    habit.reminderTime ?: "09:00"
                } else "OFF"
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = HabitTheme.colors.bgLayer3
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
                    color = HabitTheme.colors.textSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
        
        // Analytics Widgets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screenH, vertical = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s5)
        ) {
            // 1. Quick Summary (Donut Chart & 2x2 Stats Dashboard)
            CardSection(accent = themeColor, title = "한눈에 보기") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(HabitTheme.colors.bgLayer3)
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
            CardSection(accent = themeColor, title = "점수 추이") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "시간 경과에 따른 습관 지수 변화 그래프입니다.",
                            color = HabitTheme.colors.textSecondary,
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
            CardSection(accent = themeColor, title = "연속 기록 (Streak Tracker)") {
                StreakWidget(
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    unitLabel = stats.streakUnit,
                    themeColor = themeColor
                )
            }
            
            // 4. Target Goal Progress
            CardSection(accent = themeColor, title = "목표 달성률 (Monthly Progress)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TargetGoalWidget(
                        title = "이번 달 달성률 (${frequency.label})",
                        progress = progress,
                        themeColor = themeColor
                    )
                    TargetGoalWidget(
                        title = "${stats.currentPeriod.label} ${stats.currentPeriod.completed}/${stats.currentPeriod.required}회",
                        progress = stats.currentPeriod.fraction,
                        themeColor = themeColor
                    )
                }
            }
            
            // 5. History Calendar with Edit function
            CardSection(accent = themeColor, title = "실행 이력 (History Calendar)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "💡 날짜를 누르면 과거 실행 기록을 수정할 수 있습니다.",
                        color = HabitTheme.colors.textSecondary,
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
                            Text("<", color = HabitTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                            color = HabitTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = -0.5.sp
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Text(">", color = HabitTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            CardSection(accent = themeColor, title = "연간 빈도 매트릭스") {
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
        // record를 키로 — 다이얼로그가 열린 채 위젯 등에서 같은 날 기록이 바뀌면 초기값도 따라간다(옛 값으로 덮어쓰지 않게)
        var status by remember(record) { mutableStateOf(record?.status ?: "NONE") }
        var inputValue by remember(record) { mutableStateOf(ValueInput.format(record?.inputValue)) }
        
        AlertDialog(
            onDismissRequest = { selectedDateForEdit = null },
            title = {
                Text(
                    text = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 기록 수정",
                    color = HabitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "달성 상태를 선택해 주세요.",
                        color = HabitTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        letterSpacing = -0.5.sp
                    )
                    
                    // Status Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 수치형은 값이 성공/기준미달을 결정하므로 성공·실패 칩 대신 "기록" 칩 하나만 보인다.
                        val chips = if (habit.habitType == "VALUE") {
                            listOf("COMPLETED" to "기록", "SKIPPED" to "건너뜀", "NONE" to "삭제")
                        } else {
                            listOf("COMPLETED" to "성공", "FAILED" to "실패", "SKIPPED" to "건너뜀", "NONE" to "삭제")
                        }
                        chips.forEach { (statKey, statLabel) ->
                            val isSelected = status == statKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (isSelected) themeColor else HabitTheme.colors.bgLayer3)
                                    .border(1.dp, SolidColor(if (isSelected) Color.Transparent else HabitTheme.colors.lineHair), MaterialTheme.shapes.small)
                                    .clickable { status = statKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statLabel,
                                    color = if (isSelected) HabitTheme.colors.onAccent(themeColor) else HabitTheme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Numeric value input if VALUE type habit
                    if (habit.habitType == "VALUE") {
                        // 메인/위젯 입력 다이얼로그와 같은 필드·파싱·목표 안내(ValueInput)
                        ValueInputField(
                            habit = habit,
                            input = inputValue,
                            onInput = { inputValue = it },
                            accent = themeColor,
                            labelPrefix = "수치 입력",
                            placeholder = null
                        )
                    }
                }
            },
            confirmButton = {
                val value = ValueInput.parse(inputValue)
                val isValueRecord = habit.habitType == "VALUE" && status != "NONE" && status != "SKIPPED"
                TextButton(
                    // 수치형 "기록"은 값이 있어야 저장할 수 있다(값 없는 성공/실패는 만들지 않는다)
                    enabled = !isValueRecord || value != null,
                    onClick = {
                        // 수치형은 값이 곧 상태다(RecordOutcomes.statusForValue). 삭제/건너뜀 선택만 그대로 둔다.
                        val computedStatus = if (isValueRecord) {
                            RecordOutcomes.statusForValue(value, habit.targetValue, habit.targetType)
                        } else {
                            status
                        }
                        onUpdateRecordForDate(date.toString(), computedStatus, if (isValueRecord) value else null)
                        selectedDateForEdit = null
                    }
                ) {
                    // 비활성(값 없음)일 때도 습관색으로 그리면 "무반응 탭"이 된다 — ValueInputDialog와 같은 규칙
                    Text("저장", color = if (!isValueRecord || value != null) themeColor else HabitTheme.colors.textDisabled, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDateForEdit = null }) {
                    Text("취소", color = HabitTheme.colors.textSecondary)
                }
            },
            containerColor = HabitTheme.colors.bgLayer2,
            titleContentColor = HabitTheme.colors.textPrimary
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "습관 삭제",
                    color = HabitTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Text(
                    text = "정말로 이 습관을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
                    color = HabitTheme.colors.textSecondary,
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
                    Text("취소", color = HabitTheme.colors.textSecondary)
                }
            },
            containerColor = HabitTheme.colors.bgLayer2,
            titleContentColor = HabitTheme.colors.textPrimary
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
            .clip(MaterialTheme.shapes.small)
            .background(HabitTheme.colors.bgLayer3)
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
    accent: Color = HabitTheme.colors.textPrimary,
    content: @Composable () -> Unit
) {
    // 섹션 제목은 습관 액센트(가이드 B2: 액센트가 허용되는 5곳 중 하나). 카드는 layer2 + radius.md, 테두리 없음(B6).
    Column(
        verticalArrangement = Arrangement.spacedBy(Space.s3)
    ) {
        Text(
            text = title,
            color = HabitTheme.colors.accentForText(accent),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(HabitTheme.colors.bgLayer2)
                .padding(Space.card)
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
