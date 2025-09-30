package com.glassous.openqwens.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2)
}

class ThemeManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    var currentThemeMode by mutableIntStateOf(getThemeMode())
        private set
    
    fun setThemeMode(mode: ThemeMode) {
        currentThemeMode = mode.value
        prefs.edit().putInt("theme_mode", mode.value).apply()
    }
    
    fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", ThemeMode.SYSTEM.value)
    }
    
    fun getThemeModeEnum(): ThemeMode {
        return when (currentThemeMode) {
            ThemeMode.LIGHT.value -> ThemeMode.LIGHT
            ThemeMode.DARK.value -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager(context) }
}