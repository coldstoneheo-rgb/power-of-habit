package com.example.powerofhabit.ui.screens

import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDetailRepository
    private lateinit var viewModel: HabitDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDetailRepository()
        viewModel = HabitDetailViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoading() = runTest {
        assertEquals(HabitDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun setHabitId_loadsHabitAndRecordsSuccessfully() = runTest(testDispatcher) {
        val habit = HabitEntity(habitId = 1, title = "Sleep early", question = "In bed by 10?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#000000", habitType = "CHECK", unit = null)
        val records = listOf(
            HabitRecordEntity(recordId = 1, habitId = 1, date = "2026-05-29", status = "COMPLETED", inputValue = null)
        )
        fakeRepository.setHabit(habit)
        fakeRepository.setRecords(records)

        val states = mutableListOf<HabitDetailUiState>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        viewModel.setHabitId(1)
        
        advanceUntilIdle()

        val lastState = states.lastOrNull()
        assertTrue(lastState is HabitDetailUiState.Success)
        val successState = lastState as HabitDetailUiState.Success
        assertEquals("Sleep early", successState.habit.title)
        assertEquals(1, successState.records.size)
        assertEquals("COMPLETED", successState.records[0].status)

        collectJob.cancel()
    }

    private class FakeDetailRepository : DataRepository {
        private val habitFlow = MutableStateFlow<HabitEntity?>(null)
        private val recordsFlow = MutableStateFlow<List<HabitRecordEntity>>(emptyList())

        fun setHabit(habit: HabitEntity) {
            habitFlow.value = habit
        }

        fun setRecords(list: List<HabitRecordEntity>) {
            recordsFlow.value = list
        }

        override fun getAllHabits(): Flow<List<HabitEntity>> = flow { emit(emptyList()) }
        override fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitFlow
        override suspend fun insertHabit(habit: HabitEntity): Long = 0L
        override suspend fun updateHabit(habit: HabitEntity) {}
        override suspend fun deleteHabit(habit: HabitEntity) {}

        override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = recordsFlow
        override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override suspend fun insertRecord(record: HabitRecordEntity): Long = 0L
        override suspend fun updateRecordStatus(recordId: Int, status: String) {}
        override suspend fun deleteRecord(record: HabitRecordEntity) {}
    }
}
