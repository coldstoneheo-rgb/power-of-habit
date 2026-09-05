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
    val targetValue: Float? = null,
    /** 수치형 목표 방향: "AT_LEAST"(값 ≥ 목표 성공, 기본) / "AT_MOST"(값 ≤ 목표 성공). 판정은 domain/RecordOutcomes. */
    val targetType: String = "AT_LEAST",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 색을 알 수 없을 때의 기본 습관색(브랜드 오렌지). ui/theme의 HabitOrange와 같은 값 — 테마 테스트가 일치를 고정한다. */
        const val DEFAULT_THEME_COLOR_HEX = "#FF9800"
    }
}
