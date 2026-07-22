package com.godwin.orbitlauncher.ui.wallpaper

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Lets the person pinch to zoom and drag to reposition the chosen image
 * before it's applied as the wallpaper. Keeps state as scale + fractional
 * pan offset so WallpaperApplier can reproduce the exact same framing at
 * full screen resolution when rendering the final bitmap.
 */
@Composable
fun WallpaperCropScreen(
    imageUri: Uri,
    onCancel: () -> Unit,
    onConfirm: (scale: Float, offsetXFraction: Float, offsetYFraction: Float) -> Unit
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) } // px, screen-space
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(1f) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    val imageBitmap: ImageBitmap? = remember(imageUri) {
        try {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (imageBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        containerWidthPx = it.width.toFloat()
                        containerHeightPx = it.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Wallpaper preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }
        } else {
            Text(
                text = "Couldn't load that image",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = "Pinch to zoom \u00b7 drag to reposition",
            color = Color(0xCCFFFFFF),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val offsetXFraction = if (containerWidthPx > 0) offsetX / containerWidthPx else 0f
                    val offsetYFraction = if (containerHeightPx > 0) offsetY / containerHeightPx else 0f
                    onConfirm(scale, offsetXFraction, offsetYFraction)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Set wallpaper")
            }
        }
    }
}
