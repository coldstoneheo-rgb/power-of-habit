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
    private val NUMBER = Regex("[0-9]+(?:[.,][0-9]+)?")

    /**
     * "12", "2.5", "2,5"(쉼표 소수점)만 받는다. 앞뒤 공백은 무시. "5f"·"0x1p3"·"+5"·"1e40"·"1,000,000"처럼 Kotlin의 toFloatOrNull은
     * 통과하지만 사용자가 뜻한 수치가 아닌 것은 null(저장 불가). 유한값만.
     */
    fun parse(text: String): Float? {
        val t = text.trim()
        if (!NUMBER.matches(t)) return null
        return t.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
    }

    /** 저장된 값 → 입력칸 초기 문자열. 표기 규칙은 [RecordOutcomes.formatNumber] 한 곳(정수는 소수점 없이, 지수 표기 없음). null은 빈 칸. */
    fun format(value: Float?): String = value?.let(RecordOutcomes::formatNumber) ?: ""

    /** 필드 라벨: 단위가 있을 때만 괄호. "수치 ()"를 만들지 않는다. */
    fun label(prefix: String, unit: String?): String = unit?.takeIf { it.isNotBlank() }?.let { "$prefix ($it)" } ?: prefix

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
    labelPrefix: String = "수치",
    placeholder: String? = "예) 5.0",
    focusRequester: FocusRequester? = null
) {
    val hint = ValueInput.targetHint(habit)
    OutlinedTextField(
        value = input,
        onValueChange = onInput,
        label = { Text(ValueInput.label(labelPrefix, habit.unit)) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = hint?.let { { Text(it, color = HabitTheme.colors.textSecondary) } },
        // Decimal: 소수점 키가 없는 숫자 키보드(일부 삼성 IME)에서도 2.5를 넣을 수 있게
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
