package com.example.powerofhabit.ui.main

import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.powerofhabit.data.local.SettingsManager

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMyModelRepository
    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMyModelRepository()
        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        val mockSettingsManager = org.mockito.Mockito.mock(SettingsManager::class.java)
        org.mockito.Mockito.`when`(mockSettingsManager.isDarkMode).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(true))
        org.mockito.Mockito.`when`(mockSettingsManager.isDateDescending).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(true))
        viewModel = MainScreenViewModel(fakeRepository, mockSettingsManager, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoading() = runTest {
        assertEquals(MainScreenUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun uiState_loadsHabitsAndRecordsSuccessfully() = runTest(testDispatcher) {
        val states = mutableListOf<MainScreenUiState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        advanceUntilIdle()

        val lastState = states.lastOrNull()
        assertTrue(lastState is MainScreenUiState.Success)
        val successState = lastState as MainScreenUiState.Success
        assertEquals(1, successState.habits.size)
        assertEquals("Sample", successState.habits[0].title)

        collectJob.cancel()
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

        override fun getAllBadges(): Flow<List<com.example.powerofhabit.data.local.BadgeEntity>> = flow { emit(emptyList()) }
        override suspend fun insertBadge(badge: com.example.powerofhabit.data.local.BadgeEntity): Long = 0L
    }
}
