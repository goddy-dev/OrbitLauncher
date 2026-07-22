package com.godwin.orbitlauncher.ui.home

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godwin.orbitlauncher.data.notifications.OrbitNotificationListenerService
import com.godwin.orbitlauncher.di.AppGraph
import com.godwin.orbitlauncher.domain.util.WallpaperApplier
import com.godwin.orbitlauncher.domain.util.WallpaperColorExtractor
import com.godwin.orbitlauncher.ui.home.components.QuickSettingsMenu
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme
import com.godwin.orbitlauncher.ui.wallpaper.WallpaperCropScreen
import com.godwin.orbitlauncher.ui.wheel.OrbitWheelOverlay
import kotlinx.coroutines.launch

/**
 * Registered as the device HOME app.
 *
 * Premium features wired in here: real background blur behind the wheel
 * on Android 12+ (falls back to the dim scrim below that on older
 * devices), one-handed mode, Material You dynamic color, and the
 * wallpaper system (Phase 5) -- long-press any empty area of the home
 * screen opens a small menu with "Change wallpaper" plus the one-handed
 * and Material You toggles.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            var oneHandedMode by remember { mutableStateOf(false) }
            var materialYouEnabled by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                AppGraph.settingsRepository.oneHandedModeFlow.collect { oneHandedMode = it }
            }
            LaunchedEffect(Unit) {
                AppGraph.settingsRepository.materialYouFlow.collect { materialYouEnabled = it }
            }

            OrbitLauncherTheme(materialYouEnabled = materialYouEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                ) {
                    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
                    val uiState by homeViewModel.uiState.collectAsState()
                    val notifyingPackages by OrbitNotificationListenerService.activePackages
                        .collectAsState()

                    // Recomputed whenever wallpaperRefreshKey changes, i.e.
                    // right after a new wallpaper is successfully applied.
                    var wallpaperRefreshKey by remember { mutableIntStateOf(0) }
                    val wallpaperAccentColor = remember(wallpaperRefreshKey) {
                        WallpaperColorExtractor.extractAccentColor(context)
                    }

                    var wheelOpen by remember { mutableStateOf(false) }
                    var showQuickMenu by remember { mutableStateOf(false) }
                    var pickedWallpaperUri by remember { mutableStateOf<Uri?>(null) }

                    val pickImageLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri -> if (uri != null) pickedWallpaperUri = uri }

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
                                    onLongPress = { showQuickMenu = true },
                                    onTap = { showQuickMenu = false }
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
                            edgeGlowColor = wallpaperAccentColor,
                            onDismiss = { wheelOpen = false },
                            onAppSelected = { app -> homeViewModel.onLaunchApp(app) },
                            onToggleFavorite = { app -> homeViewModel.onToggleFavorite(app) }
                        )

                        if (showQuickMenu) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QuickSettingsMenu(
                                    oneHandedMode = oneHandedMode,
                                    materialYouEnabled = materialYouEnabled,
                                    onChangeWallpaper = {
                                        showQuickMenu = false
                                        pickImageLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onToggleOneHanded = {
                                        scope.launch {
                                            AppGraph.settingsRepository.setOneHandedMode(!oneHandedMode)
                                        }
                                    },
                                    onToggleMaterialYou = {
                                        scope.launch {
                                            AppGraph.settingsRepository.setMaterialYou(!materialYouEnabled)
                                        }
                                    }
                                )
                            }
                        }

                        val wallpaperUri = pickedWallpaperUri
                        if (wallpaperUri != null) {
                            WallpaperCropScreen(
                                imageUri = wallpaperUri,
                                onCancel = { pickedWallpaperUri = null },
                                onConfirm = { scale, offsetXFraction, offsetYFraction ->
                                    val metrics = context.resources.displayMetrics
                                    scope.launch {
                                        val success = WallpaperApplier.apply(
                                            context = context,
                                            sourceUri = wallpaperUri,
                                            screenWidth = metrics.widthPixels,
                                            screenHeight = metrics.heightPixels,
                                            scale = scale,
                                            offsetXFraction = offsetXFraction,
                                            offsetYFraction = offsetYFraction
                                        )
                                        if (success) {
                                            wallpaperRefreshKey++
                                        }
                                        pickedWallpaperUri = null
                                    }
                                }
                            )
                        }
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
