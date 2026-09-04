package com.example.powerofhabit.widget

import java.time.LocalDate
import java.time.YearMonth

/** 달력 위젯의 한 칸. `status`가 null이면 기록 없음. */
data class CalendarCell(val day: Int, val status: String?)

/**
 * 글랜스 캘린더 위젯용 순수 모델 (PRD §1.1.4: 텍스트 없이 격자 + 테마 컬러 점).
 * 앱 달력과 같은 일요일 시작. 행은 항상 7칸, 앞뒤 빈칸은 null.
 */
object WidgetCalendarModel {

    fun monthGrid(yearMonth: YearMonth, statusByDate: Map<LocalDate, String>): List<List<CalendarCell?>> {
        val leadingBlanks = yearMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0
        val cells = ArrayList<CalendarCell?>(42)
        repeat(leadingBlanks) { cells.add(null) }
        for (day in 1..yearMonth.lengthOfMonth()) {
            cells.add(CalendarCell(day, statusByDate[yearMonth.atDay(day)]))
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }

    /** "YYYY-MM-DD" 문자열 기록을 날짜 → 상태 맵으로. 잘못된 날짜는 버린다. */
    fun statusByDate(records: List<Pair<String, String>>): Map<LocalDate, String> =
        records.mapNotNull { (date, status) ->
            try { LocalDate.parse(date) to status } catch (e: Exception) { null }
        }.toMap()
}
