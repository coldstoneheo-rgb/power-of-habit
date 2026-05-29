package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun HabitScoreWidget(
    scores: List<Float>, // 0f to 100f
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        if (scores.isEmpty()) return@Canvas
        
        val maxScore = 100f
        val points = scores.mapIndexed { index, score ->
            Offset(
                x = index * (size.width / (scores.size - 1).coerceAtLeast(1)),
                y = size.height - (score / maxScore) * size.height
            )
        }
        
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val current = points[i]
                    val previous = points[i - 1]
                    val controlX = (previous.x + current.x) / 2
                    cubicTo(
                        controlX, previous.y,
                        controlX, current.y,
                        current.x, current.y
                    )
                }
            }
        }
        
        drawPath(
            path = path,
            color = themeColor,
            style = Stroke(width = 4.dp.toPx())
        )
        
        if (points.isNotEmpty()) {
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = points.last()
            )
            drawCircle(
                color = themeColor,
                radius = 4.dp.toPx(),
                center = points.last()
            )
        }
    }
}
