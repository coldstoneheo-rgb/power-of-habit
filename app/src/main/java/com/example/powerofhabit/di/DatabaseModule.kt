package com.example.powerofhabit.di

import android.content.Context
import androidx.room.Room
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.DefaultDataRepository
import com.example.powerofhabit.data.local.AppDatabase
import com.example.powerofhabit.data.local.HabitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Habits ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS Badges (badgeId TEXT NOT NULL PRIMARY KEY, badgeName TEXT NOT NULL, description TEXT NOT NULL, earnedAt INTEGER NOT NULL, badgeIconType TEXT NOT NULL)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "power_of_habit.db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideHabitDao(database: AppDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    @Singleton
    fun provideDataRepository(habitDao: HabitDao): DataRepository {
        return DefaultDataRepository(habitDao)
    }
}
