package com.example.powerofhabit.data

import com.example.powerofhabit.data.local.HabitDao
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DefaultDataRepositoryTest {

    private lateinit var fakeDao: FakeHabitDao
    private lateinit var repository: DefaultDataRepository

    @Before
    fun setUp() {
        fakeDao = FakeHabitDao()
        repository = DefaultDataRepository(fakeDao)
    }

    @Test
    fun getAllHabits_returnsCorrectData() = runTest {
        val habits = listOf(
            HabitEntity(habitId = 1, title = "Exercise", question = "Did you workout?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#FF0000", habitType = "CHECK", unit = null),
            HabitEntity(habitId = 2, title = "Read", question = "Did you read?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#00FF00", habitType = "CHECK", unit = null)
        )
        fakeDao.setHabits(habits)

        val result = repository.getAllHabits().first()
        assertEquals(2, result.size)
        assertEquals("Exercise", result[0].title)
        assertEquals("Read", result[1].title)
    }

    @Test
    fun insertHabit_updatesDao() = runTest {
        val habit = HabitEntity(habitId = 3, title = "Meditation", question = "Did you meditate?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#0000FF", habitType = "CHECK", unit = null)
        
        val id = repository.insertHabit(habit)
        assertEquals(3L, id)

        val result = repository.getAllHabits().first()
        assertEquals(1, result.size)
        assertEquals("Meditation", result[0].title)
    }

    @Test
    fun deleteHabit_removesFromDao() = runTest {
        val habit = HabitEntity(habitId = 1, title = "Exercise", question = "Did you workout?", frequencyType = "DAILY", frequencyValue = "", reminderTime = null, themeColor = "#FF0000", habitType = "CHECK", unit = null)
        fakeDao.setHabits(listOf(habit))

        repository.deleteHabit(habit)

        val result = fakeDao.getAllHabits().first()
        assertEquals(0, result.size)
    }

    @Test
    fun getRecordsForDate_returnsCorrectRecords() = runTest {
        val records = listOf(
            HabitRecordEntity(recordId = 1, habitId = 1, date = "2026-05-29", status = "COMPLETED", inputValue = null),
            HabitRecordEntity(recordId = 2, habitId = 2, date = "2026-05-29", status = "FAILED", inputValue = null),
            HabitRecordEntity(recordId = 3, habitId = 1, date = "2026-05-28", status = "COMPLETED", inputValue = null)
        )
        fakeDao.setRecords(records)

        val result = repository.getRecordsForDate("2026-05-29").first()
        assertEquals(2, result.size)
        assertEquals("COMPLETED", result.first { it.habitId == 1 }.status)
        assertEquals("FAILED", result.first { it.habitId == 2 }.status)
    }

    // Fake implementation of HabitDao for testing
    private class FakeHabitDao : HabitDao {
        private val habitsFlow = MutableStateFlow<List<HabitEntity>>(emptyList())
        private val recordsFlow = MutableStateFlow<List<HabitRecordEntity>>(emptyList())

        fun setHabits(list: List<HabitEntity>) {
            habitsFlow.value = list
        }

        fun setRecords(list: List<HabitRecordEntity>) {
            recordsFlow.value = list
        }

        override fun getAllHabits(): Flow<List<HabitEntity>> = habitsFlow

        override fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitsFlow.map { list ->
            list.find { it.habitId == habitId }
        }

        override suspend fun insertHabit(habit: HabitEntity): Long {
            val current = habitsFlow.value.toMutableList()
            current.add(habit)
            habitsFlow.value = current
            return habit.habitId.toLong()
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            val current = habitsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.habitId == habit.habitId }
            if (index != -1) {
                current[index] = habit
                habitsFlow.value = current
            }
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            val current = habitsFlow.value.toMutableList()
            current.removeIf { it.habitId == habit.habitId }
            habitsFlow.value = current
        }

        override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = recordsFlow.map { list ->
            list.filter { it.date == date }
        }

        override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = recordsFlow.map { list ->
            list.filter { it.habitId == habitId }
        }

        override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = recordsFlow.map { list ->
            list.filter { it.date in startDate..endDate }
        }

        override suspend fun insertRecord(record: HabitRecordEntity): Long {
            val current = recordsFlow.value.toMutableList()
            current.add(record)
            recordsFlow.value = current
            return record.recordId.toLong()
        }

        override suspend fun updateRecordStatus(recordId: Int, status: String) {
            val current = recordsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.recordId == recordId }
            if (index != -1) {
                val updated = current[index].copy(status = status)
                current[index] = updated
                recordsFlow.value = current
            }
        }

        override suspend fun deleteRecord(record: HabitRecordEntity) {
            val current = recordsFlow.value.toMutableList()
            current.removeIf { it.recordId == record.recordId }
            recordsFlow.value = current
        }
    }
}
