package com.example.powerofhabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Static definition of all available badges in the app
data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val iconType: String // "GOLD", "SILVER", "BRONZE"
)

val AllBadgeDefinitions = listOf(
    BadgeDefinition("START_FIRST", "습관 여행의 시작", "첫 번째 습관 실천을 완료했습니다!", "BRONZE"),
    BadgeDefinition("STREAK_3", "작심삼일 탈출", "습관 3일 연속 달성에 성공했습니다!", "BRONZE"),
    BadgeDefinition("STREAK_5", "꾸준한 실행가", "습관 5일 연속 달성에 성공했습니다!", "SILVER"),
    BadgeDefinition("STREAK_7", "빛나는 일주일", "일주일 7일 연속 완벽 달성에 성공했습니다!", "SILVER"),
    BadgeDefinition("STREAK_14", "2주의 기적", "2주 연속 습관 달성을 완료했습니다!", "SILVER"),
    BadgeDefinition("STREAK_21", "21일의 습관화", "습관 형성 21일의 벽을 돌파했습니다!", "SILVER"),
    BadgeDefinition("STREAK_30", "습관 마스터", "지속 가능한 성장! 습관 30일 연속 달성 완료!", "GOLD"),
    BadgeDefinition("STREAK_66", "체화된 습관", "평균 습관 형성 주기 66일을 완전히 정복했습니다!", "GOLD"),
    BadgeDefinition("STREAK_100", "백일의 대장정", "100일 연속 달성! 전설적인 완벽 마스터!", "GOLD"),
    BadgeDefinition("HABIT_COMPLETE_10", "첫 10회의 발걸음", "습관 완수 횟수 10회를 달성했습니다!", "BRONZE"),
    BadgeDefinition("HABIT_COMPLETE_50", "반백의 열정", "습관 완수 횟수 50회를 달성했습니다!", "SILVER"),
    BadgeDefinition("HABIT_COMPLETE_100", "백일의 기적", "습관 완수 횟수 100회를 돌파했습니다!", "GOLD"),
    BadgeDefinition("EARLY_BIRD", "얼리버드 습관가", "아침 일찍 습관을 실천했습니다!", "SILVER"),
    BadgeDefinition("NIGHT_OWL", "밤샘 파수꾼", "늦은 밤에도 잊지 않고 습관을 완료했습니다!", "BRONZE"),
    BadgeDefinition("HABIT_CREATOR", "습관 설계자", "3개 이상의 다양한 습관을 등록하고 관리 중입니다!", "BRONZE")
)

@HiltViewModel
class BadgesViewModel @Inject constructor(
    repository: DataRepository
) : ViewModel() {
    val earnedBadges: StateFlow<List<BadgeEntity>> = repository.getAllBadges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    viewModel: BadgesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val earned by viewModel.earnedBadges.collectAsStateWithLifecycle()
    val earnedMap = remember(earned) { earned.associateBy { it.badgeId } }
    
    var selectedBadgeDetail by remember { mutableStateOf<Pair<BadgeDefinition, BadgeEntity?>?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("업적 배지 보관함", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = -0.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header stats block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("해제한 업적", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, letterSpacing = -0.5.sp)
                    Text(
                        text = "${earned.size} / ${AllBadgeDefinitions.size}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = -0.5.sp
                    )
                    Text("꾸준한 실천으로 메탈릭 업적 배지를 획득하세요!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, letterSpacing = -0.5.sp)
                }
            }
            
            // Grid list
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AllBadgeDefinitions) { def ->
                    val earnedRecord = earnedMap[def.id]
                    val isEarned = earnedRecord != null
                    
                    BadgeItem(
                        definition = def,
                        isEarned = isEarned,
                        onClick = {
                            selectedBadgeDetail = def to earnedRecord
                        }
                    )
                }
            }
        }
    }
    
    // Badge Detail dialog
    selectedBadgeDetail?.let { (def, record) ->
        val formatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm") }
        val earnedTimeText = remember(record) {
            record?.let {
                val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.earnedAt), ZoneId.systemDefault())
                ldt.format(formatter)
            } ?: ""
        }
        
        AlertDialog(
            onDismissRequest = { selectedBadgeDetail = null },
            title = {
                Text(
                    text = def.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = -0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Massive badge icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                if (record != null) {
                                    when (def.iconType) {
                                        "GOLD" -> GoldenMetalBrush
                                        "SILVER" -> SilverMetalBrush
                                        else -> BronzeMetalBrush
                                    }
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366)))
                                }
                            )
                            .border(
                                width = if (record != null) 3.dp else 1.dp,
                                color = if (record != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (def.iconType) {
                                "GOLD" -> "🏆"
                                "SILVER" -> "🥈"
                                "BRONZE" -> "🥉"
                                else -> "🔒"
                            },
                            fontSize = 36.sp
                        )
                    }
                    
                    Text(
                        text = def.description,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        letterSpacing = -0.5.sp
                    )
                    
                    if (record != null) {
                        Text(
                            text = "획득 일자: $earnedTimeText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            letterSpacing = -0.5.sp
                        )
                    } else {
                        Text(
                            text = "아직 획득하지 못한 업적입니다.",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = -0.5.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadgeDetail = null }) {
                    Text("확인", color = HabitOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BadgeItem(
    definition: BadgeDefinition,
    isEarned: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Metallic circular badge icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isEarned) {
                        when (definition.iconType) {
                            "GOLD" -> GoldenMetalBrush
                            "SILVER" -> SilverMetalBrush
                            else -> BronzeMetalBrush
                        }
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366)))
                    }
                )
                .border(
                    width = if (isEarned) 2.dp else 1.dp,
                    color = if (isEarned) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEarned) {
                    when (definition.iconType) {
                        "GOLD" -> "🏆"
                        "SILVER" -> "🥈"
                        "BRONZE" -> "🥉"
                        else -> "🔒"
                    }
                } else {
                    "🔒"
                },
                fontSize = 24.sp
            )
        }
        
        Text(
            text = definition.name,
            color = if (isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            letterSpacing = -0.5.sp
        )
    }
}
