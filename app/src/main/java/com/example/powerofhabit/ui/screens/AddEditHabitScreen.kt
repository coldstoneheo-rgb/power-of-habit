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
    var selectedThemeHex by remember { mutableStateOf("#FF9800") }
    var habitType by remember { mutableStateOf("CHECK") }
    var unit by remember { mutableStateOf("") }
    
    // Populate form fields when habit details are loaded
    LaunchedEffect(habitState) {
        habitState?.let { habit ->
            title = habit.title
            question = habit.question
            frequencyType = habit.frequencyType
            frequencyValue = habit.frequencyValue
            reminderTime = habit.reminderTime
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
    val availableColors = listOf(
        "#FF9800" to HabitOrange,
        "#03A9F4" to HabitSkyBlue,
        "#9C27B0" to HabitPurple,
        "#4CAF50" to HabitGreen,
        "#F44336" to HabitRed,
        "#FFFFEB" to HabitYellow
    )
    
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    availableColors.forEach { (hex, color) ->
                        val isSelected = selectedThemeHex.uppercase() == hex.uppercase()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedThemeHex = hex }
                        )
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
                        reminderTime = reminderTime,
                        themeColor = selectedThemeHex,
                        habitType = habitType,
                        unit = if (habitType == "VALUE") unit else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HabitOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Habit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
