package com.example.powerofhabit.data.transfer

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.RecordOutcomes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/** 가져오기 결과 요약. UI 메시지와 테스트 검증에 쓴다. */
data class ImportSummary(
    val habitsAdded: Int,
    val habitsMatched: Int,
    val recordsAdded: Int,
    val recordsSkipped: Int,
    val badgesAdded: Int,
    /** 성공했지만 사용자가 알아야 할 것(예: 옛 DB를 -wal 없이 읽음). 화면이 요약 뒤에 덧붙인다. */
    val warnings: List<String> = emptyList()
)

/** 새로 삽입할 습관. [sourceHabitId]는 파일 안의 id이며 삽입 후 실제 id로 재매핑된다. */
data class PendingHabit(val sourceHabitId: Int, val habit: HabitEntity)

/** 삽입할 기록. [record].habitId는 파일 안의 id이므로 [ImportPlan.resolveRecords]로 실제 id를 채운다. */
data class PendingRecord(val sourceHabitId: Int, val record: HabitRecordEntity)

/**
 * 가져오기 계획. 순수 데이터라 DB 없이 검증할 수 있다.
 *
 * 실행 순서: [habitsToInsert]를 삽입해 (sourceHabitId → 새 id)를 얻고, [matchedHabitIds]와 합쳐
 * [resolveRecords]로 기록의 habitId를 채운 뒤 삽입, 마지막으로 [badgesToInsert].
 */
data class ImportPlan(
    val habitsToInsert: List<PendingHabit>,
    /** 파일 habitId → 기존 DB habitId. createdAt 일치로 맺어진 것. */
    val matchedHabitIds: Map<Int, Int>,
    val recordsToInsert: List<PendingRecord>,
    val badgesToInsert: List<BadgeEntity>,
    val summary: ImportSummary
) {
    /**
     * 실제 habitId로 채운 기록 목록. [insertedHabitIds]는 [habitsToInsert] 삽입 결과(sourceHabitId → 새 id).
     * 매핑이 없는 기록은 FK 위반을 피하기 위해 버린다(계획 단계에서 이미 걸러져 정상 경로에서는 없다).
     */
    fun resolveRecords(insertedHabitIds: Map<Int, Int>): List<HabitRecordEntity> {
        val idMap = matchedHabitIds + insertedHabitIds
        return recordsToInsert.mapNotNull { pending ->
            idMap[pending.sourceHabitId]?.let { pending.record.copy(habitId = it) }
        }
    }
}

/**
 * 로컬 JSON 이전의 순수 Kotlin 코어(Android 의존 없음). 파일 I/O는 [TransferManager]가 맡는다.
 *
 * 병합 규칙(가져오기는 항상 "덮어쓰지 않는 병합"):
 * - 습관: **createdAt**(생성 시각 ms — 사실상 습관의 안정된 id, 제목을 바꿔도 변하지 않음)이 같으면 기존 습관으로 간주해
 *   id를 재사용하고, 아니면 새 습관으로 삽입한다. 제목은 키에 넣지 않는다(기기 한쪽에서 이름을 바꿔도 중복 생성되지 않게).
 * - 기록: **재매핑 후의** (habitId, date)가 키. 파일 안에서 같은 키가 여러 개면 recordId가 큰 것(가장 최근 쓰기)을 택하고,
 *   기존 DB에 같은 키가 이미 있으면 기존 기록을 유지한다(파일 값으로 덮어쓰지 않는다).
 *   status 문자열은 해석 없이 그대로 옮긴다(모르는 값 보존).
 * - 뱃지: badgeId가 없는 것만 추가한다.
 */
object HabitTransfer {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun buildExport(
        habits: List<HabitEntity>,
        records: List<HabitRecordEntity>,
        badges: List<BadgeEntity>,
        appVersionName: String?,
        exportedAt: Instant = Instant.now()
    ): HabitExport {
        // NaN/Infinity는 JSON에 실을 수 없어 한 건이 전체 내보내기를 막는다 → null로 정화(미수행과 같은 의미)
        val habitDtos = habits.map { h -> h.toDto().let { d -> d.copy(targetValue = d.targetValue?.takeIf(Float::isFinite)) } }
        return HabitExport(
            // 이하 목표 습관이 있을 때만 2 — 옛 앱이 그 습관을 "이상"으로 뒤집어 읽는 대신 "업데이트하라"를 보게 한다
            formatVersion = HabitExport.versionFor(habitDtos),
            exportedAt = exportedAt.toString(),
            appVersionName = appVersionName,
            habits = habitDtos,
            records = records.map { r -> r.toDto().let { d -> d.copy(inputValue = d.inputValue?.takeIf(Float::isFinite)) } },
            badges = badges.map { it.toDto() }
        )
    }

    fun encode(export: HabitExport): String = json.encodeToString(HabitExport.serializer(), export)

    /**
     * @throws IllegalArgumentException 지원하지 않는 formatVersion, 또는 JSON 구조가 형식과 맞지 않을 때.
     * formatVersion은 전체 역직렬화 **전에** 먼저 읽어, 미래 형식 파일에 "형식이 아니다" 대신 "업데이트하라"를 말한다.
     * 선행 BOM(U+FEFF, 메모장 저장 등)은 제거한다.
     */
    fun decode(rawText: String): HabitExport {
        val text = rawText.removePrefix("﻿")
        val peekedVersion = try {
            json.parseToJsonElement(text).jsonObject["formatVersion"]?.jsonPrimitive?.intOrNull
        } catch (e: SerializationException) {
            throw IllegalArgumentException("습관 데이터 파일 형식이 아닙니다", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("습관 데이터 파일 형식이 아닙니다", e)
        }
        if (peekedVersion != null && peekedVersion > HabitExport.CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException(
                "지원하지 않는 형식 버전입니다 (파일 $peekedVersion, 지원 ${HabitExport.CURRENT_FORMAT_VERSION}). 앱을 업데이트해 주세요"
            )
        }
        return try {
            json.decodeFromString(HabitExport.serializer(), text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("습관 데이터 파일 형식이 아닙니다", e)
        }
    }

    fun plan(
        existingHabits: List<HabitEntity>,
        existingRecords: List<HabitRecordEntity>,
        existingBadges: List<BadgeEntity>,
        import: HabitExport
    ): ImportPlan {
        // 1) 습관 매칭(createdAt). 같은 createdAt이 기존 DB에 여럿이면 id가 가장 작은(먼저 만든) 것을 쓴다.
        val existingByCreatedAt = HashMap<Long, HabitEntity>()
        for (habit in existingHabits.sortedBy { it.habitId }) {
            existingByCreatedAt.putIfAbsent(habit.createdAt, habit)
        }
        val matched = LinkedHashMap<Int, Int>()
        val matchedHabit = HashMap<Int, HabitEntity>() // 파일 habitId → 기존 습관(목표·방향은 기존 것을 따른다)
        val toInsert = ArrayList<PendingHabit>()
        // 파일 안에서 같은 habitId가 중복되면 첫 번째만 쓴다.
        for (dto in import.habits.distinctBy { it.habitId }) {
            val hit = existingByCreatedAt[dto.createdAt]
            if (hit != null) {
                matched[dto.habitId] = hit.habitId
                matchedHabit[dto.habitId] = hit
            } else {
                toInsert += PendingHabit(dto.habitId, dto.toEntity(habitId = 0))
            }
        }
        val knownSourceIds: Set<Int> = matched.keys + toInsert.map { it.sourceHabitId }

        // 2) 기록. 키는 재매핑 후의 습관("e<기존id>" 또는 "n<파일id>")과 날짜 — 파일의 두 습관이 같은 기존 습관에 매칭돼도 중복이 안 생긴다.
        fun resolvedKey(sourceHabitId: Int): String =
            matched[sourceHabitId]?.let { "e$it" } ?: "n$sourceHabitId"
        val existingKeys: Set<Pair<Int, String>> = existingRecords.mapTo(HashSet()) { it.habitId to it.date }
        var skipped = 0
        val orphanCount = import.records.count { it.habitId !in knownSourceIds }
        skipped += orphanCount // 파일에 습관이 없는 고아 기록 — FK를 만족시킬 수 없다.
        val dedupedInFile: List<RecordDto> = import.records
            .filter { it.habitId in knownSourceIds }
            .groupBy { resolvedKey(it.habitId) to it.date }
            .values
            .map { group ->
                skipped += group.size - 1
                group.maxBy { it.recordId }
            }
        val records = ArrayList<PendingRecord>()
        for (dto in dedupedInFile) {
            val targetHabitId = matched[dto.habitId]
            if (targetHabitId != null && (targetHabitId to dto.date) in existingKeys) {
                skipped++ // 기존 기록 유지
                continue
            }
            var entity = dto.toEntity(habitId = dto.habitId)
            // 기존 습관에 붙는 수치 기록은 status 캐시를 **기존 습관의** 목표·방향으로 다시 맞춘다 —
            // 파일을 만든 기기에서 방향이 달랐다면 그대로 옮기면 통계(캐시)와 캘린더(값)가 반대를 말한다.
            val local = matchedHabit[dto.habitId]
            if (local != null && local.habitType == RecordOutcomes.TYPE_VALUE) {
                RecordOutcomes.restatusAfterTargetChange(entity.status, entity.inputValue, local.targetValue, local.targetType)
                    ?.let { entity = entity.copy(status = it) }
            }
            records += PendingRecord(dto.habitId, entity)
        }

        // 3) 뱃지. badgeId 없는 것만.
        val existingBadgeIds = existingBadges.mapTo(HashSet()) { it.badgeId }
        val badges = import.badges
            .distinctBy { it.badgeId }
            .filter { it.badgeId !in existingBadgeIds }
            .map { it.toEntity() }

        return ImportPlan(
            habitsToInsert = toInsert,
            matchedHabitIds = matched,
            recordsToInsert = records,
            badgesToInsert = badges,
            summary = ImportSummary(
                habitsAdded = toInsert.size,
                habitsMatched = matched.size,
                recordsAdded = records.size,
                recordsSkipped = skipped,
                badgesAdded = badges.size
            )
        )
    }
}
