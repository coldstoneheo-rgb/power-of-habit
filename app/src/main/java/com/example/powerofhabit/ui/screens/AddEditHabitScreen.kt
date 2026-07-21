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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    habitId: Int,
    defaultHabitType: String = "CHECK",
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
    var selectedThemeHex by remember { mutableStateOf("#42A5F5") } // Default light blue matte
    var habitType by remember { mutableStateOf(defaultHabitType) }
    var unit by remember { mutableStateOf("") }
    var targetValueString by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("AT_LEAST") } // "AT_LEAST" (적어도) vs "AT_MOST" (최대)
    
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
                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
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
            Color(0xFF42A5F5)
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
                reminderTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            },
            parsedTime.hour,
            parsedTime.minute,
            false
        )
    }

    // Save Action Lambda
    val performSave = {
        if (title.isBlank()) {
            Toast.makeText(context, "습관 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
        } else {
            val isFrequencyValid = when (frequencyType) {
                "DAILY" -> true
                "INTERVAL" -> intervalDays.toIntOrNull()?.let { it >= 1 } ?: false
                "WEEKLY_COUNT" -> weeklyCount.toIntOrNull()?.let { it in 1..7 } ?: false
                "MONTHLY_COUNT" -> monthlyCount.toIntOrNull()?.let { it in 1..31 } ?: false
                "COUNT_IN_DAYS" -> {
                    val count = countInDaysCount.toIntOrNull()
                    val period = countInDaysPeriod.toIntOrNull()
                    count != null && period != null && count in 1..period
                }
                else -> false
            }

            if (!isFrequencyValid) {
                Toast.makeText(context, "올바른 빈도 값을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (habitType == "VALUE" && unit.isBlank()) {
                Toast.makeText(context, "단위(예: km, 쪽)를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (habitType == "VALUE" && targetValueString.isNotBlank() && targetValueString.toFloatOrNull() == null) {
                Toast.makeText(context, "올바른 목표 수치를 입력해주세요 (예: 15).", Toast.LENGTH_SHORT).show()
            } else {
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
            }
        }
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
                title = { Text(if (habitId == 0) "습관 만들기" else "습관 수정하기", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
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
                    TextButton(onClick = { performSave() }) {
                        Text("저장", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    placeholder = { Text(if (habitType == "VALUE") "예) 운동" else "예) 달리기") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                // 색상 선택 버튼
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("색", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { showColorPickerDialog = true }
                    )
                }
            }
            
            // Question input
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("질문") },
                placeholder = { Text(if (habitType == "VALUE") "예 : 오늘 몇 km를 달렸나요?" else "예 : 오늘 운동을 했습니까?") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            // Habit Type selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("유형", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = habitType == "CHECK",
                        onClick = { habitType = "CHECK" },
                        label = { Text("체크 (Yes/No)") }
                    )
                    FilterChip(
                        selected = habitType == "VALUE",
                        onClick = { habitType = "VALUE" },
                        label = { Text("측정 (수치)") }
                    )
                }
            }
            
            // Unit & Target input (only visible if VALUE type)
            if (habitType == "VALUE") {
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("단위") },
                    placeholder = { Text("예) km") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetValueString,
                        onValueChange = { targetValueString = it },
                        label = { Text("목표") },
                        placeholder = { Text("예) 15") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    // 목표 유형 (적어도 vs 최대)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("목표 유형", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = targetType == "AT_LEAST",
                                onClick = { targetType = "AT_LEAST" },
                                label = { Text("적어도", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = targetType == "AT_MOST",
                                onClick = { targetType = "AT_MOST" },
                                label = { Text("최대", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
            
            // 빈도 설정 & 알림 (가로 1줄 배치)
            var showFreqDropdown by remember { mutableStateOf(false) }
            val freqLabelMap = mapOf(
                "DAILY" to "매일",
                "INTERVAL" to "며칠마다",
                "WEEKLY_COUNT" to "주 몇회",
                "MONTHLY_COUNT" to "월 몇회",
                "COUNT_IN_DAYS" to "기간내 횟수"
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 빈도 셀렉트박스 (왼쪽)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = freqLabelMap[frequencyType] ?: "매일",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("빈도") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onBackground,
                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { Text("▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showFreqDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showFreqDropdown,
                        onDismissRequest = { showFreqDropdown = false }
                    ) {
                        freqLabelMap.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    frequencyType = type
                                    showFreqDropdown = false
                                }
                            )
                        }
                    }
                }

                // 알림 토글 & 시간 (오른쪽)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("알림", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                    if (isReminderEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { timePickerDialog.show() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("시간", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = reminderTime ?: "09:00",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            
            // 빈도 세부 설정 입력
            when (frequencyType) {
                "INTERVAL" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = intervalDays,
                            onValueChange = { if (it.all { char -> char.isDigit() }) intervalDays = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("일마다 수행", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    }
                }
                "WEEKLY_COUNT" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("일주일에", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        OutlinedTextField(
                            value = weeklyCount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) weeklyCount = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("회 수행", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    }
                }
                "MONTHLY_COUNT" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("한 달에", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        OutlinedTextField(
                            value = monthlyCount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) monthlyCount = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("회 수행", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    }
                }
                "COUNT_IN_DAYS" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countInDaysPeriod,
                            onValueChange = { if (it.all { char -> char.isDigit() }) countInDaysPeriod = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("일 동안", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        OutlinedTextField(
                            value = countInDaysCount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) countInDaysCount = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text("회 수행", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    }
                }
            }
            
            // Memo input (Optional)
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모") },
                placeholder = { Text("(선택사항)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                maxLines = 4,
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
