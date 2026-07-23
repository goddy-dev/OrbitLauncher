package com.godwin.nyumbanilauncher.model

/**
 * Represents a single installed application as shown in the app drawer.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String
) {
    /** Unique key used to reference this app from grid items / folders. */
    val key: String get() = "$packageName/$activityName"
}
