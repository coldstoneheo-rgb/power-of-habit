package com.example.powerofhabit.data

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitDao
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DataRepository {
    // Habits
    fun getAllHabits(): Flow<List<HabitEntity>>
    fun getHabitById(habitId: Int): Flow<HabitEntity?>
    suspend fun insertHabit(habit: HabitEntity): Long
    suspend fun updateHabit(habit: HabitEntity)
    suspend fun deleteHabit(habit: HabitEntity)

    // Habit Records
    fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>>
    fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>>
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>>
    suspend fun insertRecord(record: HabitRecordEntity): Long
    suspend fun updateRecordStatus(recordId: Int, status: String)
    suspend fun deleteRecord(record: HabitRecordEntity)

    // Badges
    fun getAllBadges(): Flow<List<BadgeEntity>>
    suspend fun insertBadge(badge: BadgeEntity): Long
}

@Singleton
class DefaultDataRepository @Inject constructor(
    private val habitDao: HabitDao
) : DataRepository {

    override fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    override fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitDao.getHabitById(habitId)

    override suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = habitDao.getRecordsForDate(date)

    override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = habitDao.getRecordsForHabit(habitId)

    override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = habitDao.getRecordsBetween(startDate, endDate)

    override suspend fun insertRecord(record: HabitRecordEntity): Long = habitDao.insertRecord(record)

    override suspend fun updateRecordStatus(recordId: Int, status: String) = habitDao.updateRecordStatus(recordId, status)

    override suspend fun deleteRecord(record: HabitRecordEntity) = habitDao.deleteRecord(record)

    override fun getAllBadges(): Flow<List<BadgeEntity>> = habitDao.getAllBadges()

    override suspend fun insertBadge(badge: BadgeEntity): Long = habitDao.insertBadge(badge)
}
