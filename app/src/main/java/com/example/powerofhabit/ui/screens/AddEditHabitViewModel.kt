package com.example.powerofhabit.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import android.content.Context
import com.example.powerofhabit.reminder.HabitReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddEditHabitUiEvent {
    object SaveSuccess : AddEditHabitUiEvent
    data class Error(val message: String) : AddEditHabitUiEvent
}

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<AddEditHabitUiEvent>()
    val uiEvent: SharedFlow<AddEditHabitUiEvent> = _uiEvent.asSharedFlow()

    private val _habitState = MutableStateFlow<HabitEntity?>(null)
    val habitState: StateFlow<HabitEntity?> = _habitState.asStateFlow()
    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadHabit(habitId: Int) {
        if (habitId == 0) {
            _habitState.value = null
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getHabitById(habitId).collect { habit ->
                _habitState.value = habit
            }
        }
    }

    fun saveHabit(
        habitId: Int = 0,
        title: String,
        question: String,
        frequencyType: String,
        frequencyValue: String,
        reminderTime: String?,
        isReminderEnabled: Boolean,
        themeColor: String,
        habitType: String,
        unit: String?,
        memo: String? = null
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _uiEvent.emit(AddEditHabitUiEvent.Error("Title cannot be blank"))
                return@launch
            }

            try {
                val reminderManager = HabitReminderManager(context)
                if (habitId == 0) {
                    val habit = HabitEntity(
                        habitId = habitId,
                        title = title,
                        question = question,
                        frequencyType = frequencyType,
                        frequencyValue = frequencyValue,
                        reminderTime = reminderTime,
                        isReminderEnabled = isReminderEnabled,
                        themeColor = themeColor,
                        habitType = habitType,
                        unit = unit,
                        memo = memo
                    )
                    val newHabitId = repository.insertHabit(habit).toInt()
                    // Re-schedule with actual auto-generated ID
                    val savedHabit = habit.copy(habitId = newHabitId)
                    reminderManager.scheduleReminder(savedHabit)
                } else {
                    val existingHabit = repository.getHabitById(habitId).first()
                    if (existingHabit != null) {
                        val updatedHabit = existingHabit.copy(
                            title = title,
                            question = question,
                            frequencyType = frequencyType,
                            frequencyValue = frequencyValue,
                            reminderTime = reminderTime,
                            isReminderEnabled = isReminderEnabled,
                            themeColor = themeColor,
                            habitType = habitType,
                            unit = unit,
                            memo = memo
                        )
                        repository.updateHabit(updatedHabit)
                        reminderManager.scheduleReminder(updatedHabit)
                    } else {
                        _uiEvent.emit(AddEditHabitUiEvent.Error("Habit not found"))
                        return@launch
                    }
                }
                com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
                _uiEvent.emit(AddEditHabitUiEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiEvent.emit(AddEditHabitUiEvent.Error(e.message ?: "Failed to save habit"))
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habit)
                HabitReminderManager(context).cancelReminder(habit.habitId)
                com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
                _uiEvent.emit(AddEditHabitUiEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiEvent.emit(AddEditHabitUiEvent.Error(e.message ?: "Failed to delete habit"))
            }
        }
    }
}
