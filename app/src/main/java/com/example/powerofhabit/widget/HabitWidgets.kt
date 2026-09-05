package com.example.powerofhabit.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.powerofhabit.MainActivity
import com.example.powerofhabit.data.DataRepository
import com.example.powerofhabit.ui.theme.DarkTokens
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
 * - 갱신은 [WidgetRefreshObserver]가 DB 변화를 관찰해 자동으로 한다. 위젯 액션은 즉시성을 위해 [updateAll]을 직접 부른다.
 * - 위젯은 항상 다크(PRD "어두운 반투명 사각형") — 라이트 테마와 무관하게 [Colors]를 쓴다.
 */
object HabitWidgets {
    val HABIT_ID = intPreferencesKey("habit_id")
    const val EXTRA_HABIT_ID = "com.example.powerofhabit.extra.HABIT_ID"
    private const val TAG = "HabitWidgets"

    /**
     * 모든 위젯 인스턴스를 다시 그린다.
     * Glance의 `updateAll()`은 리시버→위젯 내부 매핑에 의존해 방금 추가된 위젯이나 프로세스 재시작 직후에는 빈 목록을 돌려주는 경우가 있어
     * (실기기 관찰: 설정 후 30분 주기까지 미반영), AppWidgetManager에서 provider별 appWidgetId를 직접 나열해 하나씩 갱신한다.
     */
    suspend fun updateAll(context: Context) {
        updateProvider(context, CheckWidgetReceiver::class.java, CheckGlanceWidget())
        updateProvider(context, CalendarWidgetReceiver::class.java, CalendarGlanceWidget())
    }

    /** 특정 appWidgetId 하나만 갱신(설정 액티비티에서 즉시 반영용). provider를 보고 어떤 위젯인지 결정한다. */
    suspend fun updateOne(context: Context, appWidgetId: Int) {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
        val widget: GlanceAppWidget = when (info?.provider?.className) {
            CheckWidgetReceiver::class.java.name -> CheckGlanceWidget()
            CalendarWidgetReceiver::class.java.name -> CalendarGlanceWidget()
            else -> { updateAll(context); return }
        }
        try {
            widget.update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId))
        } catch (e: Exception) {
            Log.w(TAG, "widget update failed for id=$appWidgetId", e)
        }
    }

    private suspend fun updateProvider(context: Context, receiver: Class<*>, widget: GlanceAppWidget) {
        val ids = try {
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver)) ?: IntArray(0)
        } catch (e: Exception) {
            Log.w(TAG, "getAppWidgetIds failed", e); return
        }
        if (ids.isEmpty()) return
        val manager = GlanceAppWidgetManager(context)
        for (id in ids) {
            try {
                widget.update(context, manager.getGlanceIdBy(id))
            } catch (e: Exception) {
                Log.w(TAG, "widget update failed for id=$id", e)
            }
        }
    }

    /** 위젯 탭 → 앱의 해당 습관 상세로. */
    fun openHabitIntent(context: Context, habitId: Int): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("powerofhabit://habit/$habitId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HABIT_ID, habitId)
        }

    /** 수치형 습관 1x1 탭 → 앱을 열지 않고 값만 입력하는 투명 액티비티. 렌더 날짜를 함께 보내 자정 가드를 적용한다. */
    fun valueInputIntent(context: Context, habitId: Int, renderedDate: String, appWidgetId: Int): Intent =
        Intent(context, ValueInputActivity::class.java).apply {
            action = Intent.ACTION_EDIT
            data = Uri.parse("powerofhabit://value/$habitId/$appWidgetId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ValueInputActivity.EXTRA_HABIT_ID, habitId)
            putExtra(ValueInputActivity.EXTRA_RENDERED_DATE, renderedDate)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

    /** 습관이 없거나 삭제된 위젯의 빈 상태 탭 → 습관 다시 고르기. */
    fun reconfigureIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, WidgetConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            data = Uri.parse("powerofhabit://widget/$appWidgetId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

    fun parseThemeColor(hex: String?): Color =
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { DarkTokens.primary } // 앱의 habitAccent와 같은 폴백

    /** 위젯 전용 다크 팔레트(디자인 토큰 다크 값). 배경은 drawable `widget_bg`(85% 불투명 웜 블랙). */
    object Colors {
        val ink: Color = DarkTokens.textPrimary
        val inkMuted: Color = DarkTokens.textSecondary
        val inkDisabled: Color = DarkTokens.textDisabled
        val skip: Color = DarkTokens.statusSkip
        val fail: Color = DarkTokens.statusFail
        val dotEmpty: Color = DarkTokens.textDisabled.copy(alpha = 0.45f)

        /** 기준미달 수행: 앱과 완전히 같은 함수(다크 토큰, 위젯 배경 ≈ bg.base 기준 AA 보정). */
        fun partial(accent: Color): Color = DarkTokens.partialAccent(accent)

        /** 습관색으로 채운 타일 위의 잉크(성공 타일). */
        fun onAccent(accent: Color): Color = DarkTokens.onAccent(accent)
    }
}
