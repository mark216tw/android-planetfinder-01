package com.planetfinder.app.data

import android.content.Context

class LocationStore(context: Context) {
    private val preferences = context.getSharedPreferences("observer_location", Context.MODE_PRIVATE)

    fun load(): ObserverLocation {
        if (!preferences.contains("latitude")) return TaipeiLocation
        val source = runCatching {
            LocationSource.valueOf(preferences.getString("source", LocationSource.DEFAULT.name).orEmpty())
        }.getOrDefault(LocationSource.DEFAULT)
        return ObserverLocation(
            latitude = java.lang.Double.longBitsToDouble(preferences.getLong("latitude", 0L)),
            longitude = java.lang.Double.longBitsToDouble(preferences.getLong("longitude", 0L)),
            altitudeMeters = java.lang.Double.longBitsToDouble(preferences.getLong("altitude", 0L)),
            label = preferences.getString("label", "台北").orEmpty(),
            source = source,
        )
    }

    fun save(location: ObserverLocation) {
        preferences.edit()
            .putLong("latitude", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("longitude", java.lang.Double.doubleToRawLongBits(location.longitude))
            .putLong("altitude", java.lang.Double.doubleToRawLongBits(location.altitudeMeters))
            .putString("label", location.label)
            .putString("source", location.source.name)
            .apply()
    }
}
