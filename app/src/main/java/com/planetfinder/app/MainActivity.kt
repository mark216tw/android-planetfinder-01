package com.planetfinder.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import com.planetfinder.app.data.LocationProvider
import com.planetfinder.app.data.ObserverLocation
import com.planetfinder.app.data.TaipeiLocation
import com.planetfinder.app.ui.PlanetFinderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = 0xFFF3F6FF.toInt(),
                darkScrim = 0xFF070B1C.toInt(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = 0xFFF3F6FF.toInt(),
                darkScrim = 0xFF070B1C.toInt(),
            ),
        )
        setContent {
            val provider = remember { LocationProvider(this) }
            var location by remember { mutableStateOf<ObserverLocation>(TaipeiLocation) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { provider.findLocation { location = it } }

            PlanetFinderApp(
                location = location,
                requestLocation = {
                    if (provider.hasPermission()) provider.findLocation { location = it }
                    else permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
            )
        }
    }
}
