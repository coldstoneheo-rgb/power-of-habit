package com.example.powerofhabit.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.ui.components.widgets.CheckWidget
import com.example.powerofhabit.ui.theme.BlackBackground
import com.example.powerofhabit.ui.theme.DarkGrayBackground
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.LightGrayText
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MainScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
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
                    onUpdateRecordStatus = { recordId, status ->
                        viewModel.updateRecordStatus(recordId, status)
                    },
                    onInsertRecord = { record ->
                        viewModel.insertRecord(record)
                    },
                    onDeleteRecord = { record ->
                        viewModel.deleteRecord(record)
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
    onUpdateRecordStatus: (Int, String) -> Unit,
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
                text = "Today",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
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
                                    onUpdateRecordStatus(record.recordId, nextStatus)
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
                                onUpdateRecordStatus(record.recordId, nextStatus)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = habit.question,
                color = LightGrayText,
                fontSize = 12.sp,
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
