package com.example.powerofhabit.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.powerofhabit.backup.DriveAction
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.components.ValueInputDialog
import com.example.powerofhabit.ui.components.widgets.CheckWidget
import com.example.powerofhabit.ui.theme.HabitOrange
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MainScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: (String) -> Unit,
    onNavigateToBadges: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDateDescending by viewModel.isDateDescending.collectAsStateWithLifecycle()
    val transferBusy by viewModel.transferBusy.collectAsStateWithLifecycle()
    val driveBusy by viewModel.driveBusy.collectAsStateWithLifecycle()
    val driveEmail by viewModel.driveEmail.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backupManager = remember { com.example.powerofhabit.backup.GoogleDriveBackupManager(context) }
    // 파일 이전·Drive 결과는 ViewModel 이벤트로 받아 현재(살아 있는) 액티비티 컨텍스트로 Toast를 띄운다.
    LaunchedEffect(viewModel) {
        viewModel.transferMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Google 로그인: ViewModel이 "로그인이 필요하다"고 알리면 여기서 띄우고, 성공하면 멈춘 동작을 다시 부른다.
    // pendingDriveAction·signingIn은 로그인 화면 뒤에서 프로세스가 죽어도 살아 있어야 결과를 이어받는다.
    var pendingDriveAction by rememberSaveable { mutableStateOf<String?>(null) }
    var signingIn by rememberSaveable { mutableStateOf(false) }
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signingIn = false
        val action = pendingDriveAction?.let { name -> DriveAction.entries.firstOrNull { it.name == name } }
        pendingDriveAction = null
        if (result.resultCode == android.app.Activity.RESULT_CANCELED && result.data == null) {
            Toast.makeText(context, "Google 로그인이 취소되었습니다", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        backupManager.accountFromSignInResult(result.data).fold(
            onSuccess = { account ->
                viewModel.refreshDriveAccount()
                Toast.makeText(context, "Google 계정 연결: ${account.email ?: ""}", Toast.LENGTH_SHORT).show()
                when (action) {
                    DriveAction.BACKUP -> viewModel.backup()
                    DriveAction.RESTORE -> viewModel.restore() // 로그인 직전에 확인 다이얼로그를 이미 통과했다
                    null -> Unit
                }
            },
            onFailure = { e ->
                Toast.makeText(context, backupManager.describeSignInError(e), Toast.LENGTH_LONG).show()
            }
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.driveSignInRequests.collect { action ->
            if (signingIn) return@collect
            signingIn = true
            pendingDriveAction = action.name
            Toast.makeText(
                context,
                if (action == DriveAction.RESTORE) "복원하려면 Google 계정 인증이 필요합니다." else "백업하려면 Google 계정 인증이 필요합니다.",
                Toast.LENGTH_SHORT
            ).show()
            signInLauncher.launch(backupManager.signInClient().signInIntent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state) {
            MainScreenUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HabitOrange)
                }
            }
            is MainScreenUiState.Success -> {
                val successState = state as MainScreenUiState.Success
                MainScreenContent(
                    habits = successState.habits,
                    records = successState.records,
                    scores = successState.scores,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onNavigateToBadges = onNavigateToBadges,
                    onToggleCompletion = { habitId, date -> viewModel.toggleCompletion(habitId, date) },
                    onUpdateRecordStatus = { recordId, status, habitId ->
                        viewModel.updateRecordStatus(recordId, status, habitId)
                    },
                    onInsertRecord = { record ->
                        viewModel.insertRecord(record)
                    },
                    onDeleteRecord = { record ->
                        viewModel.deleteRecord(record)
                    },
                    isDarkMode = isDarkMode,
                    isDateDescending = isDateDescending,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleDateDescending = { viewModel.toggleDateDescending() },
                    isTransferring = transferBusy,
                    onExportTo = { uri -> viewModel.exportTo(uri) },
                    onImportFrom = { uri -> viewModel.importFrom(uri) },
                    onImportLegacyDb = { uris -> viewModel.importLegacyDb(uris) },
                    driveBusy = driveBusy,
                    driveEmail = driveEmail,
                    signingIn = signingIn,
                    onBackup = { viewModel.backup() },
                    onRestore = { viewModel.restore() },
                    modifier = modifier
                )
            }
            is MainScreenUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${(state as MainScreenUiState.Error).throwable.localizedMessage}",
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
internal fun MainScreenContent(
    habits: List<HabitEntity>,
    records: Map<Int, Map<String, HabitRecordEntity>>,
    scores: Map<Int, Float> = emptyMap(),
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAddHabit: (String) -> Unit,
    onNavigateToBadges: () -> Unit,
    onUpdateRecordStatus: (Int, String, Int) -> Unit,
    onInsertRecord: (HabitRecordEntity) -> Unit,
    onToggleCompletion: (Int, LocalDate) -> Unit = { _, _ -> },
    onDeleteRecord: (HabitRecordEntity) -> Unit,
    isDarkMode: Boolean,
    isDateDescending: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleDateDescending: () -> Unit,
    /** 파일 이전 진행 중 여부(ViewModel 상태). 진행 중에는 백업·복원·닫기도 잠근다. */
    isTransferring: Boolean = false,
    /** JSON 내보내기: 대상 Uri. */
    onExportTo: (Uri) -> Unit = {},
    /** JSON 가져오기(병합): 원본 Uri. */
    onImportFrom: (Uri) -> Unit = {},
    /** 옛 앱 SQLite 파일(.db + 선택 -wal/-shm) 가져오기(병합). */
    onImportLegacyDb: (List<Uri>) -> Unit = {},
    /** 진행 중인 Drive 동작(ViewModel 상태). null이면 없음. */
    driveBusy: DriveAction? = null,
    /** 연결된 Google 계정 이메일(표시용). */
    driveEmail: String? = null,
    /** Google 로그인 화면이 떠 있는 동안 true — 버튼 중복 탭 방지. */
    signingIn: Boolean = false,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dates = remember(today, isDateDescending) {
        val base = listOf(
            today.minusDays(3),
            today.minusDays(2),
            today.minusDays(1),
            today
        )
        if (isDateDescending) base.reversed() else base
    }
    
    var showValueDialogForHabit by remember { mutableStateOf<Pair<HabitEntity, LocalDate>?>(null) }
    // 성공 폭죽을 재생할 셀 (habitId, "YYYY-MM-DD"). 애니메이션이 끝나면 null.
    var burstCell by remember { mutableStateOf<Pair<Int, String>?>(null) }
    // 설정 다이얼로그는 Google 로그인 화면 뒤에서 프로세스가 죽었다 돌아와도 열려 있어야 진행 표시가 보인다.
    var showBackupSettings by rememberSaveable { mutableStateOf(false) }
    var showAddTypeModal by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 파일 이전(JSON). 런처는 다이얼로그 밖(항상 컴포지션에 있는 곳)에 둬야 액티비티 재생성 뒤에도 결과를 받는다.
    // 진행 상태·결과 메시지는 ViewModel이 든다(회전 시 유실 방지).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportFrom(uri) }
    // 옛 앱 DB: .db와 -wal/-shm을 한 번에 여러 개 고를 수 있게 한다(-wal 없이는 마지막 체크포인트 이후 기록이 빠질 수 있다).
    val legacyDbLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) onImportLegacyDb(uris) }

    // Google Drive 백업/복원(실행·진행 상태는 ViewModel, 로그인 요청은 MainScreen). 예전에는 로그인 화면이 없어 두 버튼이 항상 실패했다.
    val driveLocked = driveBusy != null || signingIn || isTransferring
    var showRestoreConfirm by remember { mutableStateOf(false) }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("백업에서 복원할까요?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "현재 기기의 습관·기록이 Google Drive 백업 시점의 내용으로 통째로 바뀌고 앱이 다시 시작됩니다. 현재 데이터를 남기려면 먼저 '파일로 내보내기'를 해 두세요.",
                    color = HabitTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showRestoreConfirm = false; onRestore() }) {
                    Text("복원", color = HabitOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("취소", color = HabitTheme.colors.textSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAddTypeModal) {
        AlertDialog(
            onDismissRequest = { showAddTypeModal = false },
            title = {
                Text(
                    text = "습관 종류 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Option 1: Yes or No (CHECK)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeModal = false
                                onNavigateToAddHabit("CHECK")
                            },
                        colors = CardDefaults.cardColors(containerColor = HabitTheme.colors.bgLayer3),
                        border = BorderStroke(1.dp, HabitTheme.colors.lineHair),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "예 또는 아니요",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "예) 오늘 일찍 일어났나요? 운동 하셨나요? 독서를 하셨나요?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Measurable (VALUE)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddTypeModal = false
                                onNavigateToAddHabit("VALUE")
                            },
                        colors = CardDefaults.cardColors(containerColor = HabitTheme.colors.bgLayer3),
                        border = BorderStroke(1.dp, HabitTheme.colors.lineHair),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "측정 가능한",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "예) 오늘 몇 km를 달렸습니까? 몇 페이지를 읽었습니까?",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTypeModal = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "습관의 힘",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = -0.6.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add Habit Button
                IconButton(
                    onClick = { showAddTypeModal = true },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Trophy/Badge Button
                IconButton(
                    onClick = onNavigateToBadges,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text("🏆", fontSize = 18.sp)
                }

                // Settings Button
                IconButton(
                    onClick = { showBackupSettings = true },
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habits. Tap + to add one!",
                    color = HabitTheme.colors.textSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.width(152.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEach { date ->
                        Column(
                            modifier = Modifier.width(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(),
                                color = HabitTheme.colors.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (date == today) HabitOrange else MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(habits) { index, habit ->
                    HabitRow(
                        habit = habit,
                        dates = dates,
                        recordsMap = records[habit.habitId] ?: emptyMap(),
                        score = scores[habit.habitId],
                        showDivider = index < habits.lastIndex,
                        burstDate = burstCell?.takeIf { it.first == habit.habitId }?.second,
                        onBurstDone = { burstCell = null },
                        onNavigateToDetail = onNavigateToDetail,
                        onCheckClick = { date, record ->
                            if (habit.habitType == "VALUE") {
                                showValueDialogForHabit = habit to date
                            } else {
                                // 규칙(없음→완료, 완료→실패, 그 외→완료)은 HabitDao.toggleCompletion 한 곳에 있다.
                                onToggleCompletion(habit.habitId, date)
                            }
                        },
                        onCheckLongClick = { date, record ->
                            if (record != null) {
                                // 건너뜀 해제: 수치형은 값이 상태를 결정하므로 statusForValue로 되돌린다(표시와 통계가 어긋나지 않게)
                                val nextStatus = when {
                                    record.status != "SKIPPED" -> "SKIPPED"
                                    habit.habitType == "VALUE" -> RecordOutcomes.statusForValue(record.inputValue, habit.targetValue, habit.targetType)
                                    else -> "FAILED"
                                }
                                onUpdateRecordStatus(record.recordId, nextStatus, habit.habitId)
                            } else {
                                onInsertRecord(
                                    HabitRecordEntity(
                                        habitId = habit.habitId,
                                        date = date.toString(),
                                        status = "SKIPPED",
                                        inputValue = null
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Value input dialog — 위젯(ValueInputActivity)과 같은 컴포저블·저장 규칙을 쓴다
    showValueDialogForHabit?.let { (habit, date) ->
        val existingRecord = records[habit.habitId]?.get(date.toString())
        val habitThemeColor = remember(habit.themeColor) {
            try { Color(android.graphics.Color.parseColor(habit.themeColor)) } catch (e: Exception) { HabitOrange }
        }
        ValueInputDialog(
            habit = habit,
            initialValue = existingRecord?.inputValue,
            accent = habitThemeColor,
            onDismiss = { showValueDialogForHabit = null },
            onSave = { value, outcome ->
                if (existingRecord != null) onDeleteRecord(existingRecord)
                onInsertRecord(
                    HabitRecordEntity(
                        habitId = habit.habitId,
                        date = date.toString(),
                        status = RecordOutcomes.statusForValue(value, habit.targetValue, habit.targetType),
                        inputValue = value
                    )
                )
                // 성공(목표 충족)일 때만 해당 셀에 폭죽. 기준미달·미수행은 색 변화만.
                if (outcome == RecordOutcome.SUCCESS) burstCell = habit.habitId to date.toString()
                showValueDialogForHabit = null
            }
        )
    }

    // Google Drive Backup & Restore Settings Dialog
    if (showBackupSettings) {
        AlertDialog(
            onDismissRequest = { 
                if (!driveLocked) showBackupSettings = false
            },
            title = {
                Text(
                    text = "설정 및 동기화",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 테마 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "다크 모드 적용",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HabitOrange,
                                checkedTrackColor = HabitOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // 날짜 순서 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "오늘 날짜를 가장 왼쪽에 표시",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isDateDescending,
                            onCheckedChange = { onToggleDateDescending() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HabitOrange,
                                checkedTrackColor = HabitOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    Text(
                        text = "구글 드라이브의 앱 전용 폴더에 데이터베이스를 백업·복원합니다. Google 계정 인증이 필요하며, 복원은 현재 데이터를 백업 내용으로 교체합니다.",
                        color = HabitTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = -0.5.sp
                    )
                    Text(
                        text = driveEmail?.let { "연결된 계정: $it" } ?: "연결된 Google 계정 없음 — 백업/복원 버튼을 누르면 로그인을 요청합니다.",
                        color = HabitTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )

                    if (driveBusy != null || signingIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = HabitOrange, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when {
                                    signingIn -> "Google 로그인 중..."
                                    driveBusy == DriveAction.BACKUP -> "데이터 백업 중..."
                                    else -> "데이터 복원 중..."
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Divider(color = HabitTheme.colors.lineHair, thickness = 1.dp)

                    // 로컬 파일 이전(JSON) — 기기 교체·백업용. 가져오기는 덮어쓰지 않는 병합(data/transfer/HabitTransfer).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.s2)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                                exportLauncher.launch("power-of-habit-$stamp.json")
                            },
                            enabled = !driveLocked,
                            border = BorderStroke(1.dp, HabitTheme.colors.lineStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTheme.colors.textPrimary),
                            contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("파일로 내보내기", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = !driveLocked,
                            border = BorderStroke(1.dp, HabitTheme.colors.lineStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTheme.colors.textPrimary),
                            contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("파일에서 가져오기", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(
                        text = if (isTransferring) "파일 처리 중..." else "JSON 파일로 저장·불러오기 (로그인 불필요). 가져오기는 현재 데이터를 지우지 않고 병합하며, 같은 습관·같은 날짜의 기록은 기존 것을 유지합니다.",
                        color = HabitTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    // 옛 앱(com.example.powerofhabit)의 DB 파일 병합 — docs/RELEASE.md §4-2
                    OutlinedButton(
                        onClick = { legacyDbLauncher.launch(arrayOf("*/*")) },
                        enabled = !driveLocked,
                        border = BorderStroke(1.dp, HabitTheme.colors.lineStrong),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitTheme.colors.textPrimary),
                        contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("옛 앱 DB 파일 가져오기", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        text = "예전 'Power of Habit' 앱에서 꺼낸 power_of_habit.db를 고르세요. 같은 폴더에 -wal·-shm 파일이 있으면 세 개를 함께 선택해야 마지막 기록까지 들어옵니다. 위와 같은 병합 규칙이며 현재 데이터는 지워지지 않습니다.",
                        color = HabitTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBackup,
                        enabled = !driveLocked,
                        colors = ButtonDefaults.buttonColors(containerColor = HabitOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("백업하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showRestoreConfirm = true },
                        enabled = !driveLocked,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    ) {
                        Text("복원하기", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackupSettings = false },
                    enabled = !driveLocked
                ) {
                    Text("닫기", color = HabitTheme.colors.textSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DonutProgressChart(
    progress: Float,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(18.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.5.dp.toPx()
            
            // Background ring (Always visible)
            drawCircle(
                color = themeColor.copy(alpha = 0.2f),
                radius = size.minDimension / 2 - strokeWidth / 2,
                style = Stroke(width = strokeWidth)
            )
            
            // Inner subtle dot for center alignment
            drawCircle(
                color = themeColor.copy(alpha = 0.35f),
                radius = 1.5.dp.toPx()
            )
            
            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = themeColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    dates: List<LocalDate>,
    recordsMap: Map<String, HabitRecordEntity>,
    score: Float?, // 0~100, ViewModel이 전체 이력으로 계산. null이면 점수 없음
    showDivider: Boolean,
    burstDate: String? = null, // 이 날짜 셀에 성공 폭죽 재생
    onBurstDone: () -> Unit = {},
    onNavigateToDetail: (Int) -> Unit,
    onCheckClick: (LocalDate, HabitRecordEntity?) -> Unit,
    onCheckLongClick: (LocalDate, HabitRecordEntity?) -> Unit
) {
    val themeColor = remember(habit.themeColor) {
        try {
            Color(android.graphics.Color.parseColor(habit.themeColor))
        } catch (e: Exception) {
            HabitOrange
        }
    }
    
    // 점수(0~100) 그대로 0~1로. 기록 없는 새 습관은 0 → 트랙만 보인다(RingProgress.kt).
    val emaScore = scoreToRingProgress(score)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToDetail(habit.habitId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutProgressChart(
                    progress = emaScore,
                    themeColor = themeColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = habit.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = -0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Row(
                modifier = Modifier.width(152.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    val record = recordsMap[date.toString()]
                    CheckWidget(
                        status = record?.status ?: "NONE",
                        themeColor = themeColor,
                        habitType = habit.habitType,
                        unit = habit.unit,
                        inputValue = record?.inputValue,
                        targetValue = habit.targetValue,
                        targetType = habit.targetType,
                        burst = burstDate == date.toString(),
                        onBurstDone = onBurstDone,
                        onClick = { onCheckClick(date, record) },
                        onLongClick = { onCheckLongClick(date, record) }
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = HabitTheme.colors.lineHair,
                thickness = 0.5.dp
            )
        }
    }
}
