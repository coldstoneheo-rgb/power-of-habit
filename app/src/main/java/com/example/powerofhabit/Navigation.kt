package com.example.powerofhabit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun MainNavigation(
  initialHabitId: Int = -1,
  onInitialHabitConsumed: () -> Unit = {}
) {
  val backStack = rememberNavBackStack(Main)

  // 홈 위젯에서 진입: 해당 습관 상세를 스택 위에 올린다.
  LaunchedEffect(initialHabitId) {
    if (initialHabitId > 0) {
      val target = HabitDetail(habitId = initialHabitId)
      val index = backStack.indexOfLast { it == target }
      if (index >= 0) {
        while (backStack.size > index + 1) backStack.removeLastOrNull()
      } else {
        backStack.add(target)
      }
      onInitialHabitConsumed()
    }
  }

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
            onNavigateToAddHabit = { habitType ->
              backStack.add(AddEditHabit(habitId = 0, defaultHabitType = habitType))
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
            defaultHabitType = key.defaultHabitType,
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
