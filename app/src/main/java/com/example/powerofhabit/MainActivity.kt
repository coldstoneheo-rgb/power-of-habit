package com.example.powerofhabit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.powerofhabit.widget.HabitWidgets
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.powerofhabit.data.local.SettingsManager
import com.example.powerofhabit.ui.theme.PowerOfHabitTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var settingsManager: SettingsManager

  /** 위젯 탭으로 열린 경우 바로 보여줄 습관. 소비되면 -1. */
  private var pendingHabitId by mutableIntStateOf(-1)

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    pendingHabitId = intent.getIntExtra(HabitWidgets.EXTRA_HABIT_ID, -1)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingHabitId = intent?.getIntExtra(HabitWidgets.EXTRA_HABIT_ID, -1) ?: -1

    enableEdgeToEdge()
    setContent {
      val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
      val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
      val useDarkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemInDark
      }

      PowerOfHabitTheme(darkTheme = useDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation(
            initialHabitId = pendingHabitId,
            onInitialHabitConsumed = { pendingHabitId = -1 }
          )
        }
      }
    }
  }
}
