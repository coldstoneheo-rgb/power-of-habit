package com.example.powerofhabit.ui.components.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.theme.HabitTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckWidget(
    status: String,
    themeColor: Color,
    habitType: String = "CHECK",
    unit: String? = null,
    inputValue: Float? = null,
    targetValue: Float? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 표시 상태는 값·목표로 판정한다(RecordOutcomes). status는 캐시.
    val outcome = RecordOutcomes.of(habitType, status, inputValue, targetValue)
    val isCompleted = outcome == RecordOutcome.SUCCESS
    val isSkipped = outcome == RecordOutcome.SKIPPED
    
    // 선생님 시험지 채점 체크 애니메이션 State
    var triggerAnim by remember { mutableStateOf(false) }
    val animScale = remember { Animatable(0.5f) }
    val animAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    
    val handleClick = {
        if (!isCompleted) {
            triggerAnim = true
            scope.launch {
                animScale.snapTo(0.6f)
                animAlpha.snapTo(1f)
                launch {
                    animScale.animateTo(
                        targetValue = 1.7f,
                        animationSpec = tween(durationMillis = 180)
                    )
                }
                launch {
                    animAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 320)
                    )
                    triggerAnim = false
                }
            }
        }
        onClick()
    }
    
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 30.dp)
            .combinedClickable(
                onClick = handleClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (habitType == "VALUE") {
            // 수치 습관인 경우 (레퍼런스 앱 디자인 100% 반영)
            val hasRecord = inputValue != null
            val displayValue = if (hasRecord) {
                if (inputValue % 1f == 0f) "${inputValue!!.toInt()}" else "$inputValue"
            } else "0"
            val displayUnit = unit ?: ""
            
            // 3상태 색 규칙(결정 기록 2026-09-05): 성공 = 습관색 / 기준미달 = 어둡고 채도 낮은 습관색 / 미수행 = 비활성
            val partial = HabitTheme.colors.partialAccent(themeColor)
            val textColor = when (outcome) {
                RecordOutcome.SUCCESS -> themeColor
                RecordOutcome.PARTIAL -> partial
                else -> HabitTheme.colors.textDisabled
            }

            val unitColor = when (outcome) {
                RecordOutcome.SUCCESS -> themeColor.copy(alpha = 0.85f)
                RecordOutcome.PARTIAL -> partial.copy(alpha = 0.85f)
                else -> HabitTheme.colors.textDisabled
            }
            
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayValue,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (outcome == RecordOutcome.SUCCESS || outcome == RecordOutcome.PARTIAL) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 11.sp,
                    letterSpacing = -0.5.sp
                )
                Text(
                    text = displayUnit,
                    color = unitColor,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    letterSpacing = -0.5.sp
                )
            }
        } else {
            // 체크 습관인 경우 (알파벳 v/x 대신 선명한 채점 체크/닫기 벡터 아이콘)
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = themeColor,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isSkipped) {
                Text(
                    text = "–",
                    color = HabitTheme.colors.statusSkip,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Uncompleted",
                    tint = HabitTheme.colors.textDisabled,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        
        // 채점 체크 팝업 쾌감 애니메이션 오버레이 (선생님 채점 체크 느낌)
        if (triggerAnim) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Grading Check",
                tint = themeColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
            )
        }
    }
}
