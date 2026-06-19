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

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("key_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isDateDescending = MutableStateFlow(prefs.getBoolean("key_date_descending", true))
    val isDateDescending: StateFlow<Boolean> = _isDateDescending.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("key_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setDateDescending(enabled: Boolean) {
        prefs.edit().putBoolean("key_date_descending", enabled).apply()
        _isDateDescending.value = enabled
    }
}
