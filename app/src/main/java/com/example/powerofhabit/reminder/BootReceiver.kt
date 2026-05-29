package com.example.powerofhabit.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.powerofhabit.data.DataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: DataRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val habits = repository.getAllHabits().first()
                    val reminderManager = HabitReminderManager(context)
                    habits.forEach { habit ->
                        if (habit.isReminderEnabled) {
                            reminderManager.scheduleReminder(habit)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
