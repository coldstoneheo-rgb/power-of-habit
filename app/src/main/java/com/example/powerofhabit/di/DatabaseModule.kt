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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "power_of_habit.db"
        ).build()
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
