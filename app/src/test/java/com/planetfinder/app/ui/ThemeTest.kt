package com.planetfinder.app.ui

import com.planetfinder.app.data.DisplayMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test fun systemModeFollowsSystemTheme() {
        assertTrue(shouldUseDarkTheme(DisplayMode.SYSTEM, systemDarkTheme = true))
        assertFalse(shouldUseDarkTheme(DisplayMode.SYSTEM, systemDarkTheme = false))
    }

    @Test fun explicitModesOverrideSystemTheme() {
        assertFalse(shouldUseDarkTheme(DisplayMode.LIGHT, systemDarkTheme = true))
        assertTrue(shouldUseDarkTheme(DisplayMode.DARK, systemDarkTheme = false))
    }
}
