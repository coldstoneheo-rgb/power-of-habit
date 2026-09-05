package com.example.powerofhabit.widget

import android.content.Context
import android.os.Build
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.example.powerofhabit.R
import com.example.powerofhabit.data.RecordSideEffects
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 1x1 체크 위젯 (PRD §4-1): 어두운 반투명 사각형 위에 마크 하나.
 * 체크형은 탭으로 오늘 기록 토글(메인 화면 셀과 동일 규칙: 없음→완료, 완료→실패, 실패/건너뜀→완료),
 * 수치형은 값+단위를 보여주고 탭하면 투명 입력 액티비티([ValueInputActivity])에서 값만 넣는다. 습관이 없으면 탭으로 다시 고른다.
 * 성공한 날은 타일 전체를 습관색으로 채운다(API 31+).
 */
class CheckGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val habitId = prefs[HabitWidgets.HABIT_ID]
        val repo = context.widgetRepository()
        val habit = habitId?.let { repo.getHabitById(it).first() }
        val today = LocalDate.now().toString()
        val record = habit?.let { repo.getRecord(it.habitId, today) }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent { CheckContent(habit, record, today, appWidgetId) }
    }
}

class CheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CheckGlanceWidget()
}

@Composable
private fun CheckContent(habit: HabitEntity?, record: HabitRecordEntity?, renderedDate: String, appWidgetId: Int) {
    val context = LocalContext.current
    val base = GlanceModifier
        .fillMaxSize()
        .background(ImageProvider(R.drawable.widget_bg))
        .padding(6.dp)

    if (habit == null) {
        Box(
            modifier = base.clickable(actionStartActivity(HabitWidgets.reconfigureIntent(context, appWidgetId))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "습관 선택",
                style = TextStyle(color = ColorProvider(HabitWidgets.Colors.inkMuted), fontSize = 11.sp, textAlign = TextAlign.Center)
            )
        }
        return
    }

    val accent = HabitWidgets.parseThemeColor(habit.themeColor)
    // 표시 상태는 값·목표로 판정(결정 기록 2026-09-05 결정 1). status는 캐시.
    val outcome = RecordOutcomes.of(habit.habitType, record?.status, record?.inputValue, habit.targetValue)
    val completed = outcome == RecordOutcome.SUCCESS
    val isValue = habit.habitType == "VALUE"
    val action = if (isValue) {
        // 수치형: 앱을 열지 않고 투명 입력 액티비티에서 값만 넣는다 (결정 기록 2026-09-05 결정 2)
        actionStartActivity(HabitWidgets.valueInputIntent(context, habit.habitId, renderedDate, appWidgetId))
    } else {
        actionRunCallback<ToggleCheckAction>(
            actionParametersOf(ToggleCheckAction.HabitIdKey to habit.habitId, ToggleCheckAction.DateKey to renderedDate)
        )
    }

    // 레퍼런스 앱처럼 성공한 날은 타일 전체를 습관색으로 채운다(API 31+에서 둥근 모서리 배경 가능). 그 위 잉크는 onAccent.
    val fillTile = completed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val onTile = HabitWidgets.Colors.onAccent(accent)
    val tileModifier = if (fillTile) {
        GlanceModifier.fillMaxSize().background(ColorProvider(accent)).cornerRadius(20.dp).padding(6.dp)
    } else base

    Box(modifier = tileModifier.clickable(action), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isValue) {
                val value = record?.inputValue
                val valueText = when {
                    value == null -> "0"
                    value % 1f == 0f -> value.toInt().toString()
                    else -> value.toString()
                }
                // 3상태: 성공 = 습관색(채운 타일에서는 onAccent) / 기준미달 = 어두운 습관색 / 미수행(0·없음) = 비활성
                val color = when (outcome) {
                    RecordOutcome.SUCCESS -> if (fillTile) onTile else accent
                    RecordOutcome.PARTIAL -> HabitWidgets.Colors.partial(accent)
                    else -> HabitWidgets.Colors.inkDisabled
                }
                Text(
                    text = valueText + (habit.unit?.let { " $it" } ?: ""),
                    style = TextStyle(color = ColorProvider(color), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            } else if (outcome == RecordOutcome.SKIPPED) {
                Text(
                    text = "–",
                    style = TextStyle(color = ColorProvider(HabitWidgets.Colors.skip), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            } else {
                Image(
                    provider = ImageProvider(if (completed) R.drawable.ic_widget_check else R.drawable.ic_widget_close),
                    contentDescription = if (completed) "완료" else "미완료",
                    colorFilter = ColorFilter.tint(ColorProvider(if (completed) (if (fillTile) onTile else accent) else HabitWidgets.Colors.inkDisabled)),
                    modifier = GlanceModifier.size(26.dp)
                )
            }
            Spacer(modifier = GlanceModifier.height(3.dp))
            Text(
                text = habit.title,
                style = TextStyle(
                    color = ColorProvider(
                        when (outcome) {
                            RecordOutcome.SUCCESS -> if (fillTile) onTile else accent
                            RecordOutcome.PARTIAL -> HabitWidgets.Colors.partial(accent)
                            else -> HabitWidgets.Colors.inkMuted
                        }
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * 오늘 기록 토글. DAO 트랜잭션으로 처리해 연타해도 같은 날 행이 두 개 생기지 않는다.
 * 위젯이 그려진 날짜와 오늘이 다르면(자정 넘김) 기록하지 않고 다시 그리기만 한다 — 어제 상태를 보고 오늘을 잘못 찍는 것을 막는다.
 */
class ToggleCheckAction : ActionCallback {

    companion object {
        val HabitIdKey = ActionParameters.Key<Int>("habit_id")
        val DateKey = ActionParameters.Key<String>("rendered_date")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        val today = LocalDate.now().toString()
        if (parameters[DateKey] != today) {
            HabitWidgets.updateAll(context)
            return
        }
        val repo = context.widgetRepository()
        repo.toggleCompletion(habitId, today)
        RecordSideEffects.afterRecordChange(context, repo, habitId)
        HabitWidgets.updateAll(context)
    }
}
