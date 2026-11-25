package com.example.powerofhabit

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class HabitDetail(val habitId: Int) : NavKey
@Serializable data class AddEditHabit(val habitId: Int, val defaultHabitType: String = "CHECK", /** 열 때마다 다른 값 — 같은 화면을 곧바로 다시 열어도 새 ViewModel을 쓴다. */ val openId: Long = 0) : NavKey
@Serializable data object Badges : NavKey
