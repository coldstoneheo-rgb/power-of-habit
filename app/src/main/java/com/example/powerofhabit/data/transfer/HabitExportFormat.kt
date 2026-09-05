package com.example.powerofhabit.data.transfer

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.serialization.Serializable

/**
 * 로컬 JSON 내보내기/가져오기 파일 형식 (formatVersion 1).
 *
 * DTO는 Room 엔티티의 필드를 그대로 담는다(nullable 유지). 엔티티에 필드가 추가되면 DTO에도 기본값을 가진 필드로
 * 추가하고, 의미가 바뀌는 변경일 때만 formatVersion을 올린다. 읽기 쪽은 ignoreUnknownKeys = true라
 * 같은 버전 안에서 필드가 늘어난 파일도 옛 앱이 읽을 수 있다.
 */
@Serializable
data class HabitExport(
    val formatVersion: Int = 1,
    /** ISO-8601 (예: 2026-09-05T01:23:45Z). */
    val exportedAt: String,
    val appVersionName: String? = null,
    val habits: List<HabitDto> = emptyList(),
    val records: List<RecordDto> = emptyList(),
    val badges: List<BadgeDto> = emptyList()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class HabitDto(
    val habitId: Int,
    val title: String,
    val question: String,
    val frequencyType: String,
    val frequencyValue: String,
    val reminderTime: String? = null,
    val themeColor: String,
    val habitType: String,
    val unit: String? = null,
    val isReminderEnabled: Boolean = false,
    val memo: String? = null,
    val targetValue: Float? = null,
    /** 목표 방향(AT_LEAST/AT_MOST). 필드가 없는 옛 파일은 AT_LEAST — formatVersion은 그대로 1. */
    val targetType: String = "AT_LEAST",
    val createdAt: Long
)

@Serializable
data class RecordDto(
    val recordId: Int,
    val habitId: Int,
    val date: String,
    /** 모르는 값도 그대로 보존한다(새 status 문자열을 해석하거나 변환하지 않는다). */
    val status: String,
    val inputValue: Float? = null
)

@Serializable
data class BadgeDto(
    val badgeId: String,
    val badgeName: String,
    val description: String,
    val earnedAt: Long,
    val badgeIconType: String
)

fun HabitEntity.toDto(): HabitDto = HabitDto(
    habitId = habitId,
    title = title,
    question = question,
    frequencyType = frequencyType,
    frequencyValue = frequencyValue,
    reminderTime = reminderTime,
    themeColor = themeColor,
    habitType = habitType,
    unit = unit,
    isReminderEnabled = isReminderEnabled,
    memo = memo,
    targetValue = targetValue,
    targetType = targetType,
    createdAt = createdAt
)

/** [habitId]를 지정하지 않으면 0(자동 생성)으로 만들어 새 습관으로 삽입할 수 있게 한다. */
fun HabitDto.toEntity(habitId: Int = 0): HabitEntity = HabitEntity(
    habitId = habitId,
    title = title,
    question = question,
    frequencyType = frequencyType,
    frequencyValue = frequencyValue,
    reminderTime = reminderTime,
    themeColor = themeColor,
    habitType = habitType,
    unit = unit,
    isReminderEnabled = isReminderEnabled,
    memo = memo,
    targetValue = targetValue,
    targetType = targetType,
    createdAt = createdAt
)

fun HabitRecordEntity.toDto(): RecordDto = RecordDto(
    recordId = recordId,
    habitId = habitId,
    date = date,
    status = status,
    inputValue = inputValue
)

/** recordId는 항상 0(자동 생성)으로 만든다 — 대상 DB의 기존 id와 충돌하지 않게. */
fun RecordDto.toEntity(habitId: Int): HabitRecordEntity = HabitRecordEntity(
    recordId = 0,
    habitId = habitId,
    date = date,
    status = status,
    inputValue = inputValue
)

fun BadgeEntity.toDto(): BadgeDto = BadgeDto(
    badgeId = badgeId,
    badgeName = badgeName,
    description = description,
    earnedAt = earnedAt,
    badgeIconType = badgeIconType
)

fun BadgeDto.toEntity(): BadgeEntity = BadgeEntity(
    badgeId = badgeId,
    badgeName = badgeName,
    description = description,
    earnedAt = earnedAt,
    badgeIconType = badgeIconType
)
