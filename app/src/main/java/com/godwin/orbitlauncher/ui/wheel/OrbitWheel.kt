package com.godwin.orbitlauncher.ui.wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.godwin.orbitlauncher.domain.model.AppInfo
import kotlinx.coroutines.Animatable
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val VISIBLE_COUNT = 5
private const val ARC_SPAN_DEG = 140f
private const val START_ANGLE_DEG = -70f
private const val DRAG_SENSITIVITY = 55f // px of drag per one app step

/**
 * Right-edge semi-circular app wheel. Occupies a fixed-width strip;
 * caller controls overall visibility/animation (see OrbitWheelOverlay).
 *
 * Interaction: drag vertically to rotate through [apps], release snaps to
 * the nearest app, and a second tap on the centered/glowing app launches
 * it. This two-step confirm avoids accidental launches from a stray drag,
 * matching the behavior validated in the earlier prototype.
 */
@Composable
fun OrbitWheel(
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val offsetAnim = remember { Animatable(0f) }
    var lastTickIndex by remember { mutableIntStateOf(0) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(150.dp)
            .pointerInput(apps) {
                detectDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragTotal += abs(dragAmount.y)
                        scope.launch {
                            val newValue = offsetAnim.value + dragAmount.y / DRAG_SENSITIVITY
                            offsetAnim.snapTo(newValue)
                            val tickIndex = newValue.roundToInt()
                            if (tickIndex != lastTickIndex) {
                                lastTickIndex = tickIndex
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            offsetAnim.animateTo(offsetAnim.value.roundToInt().toFloat())
                        }
                    }
                )
            }
            .pointerInput(apps) {
                detectTapGestures(
                    onTap = {
                        // Only treat as a launch-tap if there was no significant drag.
                        if (dragTotal < 12f) {
                            val idx = Math.floorMod(offsetAnim.value.roundToInt(), apps.size)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppSelected(apps[idx])
                        }
                    }
                )
            }
    ) {
        val cx = size.width * 0.12f
        val cy = size.height / 2f
        val radius = size.width * 0.95f

        // Track arc
        drawArc(
            color = Color(0x33FFFFFF),
            startAngle = START_ANGLE_DEG,
            sweepAngle = ARC_SPAN_DEG,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx()),
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2)
        )

        val roundedOffset = offsetAnim.value.roundToInt()
        val frac = offsetAnim.value - roundedOffset

        for (k in -1..VISIBLE_COUNT) {
            val idx = Math.floorMod(roundedOffset + k, apps.size)
            val posInArc = k - frac
            if (posInArc < -0.5f || posInArc > VISIBLE_COUNT - 0.5f) continue

            val angleDeg = START_ANGLE_DEG + posInArc * (ARC_SPAN_DEG / (VISIBLE_COUNT - 1))
            val angleRad = angleDeg * PI.toFloat() / 180f
            val x = cx + radius * cos(angleRad)
            val y = cy + radius * sin(angleRad)

            val isCenter = posInArc in 1.5f..2.5f
            val bubbleRadius = (if (isCenter) 30f else 22f).dp.toPx()

            if (isCenter) {
                // Glow: soft outer circle behind the selected bubble.
                drawCircle(
                    color = Color(0x40FFFFFF),
                    radius = bubbleRadius * 1.4f,
                    center = Offset(x, y)
                )
            }
            drawCircle(
                color = if (isCenter) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                radius = bubbleRadius,
                center = Offset(x, y)
            )

            val app = apps[idx]
            val iconSizePx = (bubbleRadius * 1.15f).roundToInt()
            val bitmap = run {
                val b = android.graphics.Bitmap.createBitmap(
                    iconSizePx, iconSizePx, android.graphics.Bitmap.Config.ARGB_8888
                )
                val c = android.graphics.Canvas(b)
                app.icon.setBounds(0, 0, iconSizePx, iconSizePx)
                app.icon.draw(c)
                b
            }
            drawContext.canvas.nativeCanvas.drawBitmap(
                bitmap,
                x - iconSizePx / 2f,
                y - iconSizePx / 2f,
                null
            )
        }
    }
}
