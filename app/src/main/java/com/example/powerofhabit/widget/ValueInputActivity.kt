package com.example.powerofhabit.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.RecordSideEffects
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.data.local.SettingsManager
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.components.SuccessBurst
import com.example.powerofhabit.ui.components.ValueInputDialog
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * 1x1 위젯의 수치형 습관 탭 → 앱을 열지 않고 값만 입력하는 투명 액티비티 (결정 기록 2026-09-05 결정 2, 선택지 A).
 * - 메인 화면과 같은 [ValueInputDialog]와 저장 규칙([RecordOutcomes.statusForValue])을 쓴다.
 * - 위젯이 그려진 날짜(EXTRA_RENDERED_DATE)와 오늘이 다르면 기록하지 않고 위젯만 다시 그린다(자정 가드, ToggleCheckAction과 동일).
 * - 저장 후 해당 위젯만 즉시 갱신하고, 성공이면 폭죽을 잠깐 보여준 뒤 닫는다.
 */
@AndroidEntryPoint
class ValueInputActivity : ComponentActivity() {

    companion object {
        const val EXTRA_HABIT_ID = "com.example.powerofhabit.extra.HABIT_ID"
        const val EXTRA_RENDERED_DATE = "com.example.powerofhabit.extra.RENDERED_DATE"
        private const val BURST_MILLIS = 480L
    }

    @Inject lateinit var repository: DataRepository
    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
        val renderedDate = intent.getStringExtra(EXTRA_RENDERED_DATE)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val today = LocalDate.now().toString()
        if (habitId <= 0) { finish(); return }
        if (renderedDate != null && renderedDate != today) {
            // 자정을 넘긴 위젯: 어제 화면을 보고 오늘을 기록하지 않게 한다.
            Toast.makeText(this, "위젯을 새로 고쳤습니다. 다시 눌러 주세요.", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch { refreshWidget(appWidgetId); finish() }
            return
        }

        setContent {
            val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            var habit by remember { mutableStateOf<HabitEntity?>(null) }
            var existing by remember { mutableStateOf<HabitRecordEntity?>(null) }
            var loaded by remember { mutableStateOf(false) }
            var burstAccent by remember { mutableStateOf<androidx.compose.ui.graphics.Color?>(null) }

            androidx.compose.runtime.LaunchedEffect(habitId) {
                habit = repository.getHabitById(habitId).first()
                existing = repository.getRecord(habitId, today)
                loaded = true
                if (habit == null) {
                    Toast.makeText(this@ValueInputActivity, "삭제되었거나 찾을 수 없는 습관입니다", Toast.LENGTH_SHORT).show()
                    refreshWidget(appWidgetId)
                    finish()
                }
            }

            PowerOfHabitTheme(darkTheme = darkTheme, applyWindowChrome = false) {
                val h = habit
                val accent = burstAccent
                if (accent != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SuccessBurst(accent = accent, modifier = Modifier.size(160.dp), radiusScale = 1.2f)
                    }
                } else if (loaded && h != null) {
                    ValueInputDialog(
                        habit = h,
                        initialValue = existing?.inputValue,
                        accent = HabitWidgets.parseThemeColor(h.themeColor),
                        autoFocus = true,
                        onDismiss = { finish() },
                        onSave = { value, outcome ->
                            lifecycleScope.launch {
                                val saved = save(h, value, today)
                                if (!saved) {
                                    Toast.makeText(this@ValueInputActivity, "날짜가 바뀌어 저장하지 않았습니다. 다시 눌러 주세요.", Toast.LENGTH_SHORT).show()
                                    refreshWidget(appWidgetId)
                                    finish()
                                    return@launch
                                }
                                refreshWidget(appWidgetId)
                                if (outcome == RecordOutcome.SUCCESS) {
                                    burstAccent = HabitWidgets.parseThemeColor(h.themeColor)
                                    kotlinx.coroutines.delay(BURST_MILLIS)
                                }
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * 화면을 연 날짜([openedDate])에 저장한다. 쓰기 직전에 날짜가 바뀌었으면 저장하지 않고 false.
     * 쓰기는 NonCancellable + DAO 트랜잭션이라 액티비티가 도중에 종료되어도 삭제만 남거나 행이 둘이 되지 않는다.
     */
    private suspend fun save(habit: HabitEntity, value: Float, openedDate: String): Boolean {
        if (LocalDate.now().toString() != openedDate) return false
        return try {
            withContext(NonCancellable) {
                repository.upsertValueRecord(
                    habitId = habit.habitId,
                    date = openedDate,
                    status = RecordOutcomes.statusForValue(value, habit.targetValue, habit.targetType),
                    inputValue = value
                )
                RecordSideEffects.afterRecordChange(applicationContext, repository, habit.habitId)
            }
            true
        } catch (e: Exception) {
            Log.e("ValueInputActivity", "save failed", e)
            Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private suspend fun refreshWidget(appWidgetId: Int) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) HabitWidgets.updateOne(this, appWidgetId)
        else HabitWidgets.updateAll(this)
    }
}
