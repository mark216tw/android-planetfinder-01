package com.planetfinder.app.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun load(): Set<String> = preferences.getStringSet("body_ids", emptySet())?.toSet().orEmpty()

    fun save(ids: Set<String>) {
        preferences.edit().putStringSet("body_ids", ids).apply()
    }
}
