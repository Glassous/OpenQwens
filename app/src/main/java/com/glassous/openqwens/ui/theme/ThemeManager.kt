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

class ThemeManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    var currentThemeMode by mutableIntStateOf(getThemeMode())
        private set
    
    fun setThemeMode(mode: ThemeMode) {
        currentThemeMode = mode.value
        prefs.edit().putInt("theme_mode", mode.value).apply()
        // 通知所有实例更新状态
        _instance?.let { instance ->
            if (instance != this) {
                instance.currentThemeMode = mode.value
            }
        }
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
    
    companion object {
        @Volatile
        private var _instance: ThemeManager? = null
        
        fun getInstance(context: Context): ThemeManager {
            return _instance ?: synchronized(this) {
                _instance ?: ThemeManager(context.applicationContext).also { 
                    _instance = it 
                }
            }
        }
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager.getInstance(context) }
}