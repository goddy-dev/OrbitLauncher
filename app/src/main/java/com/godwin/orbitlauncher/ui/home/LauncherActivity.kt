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
import com.godwin.orbitlauncher.data.repository.HapticStrength
import com.godwin.orbitlauncher.di.AppGraph
import com.godwin.orbitlauncher.domain.util.WallpaperApplier
import com.godwin.orbitlauncher.domain.util.WallpaperColorExtractor
import com.godwin.orbitlauncher.ui.home.components.QuickSettingsMenu
import com.godwin.orbitlauncher.ui.settings.SettingsScreen
import com.godwin.orbitlauncher.ui.settings.SettingsUiValues
import com.godwin.orbitlauncher.ui.theme.OrbitLauncherTheme
import com.godwin.orbitlauncher.ui.wallpaper.WallpaperCropScreen
import com.godwin.orbitlauncher.ui.wheel.OrbitWheelOverlay
import kotlinx.coroutines.launch

/**
 * Registered as the device HOME app.
 *
 * Phase 6 (Settings and customization) wires every DataStore-backed
 * setting to something that actually changes on screen: AMOLED vs dark
 * gray background, dark/light mode, search bar and dock label
 * visibility, wheel size/animation speed/haptic strength/edge position,
 * plus the Phase 5 wallpaper picker and the one-handed-mode / Material
 * You toggles from earlier. Icon packs and custom fonts are flagged in
 * the Settings screen itself as not-yet-built rather than faked.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settings = AppGraph.settingsRepository
            val scope = rememberCoroutineScope()

            var oneHandedMode by remember { mutableStateOf(false) }
            var materialYouEnabled by remember { mutableStateOf(false) }
            var amoledMode by remember { mutableStateOf(true) }
            var darkMode by remember { mutableStateOf(true) }
            var wheelSizeScale by remember { mutableStateOf(1f) }
            var animationSpeedScale by remember { mutableStateOf(1f) }
            var searchBarVisible by remember { mutableStateOf(true) }
            var dockLabelsVisible by remember { mutableStateOf(true) }
            var hapticStrength by remember { mutableStateOf(HapticStrength.LIGHT) }
            var wheelOnRight by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) { settings.oneHandedModeFlow.collect { oneHandedMode = it } }
            LaunchedEffect(Unit) { settings.materialYouFlow.collect { materialYouEnabled = it } }
            LaunchedEffect(Unit) { settings.amoledModeFlow.collect { amoledMode = it } }
            LaunchedEffect(Unit) { settings.darkModeFlow.collect { darkMode = it } }
            LaunchedEffect(Unit) { settings.wheelSizeScaleFlow.collect { wheelSizeScale = it } }
            LaunchedEffect(Unit) { settings.animationSpeedScaleFlow.collect { animationSpeedScale = it } }
            LaunchedEffect(Unit) { settings.searchBarVisibleFlow.collect { searchBarVisible = it } }
            LaunchedEffect(Unit) { settings.dockLabelsVisibleFlow.collect { dockLabelsVisible = it } }
            LaunchedEffect(Unit) { settings.hapticStrengthFlow.collect { hapticStrength = it } }
            LaunchedEffect(Unit) { settings.wheelOnRightFlow.collect { wheelOnRight = it } }

            OrbitLauncherTheme(materialYouEnabled = materialYouEnabled) {
                // AMOLED = true black; otherwise a dark gray background,
                // both only meaningful while darkMode is on (a full light
                // scheme swap is a larger theming pass -- for now light
                // mode falls back to the same dark background rather than
                // an unfinished half-built light theme).
                val backgroundColor = if (!darkMode) {
                    Color(0xFFF5F5F5)
                } else if (amoledMode) {
                    Color.Black
                } else {
                    Color(0xFF121212)
                }

                Surface(
                    modifier = Modifier.fillMaxSize().background(backgroundColor)
                ) {
                    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
                    val uiState by homeViewModel.uiState.collectAsState()
                    val notifyingPackages by OrbitNotificationListenerService.activePackages
                        .collectAsState()

                    var wallpaperRefreshKey by remember { mutableIntStateOf(0) }
                    val wallpaperAccentColor = remember(wallpaperRefreshKey) {
                        WallpaperColorExtractor.extractAccentColor(context)
                    }

                    var wheelOpen by remember { mutableStateOf(false) }
                    var showQuickMenu by remember { mutableStateOf(false) }
                    var showSettingsScreen by remember { mutableStateOf(false) }
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
                                searchBarVisible = searchBarVisible,
                                dockLabelsVisible = dockLabelsVisible,
                                wheelOnRight = wheelOnRight,
                                viewModel = homeViewModel
                            )
                        }

                        OrbitWheelOverlay(
                            isOpen = wheelOpen,
                            apps = uiState.wheelApps,
                            favoritePackages = uiState.favoritePackages,
                            notifyingPackages = notifyingPackages,
                            edgeGlowColor = wallpaperAccentColor,
                            sizeScale = wheelSizeScale,
                            animationSpeedScale = animationSpeedScale,
                            hapticStrength = hapticStrength,
                            wheelOnRight = wheelOnRight,
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
                                        scope.launch { settings.setOneHandedMode(!oneHandedMode) }
                                    },
                                    onToggleMaterialYou = {
                                        scope.launch { settings.setMaterialYou(!materialYouEnabled) }
                                    },
                                    onOpenSettings = {
                                        showQuickMenu = false
                                        showSettingsScreen = true
                                    }
                                )
                            }
                        }

                        if (showSettingsScreen) {
                            SettingsScreen(
                                values = SettingsUiValues(
                                    amoledMode = amoledMode,
                                    darkMode = darkMode,
                                    wheelSizeScale = wheelSizeScale,
                                    animationSpeedScale = animationSpeedScale,
                                    searchBarVisible = searchBarVisible,
                                    dockLabelsVisible = dockLabelsVisible,
                                    hapticStrength = hapticStrength,
                                    wheelOnRight = wheelOnRight
                                ),
                                onBack = { showSettingsScreen = false },
                                onAmoledModeChange = { scope.launch { settings.setAmoledMode(it) } },
                                onDarkModeChange = { scope.launch { settings.setDarkMode(it) } },
                                onWheelSizeScaleChange = { scope.launch { settings.setWheelSizeScale(it) } },
                                onAnimationSpeedScaleChange = { scope.launch { settings.setAnimationSpeedScale(it) } },
                                onSearchBarVisibleChange = { scope.launch { settings.setSearchBarVisible(it) } },
                                onDockLabelsVisibleChange = { scope.launch { settings.setDockLabelsVisible(it) } },
                                onHapticStrengthChange = { scope.launch { settings.setHapticStrength(it) } },
                                onWheelOnRightChange = { scope.launch { settings.setWheelOnRight(it) } }
                            )
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
