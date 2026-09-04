package com.example.powerofhabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.powerofhabit.MainActivity
import com.example.powerofhabit.R
import com.example.powerofhabit.badges.BadgeManager
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 1x1 체크 위젯 (PRD §4-1): 어두운 반투명 사각형 위에 마크 하나.
 * 체크형은 탭으로 오늘 COMPLETED ↔ FAILED 토글(메인 화면 셀과 동일 규칙), 수치형은 앱의 상세 화면으로 이동해 입력한다.
 */
class CheckGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val habitId = prefs[HabitWidgets.HABIT_ID]
        val repo = context.widgetRepository()
        val habit = habitId?.let { repo.getHabitById(it).first() }
        val today = LocalDate.now().toString()
        val record = habit?.let { h -> repo.getRecordsForDate(today).first().firstOrNull { it.habitId == h.habitId } }
        provideContent { CheckContent(habit, record) }
    }
}

class CheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CheckGlanceWidget()
}

@Composable
private fun CheckContent(habit: HabitEntity?, record: HabitRecordEntity?) {
    val context = LocalContext.current
    val base = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(R.drawable.widget_bg))
        .padding(6.dp)

    if (habit == null) {
        Box(modifier = base.clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
            Text(
                text = "습관 선택",
                style = TextStyle(color = ColorProvider(HabitWidgets.Colors.inkMuted), fontSize = 11.sp, textAlign = TextAlign.Center)
            )
        }
        return
    }

    val accent = HabitWidgets.parseThemeColor(habit.themeColor)
    val completed = record?.status == "COMPLETED"
    val isValue = habit.habitType == "VALUE"
    val action = if (isValue) {
        androidx.glance.appwidget.action.actionStartActivity(HabitWidgets.openHabitIntent(context, habit.habitId))
    } else {
        actionRunCallback<ToggleCheckAction>(actionParametersOf(ToggleCheckAction.HabitIdKey to habit.habitId))
    }

    Box(modifier = base.clickable(action), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isValue) {
                val value = record?.inputValue
                val valueText = when {
                    value == null -> "0"
                    value % 1f == 0f -> value.toInt().toString()
                    else -> value.toString()
                }
                Text(
                    text = valueText + (habit.unit?.let { " $it" } ?: ""),
                    style = TextStyle(
                        color = ColorProvider(if (completed) accent else HabitWidgets.Colors.ink),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            } else {
                Image(
                    provider = ImageProvider(if (completed) R.drawable.ic_widget_check else R.drawable.ic_widget_close),
                    contentDescription = if (completed) "완료" else "미완료",
                    colorFilter = ColorFilter.tint(ColorProvider(if (completed) accent else HabitWidgets.Colors.inkDisabled)),
                    modifier = GlanceModifier.size(26.dp)
                )
            }
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = habit.title,
                style = TextStyle(
                    color = ColorProvider(if (completed) accent else HabitWidgets.Colors.inkMuted),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

/** 오늘 기록 토글. 메인 화면 체크 셀과 같은 규칙: 없음 → COMPLETED, COMPLETED → FAILED, 그 외 → COMPLETED. */
class ToggleCheckAction : ActionCallback {

    companion object {
        val HabitIdKey = ActionParameters.Key<Int>("habit_id")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        val repo = context.widgetRepository()
        val today = LocalDate.now().toString()
        val existing = repo.getRecordsForDate(today).first().firstOrNull { it.habitId == habitId }
        if (existing == null) {
            repo.insertRecord(HabitRecordEntity(habitId = habitId, date = today, status = "COMPLETED", inputValue = null))
        } else {
            val next = if (existing.status == "COMPLETED") "FAILED" else "COMPLETED"
            repo.updateRecordStatus(existing.recordId, next)
        }
        BadgeManager(repo, context).checkAndAwardBadges(repo.getRecordsForHabit(habitId).first())
        HabitWidgets.updateAll(context)
    }
}
