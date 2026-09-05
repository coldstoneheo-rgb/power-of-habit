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
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    val targetText = habit.targetValue?.let { t ->
        val v = if (t % 1f == 0f) t.toInt().toString() else t.toString()
        "목표 $v ${habit.unit ?: ""}".trim()
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // NaN·Infinity·빈 값은 저장하지 않는다(RecordOutcomes와 같은 유효성 기준)
                    val value = input.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() } ?: return@TextButton
                    onSave(value, RecordOutcomes.of(RecordOutcomes.TYPE_VALUE, null, value, habit.targetValue))
                }
            ) {
                Text("저장", color = accent, fontWeight = FontWeight.Bold)
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
