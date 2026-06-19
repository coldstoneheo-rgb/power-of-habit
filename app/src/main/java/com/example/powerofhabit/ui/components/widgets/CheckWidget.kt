package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Text(
                text = "v",
                color = themeColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (isSkipped) {
            Text(
                text = "–",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "x",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
