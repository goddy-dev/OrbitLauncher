package com.godwin.orbitlauncher.ui.wheel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.godwin.orbitlauncher.data.repository.HapticStrength
import com.godwin.orbitlauncher.domain.model.AppInfo

/**
 * Renders the dimmed backdrop + sliding-in OrbitWheel when [isOpen] is
 * true. Wallpaper/home content behind it stays visible (per spec: dim,
 * not opaque). The edge toggle chevron is the only way to close it.
 *
 * Settings applied here (Phase 6): [wheelOnRight] flips which edge the
 * wheel attaches to (alignment, slide direction, chevron icon all
 * mirror together); [animationSpeedScale] scales the open/close spring
 * stiffness; [hapticStrength] gates the open/close haptic pulse.
 *
 * Full RenderEffect blur (spec's "background blurs slightly") requires
 * API 31+; a dim scrim is used here as a baseline that works on every
 * device down to minSdk 26.
 */
@Composable
fun OrbitWheelOverlay(
    isOpen: Boolean,
    apps: List<AppInfo>,
    favoritePackages: Set<String>,
    notifyingPackages: Set<String>,
    edgeGlowColor: Color,
    sizeScale: Float = 1f,
    animationSpeedScale: Float = 1f,
    hapticStrength: HapticStrength = HapticStrength.LIGHT,
    wheelOnRight: Boolean = true,
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (hapticStrength != HapticStrength.OFF) {
            haptics.performHapticFeedback(
                if (isOpen) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    val speedStiffness = (350f * animationSpeedScale.coerceIn(0.25f, 4f))

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.45f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = speedStiffness + 30f),
        label = "scrimAlpha"
    )
    val slideProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = speedStiffness),
        label = "wheelSlide"
    )

    if (scrimAlpha <= 0f && slideProgress <= 0f) return

    val edgeAlignment = if (wheelOnRight) Alignment.CenterEnd else Alignment.CenterStart
    val slideDirection = if (wheelOnRight) 1f else -1f

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // Wheel slides in from off-screen toward its resting position,
        // from whichever edge it's attached to.
        Box(
            modifier = Modifier
                .align(edgeAlignment)
                .graphicsLayer {
                    translationX = (1f - slideProgress) * 260f * slideDirection
                    alpha = slideProgress
                }
        ) {
            OrbitWheel(
                apps = apps,
                favoritePackages = favoritePackages,
                notifyingPackages = notifyingPackages,
                edgeGlowColor = edgeGlowColor,
                sizeScale = sizeScale,
                animationSpeedScale = animationSpeedScale,
                hapticStrength = hapticStrength,
                wheelOnRight = wheelOnRight,
                onAppSelected = { app ->
                    onAppSelected(app)
                    onDismiss()
                },
                onToggleFavorite = onToggleFavorite
            )
        }

        // Edge toggle: small chevron button sitting right on the screen
        // edge at the wheel's vertical middle. Appears together with the
        // wheel and closes it on tap -- the only way to retract it.
        Box(
            modifier = Modifier
                .align(edgeAlignment)
                .graphicsLayer {
                    translationX = (1f - slideProgress) * 260f * slideDirection
                    alpha = slideProgress
                }
        ) {
            Surface(
                modifier = Modifier
                    .padding(start = if (wheelOnRight) 0.dp else 4.dp, end = if (wheelOnRight) 4.dp else 0.dp)
                    .size(26.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                shape = CircleShape,
                color = Color(0xFF1A1A1A)
            ) {
                Icon(
                    imageVector = if (wheelOnRight) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                    contentDescription = "Close app wheel",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
