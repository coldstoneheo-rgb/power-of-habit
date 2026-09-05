package com.example.powerofhabit.data.transfer

/**
 * 옛 앱(`com.example.powerofhabit`) SQLite 파일의 행 → 이전 DTO. 순수 Kotlin — 파일 열기는 [LegacyDbImporter]가 한다.
 *
 * 옛 DB는 스키마 v1~v5 어느 것이든 올 수 있다(v1: isReminderEnabled·memo·targetValue·targetType·Badges 없음).
 * 그래서 컬럼을 이름으로 찾고, 없거나 null이면 오늘 엔티티의 기본값을 채운다. SQLite 타입은 느슨해서(INTEGER가 Long, REAL이 Double,
 * 불리언이 0/1) 값은 [Number]/[String] 어느 쪽이 와도 받는다.
 */
object LegacyDbRows {

    const val TABLE_HABITS = "Habits"
    const val TABLE_RECORDS = "HabitRecords"
    const val TABLE_BADGES = "Badges"

    /** 필수(habitId·title)가 없으면 null — 호출자가 건너뛴 개수를 센다. */
    fun habit(row: Map<String, Any?>): HabitDto? {
        val habitId = row.int("habitId") ?: return null
        val title = row.string("title")?.takeIf { it.isNotBlank() } ?: return null
        return HabitDto(
            habitId = habitId,
            title = title,
            question = row.string("question") ?: "",
            frequencyType = row.string("frequencyType") ?: "DAILY",
            frequencyValue = row.string("frequencyValue") ?: "",
            reminderTime = row.string("reminderTime"),
            themeColor = row.string("themeColor")?.takeIf { it.isNotBlank() } ?: "#FF9800",
            habitType = row.string("habitType") ?: "CHECK",
            unit = row.string("unit"),
            isReminderEnabled = row.bool("isReminderEnabled") ?: false,
            memo = row.string("memo"),
            targetValue = row.float("targetValue")?.takeIf { it.isFinite() },
            targetType = row.string("targetType") ?: "AT_LEAST",
            // createdAt은 v1부터 있던 컬럼이지만, 혹시 없으면 0 — 매칭 키이므로 임의의 현재 시각을 넣지 않는다.
            createdAt = row.long("createdAt") ?: 0L
        )
    }

    /** habitId·date·status가 없으면 null. status는 해석하지 않고 그대로 옮긴다(JSON 가져오기와 같은 규칙). */
    fun record(row: Map<String, Any?>): RecordDto? {
        val habitId = row.int("habitId") ?: return null
        val date = row.string("date")?.takeIf { it.isNotBlank() } ?: return null
        val status = row.string("status")?.takeIf { it.isNotBlank() } ?: return null
        return RecordDto(
            recordId = row.int("recordId") ?: 0,
            habitId = habitId,
            date = date,
            status = status,
            inputValue = row.float("inputValue")?.takeIf { it.isFinite() }
        )
    }

    fun badge(row: Map<String, Any?>): BadgeDto? {
        val badgeId = row.string("badgeId")?.takeIf { it.isNotBlank() } ?: return null
        return BadgeDto(
            badgeId = badgeId,
            badgeName = row.string("badgeName") ?: badgeId,
            description = row.string("description") ?: "",
            earnedAt = row.long("earnedAt") ?: 0L,
            badgeIconType = row.string("badgeIconType") ?: "DEFAULT"
        )
    }

    /** SAF로 고른 파일 이름들에서 DB 본체와 -wal/-shm 짝을 찾는다. 본체가 정확히 하나가 아니면 null. */
    fun resolveFiles(displayNames: List<String>): LegacyDbFiles? {
        val dbNames = displayNames.filter { it.endsWith(".db", ignoreCase = true) }
        val db = dbNames.singleOrNull() ?: return null
        return LegacyDbFiles(
            db = db,
            wal = displayNames.firstOrNull { it.equals("$db-wal", ignoreCase = true) },
            shm = displayNames.firstOrNull { it.equals("$db-shm", ignoreCase = true) }
        )
    }

    // ---- 느슨한 타입 읽기 ----

    private fun Map<String, Any?>.string(key: String): String? = when (val v = this[key]) {
        null -> null
        is String -> v
        else -> v.toString()
    }

    private fun Map<String, Any?>.long(key: String): Long? = when (val v = this[key]) {
        null -> null
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: v.toDoubleOrNull()?.toLong()
        else -> null
    }

    private fun Map<String, Any?>.int(key: String): Int? = long(key)?.toInt()

    private fun Map<String, Any?>.float(key: String): Float? = when (val v = this[key]) {
        null -> null
        is Number -> v.toFloat()
        is String -> v.toFloatOrNull()
        else -> null
    }

    private fun Map<String, Any?>.bool(key: String): Boolean? = when (val v = this[key]) {
        null -> null
        is Boolean -> v
        is Number -> v.toLong() != 0L
        is String -> v == "1" || v.equals("true", ignoreCase = true)
        else -> null
    }
}

/** 고른 파일 중 DB 본체와 선택적인 WAL/SHM 짝의 표시 이름. */
data class LegacyDbFiles(val db: String, val wal: String?, val shm: String?)
