package com.example.powerofhabit.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.components.ValueInputDialog
import com.example.powerofhabit.ui.components.widgets.CheckWidget
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MainScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: (String) -> Unit,
    onNavigateToBadges: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDateDescending by viewModel.isDateDescending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backupManager = remember { com.example.powerofhabit.backup.GoogleDriveBackupManager(context) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state) {
            MainScreenUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HabitOrange)
                }
            }
            is MainScreenUiState.Success -> {
                val successState = state as MainScreenUiState.Success
                MainScreenContent(
                    habits = successState.habits,
                    records = successState.records,
                    scores = successState.scores,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onNavigateToBadges = onNavigateToBadges,
                    onToggleCompletion = { habitId, date -> viewModel.toggleCompletion(habitId, date) },
                    onUpdateRecordStatus = { recordId, status, habitId ->
                        viewModel.updateRecordStatus(recordId, status, habitId)
                    },
                    onInsertRecord = { record ->
                        viewModel.insertRecord(record)
                    },
                    onDeleteRecord = { record ->
                        viewModel.deleteRecord(record)
                    },
                    isDarkMode = isDarkMode,
                    isDateDescending = isDateDescending,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleDateDescending = { viewModel.toggleDateDescending() },
                    onExportTo = { uri, onResult -> viewModel.exportTo(uri, onResult) },
                    onImportFrom = { uri, onResult -> viewModel.importFrom(uri, onResult) },
                    modifier = modifier
                )
            }
            is MainScreenUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${(state as MainScreenUiState.Error).throwable.localizedMessage}",
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
internal fun MainScreenContent(
    habits: List<HabitEntity>,
    records: Map<Int, Map<String, HabitRecordEntity>>,
    scores: Map<Int, Float> = emptyMap(),
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: (String) -> Unit,
    onNavigateToBadges: () -> Unit,
    onUpdateRecordStatus: (Int, String, Int) -> Unit,
    onInsertRecord: (HabitRecordEntity) -> Unit,
    onToggleCompletion: (Int, LocalDate) -> Unit = { _, _ -> },
    onDeleteRecord: (HabitRecordEntity) -> Unit,
    isDarkMode: Boolean,
    isDateDescending: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleDateDescending: () -> Unit,
    /** JSON 내보내기: (대상 Uri, 결과 메시지 콜백). */
    onExportTo: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    /** JSON 가져오기(병합): (원본 Uri, 결과 메시지 콜백). */
    onImportFrom: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dates = remember(today, isDateDescending) {
        val base = listOf(
            today.minusDays(3),
            today.minusDays(2),
            today.minusDays(1),
            today
        )
        if (isDateDescending) base.reversed() else base
    }
    
    var showValueDialogForHabit by remember { mutableStateOf<Pair<HabitEntity, LocalDate>?>(null) }
    // 성공 폭죽을 재생할 셀 (habitId, "YYYY-MM-DD"). 애니메이션이 끝나면 null.
    var burstCell by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showBackupSettings by remember { mutableStateOf(false) }
    var showAddTypeModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backupManager = remember { com.example.powerofhabit.backup.GoogleDriveBackupManager(context) }

    // 파일 이전(JSON). 런처는 다이얼로그 밖(항상 컴포지션에 있는 곳)에 둬야 액티비티 재생성 뒤에도 결과를 받는다.
    var isTransferring by remember { mutableStateOf(false) }
    val onTransferResult: (String) -> Unit = { message ->
        isTransferring = false
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isTransferring = true
            onExportTo(uri, onTransferResult)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isTransferring = true
            onImportFrom(uri, onTransferResult)
        }
    }

    if (showAddTypeModal) {
        AlertDialog(
            onDismissRequest = { showAddTypeModal = false },
            title = {
                Text(
                    text = "습관 종류 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Option 1: Yes or No (CHECK)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeModal = false
                                onNavigateToAddHabit("CHECK")
                            },
                        colors = CardDefaults.cardColors(containerColor = HabitTheme.colors.bgLayer3),
                        border = BorderStroke(1.dp, HabitTheme.colors.lineHair),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "예 또는 아니요",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "예) 오늘 일찍 일어났나요? 운동 하셨나요? 독서를 하셨나요?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Measurable (VALUE)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeModal = false
                                onNavigateToAddHabit("VALUE")
                            },
                        colors = CardDefaults.cardColors(containerColor = HabitTheme.colors.bgLayer3),
                        border = BorderStroke(1.dp, HabitTheme.colors.lineHair),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "측정 가능한",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "예) 오늘 몇 km를 달렸습니까? 몇 페이지를 읽었습니까?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTypeModal = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "습관의 힘",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = -0.6.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add Habit Button
                IconButton(
                    onClick = { showAddTypeModal = true },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Trophy/Badge Button
                IconButton(
                    onClick = onNavigateToBadges,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text("🏆", fontSize = 18.sp)
                }

                // Settings Button
                IconButton(
                    onClick = { showBackupSettings = true },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habits. Tap + to add one!",
                    color = HabitTheme.colors.textSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.width(152.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEach { date ->
                        Column(
                            modifier = Modifier.width(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                                color = HabitTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (date == today) HabitOrange else MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(habits) { index, habit ->
                    HabitRow(
                        habit = habit,
                        dates = dates,
                        recordsMap = records[habit.habitId] ?: emptyMap(),
                        score = scores[habit.habitId],
                        showDivider = index < habits.lastIndex,
                        burstDate = burstCell?.takeIf { it.first == habit.habitId }?.second,
                        onBurstDone = { burstCell = null },
                        onNavigateToDetail = onNavigateToDetail,
                        onCheckClick = { date, record ->
                            if (habit.habitType == "VALUE") {
                                showValueDialogForHabit = habit to date
                            } else {
                                // 규칙(없음→완료, 완료→실패, 그 외→완료)은 HabitDao.toggleCompletion 한 곳에 있다.
                                onToggleCompletion(habit.habitId, date)
                            }
                        },
                        onCheckLongClick = { date, record ->
                            if (record != null) {
                                // 건너뜀 해제: 수치형은 값이 상태를 결정하므로 statusForValue로 되돌린다(표시와 통계가 어긋나지 않게)
                                val nextStatus = when {
                                    record.status != "SKIPPED" -> "SKIPPED"
                                    habit.habitType == "VALUE" -> RecordOutcomes.statusForValue(record.inputValue, habit.targetValue)
                                    else -> "FAILED"
                                }
                                onUpdateRecordStatus(record.recordId, nextStatus, habit.habitId)
                            } else {
                                onInsertRecord(
                                    HabitRecordEntity(
                                        habitId = habit.habitId,
                                        date = date.toString(),
                                        status = "SKIPPED",
                                        inputValue = null
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Value input dialog — 위젯(ValueInputActivity)과 같은 컴포저블·저장 규칙을 쓴다
    showValueDialogForHabit?.let { (habit, date) ->
        val existingRecord = records[habit.habitId]?.get(date.toString())
        val habitThemeColor = remember(habit.themeColor) {
            try { Color(android.graphics.Color.parseColor(habit.themeColor)) } catch (e: Exception) { HabitOrange }
        }
        ValueInputDialog(
            habit = habit,
            initialValue = existingRecord?.inputValue,
            accent = habitThemeColor,
            onDismiss = { showValueDialogForHabit = null },
            onSave = { value, outcome ->
                if (existingRecord != null) onDeleteRecord(existingRecord)
                onInsertRecord(
                    HabitRecordEntity(
                        habitId = habit.habitId,
                        date = date.toString(),
                        status = RecordOutcomes.statusForValue(value, habit.targetValue),
                        inputValue = value
                    )
                )
                // 성공(목표 충족)일 때만 해당 셀에 폭죽. 기준미달·미수행은 색 변화만.
                if (outcome == RecordOutcome.SUCCESS) burstCell = habit.habitId to date.toString()
                showValueDialogForHabit = null
            }
        )
    }

    // Google Drive Backup & Restore Settings Dialog
    if (showBackupSettings) {
        var isBackingUp by remember { mutableStateOf(false) }
        var isRestoring by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                if (!isBackingUp && !isRestoring) showBackupSettings = false 
            },
            title = {
                Text(
                    text = "설정 및 동기화",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 테마 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "다크 모드 적용",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HabitOrange,
                                checkedTrackColor = HabitOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // 날짜 순서 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "오늘 날짜를 가장 왼쪽에 표시",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDateDescending,
                            onCheckedChange = { onToggleDateDescending() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HabitOrange,
                                checkedTrackColor = HabitOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    Text(
                        text = "구글 드라이브를 통해 안전하게 습관 데이터를 동기화하고 복구할 수 있습니다.",
                        color = HabitTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = -0.5.sp
                    )

                    if (isBackingUp || isRestoring) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = HabitOrange, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isBackingUp) "데이터 백업 중..." else "데이터 복원 중...",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Divider(color = HabitTheme.colors.lineHair, thickness = 1.dp)

                    // 로컬 파일 이전(JSON) — 기기 교체·백업용. 가져오기는 덮어쓰지 않는 병합(data/transfer/HabitTransfer).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.s2)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                                exportLauncher.launch("power-of-habit-$stamp.json")
                            },
                            enabled = !isBackingUp && !isRestoring && !isTransferring,
                            border = BorderStroke(1.dp, HabitTheme.colors.lineStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTheme.colors.textPrimary),
                            contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("파일로 내보내기 (JSON)", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = !isBackingUp && !isRestoring && !isTransferring,
                            border = BorderStroke(1.dp, HabitTheme.colors.lineStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTheme.colors.textPrimary),
                            contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("파일에서 가져오기 (JSON)", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                    Text(
                        text = if (isTransferring) "파일 처리 중..." else "가져오기는 현재 데이터를 지우지 않고 병합합니다. 같은 습관·같은 날짜의 기록은 기존 것을 유지합니다.",
                        color = HabitTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isBackingUp = true
                            scope.launch {
                                val success = backupManager.backupDatabase()
                                isBackingUp = false
                                if (success) {
                                    Toast.makeText(context, "백업이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "백업에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isBackingUp && !isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = HabitOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("백업하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isRestoring = true
                            scope.launch {
                                val success = backupManager.restoreDatabase()
                                isRestoring = false
                                if (success) {
                                    Toast.makeText(context, "복원이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                    showBackupSettings = false
                                } else {
                                    Toast.makeText(context, "복원에 실패했습니다. 백업 파일을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isBackingUp && !isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    ) {
                        Text("복원하기", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupSettings = false },
                    enabled = !isBackingUp && !isRestoring
                ) {
                    Text("닫기", color = HabitTheme.colors.textSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DonutProgressChart(
    progress: Float,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(18.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.5.dp.toPx()
            
            // Background ring (Always visible)
            drawCircle(
                color = themeColor.copy(alpha = 0.2f),
                radius = size.minDimension / 2 - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
            )
            
            // Inner subtle dot for center alignment
            drawCircle(
                color = themeColor.copy(alpha = 0.35f),
                radius = 1.5.dp.toPx()
            )
            
            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = themeColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    dates: List<LocalDate>,
    recordsMap: Map<String, HabitRecordEntity>,
    score: Float?, // 0~100, ViewModel이 전체 이력으로 계산. null이면 점수 없음
    showDivider: Boolean,
    burstDate: String? = null, // 이 날짜 셀에 성공 폭죽 재생
    onBurstDone: () -> Unit = {},
    onNavigateToDetail: (Int) -> Unit,
    onCheckClick: (LocalDate, HabitRecordEntity?) -> Unit,
    onCheckLongClick: (LocalDate, HabitRecordEntity?) -> Unit
) {
    val themeColor = remember(habit.themeColor) {
        try {
            Color(android.graphics.Color.parseColor(habit.themeColor))
        } catch (e: Exception) {
            HabitOrange
        }
    }
    
    // 기록이 전혀 없는 습관은 시작 진행도 0.2로 표시, 그 외는 점수(0~100)를 0.1~1.0으로 매핑
    val emaScore = if (score == null) 0.2f else (score / 100f).coerceIn(0.1f, 1f)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToDetail(habit.habitId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutProgressChart(
                    progress = emaScore,
                    themeColor = themeColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = -0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Row(
                modifier = Modifier.width(152.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    val record = recordsMap[date.toString()]
                    CheckWidget(
                        status = record?.status ?: "NONE",
                        themeColor = themeColor,
                        habitType = habit.habitType,
                        unit = habit.unit,
                        inputValue = record?.inputValue,
                        targetValue = habit.targetValue,
                        burst = burstDate == date.toString(),
                        onBurstDone = onBurstDone,
                        onClick = { onCheckClick(date, record) },
                        onLongClick = { onCheckLongClick(date, record) }
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = HabitTheme.colors.lineHair,
                thickness = 0.5.dp
            )
        }
    }
}
