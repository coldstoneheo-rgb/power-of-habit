package com.example.powerofhabit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.powerofhabit.ui.main.MainScreen
import com.example.powerofhabit.ui.screens.HabitDetailScreen
import com.example.powerofhabit.ui.screens.AddEditHabitScreen

import com.example.powerofhabit.ui.screens.BadgesScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onNavigateToDetail = { habitId ->
              backStack.add(HabitDetail(habitId = habitId))
            },
            onNavigateToAddHabit = {
              backStack.add(AddEditHabit(habitId = 0))
            },
            onNavigateToBadges = {
              backStack.add(Badges)
            },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<HabitDetail> { key ->
          HabitDetailScreen(
            habitId = key.habitId,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToEdit = { habitId ->
              backStack.add(AddEditHabit(habitId = habitId))
            }
          )
        }
        entry<AddEditHabit> { key ->
          AddEditHabitScreen(
            habitId = key.habitId,
            onBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Badges> {
          BadgesScreen(
            onBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
