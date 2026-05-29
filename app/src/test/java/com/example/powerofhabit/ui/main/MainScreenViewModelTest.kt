package com.example.powerofhabit.ui.main

import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(viewModel.uiState.first(), MainScreenUiState.Loading)
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(viewModel.uiState.first(), MainScreenUiState.Loading)
  }
}

private class FakeMyModelRepository : DataRepository {
  private val habits = listOf(
    HabitEntity(habitId = 1, title = "Sample", question = "Question?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#FFFFFF", habitType = "CHECK", unit = null)
  )

  override fun getAllHabits(): Flow<List<HabitEntity>> = flow { emit(habits) }

  override fun getHabitById(habitId: Int): Flow<HabitEntity?> = flow { emit(habits.find { it.habitId == habitId }) }
  override suspend fun insertHabit(habit: HabitEntity): Long = 0L
  override suspend fun updateHabit(habit: HabitEntity) {}
  override suspend fun deleteHabit(habit: HabitEntity) {}

  override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
  override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
  override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
  override suspend fun insertRecord(record: HabitRecordEntity): Long = 0L
  override suspend fun updateRecordStatus(recordId: Int, status: String) {}
  override suspend fun deleteRecord(record: HabitRecordEntity) {}
}
