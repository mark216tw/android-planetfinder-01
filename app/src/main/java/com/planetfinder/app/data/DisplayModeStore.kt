package com.planetfinder.app.data

import android.content.Context

enum class DisplayMode(val displayName: String) {
    SYSTEM("系統"),
    LIGHT("淺色"),
    DARK("深色"),
}

class DisplayModeStore(context: Context) {
    private val preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)

    fun load(): DisplayMode = runCatching {
        DisplayMode.valueOf(preferences.getString("mode", DisplayMode.SYSTEM.name).orEmpty())
    }.getOrDefault(DisplayMode.SYSTEM)

    fun save(mode: DisplayMode) {
        preferences.edit().putString("mode", mode.name).apply()
    }
}
