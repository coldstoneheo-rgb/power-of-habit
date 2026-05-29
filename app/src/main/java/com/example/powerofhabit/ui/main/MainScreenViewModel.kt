package com.example.powerofhabit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(
    val habits: List<HabitEntity>,
    val records: Map<Int, Map<String, HabitRecordEntity>> // habitId -> (dateString -> record)
  ) : MainScreenUiState
}

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState: StateFlow<MainScreenUiState> = flow {
    val today = LocalDate.now()
    val startDate = today.minusDays(3).toString()
    val endDate = today.toString()
    emit(startDate to endDate)
  }.flatMapLatest { (start, end) ->
    val habitsFlow = dataRepository.getAllHabits()
    val recordsFlow = dataRepository.getRecordsBetween(start, end)
    
    combine(habitsFlow, recordsFlow) { habits, records ->
      val recordsMap = records.groupBy { it.habitId }
        .mapValues { entry ->
          entry.value.associateBy { it.date }
        }
      MainScreenUiState.Success(habits, recordsMap) as MainScreenUiState
    }
  }
  .catch { emit(MainScreenUiState.Error(it)) }
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun updateRecordStatus(recordId: Int, status: String, habitId: Int) {
    viewModelScope.launch {
      try {
        dataRepository.updateRecordStatus(recordId, status)
        val records = dataRepository.getRecordsForHabit(habitId).first()
        com.example.powerofhabit.badges.BadgeManager(dataRepository, context).checkAndAwardBadges(records)
        com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to update record status", e)
      }
    }
  }

  fun insertRecord(record: HabitRecordEntity) {
    viewModelScope.launch {
      try {
        dataRepository.insertRecord(record)
        val records = dataRepository.getRecordsForHabit(record.habitId).first()
        com.example.powerofhabit.badges.BadgeManager(dataRepository, context).checkAndAwardBadges(records)
        com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to insert record", e)
      }
    }
  }

  fun deleteRecord(record: HabitRecordEntity) {
    viewModelScope.launch {
      try {
        dataRepository.deleteRecord(record)
        val records = dataRepository.getRecordsForHabit(record.habitId).first()
        com.example.powerofhabit.badges.BadgeManager(dataRepository, context).checkAndAwardBadges(records)
        com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to delete record", e)
      }
    }
  }
}
