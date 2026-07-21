package com.godwin.orbitlauncher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Static fallback dark theme matching the reference images (true black,
 * red accent) -- used on Android 11 and below, where Material You's
 * dynamic color extraction isn't available.
 */
private val OrbitStaticDarkColorScheme = darkColorScheme(
    primary = OrbitAccentRed,
    background = OrbitBlack,
    surface = OrbitSurfaceDark,
    onBackground = OrbitOnDark,
    onSurface = OrbitOnDark
)

/**
 * Material You: on Android 12+, colors are derived from the user's
 * current wallpaper via the system's own palette (dynamicDarkColorScheme),
 * rather than our static red accent. This works today even before our
 * own custom wallpaper picker (Phase 5) exists, since it reads whatever
 * wallpaper is already set on the device.
 */
/**
 * Material You: on Android 12+, when enabled in settings, colors are
 * derived from the user's current wallpaper via the system's own
 * palette (dynamicDarkColorScheme) instead of our static red accent.
 * Works today even before our own custom wallpaper picker (Phase 5)
 * exists, since it reads whatever wallpaper is already set on the
 * device. Falls back to the static scheme when disabled or unsupported.
 */
@Composable
fun OrbitLauncherTheme(
    materialYouEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (materialYouEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        OrbitStaticDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OrbitTypography,
        content = content
    )
}
