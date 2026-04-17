package com.example.myapplication.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global theme manager that persists dark mode preference.
 * Inject or access via singleton to control theme from any screen.
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun toggle(context: Context) {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, newValue).apply()
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        _isDarkMode.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}
