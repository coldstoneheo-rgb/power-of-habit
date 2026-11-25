package com.example.powerofhabit.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import com.example.powerofhabit.data.local.HabitEntity
import com.example.powerofhabit.ui.main.MainScreenContent
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme
import java.time.LocalDate
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class CurrentDateTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reopenedNextDay_showsNewDate() {
        var now = LocalDateTime.of(2026, 11, 25, 22, 0)
        rule.setContent { Text(rememberCurrentDate { now }.toString()) }
        rule.onNodeWithText("2026-11-25").assertIsDisplayed()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED) // 홈 버튼
        now = LocalDateTime.of(2026, 11, 26, 7, 0)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED) // 다음 날 다시 열기
        rule.onNodeWithText("2026-11-26").assertIsDisplayed()
    }

    @Test
    fun openAcrossMidnight_showsNewDate() {
        var now = LocalDateTime.of(2026, 11, 25, 23, 59, 50)
        rule.setContent { Text(rememberCurrentDate { now }.toString()) }
        rule.onNodeWithText("2026-11-25").assertIsDisplayed()
        now = LocalDateTime.of(2026, 11, 26, 0, 0, 1)
        rule.mainClock.advanceTimeBy(12_000)
        rule.onNodeWithText("2026-11-26").assertIsDisplayed()
    }

    @Test
    fun home_reopenedNextDay_recordsOnNewDate() {
        var now = LocalDateTime.of(2026, 11, 25, 22, 0)
        val toggled = mutableListOf<LocalDate>()
        val habit = HabitEntity(
            habitId = 1, title = "물", question = "", frequencyType = "DAILY", frequencyValue = "",
            reminderTime = null, themeColor = "#FFA726", habitType = "CHECK", unit = null
        )
        rule.setContent {
            CompositionLocalProvider(LocalNow provides { now }) {
                PowerOfHabitTheme(darkTheme = true) {
                    MainScreenContent(
                        habits = listOf(habit), records = emptyMap(),
                        onNavigateToDetail = {}, onNavigateToAddHabit = {}, onNavigateToBadges = {},
                        onUpdateRecordStatus = { _, _, _ -> }, onInsertRecord = {}, onDeleteRecord = {},
                        onToggleCompletion = { _, date -> toggled += date },
                        isDarkMode = true, isDateDescending = true, onToggleDarkMode = {}, onToggleDateDescending = {}
                    )
                }
            }
        }
        rule.onAllNodesWithContentDescription("미완료").onFirst().performClick()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        now = LocalDateTime.of(2026, 11, 26, 7, 0)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("미완료").onFirst().performClick()
        assertEquals(listOf(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 26)), toggled)
    }

    @Test
    fun millisUntilNextDay_landsAfterMidnight() {
        assertEquals(11_000L, millisUntilNextDay(LocalDateTime.of(2026, 12, 31, 23, 59, 50)))
        assertEquals(86_401_000L, millisUntilNextDay(LocalDateTime.of(2027, 1, 1, 0, 0)))
    }
}
