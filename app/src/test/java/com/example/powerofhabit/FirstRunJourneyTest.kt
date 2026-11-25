package com.example.powerofhabit

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.reminder.HabitReminderManager
import com.example.powerofhabit.ui.legal.PrivacyPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.time.LocalDate

/** 처음 설치한 사람의 한 바퀴: 실제 액티비티·Hilt·Room으로 등록 → 기록 → 다시 열기 → 수정 → 삭제. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w360dp-h780dp-xxhdpi")
class FirstRunJourneyTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val app: PowerOfHabitApp get() = context as PowerOfHabitApp

    private fun habits(): List<HabitEntity> = runBlocking { app.repository.getAllHabits().first() }

    private fun waitForText(text: String) =
        rule.waitUntil("«$text» 표시", 10_000) { rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForIcon(description: String) =
        rule.waitUntil("«$description» 아이콘 표시", 10_000) {
            rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
        }

    /** build/outputs/journey 아래에 PNG를 남긴다(커밋하지 않는다). */
    private fun shot(name: String) {
        val dir = File("build/outputs/journey").apply { mkdirs() }
        rule.waitForIdle()
        // captureToImage는 JVM에서 그리기를 기다리다 시간이 넘는다 — 맨 위 창의 뷰를 직접 그린다
        val view = ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView ?: rule.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun chooseCheckType() {
        waitForText("예 또는 아니요")
        rule.onNodeWithText("예 또는 아니요").performClick()
        waitForText("습관 만들기")
    }

    @Test
    fun install_create_record_reopen_edit_delete() {
        waitForText("아직 습관이 없습니다")
        shot("01-empty")
        rule.onNodeWithText("첫 습관 만들기").performClick()
        chooseCheckType()
        rule.onNode(hasSetTextAction() and hasText("제목")).performTextInput("책 10쪽")
        shot("02-create")
        rule.onNodeWithText("저장").performClick()
        waitForText("책 10쪽")
        rule.waitUntil(10_000) { habits().isNotEmpty() }
        assertEquals(1, habits().size)
        waitForIcon("미완료") // 목록으로 돌아와 칸이 그려질 때까지

        // 오늘 칸(기본 설정은 오늘이 가장 왼쪽) 체크
        rule.onAllNodesWithContentDescription("미완료").onFirst().performClick()
        rule.waitUntil(10_000) { runBlocking { app.repository.getAllRecords().first() }.isNotEmpty() }
        val record = runBlocking { app.repository.getAllRecords().first() }.single()
        assertEquals(LocalDate.now().toString(), record.date)
        assertEquals("COMPLETED", record.status)
        waitForIcon("완료")
        shot("03-home-checked")

        // 다시 열기
        rule.activityRule.scenario.recreate()
        waitForText("책 10쪽")
        waitForIcon("완료")

        // 수정 화면에 들어갔다 나온 뒤 «만들기»는 빈 칸이어야 한다
        rule.onNodeWithText("책 10쪽").performClick()
        waitForIcon("수정")
        shot("04-detail")
        rule.onNodeWithContentDescription("수정").performClick()
        waitForText("습관 수정하기")
        waitForText("책 10쪽")
        rule.onNodeWithContentDescription("뒤로").performClick()
        waitForIcon("수정")
        rule.onNodeWithContentDescription("뒤로").performClick()
        waitForIcon("습관 추가")
        rule.onNodeWithContentDescription("습관 추가").performClick()
        chooseCheckType()
        rule.waitForIdle()
        assertEquals(0, rule.onAllNodesWithText("책 10쪽").fetchSemanticsNodes().size)
        rule.onNodeWithContentDescription("뒤로").performClick()
        waitForText("책 10쪽")

        // 수정 화면의 삭제는 한 번 묻고, 목록으로 바로 돌아온다
        rule.onNodeWithText("책 10쪽").performClick()
        waitForIcon("수정")
        rule.onNodeWithContentDescription("수정").performClick()
        waitForIcon("습관 삭제")
        rule.onNodeWithContentDescription("습관 삭제").performClick()
        waitForText("이 습관과 그동안의 기록이 모두 삭제됩니다. 되돌릴 수 없습니다.")
        assertEquals(1, habits().size)
        rule.onNodeWithText("취소").performClick()
        rule.waitForIdle()
        assertEquals(1, habits().size)
        rule.onNodeWithContentDescription("습관 삭제").performClick()
        rule.onNodeWithText("삭제").performClick()
        waitForText("아직 습관이 없습니다")
        assertEquals(0, habits().size)
        assertEquals("삭제되었습니다.", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun settings_showsActualTheme_andPrivacyLink() {
        waitForText("아직 습관이 없습니다")
        rule.onNodeWithContentDescription("설정").performClick()
        waitForText("설정 및 동기화")
        // 테마를 고른 적이 없고 시스템이 라이트면 스위치도 꺼져 있다
        rule.onAllNodes(isToggleable()).onFirst().assertIsOff()
        rule.onNodeWithText("개인정보처리방침").performScrollTo().assertIsDisplayed().performClick()
        shot("05-settings")
        if (PrivacyPolicy.URL.isBlank()) {
            assertEquals("개인정보처리방침 페이지를 준비하고 있습니다.", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun privacyLink_opensGivenUrl() {
        PrivacyPolicy.open(context, "https://example.com/p.html")
        assertEquals("https://example.com/p.html", shadowOf(context as Application).nextStartedActivity.dataString)
    }

    private fun reminderHabit(id: Int, time: String) = HabitEntity(
        habitId = id, title = "물", question = "", frequencyType = "DAILY", frequencyValue = "",
        reminderTime = time, isReminderEnabled = true, themeColor = "#FFA726", habitType = "CHECK", unit = null
    )

    private fun alarms() = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms

    @Test
    fun reminder_withoutExactAlarmPermission_isAllowedWhileIdle() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        HabitReminderManager(context).scheduleReminder(reminderHabit(7, "09:00"))
        assertTrue(alarms().single().isAllowWhileIdle)
    }

    @Test
    fun scheduleMissing_restoresLostAlarm_butKeepsExistingOne() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val manager = HabitReminderManager(context)
        manager.scheduleReminder(reminderHabit(1, "09:00"))
        val before = alarms().single().triggerAtMs
        manager.scheduleMissing(
            listOf(
                reminderHabit(1, "21:30"),
                reminderHabit(2, "10:00"),
                reminderHabit(3, "10:00").copy(isReminderEnabled = false)
            )
        )
        assertEquals(2, alarms().size)
        assertTrue(alarms().any { it.triggerAtMs == before })
    }
}
