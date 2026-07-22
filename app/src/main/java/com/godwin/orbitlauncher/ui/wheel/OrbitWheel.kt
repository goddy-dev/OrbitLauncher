package com.godwin.orbitlauncher.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.godwin.orbitlauncher.data.repository.HapticStrength
import com.godwin.orbitlauncher.domain.model.AppInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val VISIBLE_COUNT = 16
private const val DRAG_SENSITIVITY = 55f // px of drag per one app step
private const val BASE_SNAP_DURATION_MS = 150

// Panel proportions, per reference: thickness ~15-20% of screen width at
// its widest, height ~75-85% of screen height, very large radius for a
// gentle bend (pivot sits far off-screen, not at the edge itself).
private const val HEIGHT_FRACTION_OF_SCREEN = 0.80f
private const val BASE_WIDTH_DP = 110

/**
 * Vertical crescent panel, attachable to either screen edge -- an
 * annular sector (slice of a ring), not a floating row of bubbles. Two
 * curved lines (fixed black) bound a lens-shaped band where icon cards
 * sit, fanned slightly along the arc. [edgeGlowColor] (usually pulled
 * from the wallpaper) is used for the favorite-app badge dot only.
 *
 * Settings applied here (Phase 6): [sizeScale] scales the panel's width
 * and line thickness, [animationSpeedScale] scales snap-animation
 * duration, [hapticStrength] gates/adjusts feedback intensity, and
 * [wheelOnRight] mirrors the whole geometry to the left edge when false.
 *
 * Interaction: drag vertically to rotate through [apps] (already ordered
 * favorites-first-then-by-usage by the caller). Release snaps to the
 * nearest app; a tap on the centered card launches it; a long-press
 * toggles it as a favorite. [notifyingPackages] draws a small dot on any
 * card belonging to an app with an active notification.
 */
@Composable
fun OrbitWheel(
    apps: List<AppInfo>,
    favoritePackages: Set<String>,
    notifyingPackages: Set<String>,
    edgeGlowColor: Color,
    sizeScale: Float = 1f,
    animationSpeedScale: Float = 1f,
    hapticStrength: HapticStrength = HapticStrength.LIGHT,
    wheelOnRight: Boolean = true,
    onAppSelected: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun feedback(type: HapticFeedbackType) {
        if (hapticStrength != HapticStrength.OFF) {
            haptics.performHapticFeedback(type)
        }
    }

    val offsetAnim = remember { Animatable(0f) }
    var lastTickIndex by remember { mutableIntStateOf(0) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxHeight(HEIGHT_FRACTION_OF_SCREEN)
            .width((BASE_WIDTH_DP * sizeScale).dp)
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
                                feedback(
                                    if (hapticStrength == HapticStrength.STRONG)
                                        HapticFeedbackType.LongPress
                                    else
                                        HapticFeedbackType.TextHandleMove
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val durationMs = (BASE_SNAP_DURATION_MS / animationSpeedScale.coerceIn(0.25f, 4f)).roundToInt()
                            offsetAnim.animateTo(
                                offsetAnim.value.roundToInt().toFloat(),
                                animationSpec = tween(durationMillis = durationMs)
                            )
                        }
                    }
                )
            }
            .pointerInput(apps) {
                detectTapGestures(
                    onTap = {
                        if (dragTotal < 12f) {
                            val idx = Math.floorMod(offsetAnim.value.roundToInt(), apps.size)
                            feedback(HapticFeedbackType.LongPress)
                            onAppSelected(apps[idx])
                        }
                    },
                    onLongPress = {
                        if (dragTotal < 12f) {
                            val idx = Math.floorMod(offsetAnim.value.roundToInt(), apps.size)
                            feedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(apps[idx])
                        }
                    }
                )
            }
    ) {
        // cxEdge sits on whichever screen edge the wheel is attached to;
        // edgeSign flips the "into screen" direction for the mirrored
        // left-edge case (see the algebra note in pointOnCurve).
        val cxEdge = if (wheelOnRight) size.width else 0f
        val edgeSign = if (wheelOnRight) 1f else -1f
        val cy = size.height / 2f
        val halfHeight = size.height / 2f

        // Two curves (per the approved mockup): both meet the screen
        // edge at the same top/bottom points, but bulge inward to
        // different depths -- sFar is the deeper/inner line, sNear the
        // shallower/outer line. The lens-shaped gap between them is
        // where the icon cards sit. sizeScale grows/shrinks both
        // together so the whole panel scales as one piece.
        val sFar = size.width * 0.82f * sizeScale
        val sNear = size.width * 0.55f * sizeScale

        fun curvePoints(s: Float): Triple<Float, Float, Float> {
            val radius = (s * s + halfHeight * halfHeight) / (2f * s)
            val thetaMax = asin((halfHeight / radius).coerceIn(-1f, 1f))
            return Triple(s, radius, thetaMax)
        }

        fun pointOnCurve(s: Float, radius: Float, theta: Float): Offset {
            // Right edge: x = cxEdge - (s - radius*(1-cosTheta))
            // Left edge (mirrored): x = cxEdge + (s - radius*(1-cosTheta))
            val x = cxEdge - edgeSign * (s - radius * (1f - cos(theta)))
            val y = cy + radius * sin(theta)
            return Offset(x, y)
        }

        val (_, radiusFar, thetaMaxFar) = curvePoints(sFar)
        val (_, radiusNear, thetaMaxNear) = curvePoints(sNear)

        fun buildCurvePath(s: Float, radius: Float, thetaMax: Float, steps: Int = 40): Path {
            val path = Path()
            for (i in 0..steps) {
                val t = -thetaMax + (2f * thetaMax * i / steps)
                val p = pointOnCurve(s, radius, t)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            return path
        }

        val farPath = buildCurvePath(sFar, radiusFar, thetaMaxFar)
        val nearPath = buildCurvePath(sNear, radiusNear, thetaMaxNear)

        val bandPath = Path().apply {
            addPath(farPath)
            for (i in 40 downTo 0) {
                val t = -thetaMaxNear + (2f * thetaMaxNear * i / 40)
                val p = pointOnCurve(sNear, radiusNear, t)
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(bandPath, color = Color(0x1AFFFFFF))

        val glowStroke = Stroke(width = 6.dp.toPx())
        val coreStroke = Stroke(width = 1.5.dp.toPx())
        val lineColor = Color.Black
        drawPath(farPath, color = lineColor.copy(alpha = 0.55f), style = glowStroke)
        drawPath(farPath, color = lineColor, style = coreStroke)
        drawPath(nearPath, color = lineColor.copy(alpha = 0.55f), style = glowStroke)
        drawPath(nearPath, color = lineColor, style = coreStroke)

        val sMid = (sFar + sNear) / 2f
        val (_, radius, thetaMax) = curvePoints(sMid)
        fun pointAt(theta: Float) = pointOnCurve(sMid, radius, theta)

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

            val centerIndex = (VISIBLE_COUNT - 1) / 2f
            val isCenter = posInArc in (centerIndex - 0.5f)..(centerIndex + 0.5f)
            val cardSize = (if (isCenter) 32f else 24f).dp.toPx() * sizeScale
            val tiltDeg = (posInArc - centerIndex) * 3f * edgeSign

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
            val iconSizePx = (cardSize * 0.62f).roundToInt().coerceAtLeast(1)
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
                    color = edgeGlowColor,
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
