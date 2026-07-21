package com.godwin.orbitlauncher.ui.home

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godwin.orbitlauncher.data.notifications.OrbitNotificationListenerService
import com.godwin.orbitlauncher.di.AppGraph
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme
import com.godwin.orbitlauncher.ui.wheel.OrbitWheelOverlay
import kotlinx.coroutines.launch

/**
 * Registered as the device HOME app.
 *
 * Premium features wired in here: real background blur behind the wheel
 * on Android 12+ (falls back to the dim scrim below that on older
 * devices), and one-handed mode (long-press any empty area of the home
 * screen to toggle -- shrinks and anchors the whole UI toward the
 * bottom for easier one-handed reach).
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
                    val notifyingPackages by OrbitNotificationListenerService.activePackages
                        .collectAsState()

                    var wheelOpen by remember { mutableStateOf(false) }
                    var oneHandedMode by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        AppGraph.settingsRepository.oneHandedModeFlow.collect {
                            oneHandedMode = it
                        }
                    }

                    val blurRadius = if (wheelOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        16.dp
                    } else {
                        0.dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        scope.launch {
                                            AppGraph.settingsRepository.setOneHandedMode(!oneHandedMode)
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(blurRadius)
                                .let { base ->
                                    if (oneHandedMode) {
                                        base.graphicsLayer {
                                            scaleX = 0.82f
                                            scaleY = 0.82f
                                            transformOrigin = TransformOrigin(0.5f, 1f)
                                        }
                                    } else {
                                        base
                                    }
                                },
                            contentAlignment = if (oneHandedMode) Alignment.BottomCenter else Alignment.TopStart
                        ) {
                            HomeScreen(
                                onOpenWheel = { wheelOpen = true },
                                viewModel = homeViewModel
                            )
                        }

                        OrbitWheelOverlay(
                            isOpen = wheelOpen,
                            apps = uiState.wheelApps,
                            favoritePackages = uiState.favoritePackages,
                            notifyingPackages = notifyingPackages,
                            onDismiss = { wheelOpen = false },
                            onAppSelected = { app -> homeViewModel.onLaunchApp(app) },
                            onToggleFavorite = { app -> homeViewModel.onToggleFavorite(app) }
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
