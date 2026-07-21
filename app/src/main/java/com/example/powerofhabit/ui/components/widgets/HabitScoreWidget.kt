package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun HabitScoreWidget(
    scores: List<Float>, // 0f to 100f
    dates: List<String>, // "MM/DD" or "MM월 DD일"
    selectedFilter: String, // "일", "주", "월", "분기", "년"
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val yLabels = listOf("100%", "80%", "60%", "40%", "20%")
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            // Y-axis percentage labels
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Canvas area for grid lines, trend line, and nodes
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val width = size.width
                val height = size.height

                // Draw Y-axis horizontal grid lines
                val stepY = height / 4f
                for (i in 0..4) {
                    val y = i * stepY
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                if (scores.isEmpty()) return@Canvas

                // Compute points
                val points = scores.mapIndexed { index, score ->
                    val x = if (scores.size > 1) index * (width / (scores.size - 1)) else width / 2f
                    val y = height - ((score.coerceIn(0f, 100f) / 100f) * height)
                    Offset(x, y)
                }

                // Draw connecting line
                val path = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = themeColor,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw nodes for each data point
                points.forEach { point ->
                    drawCircle(
                        color = themeColor,
                        radius = 4.5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-axis Date Labels Row
        if (dates.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val step = (dates.size / 6).coerceAtLeast(1)
                val displayDates = dates.filterIndexed { index, _ -> index % step == 0 || index == dates.lastIndex }
                displayDates.take(7).forEach { dateStr ->
                    Text(
                        text = dateStr,
                        color = labelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
