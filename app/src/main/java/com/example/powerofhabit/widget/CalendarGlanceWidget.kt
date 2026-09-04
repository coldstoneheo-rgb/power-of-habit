package com.example.powerofhabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.powerofhabit.MainActivity
import com.example.powerofhabit.R
import com.example.powerofhabit.data.local.HabitEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

/**
 * 2x2 캘린더 글랜스 위젯 (PRD §1.1.4): 텍스트 없이 이번 달 격자와 테마 컬러 점만.
 * 완료 = 액센트 큰 점, 실패/건너뜀 = 작은 회색 점, 기록 없음 = 희미한 점, 오늘 = 링 표시. 탭하면 상세 화면.
 */
class CalendarGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val habitId = prefs[HabitWidgets.HABIT_ID]
        val repo = context.widgetRepository()
        val habit = habitId?.let { repo.getHabitById(it).first() }
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val statusByDate = habit?.let { h ->
            WidgetCalendarModel.statusByDate(repo.getRecordsForHabit(h.habitId).first().map { it.date to it.status })
        }.orEmpty()
        val grid = WidgetCalendarModel.monthGrid(month, statusByDate)
        provideContent { CalendarContent(habit, month, grid, today) }
    }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarGlanceWidget()
}

@Composable
private fun CalendarContent(habit: HabitEntity?, month: YearMonth, grid: List<List<CalendarCell?>>, today: LocalDate) {
    val context = LocalContext.current
    val base = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(R.drawable.widget_bg))
        .padding(horizontal = 10.dp, vertical = 8.dp)

    if (habit == null) {
        Box(modifier = base.clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
            Text(text = "습관 선택", style = TextStyle(color = ColorProvider(HabitWidgets.Colors.inkMuted), fontSize = 12.sp))
        }
        return
    }

    val accent = HabitWidgets.parseThemeColor(habit.themeColor)
    Column(modifier = base.clickable(androidx.glance.appwidget.action.actionStartActivity(HabitWidgets.openHabitIntent(context, habit.habitId)))) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = habit.title,
                style = TextStyle(color = ColorProvider(accent), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "${month.monthValue}월",
                style = TextStyle(color = ColorProvider(HabitWidgets.Colors.inkMuted), fontSize = 10.sp),
                maxLines = 1
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        grid.forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                week.forEach { cell ->
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != null) {
                            val isToday = month.atDay(cell.day) == today
                            val (color, dotSize) = dotStyle(cell.status, accent)
                            Image(
                                provider = ImageProvider(if (isToday && cell.status != "COMPLETED") R.drawable.widget_ring else R.drawable.widget_dot),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(ColorProvider(if (isToday && cell.status != "COMPLETED") accent else color)),
                                modifier = GlanceModifier.size(if (isToday) 9.dp else dotSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dotStyle(status: String?, accent: Color): Pair<Color, androidx.compose.ui.unit.Dp> = when (status) {
    "COMPLETED" -> accent to 9.dp
    "FAILED", "SKIPPED" -> HabitWidgets.Colors.inkDisabled to 5.dp
    else -> HabitWidgets.Colors.dotEmpty to 3.dp
}
