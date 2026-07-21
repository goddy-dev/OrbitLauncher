package com.godwin.orbitlauncher.ui.wheel

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.godwin.orbitlauncher.domain.model.AppInfo
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val VISIBLE_COUNT = 5
private const val DRAG_SENSITIVITY = 55f // px of drag per one app step

// Panel proportions, per reference: thickness ~15-20% of screen width at
// its widest, height ~75-85% of screen height, very large radius for a
// gentle bend (pivot sits far off-screen, not at the edge itself).
private const val THICKNESS_FRACTION_OF_WIDTH = 0.82f // of the container's own width
private const val HEIGHT_FRACTION_OF_SCREEN = 0.80f

/**
 * Right-edge vertical crescent panel -- an annular sector (slice of a
 * ring) clipped to the screen edge, not a floating row of bubbles. The
 * outer edge is flush with the screen edge; the inner edge is a smooth,
 * large-radius convex curve bowing toward the screen center, traced with
 * a glowing outline. Icons sit in slightly tilted card slices that fan
 * along the arc.
 *
 * Interaction unchanged: drag vertically to rotate through [apps]
 * (already ordered favorites-first-then-by-usage by the caller -- the
 * "adaptive wheel" + "Favorite Ring" premium features). Release snaps to
 * the nearest app; a tap on the centered card launches it; a long-press
 * toggles it as a favorite. [notifyingPackages] draws a small dot on any
 * card belonging to an app with an active notification.
 */
@Composable
fun OrbitWheel(
    apps: List<AppInfo>,
    favoritePackages: Set<String>,
    notifyingPackages: Set<String>,
    onAppSelected: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
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
            .fillMaxHeight(HEIGHT_FRACTION_OF_SCREEN)
            .width(110.dp)
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
                        if (dragTotal < 12f) {
                            val idx = Math.floorMod(offsetAnim.value.roundToInt(), apps.size)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppSelected(apps[idx])
                        }
                    },
                    onLongPress = {
                        if (dragTotal < 12f) {
                            val idx = Math.floorMod(offsetAnim.value.roundToInt(), apps.size)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(apps[idx])
                        }
                    }
                )
            }
    ) {
        val cxEdge = size.width // outer edge, flush with the screen edge
        val cy = size.height / 2f
        val halfHeight = size.height / 2f

        // Sagitta geometry: s = max inward bulge, h/2 = half the visible
        // height, R = the (large) radius that makes both true at once.
        // Derivation: a circle whose center sits (R - s) beyond the edge
        // intersects the edge line at top/bottom and bulges inward by s
        // at the middle -- see conversation notes for the full algebra.
        val s = size.width * THICKNESS_FRACTION_OF_WIDTH
        val radius = (s * s + halfHeight * halfHeight) / (2f * s)
        val thetaMax = asin((halfHeight / radius).coerceIn(-1f, 1f))

        val circleCenterX = cxEdge + (radius - s)

        fun pointAt(theta: Float): Offset {
            val x = cxEdge - s + radius * (1f - cos(theta))
            val y = cy + radius * sin(theta)
            return Offset(x, y)
        }

        // --- Filled crescent panel with glowing inner-edge border ---
        val topPoint = pointAt(-thetaMax)
        val bottomPoint = pointAt(thetaMax)
        val thetaMaxDeg = thetaMax * 180f / PI.toFloat()

        val panelPath = Path().apply {
            moveTo(topPoint.x, topPoint.y)
            arcTo(
                rect = Rect(
                    circleCenterX - radius, cy - radius,
                    circleCenterX + radius, cy + radius
                ),
                startAngleDegrees = 180f + thetaMaxDeg,
                sweepAngleDegrees = -2f * thetaMaxDeg,
                forceMoveTo = false
            )
            lineTo(bottomPoint.x, bottomPoint.y)
            lineTo(cxEdge, bottomPoint.y)
            lineTo(cxEdge, topPoint.y)
            close()
        }

        drawPath(panelPath, color = Color(0x1AFFFFFF))

        // Inner curved edge only (not the straight outer edge) -- traced
        // with a soft glow (wide, faint stroke) plus a crisp core line.
        val innerEdgePath = Path().apply {
            moveTo(topPoint.x, topPoint.y)
            arcTo(
                rect = Rect(
                    circleCenterX - radius, cy - radius,
                    circleCenterX + radius, cy + radius
                ),
                startAngleDegrees = 180f + thetaMaxDeg,
                sweepAngleDegrees = -2f * thetaMaxDeg,
                forceMoveTo = false
            )
        }
        drawPath(innerEdgePath, color = Color(0x40E53935), style = Stroke(width = 8.dp.toPx()))
        drawPath(innerEdgePath, color = Color(0xFFE53935), style = Stroke(width = 1.5.dp.toPx()))

        // --- Icon cards along the curve ---
        val roundedOffset = offsetAnim.value.roundToInt()
        val frac = offsetAnim.value - roundedOffset

        for (k in -1..VISIBLE_COUNT) {
            val idx = Math.floorMod(roundedOffset + k, apps.size)
            val posInArc = k - frac
            if (posInArc < -0.5f || posInArc > VISIBLE_COUNT - 0.5f) continue

            val theta = -thetaMax + posInArc * (2f * thetaMax / (VISIBLE_COUNT - 1))
            val point = pointAt(theta)
            val x = point.x
            val y = point.y

            val isCenter = posInArc in 1.5f..2.5f
            val cardSize = (if (isCenter) 52f else 42f).dp.toPx()
            // Fan tilt: cards angle slightly away from center, like a
            // shallow carousel, rather than sitting perfectly upright.
            val tiltDeg = (posInArc - 2f) * 9f

            if (isCenter) {
                drawCircle(
                    color = Color(0x40FFFFFF),
                    radius = cardSize * 0.75f,
                    center = Offset(x, y)
                )
            }

            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(tiltDeg, x, y)
                val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isCenter) 0x66FFFFFF.toInt() else 0x33FFFFFF.toInt()
                }
                val half = cardSize / 2f
                drawRoundRect(
                    x - half, y - half, x + half, y + half,
                    10.dp.toPx(), 10.dp.toPx(),
                    cardPaint
                )
                restore()
            }

            val app = apps[idx]
            val iconSizePx = (cardSize * 0.62f).roundToInt()
            val bitmap = run {
                val b = android.graphics.Bitmap.createBitmap(
                    iconSizePx, iconSizePx, android.graphics.Bitmap.Config.ARGB_8888
                )
                val c = android.graphics.Canvas(b)
                app.icon.setBounds(0, 0, iconSizePx, iconSizePx)
                app.icon.draw(c)
                b
            }
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(tiltDeg, x, y)
                drawBitmap(bitmap, x - iconSizePx / 2f, y - iconSizePx / 2f, null)
                restore()
            }

            val badgeOffset = cardSize * 0.42f
            if (app.packageName in favoritePackages) {
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 4.dp.toPx(),
                    center = Offset(x + badgeOffset, y + badgeOffset)
                )
            }
            if (app.packageName in notifyingPackages) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(x + badgeOffset, y - badgeOffset)
                )
            }
        }
    }
}
