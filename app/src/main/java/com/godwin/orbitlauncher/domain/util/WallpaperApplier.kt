package com.godwin.orbitlauncher.domain.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

/**
 * Renders the person's chosen crop/zoom/pan of a source image into a
 * bitmap matching the device's screen size, then applies it via
 * WallpaperManager. Persistence across reboots is handled natively by
 * Android once the wallpaper is set this way -- no extra storage code
 * needed on our end.
 */
object WallpaperApplier {

    /**
     * @param scale zoom factor applied to the source image (1f = fit).
     * @param offsetXFraction / offsetYFraction pan offset as a fraction
     *   of the screen dimensions (0f = centered), matching whatever the
     *   crop UI's drag gesture produced.
     */
    fun apply(
        context: Context,
        sourceUri: Uri,
        screenWidth: Int,
        screenHeight: Int,
        scale: Float,
        offsetXFraction: Float,
        offsetYFraction: Float
    ): Boolean {
        return try {
            val sourceBitmap = loadBitmap(context, sourceUri) ?: return false
            val output = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            // Fit the source image to cover the screen at scale=1, then
            // apply the person's extra zoom/pan on top of that base fit.
            val baseScale = maxOf(
                screenWidth.toFloat() / sourceBitmap.width,
                screenHeight.toFloat() / sourceBitmap.height
            )
            val totalScale = baseScale * scale

            val scaledWidth = sourceBitmap.width * totalScale
            val scaledHeight = sourceBitmap.height * totalScale

            val matrix = Matrix().apply {
                postScale(totalScale, totalScale)
                val centeredX = (screenWidth - scaledWidth) / 2f
                val centeredY = (screenHeight - scaledHeight) / 2f
                postTranslate(
                    centeredX + offsetXFraction * screenWidth,
                    centeredY + offsetYFraction * screenHeight
                )
            }
            canvas.drawBitmap(sourceBitmap, matrix, null)

            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(output)
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}
