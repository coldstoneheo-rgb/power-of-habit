package com.example.powerofhabit.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    // Habits
    @Query("SELECT * FROM Habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM Habits WHERE habitId = :habitId LIMIT 1")
    fun getHabitById(habitId: Int): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    // Habit Records
    @Query("SELECT * FROM HabitRecords WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM HabitRecords WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM HabitRecords WHERE date >= :startDate AND date <= :endDate")
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: HabitRecordEntity): Long

    @Query("UPDATE HabitRecords SET status = :status WHERE recordId = :recordId")
    suspend fun updateRecordStatus(recordId: Int, status: String)

    @Delete
    suspend fun deleteRecord(record: HabitRecordEntity)
}
