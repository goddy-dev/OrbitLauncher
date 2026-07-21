package com.godwin.orbitlauncher.data.repository

import com.godwin.orbitlauncher.data.local.AppUsageDao
import com.godwin.orbitlauncher.data.local.AppUsageEntity
import kotlinx.coroutines.flow.Flow

class UsageRepository(private val appUsageDao: AppUsageDao) {

    fun observeMostUsed(): Flow<List<AppUsageEntity>> = appUsageDao.observeMostUsed()

    suspend fun recordLaunch(packageName: String) {
        val existing = appUsageDao.get(packageName)
        appUsageDao.upsert(
            AppUsageEntity(
                packageName = packageName,
                launchCount = (existing?.launchCount ?: 0) + 1,
                lastLaunchedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
