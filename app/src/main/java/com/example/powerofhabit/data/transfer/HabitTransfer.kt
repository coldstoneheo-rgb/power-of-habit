package com.example.powerofhabit.data.transfer

import com.example.powerofhabit.data.local.BadgeEntity
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant

/** 가져오기 결과 요약. UI 메시지와 테스트 검증에 쓴다. */
data class ImportSummary(
    val habitsAdded: Int,
    val habitsMatched: Int,
    val recordsAdded: Int,
    val recordsSkipped: Int,
    val badgesAdded: Int
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
    /** 파일 habitId → 기존 DB habitId. (title, createdAt) 일치로 맺어진 것. */
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
 * - 습관: (title, createdAt)이 같으면 기존 습관으로 간주해 id를 재사용하고, 아니면 새 습관으로 삽입한다.
 * - 기록: (habitId, date)가 키. 파일 안에서 같은 키가 여러 개면 recordId가 큰 것(가장 최근 쓰기)을 택하고,
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
    ): HabitExport = HabitExport(
        formatVersion = HabitExport.CURRENT_FORMAT_VERSION,
        exportedAt = exportedAt.toString(),
        appVersionName = appVersionName,
        habits = habits.map { it.toDto() },
        records = records.map { it.toDto() },
        badges = badges.map { it.toDto() }
    )

    fun encode(export: HabitExport): String = json.encodeToString(HabitExport.serializer(), export)

    /** @throws IllegalArgumentException 지원하지 않는 formatVersion, 또는 JSON 구조가 형식과 맞지 않을 때. */
    fun decode(text: String): HabitExport {
        val export = try {
            json.decodeFromString(HabitExport.serializer(), text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("습관 데이터 파일 형식이 아닙니다", e)
        }
        if (export.formatVersion > HabitExport.CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException(
                "지원하지 않는 형식 버전입니다 (파일 ${export.formatVersion}, 지원 ${HabitExport.CURRENT_FORMAT_VERSION}). 앱을 업데이트해 주세요"
            )
        }
        return export
    }

    fun plan(
        existingHabits: List<HabitEntity>,
        existingRecords: List<HabitRecordEntity>,
        existingBadges: List<BadgeEntity>,
        import: HabitExport
    ): ImportPlan {
        // 1) 습관 매칭. 같은 (title, createdAt)이 기존 DB에 여럿이면 id가 가장 작은(먼저 만든) 것을 쓴다.
        val existingByKey = HashMap<Pair<String, Long>, HabitEntity>()
        for (habit in existingHabits.sortedBy { it.habitId }) {
            existingByKey.putIfAbsent(habit.title to habit.createdAt, habit)
        }
        val matched = LinkedHashMap<Int, Int>()
        val toInsert = ArrayList<PendingHabit>()
        // 파일 안에서 같은 habitId가 중복되면 첫 번째만 쓴다.
        for (dto in import.habits.distinctBy { it.habitId }) {
            val hit = existingByKey[dto.title to dto.createdAt]
            if (hit != null) {
                matched[dto.habitId] = hit.habitId
            } else {
                toInsert += PendingHabit(dto.habitId, dto.toEntity(habitId = 0))
            }
        }
        val knownSourceIds: Set<Int> = matched.keys + toInsert.map { it.sourceHabitId }

        // 2) 기록. 파일 내부 (habitId, date) 중복은 recordId 큰 것 우선.
        val existingKeys: Set<Pair<Int, String>> = existingRecords.mapTo(HashSet()) { it.habitId to it.date }
        var skipped = 0
        val dedupedInFile: List<RecordDto> = import.records
            .groupBy { it.habitId to it.date }
            .values
            .map { group ->
                skipped += group.size - 1
                group.maxBy { it.recordId }
            }
        val records = ArrayList<PendingRecord>()
        for (dto in dedupedInFile) {
            if (dto.habitId !in knownSourceIds) {
                skipped++ // 파일에 습관이 없는 고아 기록 — FK를 만족시킬 수 없다.
                continue
            }
            val targetHabitId = matched[dto.habitId]
            if (targetHabitId != null && (targetHabitId to dto.date) in existingKeys) {
                skipped++ // 기존 기록 유지
                continue
            }
            records += PendingRecord(dto.habitId, dto.toEntity(habitId = dto.habitId))
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
