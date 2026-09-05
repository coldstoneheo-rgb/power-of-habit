package com.example.powerofhabit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.theme.HabitTheme

/**
 * 수치 입력의 파싱·표기 규칙 한 곳. 메인/위젯 입력 다이얼로그([ValueInputDialog])와 상세 화면의 기록 편집 다이얼로그가 같은 것을 쓴다.
 * (#33 자체 리뷰: 두 다이얼로그가 각자 필드·파싱·힌트를 갖고 있어 규칙이 한쪽에만 고쳐지던 문제.)
 */
object ValueInput {
    /** 쉼표 소수점 허용, 유한값만. 빈 문자열·NaN·Infinity는 null(저장 불가). */
    fun parse(text: String): Float? = text.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }

    /** 저장된 값 → 입력칸 초기 문자열. 정수는 소수점 없이("5.0" 대신 "5"). null은 빈 칸. */
    fun format(value: Float?): String = value?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: ""

    /** 필드 아래 목표 안내. 이하 목표는 0도 성공이라는 힌트를 덧붙인다. 목표가 없으면 null. */
    fun targetHint(habit: HabitEntity): String? =
        RecordOutcomes.targetLabel(habit.targetValue, habit.unit, habit.targetType)?.let { label ->
            if (RecordOutcomes.isAtMost(habit.targetType)) "목표 $label (0도 기록하세요)" else "목표 $label"
        }
}

/**
 * 수치 입력 필드 + 목표 안내. 값 파싱은 호출자가 [ValueInput.parse]로 한다(저장 버튼 활성화 판단에 같은 결과를 쓰기 위해).
 * @param focusRequester 다이얼로그가 열리자마다 키보드를 올릴 때 넘긴다(호출자가 같은 슬롯 안에서 requestFocus).
 */
@Composable
fun ValueInputField(
    habit: HabitEntity,
    input: String,
    onInput: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    label: String = "수치 (${habit.unit ?: ""})",
    placeholder: String? = "예) 5.0",
    focusRequester: FocusRequester? = null
) {
    val hint = ValueInput.targetHint(habit)
    OutlinedTextField(
        value = input,
        onValueChange = onInput,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = hint?.let { { Text(it, color = HabitTheme.colors.textSecondary) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HabitTheme.colors.textPrimary,
            unfocusedTextColor = HabitTheme.colors.textPrimary,
            focusedBorderColor = accent,
            unfocusedBorderColor = HabitTheme.colors.lineStrong
        ),
        modifier = modifier
            .fillMaxWidth()
            .let { m -> if (focusRequester != null) m.focusRequester(focusRequester) else m }
    )
}
