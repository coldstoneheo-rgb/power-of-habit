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

    @Query("SELECT * FROM HabitRecords")
    fun getAllRecords(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM HabitRecords WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getRecord(habitId: Int, date: String): HabitRecordEntity?

    @Query("SELECT * FROM HabitRecords WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate")
    fun getRecordsForHabitBetween(habitId: Int, startDate: String, endDate: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM HabitRecords WHERE date >= :startDate AND date <= :endDate")
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: HabitRecordEntity): Long

    @Query("UPDATE HabitRecords SET status = :status WHERE recordId = :recordId")
    suspend fun updateRecordStatus(recordId: Int, status: String)

    @Delete
    suspend fun deleteRecord(record: HabitRecordEntity)

    /**
     * 체크 토글의 단일 정의: 없음 → COMPLETED, COMPLETED → FAILED, 그 외(FAILED/SKIPPED) → COMPLETED.
     * 트랜잭션이라 동시 호출(위젯 연타)에도 같은 날 행이 두 개 생기지 않는다. 결과 상태를 돌려준다.
     */
    @Transaction
    suspend fun toggleCompletion(habitId: Int, date: String): String {
        val existing = getRecord(habitId, date)
        return if (existing == null) {
            insertRecord(HabitRecordEntity(habitId = habitId, date = date, status = "COMPLETED", inputValue = null))
            "COMPLETED"
        } else {
            val next = if (existing.status == "COMPLETED") "FAILED" else "COMPLETED"
            updateRecordStatus(existing.recordId, next)
            next
        }
    }

    /**
     * 수치형 값 저장의 단일 정의: 같은 (habitId, date) 기존 행을 지우고 새 값으로 넣는다.
     * 트랜잭션이라 위젯 입력 액티비티가 도중에 종료되거나 동시 쓰기가 있어도 행이 두 개가 되거나 삭제만 남지 않는다.
     */
    @Transaction
    suspend fun upsertValueRecord(habitId: Int, date: String, status: String, inputValue: Float): Long {
        getRecord(habitId, date)?.let { deleteRecord(it) }
        return insertRecord(HabitRecordEntity(habitId = habitId, date = date, status = status, inputValue = inputValue))
    }

    // Badges
    @Query("SELECT * FROM Badges ORDER BY earnedAt DESC")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeEntity): Long
}
