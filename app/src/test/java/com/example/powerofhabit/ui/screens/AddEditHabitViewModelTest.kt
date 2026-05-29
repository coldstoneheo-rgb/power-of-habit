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

    @Test
    fun saveHabit_existingHabit_updatesAndPreservesCreatedAt() = runTest(testDispatcher) {
        val existingHabit = HabitEntity(
            habitId = 42,
            title = "Old Title",
            question = "Old Question",
            frequencyType = "DAILY",
            frequencyValue = "",
            reminderTime = null,
            themeColor = "#000000",
            habitType = "CHECK",
            unit = null,
            createdAt = 123456789L
        )
        fakeRepository.habits[42] = existingHabit

        val events = mutableListOf<AddEditHabitUiEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveHabit(
            habitId = 42,
            title = "New Title",
            question = "New Question",
            frequencyType = "WEEKLY",
            frequencyValue = "1",
            reminderTime = "08:00",
            themeColor = "#FFFFFF",
            habitType = "CHECK",
            unit = "pages"
        )

        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is AddEditHabitUiEvent.SaveSuccess)
        
        val updated = fakeRepository.habits[42]
        assertEquals("New Title", updated?.title)
        assertEquals("New Question", updated?.question)
        assertEquals("WEEKLY", updated?.frequencyType)
        assertEquals("1", updated?.frequencyValue)
        assertEquals("08:00", updated?.reminderTime)
        assertEquals("#FFFFFF", updated?.themeColor)
        assertEquals("CHECK", updated?.habitType)
        assertEquals("pages", updated?.unit)
        assertEquals(123456789L, updated?.createdAt) // Check that createdAt is preserved!

        collectJob.cancel()
    }

    private class FakeAddEditRepository : DataRepository {
        val habits = mutableMapOf<Int, HabitEntity>()
        val insertedHabits = mutableListOf<HabitEntity>()

        override fun getAllHabits(): Flow<List<HabitEntity>> = flow { emit(habits.values.toList()) }
        override fun getHabitById(habitId: Int): Flow<HabitEntity?> = flow { emit(habits[habitId]) }
        
        override suspend fun insertHabit(habit: HabitEntity): Long {
            insertedHabits.add(habit)
            val id = if (habit.habitId == 0) habits.size + 1 else habit.habitId
            habits[id] = habit.copy(habitId = id)
            return id.toLong()
        }
        
        override suspend fun updateHabit(habit: HabitEntity) {
            habits[habit.habitId] = habit
        }
        
        override suspend fun deleteHabit(habit: HabitEntity) {
            habits.remove(habit.habitId)
        }

        override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = flow { emit(emptyList()) }
        override suspend fun insertRecord(record: HabitRecordEntity): Long = 0L
        override suspend fun updateRecordStatus(recordId: Int, status: String) {}
        override suspend fun deleteRecord(record: HabitRecordEntity) {}
    }
}
