package com.example.powerofhabit.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.domain.RecordOutcomes
import android.content.Context
import android.util.Log
import com.example.powerofhabit.reminder.HabitReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AddEditHabitUiEvent {
    object SaveSuccess : AddEditHabitUiEvent
    object DeleteSuccess : AddEditHabitUiEvent
    data class Error(val message: String) : AddEditHabitUiEvent
}

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 저장이 끝나는 순간 화면이 회전 중이어도 결과를 잃지 않도록 Channel에 담아 둔다.
    private val _uiEvent = Channel<AddEditHabitUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<AddEditHabitUiEvent> = _uiEvent.receiveAsFlow()

    private val _habitState = MutableStateFlow<HabitEntity?>(null)
    val habitState: StateFlow<HabitEntity?> = _habitState.asStateFlow()
    /** 저장·삭제를 시작하면 true, 실패했을 때만 풀린다. 빠르게 두 번 눌러도 습관이 두 개 생기지 않게 한다(화면마다 새 ViewModel). */
    private var writing = false

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
        memo: String? = null,
        targetValue: Float? = null,
        /** 수치형 목표 방향(RecordOutcomes.TARGET_AT_LEAST / TARGET_AT_MOST). */
        targetType: String = "AT_LEAST"
    ) {
        if (writing) return
        writing = true
        viewModelScope.launch {
            if (title.isBlank()) {
                writing = false
                _uiEvent.send(AddEditHabitUiEvent.Error("습관 이름을 입력해주세요."))
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
                        memo = memo,
                        targetValue = targetValue,
                        targetType = targetType
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
                            memo = memo,
                            targetValue = targetValue,
                            targetType = targetType
                        )
                        val targetChanged = updatedHabit.habitType == "VALUE" &&
                            (existingHabit.targetValue != updatedHabit.targetValue || existingHabit.targetType != updatedHabit.targetType ||
                                existingHabit.habitType != updatedHabit.habitType)
                        repository.inTransaction {
                            repository.updateHabit(updatedHabit)
                            // 목표(값·방향)가 바뀌면 기존 기록의 status 캐시도 새 규칙으로 — 통계·뱃지는 캐시만 보기 때문(RecordOutcomes 문서).
                            if (targetChanged) {
                                repository.getRecordsForHabit(habitId).first().forEach { record ->
                                    RecordOutcomes.restatusAfterTargetChange(
                                        record.status, record.inputValue, updatedHabit.targetValue, updatedHabit.targetType
                                    )?.let { repository.updateRecordStatus(record.recordId, it) }
                                }
                            }
                        }
                        reminderManager.scheduleReminder(updatedHabit)
                        // 빈도·목표가 바뀌면 스트릭·완료 수가 달라지므로 기록이 바뀐 것과 같이 뱃지를 다시 판정한다.
                        val frequencyChanged = existingHabit.frequencyType != updatedHabit.frequencyType ||
                            existingHabit.frequencyValue != updatedHabit.frequencyValue
                        if (targetChanged || frequencyChanged) {
                            com.example.powerofhabit.data.RecordSideEffects.afterRecordChange(context, repository, habitId)
                        }
                    } else {
                        writing = false
                        _uiEvent.send(AddEditHabitUiEvent.Error("습관을 찾을 수 없습니다."))
                        return@launch
                    }
                }
                com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
                _uiEvent.send(AddEditHabitUiEvent.SaveSuccess)
            } catch (e: Exception) {
                Log.e("AddEditHabitViewModel", "Failed to save habit", e)
                writing = false
                _uiEvent.send(AddEditHabitUiEvent.Error("습관을 저장하지 못했습니다."))
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        if (writing) return
        writing = true
        viewModelScope.launch {
            try {
                repository.deleteHabit(habit)
                HabitReminderManager(context).cancelReminder(habit.habitId)
                com.example.powerofhabit.backup.GoogleDriveBackupManager(context).scheduleAutoBackup()
                _uiEvent.send(AddEditHabitUiEvent.DeleteSuccess)
            } catch (e: Exception) {
                Log.e("AddEditHabitViewModel", "Failed to delete habit", e)
                writing = false
                _uiEvent.send(AddEditHabitUiEvent.Error("습관을 삭제하지 못했습니다."))
            }
        }
    }
}
