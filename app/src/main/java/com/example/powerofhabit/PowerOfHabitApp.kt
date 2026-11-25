package com.example.powerofhabit

import android.app.Application
import android.util.Log
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.reminder.HabitReminderManager
import com.example.powerofhabit.widget.WidgetRefreshObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PowerOfHabitApp : Application() {

    @Inject lateinit var widgetRefreshObserver: WidgetRefreshObserver
    @Inject lateinit var repository: DataRepository

    override fun onCreate() {
        super.onCreate()
        // 홈 위젯은 DB 변화를 관찰해 자동 갱신한다(위젯 브로드캐스트로 프로세스가 깨어나도 여기서 시작됨).
        widgetRefreshObserver.start()
        restoreMissingReminders()
    }

    /** 복원·강제 종료로 알람만 사라진 상태를 프로세스가 뜰 때 되살린다. */
    private fun restoreMissingReminders() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                HabitReminderManager(this@PowerOfHabitApp).scheduleMissing(repository.getAllHabits().first())
            } catch (e: Exception) {
                Log.e("PowerOfHabitApp", "Failed to restore reminders", e)
            }
        }
    }
}
