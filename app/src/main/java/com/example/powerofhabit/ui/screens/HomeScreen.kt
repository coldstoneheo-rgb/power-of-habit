package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.theme.HabitTheme

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitTheme.colors.bgBase)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Today",
            color = HabitTheme.colors.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Placeholder for Habit List
        Text(
            text = "No habits for today. Add one to get started!",
            color = HabitTheme.colors.textSecondary,
            fontSize = 16.sp
        )
    }
}
