package com.example.powerofhabit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HabitEntity::class, HabitRecordEntity::class, BadgeEntity::class], version = AppDatabase.SCHEMA_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        /** Room 스키마 버전(SQLite user_version). Drive 복원이 더 새 버전의 DB 파일을 거부할 때도 이 값을 본다. */
        const val SCHEMA_VERSION = 5
    }
}
