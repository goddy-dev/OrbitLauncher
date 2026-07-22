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
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val VISIBLE_COUNT = 8
private const val DRAG_SENSITIVITY = 55f // px of drag per one app step

// Panel proportions, per reference: thickness ~15-20% of screen width at
// its widest, height ~75-85% of screen height, very large radius for a
// gentle bend (pivot sits far off-screen, not at the edge itself).
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
    edgeGlowColor: Color,
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
        val cxEdge = size.width // screen edge, both curves meet here at top/bottom
        val cy = size.height / 2f
        val halfHeight = size.height / 2f

        // Two curves (per the approved mockup): both meet the screen
        // edge at the same top/bottom points, but bulge inward to
        // different depths -- sFar is the deeper/inner line, sNear the
        // shallower/outer line. The lens-shaped gap between them is
        // where the icon cards sit. Both share the same sagitta
        // derivation as before, just at two different thicknesses.
        val sFar = size.width * 0.82f
        val sNear = size.width * 0.55f

        fun curvePoints(s: Float): Triple<Float, Float, Float> {
            val radius = (s * s + halfHeight * halfHeight) / (2f * s)
            val thetaMax = asin((halfHeight / radius).coerceIn(-1f, 1f))
            return Triple(s, radius, thetaMax)
        }

        fun pointOnCurve(s: Float, radius: Float, theta: Float): Offset {
            val x = cxEdge - s + radius * (1f - cos(theta))
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

        // Filled lens/band between the two curves.
        val bandPath = Path().apply {
            addPath(farPath)
            // Walk back along the near curve to close the shape into a
            // lens/band region rather than two disconnected lines.
            for (i in 40 downTo 0) {
                val t = -thetaMaxNear + (2f * thetaMaxNear * i / 40)
                val p = pointOnCurve(sNear, radiusNear, t)
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(bandPath, color = Color(0x1AFFFFFF))

        // Both lines traced with a soft glow (wide, faint) plus a crisp
        // core line, tinted with the wallpaper's own accent color.
        val glowStroke = Stroke(width = 6.dp.toPx())
        val coreStroke = Stroke(width = 1.5.dp.toPx())
        drawPath(farPath, color = edgeGlowColor.copy(alpha = 0.35f), style = glowStroke)
        drawPath(farPath, color = edgeGlowColor, style = coreStroke)
        drawPath(nearPath, color = edgeGlowColor.copy(alpha = 0.35f), style = glowStroke)
        drawPath(nearPath, color = edgeGlowColor, style = coreStroke)

        // Icons sit at the midpoint depth between the two lines.
        val sMid = (sFar + sNear) / 2f
        val (_, radius, thetaMax) = curvePoints(sMid)
        fun pointAt(theta: Float) = pointOnCurve(sMid, radius, theta)

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

            val centerIndex = (VISIBLE_COUNT - 1) / 2f
            val isCenter = posInArc in (centerIndex - 0.5f)..(centerIndex + 0.5f)
            val cardSize = (if (isCenter) 44f else 34f).dp.toPx()
            // Fan tilt: cards angle slightly away from center, like a
            // shallow carousel, rather than sitting perfectly upright.
            val tiltDeg = (posInArc - centerIndex) * 6f

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
