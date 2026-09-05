package com.example.powerofhabit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    var input by remember(habit.habitId, initialValue) { mutableStateOf(ValueInput.format(initialValue)) }
    val focusRequester = remember { FocusRequester() }
    // 저장 가능 여부는 버튼 활성화로 드러낸다(빈 값·NaN·Infinity는 저장 불가) — 무반응 탭을 없앤다. 규칙은 ValueInput 한 곳.
    val parsedValue = ValueInput.parse(input)

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
                ValueInputField(
                    habit = habit,
                    input = input,
                    onInput = { input = it },
                    accent = accent,
                    focusRequester = focusRequester
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
