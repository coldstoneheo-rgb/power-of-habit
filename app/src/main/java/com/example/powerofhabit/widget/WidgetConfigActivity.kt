package com.example.powerofhabit.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.SettingsManager
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme
import com.example.powerofhabit.ui.theme.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 위젯 설정 액티비티: 어떤 습관을 보여줄지 고른다. 체크·캘린더 위젯이 공유한다.
 * 선택 즉시 Glance 상태에 habitId를 쓰고 해당 위젯을 갱신한 뒤 RESULT_OK로 닫는다.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var repository: DataRepository
    @Inject lateinit var settingsManager: SettingsManager

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private companion object { const val TAG = "WidgetConfig" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // 런처가 취소하면 위젯이 남지 않도록 기본은 CANCELED. (빈 위젯의 재설정 탭으로도 열린다 — 결과는 무시됨)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val habits by repository.getAllHabits().collectAsStateWithLifecycle(initialValue = emptyList())
            PowerOfHabitTheme(darkTheme = darkTheme) {
                HabitPicker(habits = habits, onPick = ::onHabitPicked)
            }
        }
    }

    private fun onHabitPicked(habit: HabitEntity) {
        lifecycleScope.launch {
            try {
                // getGlanceIdBy는 런처가 이미 지운 id면 IllegalArgumentException — 조용히 죽지 않고 안내 후 닫는다.
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                    prefs[HabitWidgets.HABIT_ID] = habit.habitId
                }
                // 방금 추가된 위젯은 Glance 내부 매핑에 아직 없을 수 있으므로 provider 기준으로 이 인스턴스만 즉시 그린다.
                HabitWidgets.updateOne(this@WidgetConfigActivity, appWidgetId)
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            } catch (e: CancellationException) {
                throw e // 액티비티가 먼저 닫힌 것 — 실패가 아니다
            } catch (e: Exception) {
                Log.w(TAG, "widget configure failed id=$appWidgetId", e)
                Toast.makeText(this@WidgetConfigActivity, "위젯을 설정할 수 없습니다. 위젯을 지우고 다시 추가해 주세요.", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}

@Composable
private fun HabitPicker(habits: List<HabitEntity>, onPick: (HabitEntity) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HabitTheme.colors.bgBase)
            .safeDrawingPadding()
            .padding(horizontal = Space.screenH, vertical = Space.s4)
    ) {
        Text(
            text = "위젯에 표시할 습관",
            color = HabitTheme.colors.textPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(Space.s4))
        if (habits.isEmpty()) {
            Text(
                text = "등록된 습관이 없습니다. 앱에서 먼저 습관을 추가하세요.",
                color = HabitTheme.colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(HabitTheme.colors.bgLayer2)
        ) {
            items(habits, key = { it.habitId }) { habit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(habit) }
                        .padding(horizontal = Space.s4, vertical = Space.s3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s3)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(HabitWidgets.parseThemeColor(habit.themeColor))
                    )
                    Text(
                        text = habit.title,
                        color = HabitTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(0.dp).weight(1f)
                    )
                    Text(
                        text = if (habit.habitType == "VALUE") "수치" else "체크",
                        color = HabitTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                HorizontalDivider(color = HabitTheme.colors.lineHair, thickness = 0.5.dp)
            }
        }
    }
}
