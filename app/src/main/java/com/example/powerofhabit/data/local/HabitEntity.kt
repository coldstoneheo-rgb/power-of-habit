package com.example.powerofhabit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "Habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val habitId: Int = 0,
    val title: String,
    val question: String,
    val frequencyType: String, // DAILY, WEEKLY, SPECIFIC_DAYS
    val frequencyValue: String, // e.g. "MON,WED,FRI"
    val reminderTime: String?, // Stored as ISO string "09:00"
    val themeColor: String, // e.g. "#FFCC00"
    val habitType: String, // "CHECK", "VALUE"
    val unit: String?,
    val isReminderEnabled: Boolean = false,
    val memo: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
