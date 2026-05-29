package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryCalendarWidget(
    yearMonth: YearMonth,
    records: Map<LocalDate, String>, // "COMPLETED", "FAILED", "SKIPPED"
    themeColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // Sun=0
        val cols = 7
        val rows = 6
        
        val cellWidth = size.width / cols
        val cellHeight = size.height / rows
        
        var day = 1
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (r == 0 && (c < firstDayOfWeek)) continue
                if (day > daysInMonth) break
                
                val date = yearMonth.atDay(day)
                val status = records[date]
                val centerOffset = Offset(
                    x = (c * cellWidth) + (cellWidth / 2f),
                    y = (r * cellHeight) + (cellHeight / 2f),
                )
                
                // Draw background circle for status
                when (status) {
                    "COMPLETED" -> drawCircle(color = themeColor, radius = cellWidth / 2.5f, center = centerOffset)
                    "FAILED" -> drawCircle(color = Color.DarkGray, radius = cellWidth / 8f, center = centerOffset)
                    // SKIPPED or NONE -> no circle
                }
                
                day++
            }
        }
    }
}
