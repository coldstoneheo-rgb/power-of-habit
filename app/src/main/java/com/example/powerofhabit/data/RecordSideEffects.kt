package com.example.powerofhabit.data

import android.content.Context
import com.example.powerofhabit.backup.GoogleDriveBackupManager
import com.example.powerofhabit.badges.BadgeManager
import kotlinx.coroutines.flow.first

/**
 * 기록(HabitRecords)이 바뀐 뒤 항상 따라와야 하는 부수효과의 단일 정의.
 * 화면(ViewModel)과 홈 위젯 액션이 같은 파이프라인을 타야 뱃지·백업이 어느 경로에서도 빠지지 않는다.
 * 위젯 갱신은 [com.example.powerofhabit.widget.WidgetRefreshObserver]가 DB 변화를 보고 처리하므로 여기 없다.
 */
object RecordSideEffects {
    suspend fun afterRecordChange(context: Context, repository: DataRepository, habitId: Int) {
        // 뱃지 스트릭은 습관의 빈도(주기)를 알아야 판정한다 — 습관이 이미 지워졌으면 뱃지는 건너뛴다.
        repository.getHabitById(habitId).first()?.let { habit ->
            BadgeManager(repository, context).checkAndAwardBadges(habit, repository.getRecordsForHabit(habitId).first())
        }
        GoogleDriveBackupManager(context).scheduleAutoBackup()
    }
}
