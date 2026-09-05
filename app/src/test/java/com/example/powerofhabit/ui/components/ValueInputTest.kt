package com.example.powerofhabit.ui.components

import com.example.powerofhabit.data.local.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValueInputTest {

    private fun habit(target: Float?, type: String = "AT_LEAST", unit: String? = "km") = HabitEntity(
        habitId = 1, title = "t", question = "q", frequencyType = "DAILY", frequencyValue = "", reminderTime = null,
        themeColor = "#FF9800", habitType = "VALUE", unit = unit, targetValue = target, targetType = type
    )

    @Test
    fun parse_acceptsCommaDecimal_rejectsBlankAndNonFinite() {
        assertEquals(5f, ValueInput.parse("5")!!, 0f)
        assertEquals(2.5f, ValueInput.parse("2,5")!!, 0f)
        assertEquals(0f, ValueInput.parse("0")!!, 0f)
        assertNull(ValueInput.parse(""))
        assertNull(ValueInput.parse("abc"))
        assertNull(ValueInput.parse("NaN"))
        assertNull(ValueInput.parse("Infinity"))
        assertNull(ValueInput.parse("1e40"))
    }

    @Test
    fun format_dropsTrailingZero_forWholeNumbers() {
        assertEquals("5", ValueInput.format(5f))
        assertEquals("2.5", ValueInput.format(2.5f))
        assertEquals("0", ValueInput.format(0f))
        assertEquals("", ValueInput.format(null))
    }

    @Test
    fun format_thenParse_roundTrips() {
        for (v in listOf(0f, 1f, 2.5f, 12.75f, 100f)) assertEquals(v, ValueInput.parse(ValueInput.format(v))!!, 0f)
    }

    @Test
    fun targetHint_followsDirection_andIsNullWithoutTarget() {
        assertEquals("목표 5 km", ValueInput.targetHint(habit(5f)))
        assertEquals("목표 5 개비 이하 (0도 기록하세요)", ValueInput.targetHint(habit(5f, "AT_MOST", "개비")))
        assertNull(ValueInput.targetHint(habit(null)))
    }
}
