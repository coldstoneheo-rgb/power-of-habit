package com.example.powerofhabit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.data.local.HabitRecordEntity
import com.example.powerofhabit.domain.RecordOutcomes
import com.example.powerofhabit.ui.theme.PremiumMatteColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * 지어낸 습관과 기록으로 실제 화면을 그려 스토어 스크린샷(1080×1920)을 build/outputs/store-shots에 남긴다.
 * 홈의 앱 이름이 바뀌면 다시 돌리면 된다. 이미지는 커밋하지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w360dp-h640dp-xxhdpi")
class StoreShotsTest {

    @get:Rule val rule = createEmptyComposeRule()

    private val app: PowerOfHabitApp get() = ApplicationProvider.getApplicationContext()

    private fun seed() = runBlocking {
        val today = LocalDate.now()
        val repo = app.repository
        data class Sample(val title: String, val question: String, val unit: String?, val target: Float?, val missEvery: Int)
        listOf(
            Sample("책 10쪽", "오늘 몇 쪽을 읽었나요?", "쪽", 10f, 9),
            Sample("물 1L", "물을 1L 마셨나요?", null, null, 11),
            Sample("스트레칭", "스트레칭을 했나요?", null, null, 6),
            Sample("30분 걷기", "몇 분 걸었나요?", "분", 30f, 7),
            Sample("일기 쓰기", "일기를 썼나요?", null, null, 5),
            Sample("영어 문장 3개", "문장 3개를 외웠나요?", null, null, 8),
            Sample("11시 전에 눕기", "11시 전에 누웠나요?", null, null, 4)
        ).forEachIndexed { i, s ->
            val habit = HabitEntity(
                title = s.title, question = s.question, frequencyType = "DAILY", frequencyValue = "",
                reminderTime = null, themeColor = PremiumMatteColors[(i * 5) % PremiumMatteColors.size].first,
                habitType = if (s.unit == null) "CHECK" else "VALUE", unit = s.unit, targetValue = s.target
            )
            val id = repo.insertHabit(habit).toInt()
            for (d in 0L..70L) {
                val miss = (d + i).toInt() % s.missEvery == s.missEvery - 1
                val value = s.target?.let { if (miss) it / 2 else it + (d % 3) }
                val status = if (value != null) RecordOutcomes.statusForValue(value, s.target, RecordOutcomes.TARGET_AT_LEAST)
                else if (miss) "FAILED" else "COMPLETED"
                repo.insertRecord(HabitRecordEntity(habitId = id, date = today.minusDays(d).toString(), status = status, inputValue = value))
            }
        }
    }

    private fun waitFor(text: String) =
        rule.waitUntil("«$text» 표시", 10_000) { rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForIcon(d: String) =
        rule.waitUntil("«$d» 아이콘 표시", 10_000) { rule.onAllNodes(hasContentDescription(d)).fetchSemanticsNodes().isNotEmpty() }

    private fun shot(scenario: ActivityScenario<MainActivity>, name: String) {
        rule.waitForIdle()
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            assertEquals(1080, view.width)
            assertEquals(1920, view.height)
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val dir = File("build/outputs/store-shots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun storeScreens() {
        app.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).edit().putString("key_theme_mode", "DARK").commit()
        seed()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("책 10쪽")
            shot(scenario, "1-home")
            rule.onAllNodesWithText("책 10쪽").onFirst().performClick()
            waitForIcon("수정")
            shot(scenario, "2-detail-score")
            rule.onNodeWithText("연간 빈도 매트릭스").performScrollTo()
            shot(scenario, "3-detail-history")
            rule.onNodeWithContentDescription("수정").performScrollTo().performClick() // 위쪽 막대도 함께 스크롤된다
            waitFor("습관 수정하기")
            waitFor("책 10쪽")
            shot(scenario, "4-edit")
        }
        assertEquals(7, runBlocking { app.repository.getAllHabits().first() }.size)
    }
}
