package com.godwin.orbitlauncher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of an app pinned to a slot (dock, or the Favorite Ring
 * on the wheel described in the spec). `slotType` distinguishes which UI
 * surface the pin belongs to so Phase 3's Favorite Ring and the dock can
 * share one table.
 */
@Entity(tableName = "pinned_apps")
data class PinnedAppEntity(
    @PrimaryKey val slotKey: String, // e.g. "dock_0", "ring_3"
    val packageName: String,
    val activityClassName: String
)
