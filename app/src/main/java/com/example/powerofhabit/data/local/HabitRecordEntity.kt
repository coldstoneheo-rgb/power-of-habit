package com.example.powerofhabit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "HabitRecords",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["habitId"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val habitId: Int,
    val date: String, // "YYYY-MM-DD"
    val status: String, // "COMPLETED", "FAILED", "SKIPPED"
    val inputValue: Float? // Nullable
)
