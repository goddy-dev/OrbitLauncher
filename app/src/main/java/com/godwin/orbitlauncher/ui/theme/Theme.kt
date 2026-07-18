package com.godwin.orbitlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Phase 1: static dark theme matching the reference images (true black,
 * red accent). Material You dynamic color extraction from the wallpaper
 * is a Phase 5/6 feature per the spec and will replace this scheme's
 * accent color once the wallpaper system exists.
 */
private val OrbitDarkColorScheme = darkColorScheme(
    primary = OrbitAccentRed,
    background = OrbitBlack,
    surface = OrbitSurfaceDark,
    onBackground = OrbitOnDark,
    onSurface = OrbitOnDark
)

@Composable
fun OrbitLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OrbitDarkColorScheme,
        typography = OrbitTypography,
        content = content
    )
}
