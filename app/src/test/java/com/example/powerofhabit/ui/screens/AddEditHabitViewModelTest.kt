package com.example.powerofhabit.ui.screens

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

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAddEditRepository
    private lateinit var viewModel: AddEditHabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAddEditRepository()
        viewModel = AddEditHabitViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveHabit_withBlankTitle_emitsErrorEvent() = runTest(testDispatcher) {
        val events = mutableListOf<AddEditHabitUiEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveHabit(
            title = "   ",
            question = "Did you make bed?",
            frequencyType = "DAILY",
            frequencyValue = "",
            reminderTime = null,
            themeColor = "#FF5722",
            habitType = "CHECK",
            unit = null
        )
        
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is AddEditHabitUiEvent.Error)
        assertEquals("Title cannot be blank", (events[0] as AddEditHabitUiEvent.Error).message)

        collectJob.cancel()
    }

    @Test
    fun saveHabit_withValidData_emitsSaveSuccessEvent() = runTest(testDispatcher) {
        val events = mutableListOf<AddEditHabitUiEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveHabit(
            title = "Make bed",
            question = "Did you make bed?",
            frequencyType = "DAILY",
            frequencyValue = "",
            reminderTime = null,
            themeColor = "#FF5722",
            habitType = "CHECK",
            unit = null
        )
        
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is AddEditHabitUiEvent.SaveSuccess)
        assertEquals(1, fakeRepository.insertedHabits.size)
        assertEquals("Make bed", fakeRepository.insertedHabits[0].title)

        collectJob.cancel()
    }

    private class FakeAddEditRepository : DataRepository {
        val insertedHabits = mutableListOf<HabitEntity>()

        override fun getAllHabits(): Flow<List<HabitEntity>> = flow { emit(emptyList()) }
        override fun getHabitById(habitId: Int): Flow<HabitEntity?> = flow { emit(null) }
        
        override suspend fun insertHabit(habit: HabitEntity): Long {
            insertedHabits.add(habit)
            return 1L
        }
        
        override suspend fun updateHabit(habit: HabitEntity) {}
        override suspend fun deleteHabit(habit: HabitEntity) {}

        override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override suspend fun insertRecord(record: HabitRecordEntity): Long = 0L
        override suspend fun updateRecordStatus(recordId: Int, status: String) {}
        override suspend fun deleteRecord(record: HabitRecordEntity) {}
    }
}
