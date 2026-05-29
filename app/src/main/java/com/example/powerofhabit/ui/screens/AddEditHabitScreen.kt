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
    var frequencyValue by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<String?>(null) }
    var isReminderEnabled by remember { mutableStateOf(false) }
    var selectedThemeHex by remember { mutableStateOf("#E57373") } // Default premium matte red
    var habitType by remember { mutableStateOf("CHECK") }
    var unit by remember { mutableStateOf("") }
    
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
            frequencyValue = habit.frequencyValue
            reminderTime = habit.reminderTime
            isReminderEnabled = habit.isReminderEnabled
            selectedThemeHex = habit.themeColor
            habitType = habit.habitType
            unit = habit.unit ?: ""
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (habitId == 0) "New Habit" else "Edit Habit", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackBackground)
            )
        },
        containerColor = BlackBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BlackBackground)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                placeholder = { Text("e.g. Read books, Gym") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HabitOrange,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )
            
            // Question input
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Daily Question") },
                placeholder = { Text("e.g. Did you read 10 pages?") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HabitOrange,
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true
            )
            
            // Habit Type selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Habit Type", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HabitOrange,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    singleLine = true
                )
            }
            
            // Theme color picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Theme Color (32 Premium Matte Tones)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                // Render 32 colors in a 4x8 Grid
                val chunkedColors = remember { PremiumMatteColors.chunked(8) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedColors.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowColors.forEach { (hex, color) ->
                                val isSelected = selectedThemeHex.uppercase() == hex.uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedThemeHex = hex }
                                )
                            }
                        }
                    }
                }
            }
            
            // Notification / Reminder Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkGrayBackground)
                    .border(1.dp, MetalBorderBrush, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Habit Notification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            checkedTrackColor = try { Color(android.graphics.Color.parseColor(selectedThemeHex)) } catch (e: Exception) { Color.White },
                            uncheckedThumbColor = LightGrayText,
                            uncheckedTrackColor = BlackBackground
                        )
                    )
                }
                
                if (isReminderEnabled) {
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder Time", color = Color.White, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlackBackground)
                                .border(1.dp, MetalBorderBrush, RoundedCornerShape(8.dp))
                                .clickable { timePickerDialog.show() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = reminderTime ?: "09:00",
                                color = Color(android.graphics.Color.parseColor(selectedThemeHex)),
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
                    viewModel.saveHabit(
                        habitId = habitId,
                        title = title,
                        question = question,
                        frequencyType = frequencyType,
                        frequencyValue = frequencyValue,
                        reminderTime = reminderTime ?: "09:00",
                        isReminderEnabled = isReminderEnabled,
                        themeColor = selectedThemeHex,
                        habitType = habitType,
                        unit = if (habitType == "VALUE") unit else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, MetalBorderBrush, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(selectedThemeHex))),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Habit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
