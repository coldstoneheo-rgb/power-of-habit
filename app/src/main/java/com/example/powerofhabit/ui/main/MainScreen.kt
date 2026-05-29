package com.example.powerofhabit.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainScreenUiState.Loading -> {
      // Blank
    }
    is MainScreenUiState.Success -> {
      MainScreen(habits = (state as MainScreenUiState.Success).habits, modifier = modifier)
    }
    is MainScreenUiState.Error -> {
      Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
    }
  }
}

@Composable
internal fun MainScreen(habits: List<HabitEntity>, modifier: Modifier = Modifier) {
  Column(modifier) { habits.forEach { Greeting(it.title) } }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  PowerOfHabitTheme { MainScreen(emptyList()) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  PowerOfHabitTheme { MainScreen(emptyList()) }
}
