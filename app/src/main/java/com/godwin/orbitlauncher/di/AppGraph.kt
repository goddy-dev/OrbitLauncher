package com.godwin.orbitlauncher.di

import android.content.Context
import com.godwin.orbitlauncher.data.local.OrbitDatabase
import com.godwin.orbitlauncher.data.repository.ContactsRepository
import com.godwin.orbitlauncher.data.repository.FileSearchRepository
import com.godwin.orbitlauncher.data.repository.InstalledAppsRepository
import com.godwin.orbitlauncher.data.repository.InstalledAppsRepositoryImpl
import com.godwin.orbitlauncher.data.repository.RecentSearchesRepository
import com.godwin.orbitlauncher.data.repository.SettingsRepository

/**
 * Minimal manual DI graph. Kept intentionally simple (no Hilt/Koin) for
 * Phase 1 so the project builds fast and stays easy to reason about.
 * Can be swapped for Hilt later without touching call sites much, since
 * everything is accessed through this single object.
 */
object AppGraph {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: OrbitDatabase by lazy { OrbitDatabase.getInstance(appContext) }

    val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepositoryImpl(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val recentSearchesRepository: RecentSearchesRepository by lazy {
        RecentSearchesRepository(appContext)
    }

    val contactsRepository: ContactsRepository by lazy {
        ContactsRepository(appContext)
    }

    val fileSearchRepository: FileSearchRepository by lazy {
        FileSearchRepository(appContext)
    }
}
