package com.godwin.orbitlauncher.domain.model

import android.graphics.drawable.Drawable

/**
 * Core domain representation of an installed, launchable app.
 * Icon is loaded lazily by the repository layer, not persisted.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityClassName: String,
    val icon: Drawable
)
