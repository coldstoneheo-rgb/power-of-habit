package com.example.powerofhabit.widget

import android.content.Context
import android.util.Log
import com.example.powerofhabit.data.DataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위젯 자동 갱신. Room의 무효화 Flow를 관찰해 습관·기록이 바뀔 때마다 모든 위젯을 다시 그린다.
 * 화면·백업 복원·위젯 액션 등 어떤 경로로 DB가 바뀌어도 한 곳에서 처리하므로 쓰기 지점마다 갱신 코드를 붙일 필요가 없다.
 * 자정이 지나면 "오늘"이 바뀌므로 자정 직후에도 한 번 갱신한다(프로세스가 살아 있는 동안).
 */
@Singleton
class WidgetRefreshObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DataRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(repository.getAllHabits(), repository.getAllRecords()) { _, _ -> Unit }
                .conflate()
                .collect {
                    delay(150) // 연속 쓰기(체크 + 뱃지 등)를 한 번의 렌더로 합친다
                    HabitWidgets.updateAll(context)
                }
        }
        scope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
                delay(Duration.between(now, nextMidnight).toMillis() + 1_000)
                try {
                    HabitWidgets.updateAll(context)
                } catch (e: Exception) {
                    Log.w("WidgetRefreshObserver", "midnight refresh failed", e)
                }
            }
        }
    }
}
