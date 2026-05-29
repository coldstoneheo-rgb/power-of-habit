package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.theme.DarkGrayBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckWidget(
    status: String,
    themeColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isCompleted = status == "COMPLETED"
    val isSkipped = status == "SKIPPED"
    
    val bgColor = if (isCompleted) themeColor else DarkGrayBackground
    val borderColor = if (isCompleted || isSkipped) Color.Transparent else Color.DarkGray
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Text("✔", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else if (isSkipped) {
            Text("–", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
