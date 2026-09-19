package com.planetfinder.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.planetfinder.app.data.LocationProvider
import com.planetfinder.app.ui.PlanetFinderApp
import com.planetfinder.app.ui.PlanetFinderViewModel

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
            val planetFinderViewModel: PlanetFinderViewModel = viewModel()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (permissions.values.any { it }) {
                    provider.findLocation(planetFinderViewModel::setLocation)
                }
            }

            PlanetFinderApp(
                viewModel = planetFinderViewModel,
                onDarkThemeChanged = ::applySystemBars,
                requestDeviceLocation = {
                    if (provider.hasPermission()) provider.findLocation(planetFinderViewModel::setLocation)
                    else permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
            )
        }
    }

    private fun applySystemBars(darkTheme: Boolean) {
        val background = if (darkTheme) 0xFF070B1C.toInt() else 0xFFF3F6FF.toInt()
        val style = if (darkTheme) {
            SystemBarStyle.dark(background)
        } else {
            SystemBarStyle.light(background, background)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
