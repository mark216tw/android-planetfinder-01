package com.planetfinder.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper

val TaipeiLocation = ObserverLocation(25.0330, 121.5654, label = "台北", source = LocationSource.DEFAULT)

class LocationProvider(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun findLocation(onResult: (ObserverLocation) -> Unit) {
        if (!hasPermission()) {
            onResult(TaipeiLocation)
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(manager::isProviderEnabled)
        val last = providers.mapNotNull(manager::getLastKnownLocation).maxByOrNull(Location::getTime)
        if (last != null) onResult(last.toObserver())
        val provider = providers.firstOrNull() ?: run {
            onResult(last?.toObserver() ?: TaipeiLocation)
            return
        }
        val result: (Location?) -> Unit = { location ->
            onResult(location?.toObserver() ?: last?.toObserver() ?: TaipeiLocation)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mainHandler = Handler(Looper.getMainLooper())
            manager.getCurrentLocation(provider, null, { command -> mainHandler.post(command) }, result)
        } else {
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) = result(location)
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }, Looper.getMainLooper())
        }
    }

    private fun Location.toObserver() = ObserverLocation(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = if (hasAltitude()) altitude else 0.0,
        label = "目前位置",
        source = LocationSource.DEVICE,
    )
}
