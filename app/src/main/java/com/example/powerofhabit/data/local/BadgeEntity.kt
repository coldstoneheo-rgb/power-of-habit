package com.example.powerofhabit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Badges")
data class BadgeEntity(
    @PrimaryKey val badgeId: String, // e.g. "START_FIRST", "STREAK_3", "STREAK_5", "STREAK_30", "HABIT_COMPLETE"
    val badgeName: String,
    val description: String,
    val earnedAt: Long,
    val badgeIconType: String // e.g. "GOLD", "SILVER", "BRONZE", "DEFAULT"
)
