package com.example.powerofhabit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.stats.HabitFrequency
import com.example.powerofhabit.domain.stats.HabitStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.example.powerofhabit.data.local.SettingsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(
    val habits: List<HabitEntity>,
    val records: Map<Int, Map<String, HabitRecordEntity>>, // habitId -> (dateString -> record)
    val scores: Map<Int, Float> = emptyMap() // habitId -> 최신 EMA 점수(0~100), 전체 이력 기반
  ) : MainScreenUiState
}

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

  val isDarkMode: StateFlow<Boolean> = settingsManager.isDarkMode
  val isDateDescending: StateFlow<Boolean> = settingsManager.isDateDescending

  fun toggleDarkMode() {
    settingsManager.setDarkMode(!isDarkMode.value)
  }

  fun toggleDateDescending() {
    settingsManager.setDateDescending(!isDateDescending.value)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val uiState: StateFlow<MainScreenUiState> = flow {
    val today = LocalDate.now()
    val limitDate = today.minusDays(3)
    val monthStartDate = today.withDayOfMonth(1)
    val startDate = if (limitDate.isBefore(monthStartDate)) limitDate else monthStartDate
    emit(startDate.toString() to today.toString())
  }.flatMapLatest { (start, end) ->
    val habitsFlow = dataRepository.getAllHabits()
    val recordsFlow = dataRepository.getRecordsBetween(start, end)
    val allRecordsFlow = dataRepository.getAllRecords()

    combine(habitsFlow, recordsFlow, allRecordsFlow) { habits, records, allRecords ->
      val recordsMap = records.groupBy { it.habitId }
        .mapValues { entry ->
          entry.value.associateBy { it.date }
        }
      // 도넛 점수는 화면 창(이달/최근 3일)이 아니라 전체 이력으로 계산해야 상세 화면과 일치한다.
      val today = LocalDate.now()
      val allByHabit = allRecords.groupBy { it.habitId }
      val scores = habits.associate { habit ->
        val stats = HabitStatsCalculator.compute(
          records = allByHabit[habit.habitId].orEmpty(),
          frequency = HabitFrequency.parse(habit.frequencyType, habit.frequencyValue),
          today = today,
          anchorDate = HabitStatsCalculator.anchorFromEpochMillis(habit.createdAt)
        )
        habit.habitId to stats.latestScore
      }
      MainScreenUiState.Success(habits, recordsMap, scores) as MainScreenUiState
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
        com.example.powerofhabit.widget.HabitWidgets.updateAll(context)
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
        com.example.powerofhabit.widget.HabitWidgets.updateAll(context)
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
        com.example.powerofhabit.widget.HabitWidgets.updateAll(context)
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to delete record", e)
      }
    }
  }
}
