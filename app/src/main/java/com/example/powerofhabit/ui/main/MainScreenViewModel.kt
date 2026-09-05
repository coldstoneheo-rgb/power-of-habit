package com.example.powerofhabit.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.RecordSideEffects
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
import android.net.Uri
import com.example.powerofhabit.data.transfer.ImportSummary
import com.example.powerofhabit.data.transfer.TransferManager
import com.example.powerofhabit.backup.DriveAction
import com.example.powerofhabit.backup.DriveOutcome
import com.example.powerofhabit.backup.GoogleDriveBackupManager

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

  private val transferManager = TransferManager(dataRepository)
  private val backupManager = GoogleDriveBackupManager(context)

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
        val stats = HabitStatsCalculator.compute(habit, allByHabit[habit.habitId].orEmpty(), today)
        habit.habitId to stats.latestScore
      }
      MainScreenUiState.Success(habits, recordsMap, scores) as MainScreenUiState
    }
  }
  .catch { emit(MainScreenUiState.Error(it)) }
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  /** 체크형 습관의 셀 탭. 위젯과 같은 트랜잭션 토글을 쓴다. */
  fun toggleCompletion(habitId: Int, date: LocalDate) {
    viewModelScope.launch {
      try {
        dataRepository.toggleCompletion(habitId, date.toString())
        RecordSideEffects.afterRecordChange(context, dataRepository, habitId)
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to toggle completion", e)
      }
    }
  }

  fun updateRecordStatus(recordId: Int, status: String, habitId: Int) {
    viewModelScope.launch {
      try {
        dataRepository.updateRecordStatus(recordId, status)
        RecordSideEffects.afterRecordChange(context, dataRepository, habitId)
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to update record status", e)
      }
    }
  }

  fun insertRecord(record: HabitRecordEntity) {
    viewModelScope.launch {
      try {
        dataRepository.insertRecord(record)
        RecordSideEffects.afterRecordChange(context, dataRepository, record.habitId)
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to insert record", e)
      }
    }
  }

  fun deleteRecord(record: HabitRecordEntity) {
    viewModelScope.launch {
      try {
        dataRepository.deleteRecord(record)
        RecordSideEffects.afterRecordChange(context, dataRepository, record.habitId)
      } catch (e: Exception) {
        android.util.Log.e("MainScreenViewModel", "Failed to delete record", e)
      }
    }
  }

  // 파일 이전 상태는 ViewModel이 들고 있어 액티비티 재생성(회전) 뒤에도 "처리 중"과 결과 메시지가 유지된다.
  private val _transferBusy = MutableStateFlow(false)
  val transferBusy: StateFlow<Boolean> = _transferBusy.asStateFlow()
  private val _transferMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  /** 내보내기/가져오기 결과(한국어). 화면이 수집해 Toast로 보여준다. */
  val transferMessages: SharedFlow<String> = _transferMessages.asSharedFlow()

  /**
   * 파일 이전 공통 골격: 동시 실행 방지(이전끼리·Drive 복원과) → 실행 → 결과 문구 → Toast 이벤트.
   * Drive 복원은 DB 파일을 통째로 바꾸므로 그 사이에 가져오기가 끼면 쓴 행이 사라진다 — driveBusy도 함께 잠근다.
   */
  private fun runTransfer(label: String, op: suspend () -> Result<String>) {
    if (_driveBusy.value != null) return
    if (!_transferBusy.compareAndSet(expect = false, update = true)) return
    viewModelScope.launch {
      val message = try {
        op().getOrElse { e ->
          android.util.Log.e("MainScreenViewModel", "$label failed", e)
          "${label}에 실패했습니다: ${e.message ?: e.javaClass.simpleName}"
        }
      } finally {
        _transferBusy.value = false
      }
      _transferMessages.emit(message)
    }
  }

  private fun ImportSummary.describe(prefix: String): String {
    val base = "$prefix 완료 — 습관 +$habitsAdded(기존 일치 $habitsMatched), 기록 +$recordsAdded(건너뜀 $recordsSkipped), 뱃지 +$badgesAdded"
    return if (warnings.isEmpty()) base else base + "\n" + warnings.joinToString("\n")
  }

  /** 습관·기록·뱃지를 JSON 파일로 내보낸다. */
  fun exportTo(uri: Uri) = runTransfer("내보내기") {
    transferManager.exportTo(context, uri).map { count -> "습관 ${count}개를 파일로 내보냈습니다." }
  }

  // Google Drive 백업/복원 상태도 ViewModel이 든다 — 업로드 중 회전해도 코루틴이 끊기지 않고 "진행 중"이 유지된다.
  private val _driveBusy = MutableStateFlow<DriveAction?>(null)
  /** 진행 중인 Drive 동작(null = 없음). */
  val driveBusy: StateFlow<DriveAction?> = _driveBusy.asStateFlow()
  private val _driveEmail = MutableStateFlow<String?>(null)
  /** 연결된 Google 계정(표시용). 로그인 결과가 오면 [refreshDriveAccount]로 갱신한다. */
  val driveEmail: StateFlow<String?> = _driveEmail.asStateFlow()

  init {
    // JVM 단위 테스트(가짜 Context)에서는 GMS 조회가 실패할 수 있으므로 생성은 절대 막지 않는다.
    runCatching { refreshDriveAccount() }
  }
  private val _driveSignInRequests = MutableSharedFlow<DriveAction>(extraBufferCapacity = 1)
  /** 로그인이 필요해 멈춘 동작. 화면이 수집해 Google 로그인을 띄우고, 성공하면 같은 동작을 다시 부른다. */
  val driveSignInRequests: SharedFlow<DriveAction> = _driveSignInRequests.asSharedFlow()

  fun refreshDriveAccount() {
    _driveEmail.value = backupManager.signedInEmail()
  }

  /** Drive appDataFolder로 DB 백업. 로그인이 없거나 권한이 회수됐으면 [driveSignInRequests]로 알린다. */
  fun backup() = runDrive(DriveAction.BACKUP)

  /** Drive 백업으로 DB 교체 후 앱 재시작. 호출 전 화면에서 확인을 받는다(현재 데이터가 사라진다). */
  fun restore() = runDrive(DriveAction.RESTORE)

  private fun runDrive(action: DriveAction) {
    if (_transferBusy.value) return
    if (!_driveBusy.compareAndSet(expect = null, update = action)) return // 동시 실행 방지
    viewModelScope.launch {
      val outcome = try {
        when (action) {
          DriveAction.BACKUP -> backupManager.backupDatabase()
          DriveAction.RESTORE -> backupManager.restoreDatabase()
        }
      } finally {
        _driveBusy.value = null
      }
      when (outcome) {
        DriveOutcome.SUCCESS -> if (action == DriveAction.BACKUP) _transferMessages.emit("백업이 완료되었습니다.")
        DriveOutcome.NEEDS_SIGN_IN -> _driveSignInRequests.emit(action)
        DriveOutcome.FAILED -> _transferMessages.emit(
          if (action == DriveAction.BACKUP) "백업에 실패했습니다." else "복원에 실패했습니다. Drive에 올바른 백업 파일이 없습니다."
        )
        DriveOutcome.BACKUP_TOO_NEW -> _transferMessages.emit("백업이 더 새 버전의 앱에서 만들어졌습니다. 앱을 업데이트한 뒤 복원해 주세요.")
      }
    }
  }

  /** 옛 앱(com.example) SQLite 파일(.db + 선택 -wal/-shm)을 JSON 가져오기와 같은 규칙으로 병합한다. */
  fun importLegacyDb(uris: List<Uri>) {
    if (uris.isEmpty()) return
    runTransfer("옛 앱 DB 가져오기") {
      transferManager.importLegacyDb(context, uris).map { it.describe("옛 앱 DB 가져오기") }
    }
  }

  /** JSON 파일을 현재 데이터에 병합한다(덮어쓰지 않음). 규칙은 data/transfer/HabitTransfer 참조. */
  fun importFrom(uri: Uri) = runTransfer("가져오기") {
    transferManager.importFrom(context, uri).map { it.describe("가져오기") }
  }
}
