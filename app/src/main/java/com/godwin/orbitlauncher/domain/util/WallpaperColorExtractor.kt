package com.godwin.orbitlauncher.domain.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/**
 * Reads the current system wallpaper and pulls a representative accent
 * color from it via the Palette library -- used to tint the Orbit
 * Wheel's edge lines so they match the wallpaper (per the person's
 * request), independent of whether our own wallpaper picker (a later
 * phase) has been built yet: this reads whatever wallpaper is already
 * set on the device today.
 */
object WallpaperColorExtractor {

    private val FALLBACK = Color(0xFFE53935)

    fun extractAccentColor(context: Context): Color {
        return try {
            val drawable = WallpaperManager.getInstance(context).drawable ?: return FALLBACK
            val bitmap = drawableToBitmap(drawable) ?: return FALLBACK
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            swatch?.let { Color(it.rgb) } ?: FALLBACK
        } catch (e: SecurityException) {
            FALLBACK
        } catch (e: Exception) {
            FALLBACK
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 200
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
