package com.example.powerofhabit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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

  // 닫히는 중인 화면의 버튼을 다시 눌러도 다른 화면이 닫히거나 백스택이 비지 않게, 자기가 맨 위일 때만 닫는다.
  fun popFrom(key: NavKey) {
    if (backStack.size > 1 && backStack.lastOrNull() == key) backStack.removeLastOrNull()
  }

  NavDisplay(
    backStack = backStack,
    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
    // 화면마다 ViewModel을 따로 둔다. 없으면 액티비티에 하나만 생겨 «수정»에서 열었던 값이 «만들기»에 남는다.
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator()
    ),
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onNavigateToDetail = { habitId ->
              backStack.add(HabitDetail(habitId = habitId))
            },
            onNavigateToAddHabit = { habitType ->
              backStack.add(AddEditHabit(habitId = 0, defaultHabitType = habitType, openId = System.nanoTime()))
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
            onBack = { popFrom(key) },
            onNavigateToEdit = { habitId ->
              backStack.add(AddEditHabit(habitId = habitId, openId = System.nanoTime()))
            }
          )
        }
        entry<AddEditHabit> { key ->
          AddEditHabitScreen(
            habitId = key.habitId,
            defaultHabitType = key.defaultHabitType,
            onBack = { popFrom(key) },
            onDeleted = { while (backStack.size > 1) backStack.removeLastOrNull() }
          )
        }
        entry<Badges> {
          BadgesScreen(
            onBack = { popFrom(Badges) }
          )
        }
      },
  )
}
