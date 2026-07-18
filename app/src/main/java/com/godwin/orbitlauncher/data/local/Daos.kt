package com.godwin.orbitlauncher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedAppDao {
    @Query("SELECT * FROM pinned_apps")
    fun observeAll(): Flow<List<PinnedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PinnedAppEntity)

    @Query("DELETE FROM pinned_apps WHERE slotKey = :slotKey")
    suspend fun clearSlot(slotKey: String)
}

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage_stats ORDER BY launchCount DESC")
    fun observeMostUsed(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppUsageEntity)
}
