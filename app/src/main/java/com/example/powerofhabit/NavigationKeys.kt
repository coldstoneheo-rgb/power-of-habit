package com.example.powerofhabit

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class HabitDetail(val habitId: Int) : NavKey
@Serializable data class AddEditHabit(val habitId: Int) : NavKey
@Serializable data object Badges : NavKey
