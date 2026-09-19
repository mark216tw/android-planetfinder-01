package com.planetfinder.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.planetfinder.app.data.DisplayMode

val Night = Color(0xFF070B1C)
val DeepBlue = Color(0xFF0D1531)
val Stardust = Color(0xFFE8E9FF)
val ElectricBlue = Color(0xFF6F8CFF)
val Aurora = Color(0xFF69E4CE)
val Solar = Color(0xFFFFC85C)
val Muted = Color(0xFFAAB1D0)
val DaySky = Color(0xFFF3F6FF)
val DaySurface = Color(0xFFFFFFFF)
val DayInk = Color(0xFF111936)
val DayMuted = Color(0xFF59627E)

private val NightColors = darkColorScheme(
    primary = ElectricBlue,
    secondary = Aurora,
    tertiary = Solar,
    background = Night,
    surface = DeepBlue,
    onPrimary = Color.White,
    onBackground = Stardust,
    onSurface = Stardust,
    onSurfaceVariant = Muted,
)

private val DayColors = lightColorScheme(
    primary = Color(0xFF405BC5),
    secondary = Color(0xFF087E70),
    tertiary = Color(0xFF8A5900),
    background = DaySky,
    surface = DaySurface,
    surfaceVariant = Color(0xFFE7ECFA),
    onPrimary = Color.White,
    onBackground = DayInk,
    onSurface = DayInk,
    onSurfaceVariant = DayMuted,
)

@Composable
fun PlanetFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) NightColors else DayColors, content = content)
}

fun shouldUseDarkTheme(displayMode: DisplayMode, systemDarkTheme: Boolean): Boolean = when (displayMode) {
    DisplayMode.SYSTEM -> systemDarkTheme
    DisplayMode.LIGHT -> false
    DisplayMode.DARK -> true
}
