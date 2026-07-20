package com.godwin.orbitlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentSearchesDataStore by preferencesDataStore(name = "orbit_recent_searches")

private const val MAX_RECENT = 6
private const val SEPARATOR = "\u0001" // unit separator, won't appear in normal queries

class RecentSearchesRepository(private val context: Context) {

    private object Keys {
        val RECENT = stringPreferencesKey("recent_queries")
    }

    val recentSearchesFlow: Flow<List<String>> =
        context.recentSearchesDataStore.data.map { prefs ->
            prefs[Keys.RECENT]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun addSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.recentSearchesDataStore.edit { prefs ->
            val existing = prefs[Keys.RECENT]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(trimmed) + existing.filter { it != trimmed }).take(MAX_RECENT)
            prefs[Keys.RECENT] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun clear() {
        context.recentSearchesDataStore.edit { prefs -> prefs[Keys.RECENT] = "" }
    }
}
