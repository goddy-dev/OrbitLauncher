package com.godwin.orbitlauncher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PinnedAppEntity::class, AppUsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun pinnedAppDao(): PinnedAppDao
    abstract fun appUsageDao(): AppUsageDao

    companion object {
        @Volatile private var instance: OrbitDatabase? = null

        fun getInstance(context: Context): OrbitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OrbitDatabase::class.java,
                    "orbit_launcher.db"
                ).build().also { instance = it }
            }
    }
}
