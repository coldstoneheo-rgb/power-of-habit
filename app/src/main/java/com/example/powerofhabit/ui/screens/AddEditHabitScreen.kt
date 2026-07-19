package com.example.powerofhabit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.powerofhabit.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    habitId: Int,
    onBack: () -> Unit,
    viewModel: AddEditHabitViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val habitState by viewModel.habitState.collectAsStateWithLifecycle()
    
    // Load habit info if editing
    LaunchedEffect(habitId) {
        viewModel.loadHabit(habitId)
    }
    
    // Form fields
    var title by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var frequencyType by remember { mutableStateOf("DAILY") }
    
    // 빈도 세부 변수
    var intervalDays by remember { mutableStateOf("1") }
    var weeklyCount by remember { mutableStateOf("3") }
    var monthlyCount by remember { mutableStateOf("10") }
    var countInDaysCount by remember { mutableStateOf("10") }
    var countInDaysPeriod by remember { mutableStateOf("30") }
    
    var memo by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<String?>(null) }
    var isReminderEnabled by remember { mutableStateOf(false) }
    var selectedThemeHex by remember { mutableStateOf("#E57373") } // Default premium matte red
    var habitType by remember { mutableStateOf("CHECK") }
    var unit by remember { mutableStateOf("") }
    var targetValueString by remember { mutableStateOf("") }
    
    var showColorPickerDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isReminderEnabled = isGranted
        if (!isGranted) {
            Toast.makeText(context, "알림을 받으려면 알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Populate form fields when habit details are loaded
    LaunchedEffect(habitState) {
        habitState?.let { habit ->
            title = habit.title
            question = habit.question
            frequencyType = habit.frequencyType
            memo = habit.memo ?: ""
            
            // 파싱 부분
            when (habit.frequencyType) {
                "INTERVAL" -> intervalDays = habit.frequencyValue
                "WEEKLY_COUNT" -> weeklyCount = habit.frequencyValue
                "MONTHLY_COUNT" -> monthlyCount = habit.frequencyValue
                "COUNT_IN_DAYS" -> {
                    val parts = habit.frequencyValue.split("/")
                    if (parts.size == 2) {
                        countInDaysCount = parts[0]
                        countInDaysPeriod = parts[1]
                    }
                }
            }
            
            reminderTime = habit.reminderTime
            isReminderEnabled = habit.isReminderEnabled
            selectedThemeHex = habit.themeColor
            habitType = habit.habitType
            unit = habit.unit ?: ""
            targetValueString = habit.targetValue?.toString() ?: ""
        }
    }
    
    // Collect event flows (Save Success / Error)
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddEditHabitUiEvent.SaveSuccess -> {
                    Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is AddEditHabitUiEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    val themeColor = remember(selectedThemeHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedThemeHex))
        } catch (e: Exception) {
            Color(0xFFE57373)
        }
    }
    
    val scrollState = rememberScrollState()
    
    // Android native TimePickerDialog
    val parsedTime = remember(reminderTime) {
        try {
            java.time.LocalTime.parse(reminderTime ?: "09:00")
        } catch (e: Exception) {
            java.time.LocalTime.of(9, 0)
        }
    }
    val timePickerDialog = remember(context, parsedTime) {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                reminderTime = String.format("%02d:%02d", hour, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            false
        )
    }
    
    // 색상 팝업 다이얼로그
    if (showColorPickerDialog) {
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = {
                Text(
                    text = "테마 색상 선택",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                val chunkedColors = remember { PremiumMatteColors.chunked(6) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    chunkedColors.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowColors.forEach { (hex, color) ->
                                val isSelected = selectedThemeHex.uppercase() == hex.uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedThemeHex = hex
                                            showColorPickerDialog = false
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (habitId == 0) "New Habit" else "Edit Habit", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (habitId != 0 && habitState != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteHabit(habitState!!)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 현재 설정된 색상을 보여주는 원형 버튼
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape)
                        .clickable { showColorPickerDialog = true }
                )

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Title") },
                    placeholder = { Text("e.g. Read books, Gym") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HabitOrange,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )
            }
            
            // Question input
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Daily Question") },
                placeholder = { Text("e.g. Did you read 10 pages?") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = HabitOrange,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )
            
            // Habit Type selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Habit Type", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = habitType == "CHECK",
                        onClick = { habitType = "CHECK" },
                        label = { Text("Check (Yes/No)") }
                    )
                    FilterChip(
                        selected = habitType == "VALUE",
                        onClick = { habitType = "VALUE" },
                        label = { Text("Value (Numeric)") }
                    )
                }
            }
            
            // Unit input (only visible if VALUE type)
            if (habitType == "VALUE") {
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    placeholder = { Text("e.g. pages, kg, mins") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HabitOrange,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetValueString,
                    onValueChange = { targetValueString = it },
                    label = { Text("수행 완료 기준수치") },
                    placeholder = { Text("예: 3, 50, 1000") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HabitOrange,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            
            // 빈도 설정 (Frequency)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("빈도 설정 (Frequency)", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                val frequencies = listOf(
                    "DAILY" to "매일",
                    "INTERVAL" to "며칠마다",
                    "WEEKLY_COUNT" to "주 몇회",
                    "MONTHLY_COUNT" to "월 몇회",
                    "COUNT_IN_DAYS" to "기간내 횟수"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    frequencies.take(3).forEach { (type, label) ->
                        FilterChip(
                            selected = frequencyType == type,
                            onClick = { frequencyType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    frequencies.drop(3).forEach { (type, label) ->
                        FilterChip(
                            selected = frequencyType == type,
                            onClick = { frequencyType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (frequencyType) {
                    "DAILY" -> {
                        Text("매일 습관을 수행합니다.", color = LightGrayText, fontSize = 13.sp)
                    }
                    "INTERVAL" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = intervalDays,
                                onValueChange = { if (it.all { char -> char.isDigit() }) intervalDays = it },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = HabitOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Text("일마다 수행합니다.", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        }
                    }
                    "WEEKLY_COUNT" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("일주일에", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                            OutlinedTextField(
                                value = weeklyCount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) weeklyCount = it },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = HabitOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Text("번 수행합니다.", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        }
                    }
                    "MONTHLY_COUNT" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("한 달에", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                            OutlinedTextField(
                                value = monthlyCount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) monthlyCount = it },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = HabitOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Text("번 수행합니다.", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        }
                    }
                    "COUNT_IN_DAYS" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = countInDaysPeriod,
                                onValueChange = { if (it.all { char -> char.isDigit() }) countInDaysPeriod = it },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = HabitOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Text("일 동안", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                            OutlinedTextField(
                                value = countInDaysCount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) countInDaysCount = it },
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedBorderColor = HabitOrange,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                singleLine = true
                            )
                            Text("번 수행합니다.", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            // Memo input (Optional)
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택사항)") },
                placeholder = { Text("습관에 대한 추가 설명이나 메모를 입력해 보세요.") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = HabitOrange,
                    unfocusedBorderColor = Color.DarkGray
                ),
                maxLines = 4,
                minLines = 2
            )
            
            // Notification / Reminder Settings (유지)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Habit Notification", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Get reminded to build your habit", color = LightGrayText, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (!hasPermission) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    isReminderEnabled = true
                                }
                            } else {
                                isReminderEnabled = checked
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = themeColor,
                            uncheckedThumbColor = LightGrayText,
                            uncheckedTrackColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
                
                if (isReminderEnabled) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder Time", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { timePickerDialog.show() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = reminderTime ?: "09:00",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "습관 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val isFrequencyValid = when (frequencyType) {
                        "DAILY" -> true
                        "INTERVAL" -> {
                            val days = intervalDays.toIntOrNull()
                            days != null && days >= 1
                        }
                        "WEEKLY_COUNT" -> {
                            val count = weeklyCount.toIntOrNull()
                            count != null && count in 1..7
                        }
                        "MONTHLY_COUNT" -> {
                            val count = monthlyCount.toIntOrNull()
                            count != null && count in 1..31
                        }
                        "COUNT_IN_DAYS" -> {
                            val count = countInDaysCount.toIntOrNull()
                            val period = countInDaysPeriod.toIntOrNull()
                            count != null && period != null && count >= 1 && period >= 1 && count <= period
                        }
                        else -> false
                    }
                    
                    if (!isFrequencyValid) {
                        val errMsg = when (frequencyType) {
                            "INTERVAL" -> "올바른 간격(1일 이상)을 입력해주세요."
                            "WEEKLY_COUNT" -> "일주일 수행 횟수는 1~7회 사이여야 합니다."
                            "MONTHLY_COUNT" -> "한 달 수행 횟수는 1~31회 사이여야 합니다."
                            "COUNT_IN_DAYS" -> "올바른 수행 기간 및 횟수(횟수 <= 기간)를 입력해주세요."
                            else -> "올바른 빈도 값을 입력해주세요."
                        }
                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (habitType == "VALUE" && unit.isBlank()) {
                        Toast.makeText(context, "수치 단위(예: kg, ml)를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (habitType == "VALUE" && targetValueString.isNotBlank() && targetValueString.toFloatOrNull() == null) {
                        Toast.makeText(context, "올바른 완료 기준 수치를 입력해주세요 (예: 3, 10.5).", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val computedFrequencyValue = when (frequencyType) {
                        "DAILY" -> ""
                        "INTERVAL" -> intervalDays
                        "WEEKLY_COUNT" -> weeklyCount
                        "MONTHLY_COUNT" -> monthlyCount
                        "COUNT_IN_DAYS" -> "$countInDaysCount/$countInDaysPeriod"
                        else -> ""
                    }
                    viewModel.saveHabit(
                        habitId = habitId,
                        title = title,
                        question = question,
                        frequencyType = frequencyType,
                        frequencyValue = computedFrequencyValue,
                        reminderTime = reminderTime ?: "09:00",
                        isReminderEnabled = isReminderEnabled,
                        themeColor = selectedThemeHex,
                        habitType = habitType,
                        unit = if (habitType == "VALUE") unit else null,
                        memo = if (memo.isBlank()) null else memo,
                        targetValue = if (habitType == "VALUE") targetValueString.toFloatOrNull() else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Habit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
