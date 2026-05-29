package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeatmapWidget(
    frequencies: List<List<Int>>, // 7 rows (days), N cols (weeks)
    themeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val rows = 7
        val cols = frequencies.firstOrNull()?.size ?: 0
        if (cols == 0) return@Canvas

        val cellWidth = size.width / cols
        val cellHeight = size.height / rows
        
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val freq = frequencies.getOrNull(r)?.getOrNull(c) ?: 0
                val maxRadius = minOf(cellWidth, cellHeight) / 2.5f
                val radius = maxRadius * (freq / 10f) // Assuming 10 is max frequency
                val alpha = (freq / 10f).coerceIn(0.2f, 1.0f)
                
                if (freq > 0) {
                    drawCircle(
                        color = themeColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(
                            x = (c * cellWidth) + (cellWidth / 2f),
                            y = (r * cellHeight) + (cellHeight / 2f),
                        )
                    )
                } else {
                    drawCircle(
                        color = Color.DarkGray,
                        radius = maxRadius * 0.2f,
                        center = Offset(
                            x = (c * cellWidth) + (cellWidth / 2f),
                            y = (r * cellHeight) + (cellHeight / 2f),
                        )
                    )
                }
            }
        }
    }
}
