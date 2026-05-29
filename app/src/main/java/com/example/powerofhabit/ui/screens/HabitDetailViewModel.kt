package com.example.powerofhabit.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

sealed interface HabitDetailUiState {
    object Loading : HabitDetailUiState
    data class Error(val throwable: Throwable) : HabitDetailUiState
    data class Success(
        val habit: HabitEntity,
        val records: List<HabitRecordEntity>
    ) : HabitDetailUiState
}

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _habitId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HabitDetailUiState> = _habitId
        .filterNotNull()
        .flatMapLatest { id ->
            val habitFlow = repository.getHabitById(id)
            val recordsFlow = repository.getRecordsForHabit(id)
            
            combine(habitFlow, recordsFlow) { habit, records ->
                if (habit == null) {
                    HabitDetailUiState.Error(IllegalArgumentException("Habit not found with id $id"))
                } else {
                    HabitDetailUiState.Success(habit, records)
                }
            }
            .catch { emit(HabitDetailUiState.Error(it)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitDetailUiState.Loading)

    fun setHabitId(id: Int) {
        _habitId.value = id
    }

    fun updateRecordStatus(recordId: Int, status: String) {
        viewModelScope.launch {
            try {
                repository.updateRecordStatus(recordId, status)
            } catch (e: Exception) {
                android.util.Log.e("HabitDetailViewModel", "Failed to update record status", e)
            }
        }
    }

    fun insertRecord(record: HabitRecordEntity) {
        viewModelScope.launch {
            try {
                repository.insertRecord(record)
            } catch (e: Exception) {
                android.util.Log.e("HabitDetailViewModel", "Failed to insert record", e)
            }
        }
    }

    fun updateRecordForDate(date: String, status: String, inputValue: Float? = null) {
        val habitId = _habitId.value ?: return
        viewModelScope.launch {
            try {
                val currentRecords = repository.getRecordsForHabit(habitId).first()
                val existingRecord = currentRecords.find { it.date == date }
                
                if (status == "NONE") {
                    if (existingRecord != null) {
                        repository.deleteRecord(existingRecord)
                    }
                } else {
                    if (existingRecord != null) {
                        repository.deleteRecord(existingRecord)
                    }
                    repository.insertRecord(
                        HabitRecordEntity(
                            habitId = habitId,
                            date = date,
                            status = status,
                            inputValue = inputValue
                        )
                    )
                }

                val updatedRecords = repository.getRecordsForHabit(habitId).first()
                com.example.powerofhabit.badges.BadgeManager(repository, context).checkAndAwardBadges(updatedRecords)
                com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
            } catch (e: Exception) {
                android.util.Log.e("HabitDetailViewModel", "Failed to update record for date $date", e)
            }
        }
    }
}
