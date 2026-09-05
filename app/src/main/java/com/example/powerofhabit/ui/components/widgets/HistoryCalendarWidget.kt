package com.example.powerofhabit.ui.components.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.ui.theme.HabitTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryCalendarWidget(
    yearMonth: YearMonth,
    records: Map<LocalDate, String>,
    themeColor: Color,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // Sun = 0, Mon = 1, ...
    
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HabitTheme.colors.bgLayer2)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weekday Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    color = HabitTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Days Grid
        var day = 1
        val totalCells = daysInMonth + firstDayOfWeek
        val rows = (totalCells + 6) / 7
        
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    if (cellIndex < firstDayOfWeek || day > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val currentDate = yearMonth.atDay(day)
                        val status = records[currentDate] ?: "NONE"
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    // status = RecordOutcome 이름(SUCCESS/PARTIAL/NONE/SKIPPED). NONE은 기록이 있는데 미수행 = 실패 표시.
                                    when (status) {
                                        "SUCCESS" -> themeColor
                                        "PARTIAL" -> HabitTheme.colors.partialAccent(themeColor).copy(alpha = 0.35f)
                                        "NONE" -> HabitTheme.colors.statusFail.copy(alpha = 0.16f)
                                        "SKIPPED" -> HabitTheme.colors.lineHair
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (status == "SKIPPED") 1.dp else 0.dp,
                                    color = if (status == "SKIPPED") HabitTheme.colors.lineFocus else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onDateClick(currentDate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = when (status) {
                                    "SUCCESS" -> HabitTheme.colors.onAccent(themeColor)
                                    "PARTIAL" -> HabitTheme.colors.partialAccent(themeColor)
                                    "NONE" -> HabitTheme.colors.statusFail
                                    "SKIPPED" -> HabitTheme.colors.textPrimary.copy(alpha = 0.9f)
                                    else -> HabitTheme.colors.textPrimary.copy(alpha = 0.7f)
                                },
                                fontSize = 13.sp,
                                fontWeight = if (status != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        day++
                    }
                }
            }
        }
    }
}
