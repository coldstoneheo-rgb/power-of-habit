package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.theme.BlackBackground

@Composable
fun AddEditHabitScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Add Habit", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        
        // Form placeholders
        Text("What is the title?", color = Color.LightGray)
        Text("What is your goal question?", color = Color.LightGray)
    }
}
