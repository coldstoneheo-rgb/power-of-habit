package com.example.powerofhabit.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("key_theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isDateDescending = MutableStateFlow(prefs.getBoolean("key_date_descending", true))
    val isDateDescending: StateFlow<Boolean> = _isDateDescending.asStateFlow()

    companion object {
        /** 저장된 테마 설정을 실제 테마로. 고른 적이 없으면(SYSTEM) 시스템 테마를 따른다. */
        fun usesDarkTheme(themeMode: String, systemInDark: Boolean): Boolean = when (themeMode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> systemInDark
        }
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("key_theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) "DARK" else "LIGHT")
    }

    fun setDateDescending(enabled: Boolean) {
        prefs.edit().putBoolean("key_date_descending", enabled).apply()
        _isDateDescending.value = enabled
    }
}
