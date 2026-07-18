package com.godwin.orbitlauncher.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme
import com.godwin.orbitlauncher.ui.wheel.OrbitWheelOverlay

/**
 * Registered as the device HOME app.
 * Phase 3: the drawer button now opens the real Orbit Wheel -- a
 * right-edge radial app selector layered on top of the home screen.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                ) {
                    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
                    val uiState by homeViewModel.uiState.collectAsState()

                    var wheelOpen by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(
                            onOpenWheel = { wheelOpen = true },
                            viewModel = homeViewModel
                        )

                        OrbitWheelOverlay(
                            isOpen = wheelOpen,
                            apps = uiState.allApps,
                            onDismiss = { wheelOpen = false },
                            onAppSelected = { app -> homeViewModel.onLaunchApp(app) }
                        )
                    }
                }
            }
        }
    }

    // Home screen should not be exitable via back press, except to close
    // the wheel if it's open -- handled inside Compose via onDismiss, so
    // back press here remains a no-op for the home screen itself.
    override fun onBackPressed() {
        // Intentionally empty.
    }
}
