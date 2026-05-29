package com.example.powerofhabit.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.powerofhabit.data.local.HabitEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HabitReminderManager(private val context: Context) {

    fun scheduleReminder(habit: HabitEntity) {
        if (!habit.isReminderEnabled || habit.reminderTime.isNullOrBlank()) {
            cancelReminder(habit.habitId)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habit.habitId)
            putExtra("HABIT_TITLE", habit.title)
            putExtra("HABIT_QUESTION", habit.question)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val timeParts = habit.reminderTime.split(":")
            if (timeParts.size != 2) return
            val hour = timeParts[0].toIntOrNull() ?: 9
            val minute = timeParts[1].toIntOrNull() ?: 0

            val reminderTime = LocalTime.of(hour, minute)
            var targetDateTime = LocalDateTime.of(LocalDate.now(), reminderTime)

            // If scheduled time has already passed today, schedule for tomorrow
            if (targetDateTime.isBefore(LocalDateTime.now())) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val triggerTimeMs = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d("HabitReminderManager", "Scheduled reminder for habit ${habit.habitId} at $targetDateTime")
        } catch (e: Exception) {
            Log.e("HabitReminderManager", "Failed to schedule alarm for habit ${habit.habitId}", e)
        }
    }

    fun cancelReminder(habitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("HabitReminderManager", "Cancelled reminder for habit $habitId")
        }
    }
}
