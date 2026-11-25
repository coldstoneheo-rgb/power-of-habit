package com.example.powerofhabit.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.powerofhabit.MainActivity
import com.example.powerofhabit.R
import com.example.powerofhabit.data.DataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: DataRepository

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("HABIT_ID", -1)
        val habitTitle = intent.getStringExtra("HABIT_TITLE")?.takeIf { it.isNotBlank() } ?: "습관 알림"
        val habitQuestion = intent.getStringExtra("HABIT_QUESTION")?.takeIf { it.isNotBlank() } ?: "오늘 기록을 남겨 보세요."

        if (habitId == -1) return

        // 1. Show notification
        showNotification(context, habitId, habitTitle, habitQuestion)

        // 2. Re-schedule next alarm for the next day
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = repository.getHabitById(habitId).first()
                if (habit != null && habit.isReminderEnabled) {
                    HabitReminderManager(context).scheduleReminder(habit)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, habitId: Int, title: String, message: String) {
        val channelId = "habit_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "습관 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "정해 둔 시각에 습관을 알려 줍니다"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_widget_check)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habitId, notification)
    }
}
