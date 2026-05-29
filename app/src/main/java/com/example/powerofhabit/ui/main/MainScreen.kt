package com.example.powerofhabit.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backupManager = remember { com.example.powerofhabit.backup.GoogleDriveBackupManager(context) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
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
                        scope.launch { backupManager.backupDatabase() }
                    },
                    onInsertRecord = { record ->
                        viewModel.insertRecord(record)
                        scope.launch { backupManager.backupDatabase() }
                    },
                    onDeleteRecord = { record ->
                        viewModel.deleteRecord(record)
                        scope.launch { backupManager.backupDatabase() }
                    },
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
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dates = remember(today) {
        listOf(
            today.minusDays(3),
            today.minusDays(2),
            today.minusDays(1),
            today
        )
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
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "오늘의 습관",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = -0.6.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Trophy/Badge Button
                IconButton(
                    onClick = onNavigateToBadges,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGrayBackground)
                ) {
                    Text("🏆", fontSize = 18.sp)
                }

                IconButton(
                    onClick = { showBackupSettings = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGrayBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Backup Settings",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = onNavigateToAddHabit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGrayBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = Color.White
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
            // Header Row for dates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.width(224.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEach { date ->
                        Column(
                            modifier = Modifier.width(48.dp),
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
                                color = if (date == today) HabitOrange else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "동기화 및 백업 설정",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                color = Color.White,
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGrayBackground),
                        modifier = Modifier.weight(1f).border(1.dp, MetalBorderBrush, RoundedCornerShape(20.dp))
                    ) {
                        Text("복원하기", color = Color.White, fontWeight = FontWeight.Bold)
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
            containerColor = DarkGrayBackground,
            titleContentColor = Color.White
        )
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGrayBackground)
            .border(1.dp, MetalBorderBrush, RoundedCornerShape(16.dp))
            .clickable { onNavigateToDetail(habit.habitId) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(themeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = -0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = habit.question,
                color = LightGrayText,
                fontSize = 12.sp,
                letterSpacing = -0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Row(
            modifier = Modifier.width(224.dp),
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
}
