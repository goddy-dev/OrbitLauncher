package com.godwin.orbitlauncher.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme

/**
 * Registered as the device HOME app.
 * Phase 2: real home screen (clock, search bar, dock, drawer button).
 * The drawer button currently only flips a boolean flag -- the actual
 * radial Orbit Wheel UI is built in Phase 3 and will replace this stub.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                ) {
                    var wheelOpen by remember { mutableStateOf(false) }

                    HomeScreen(
                        onOpenWheel = { wheelOpen = true }
                    )

                    // Phase 3 will replace this stub with the real Orbit
                    // Wheel overlay. For now, tapping the drawer button
                    // simply resets the flag (no-op) so nothing breaks.
                    if (wheelOpen) {
                        wheelOpen = false
                    }
                }
            }
        }
    }

    // Home screen should not be exitable via back press.
    override fun onBackPressed() {
        // Intentionally empty.
    }
}
