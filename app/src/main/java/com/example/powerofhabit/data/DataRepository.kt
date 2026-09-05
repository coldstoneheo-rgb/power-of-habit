package com.example.powerofhabit.data

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitDao
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import androidx.room.withTransaction
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
    fun getAllRecords(): Flow<List<HabitRecordEntity>>
    suspend fun getRecord(habitId: Int, date: String): HabitRecordEntity?
    fun getRecordsForHabitBetween(habitId: Int, startDate: String, endDate: String): Flow<List<HabitRecordEntity>>
    /** 체크 토글(트랜잭션). 규칙은 HabitDao.toggleCompletion 참조. 결과 상태 반환. */
    suspend fun toggleCompletion(habitId: Int, date: String): String
    /** 수치형 값 저장(트랜잭션): 같은 날 기존 행 교체. 규칙은 HabitDao.upsertValueRecord 참조. */
    suspend fun upsertValueRecord(habitId: Int, date: String, status: String, inputValue: Float): Long
    fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>>
    suspend fun insertRecord(record: HabitRecordEntity): Long
    /** 일괄 삽입(가져오기용). 반환은 rowId 목록. */
    suspend fun insertRecords(records: List<HabitRecordEntity>): List<Long>
    suspend fun updateRecordStatus(recordId: Int, status: String)
    suspend fun deleteRecord(record: HabitRecordEntity)

    // Badges
    fun getAllBadges(): Flow<List<BadgeEntity>>
    suspend fun insertBadge(badge: BadgeEntity): Long

    /** 여러 쓰기를 하나의 DB 트랜잭션으로 묶는다(가져오기 등). 실패하면 전부 롤백. */
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

@Singleton
class DefaultDataRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val database: androidx.room.RoomDatabase? = null
) : DataRepository {

    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database?.withTransaction { block() } ?: block()

    override fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    override fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitDao.getHabitById(habitId)

    override suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    override suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    override fun getRecordsForDate(date: String): Flow<List<HabitRecordEntity>> = habitDao.getRecordsForDate(date)

    override fun getRecordsForHabit(habitId: Int): Flow<List<HabitRecordEntity>> = habitDao.getRecordsForHabit(habitId)

    override fun getAllRecords(): Flow<List<HabitRecordEntity>> = habitDao.getAllRecords()

    override suspend fun getRecord(habitId: Int, date: String): HabitRecordEntity? = habitDao.getRecord(habitId, date)

    override fun getRecordsForHabitBetween(habitId: Int, startDate: String, endDate: String): Flow<List<HabitRecordEntity>> =
        habitDao.getRecordsForHabitBetween(habitId, startDate, endDate)

    override suspend fun toggleCompletion(habitId: Int, date: String): String = habitDao.toggleCompletion(habitId, date)

    override suspend fun upsertValueRecord(habitId: Int, date: String, status: String, inputValue: Float): Long =
        habitDao.upsertValueRecord(habitId, date, status, inputValue)

    override fun getRecordsBetween(startDate: String, endDate: String): Flow<List<HabitRecordEntity>> = habitDao.getRecordsBetween(startDate, endDate)

    override suspend fun insertRecord(record: HabitRecordEntity): Long = habitDao.insertRecord(record)

    override suspend fun insertRecords(records: List<HabitRecordEntity>): List<Long> = habitDao.insertRecords(records)

    override suspend fun updateRecordStatus(recordId: Int, status: String) = habitDao.updateRecordStatus(recordId, status)

    override suspend fun deleteRecord(record: HabitRecordEntity) = habitDao.deleteRecord(record)

    override fun getAllBadges(): Flow<List<BadgeEntity>> = habitDao.getAllBadges()

    override suspend fun insertBadge(badge: BadgeEntity): Long = habitDao.insertBadge(badge)
}
