package com.example.powerofhabit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.domain.RecordOutcome
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.theme.HabitTheme
import com.example.powerofhabit.ui.theme.Space

/**
 * 수치형 습관 값 입력 다이얼로그. 메인 화면 셀과 홈 위젯(ValueInputActivity)이 공유한다.
 * 저장 규칙은 호출자가 아니라 [RecordOutcomes.statusForValue]에 있다 — 여기서는 값만 넘긴다.
 *
 * @param initialValue 기존 기록 값(없으면 null)
 * @param onSave 파싱된 값과 그 값의 표시 상태(성공/기준미달/미수행)를 돌려준다
 */
@Composable
fun ValueInputDialog(
    habit: HabitEntity,
    initialValue: Float?,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (value: Float, outcome: RecordOutcome) -> Unit,
    autoFocus: Boolean = false
) {
    var input by remember(habit.habitId, initialValue) {
        mutableStateOf(initialValue?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "")
    }
    val focusRequester = remember { FocusRequester() }
    // 저장 가능 여부는 버튼 활성화로 드러낸다(빈 값·NaN·Infinity는 저장 불가) — 무반응 탭을 없앤다.
    val parsedValue = input.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
    // 목표 표기는 RecordOutcomes.targetLabel 한 곳. 이하 목표는 0도 성공이라는 힌트를 덧붙인다.
    val targetText = RecordOutcomes.targetLabel(habit.targetValue, habit.unit, habit.targetType)?.let { label ->
        if (RecordOutcomes.isAtMost(habit.targetType)) "목표 $label (0도 기록하세요)" else "목표 $label"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = habit.title, color = HabitTheme.colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = habit.question,
                    color = HabitTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Space.s4)
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("수치 (${habit.unit ?: ""})") },
                    placeholder = { Text("예) 5.0") },
                    supportingText = targetText?.let { { Text(it, color = HabitTheme.colors.textSecondary) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HabitTheme.colors.textPrimary,
                        unfocusedTextColor = HabitTheme.colors.textPrimary,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = HabitTheme.colors.lineStrong
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                // 다이얼로그는 별도 창(컴포지션)이라 포커스 요청은 텍스트 필드와 같은 슬롯 안에서 해야 노드가 붙은 뒤 실행된다
                if (autoFocus) {
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedValue != null,
                onClick = {
                    val value = parsedValue ?: return@TextButton
                    onSave(value, RecordOutcomes.of(RecordOutcomes.TYPE_VALUE, null, value, habit.targetValue, habit.targetType))
                }
            ) {
                Text("저장", color = if (parsedValue != null) accent else HabitTheme.colors.textDisabled, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = HabitTheme.colors.textSecondary)
            }
        },
        containerColor = HabitTheme.colors.bgLayer2,
        titleContentColor = HabitTheme.colors.textPrimary,
        shape = MaterialTheme.shapes.large
    )
}
