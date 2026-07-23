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
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckWidget(
    status: String,
    themeColor: Color,
    habitType: String = "CHECK",
    unit: String? = null,
    inputValue: Float? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isCompleted = status == "COMPLETED"
    val isSkipped = status == "SKIPPED"
    
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
            
            // 색상 규칙: 
            // 1. 완료(COMPLETED) -> 습관 테마 색상 (themeColor)
            // 2. 미달성 수치 입력(FAILED / 미달) -> 진한 회색 (MaterialTheme.colorScheme.onSurface)
            // 3. 미기록(NONE) -> 연한 회색 (0.25f alpha)
            val textColor = when {
                isCompleted -> themeColor
                hasRecord -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            }
            
            val unitColor = when {
                isCompleted -> themeColor.copy(alpha = 0.85f)
                hasRecord -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            }
            
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayValue,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (isCompleted || hasRecord) FontWeight.Bold else FontWeight.Normal,
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Uncompleted",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
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
