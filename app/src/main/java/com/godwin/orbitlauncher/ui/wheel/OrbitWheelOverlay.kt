package com.godwin.orbitlauncher.ui.wheel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.godwin.orbitlauncher.domain.model.AppInfo

/**
 * Renders the dimmed backdrop + sliding-in OrbitWheel when [isOpen] is
 * true. Wallpaper/home content behind it stays visible (per spec: dim,
 * not opaque). Tapping the backdrop area outside the wheel closes it.
 *
 * Full RenderEffect blur (spec's "background blurs slightly") requires
 * API 31+; a dim scrim is used here as a baseline that works on every
 * device down to minSdk 26. A blur upgrade path is straightforward to
 * add later behind an SDK version check without changing this API.
 */
@Composable
fun OrbitWheelOverlay(
    isOpen: Boolean,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        haptics.performHapticFeedback(
            if (isOpen) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.45f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "scrimAlpha"
    )
    val slideProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 350f), // ~250-300ms feel
        label = "wheelSlide"
    )

    if (scrimAlpha <= 0f && slideProgress <= 0f) return

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed backdrop, tap outside the wheel to close.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Wheel slides in from off-screen-right toward its resting position.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = (1f - slideProgress) * 260f
                    alpha = slideProgress
                }
        ) {
            OrbitWheel(
                apps = apps,
                onAppSelected = { app ->
                    onAppSelected(app)
                    onDismiss()
                }
            )
        }
    }
}
