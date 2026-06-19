package com.example.powerofhabit.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.ui.components.widgets.CheckWidget
import com.example.powerofhabit.ui.theme.BlackBackground
import com.example.powerofhabit.ui.theme.DarkGrayBackground
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.LightGrayText
import com.example.powerofhabit.ui.theme.MetalBorderBrush
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MainScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: () -> Unit,
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
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onNavigateToBadges = onNavigateToBadges,
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
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: () -> Unit,
    onNavigateToBadges: () -> Unit,
    onUpdateRecordStatus: (Int, String, Int) -> Unit,
    onInsertRecord: (HabitRecordEntity) -> Unit,
    onDeleteRecord: (HabitRecordEntity) -> Unit,
    isDarkMode: Boolean,
    isDateDescending: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleDateDescending: () -> Unit,
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
    var showBackupSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backupManager = remember { com.example.powerofhabit.backup.GoogleDriveBackupManager(context) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
                    onClick = onNavigateToAddHabit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text("🏆", fontSize = 18.sp)
                }

                // Settings Button
                IconButton(
                    onClick = { showBackupSettings = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
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
                    color = LightGrayText,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
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
                                color = LightGrayText,
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
                items(habits) { habit ->
                    HabitRow(
                        habit = habit,
                        dates = dates,
                        recordsMap = records[habit.habitId] ?: emptyMap(),
                        onNavigateToDetail = onNavigateToDetail,
                        onCheckClick = { date, record ->
                            if (habit.habitType == "VALUE") {
                                showValueDialogForHabit = habit to date
                            } else {
                                if (record != null) {
                                    val nextStatus = if (record.status == "COMPLETED") "FAILED" else "COMPLETED"
                                    onUpdateRecordStatus(record.recordId, nextStatus, habit.habitId)
                                } else {
                                    onInsertRecord(
                                        HabitRecordEntity(
                                            habitId = habit.habitId,
                                            date = date.toString(),
                                            status = "COMPLETED",
                                            inputValue = null
                                        )
                                    )
                                }
                            }
                        },
                        onCheckLongClick = { date, record ->
                            if (record != null) {
                                val nextStatus = if (record.status == "SKIPPED") "FAILED" else "SKIPPED"
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
    
    // Value input dialog
    showValueDialogForHabit?.let { (habit, date) ->
        val existingRecord = records[habit.habitId]?.get(date.toString())
        var inputValue by remember { mutableStateOf(existingRecord?.inputValue?.toString() ?: "") }
        
        AlertDialog(
            onDismissRequest = { showValueDialogForHabit = null },
            title = { Text(text = habit.title, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = habit.question,
                        color = LightGrayText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("Value (${habit.unit ?: ""})") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HabitOrange,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = inputValue.toFloatOrNull()
                        if (value != null) {
                            if (existingRecord != null) {
                                onDeleteRecord(existingRecord)
                            }
                            onInsertRecord(
                                HabitRecordEntity(
                                    habitId = habit.habitId,
                                    date = date.toString(),
                                    status = "COMPLETED",
                                    inputValue = value
                                )
                            )
                        }
                        showValueDialogForHabit = null
                    }
                ) {
                    Text("Save", color = HabitOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showValueDialogForHabit = null }) {
                    Text("Cancel", color = LightGrayText)
                }
            },
            containerColor = DarkGrayBackground,
            titleContentColor = Color.White
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
                        color = LightGrayText,
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
                        modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
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
                    Text("닫기", color = LightGrayText)
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
            val strokeWidth = 2.5f.dp.toPx()
            
            // Background ring
            drawCircle(
                color = themeColor.copy(alpha = 0.15f),
                radius = size.minDimension / 2 - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
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
    
    val completionRate = remember(recordsMap) {
        val today = LocalDate.now()
        val currentMonthPrefix = today.toString().substring(0, 7) // "YYYY-MM"
        val thisMonthRecords = recordsMap.filter { it.key.startsWith(currentMonthPrefix) }
        val completedCount = thisMonthRecords.values.count { it.status == "COMPLETED" }
        val totalCount = thisMonthRecords.values.count { it.status != "SKIPPED" }
        if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDetail(habit.habitId) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutProgressChart(
                    progress = completionRate,
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
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(
                modifier = Modifier.width(152.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    val record = recordsMap[date.toString()]
                    CheckWidget(
                        status = record?.status ?: "NONE",
                        themeColor = themeColor,
                        onClick = { onCheckClick(date, record) },
                        onLongClick = { onCheckLongClick(date, record) }
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )
    }
}
