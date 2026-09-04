package com.example.powerofhabit.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.updateAll
import com.example.powerofhabit.MainActivity
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.ui.theme.DarkTokens
import com.example.powerofhabit.ui.theme.HabitOrange
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Glance 위젯은 Hilt 주입을 받지 못하므로 EntryPoint로 저장소를 꺼낸다. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun dataRepository(): DataRepository
}

internal fun Context.widgetRepository(): DataRepository =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java).dataRepository()

/**
 * 홈 화면 위젯 공통 진입점 (PRD §1.1.4 · §4-1).
 * - 위젯당 상태: `HABIT_ID` (Glance Preferences). 설정 액티비티가 기록한다.
 * - 기록이 바뀌면 ViewModel/액션이 [updateAll]을 호출해 모든 위젯을 다시 그린다.
 * - 위젯은 항상 다크(PRD "어두운 반투명 사각형") — 라이트 테마와 무관하게 [Colors]를 쓴다.
 */
object HabitWidgets {
    val HABIT_ID = intPreferencesKey("habit_id")
    const val EXTRA_HABIT_ID = "com.example.powerofhabit.extra.HABIT_ID"
    private const val TAG = "HabitWidgets"

    /** 위젯 갱신 실패가 저장·기록 흐름을 깨면 안 되므로 Throwable까지 삼킨다(JVM 단위 테스트의 Glance 초기화 오류 포함). */
    suspend fun updateAll(context: Context) {
        try {
            CheckGlanceWidget().updateAll(context)
            CalendarGlanceWidget().updateAll(context)
        } catch (t: Throwable) {
            Log.w(TAG, "widget update failed", t)
        }
    }

    /** 위젯 탭 → 앱의 해당 습관 상세로. data URI를 다르게 해 위젯마다 PendingIntent가 분리되게 한다. */
    fun openHabitIntent(context: Context, habitId: Int): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("powerofhabit://habit/$habitId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HABIT_ID, habitId)
        }

    fun parseThemeColor(hex: String?): Color =
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { HabitOrange }

    /** 위젯 전용 다크 팔레트(디자인 토큰 다크 값). 배경은 drawable `widget_bg`(85% 불투명 웜 블랙). */
    object Colors {
        val ink: Color = DarkTokens.textPrimary
        val inkMuted: Color = DarkTokens.textSecondary
        val inkDisabled: Color = DarkTokens.textDisabled
        val dotEmpty: Color = DarkTokens.textDisabled.copy(alpha = 0.45f)
    }
}
