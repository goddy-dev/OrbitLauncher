package com.godwin.orbitlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks how often and how recently an app is launched from the launcher.
 * Powers the "Adaptive wheel" premium feature from the spec (frequently
 * used apps surface first) without needing it wired up until that phase.
 */
@Entity(tableName = "app_usage_stats")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val launchCount: Int = 0,
    val lastLaunchedAtMillis: Long = 0L
)
