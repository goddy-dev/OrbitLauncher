package com.godwin.orbitlauncher.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godwin.orbitlauncher.data.repository.ContactResult
import com.godwin.orbitlauncher.data.repository.ContactsRepository
import com.godwin.orbitlauncher.data.repository.FileResult
import com.godwin.orbitlauncher.data.repository.FileSearchRepository
import com.godwin.orbitlauncher.data.repository.InstalledAppsRepository
import com.godwin.orbitlauncher.data.repository.RecentSearchesRepository
import com.godwin.orbitlauncher.data.repository.UsageRepository
import com.godwin.orbitlauncher.domain.model.AppInfo
import com.godwin.orbitlauncher.domain.model.SettingsCatalog
import com.godwin.orbitlauncher.domain.model.SettingsEntry
import com.godwin.orbitlauncher.domain.util.SimpleCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val matchingApps: List<AppInfo> = emptyList(),
    val matchingSettings: List<SettingsEntry> = emptyList(),
    val matchingContacts: List<ContactResult> = emptyList(),
    val matchingFiles: List<FileResult> = emptyList(),
    val calculatorResult: String? = null,
    val recentSearches: List<String> = emptyList()
)

/**
 * Universal search covering apps, settings, contacts, files, a
 * calculator, web fallback, recent searches, and voice input (voice
 * feeds recognized text back through onQueryChange from the UI layer,
 * since the SpeechRecognizer intent itself is launched from Compose).
 *
 * Contacts and file results depend on runtime permissions the UI is
 * responsible for requesting; the repositories behind them simply
 * return empty lists if permission hasn't been granted, so this class
 * doesn't need to track permission state itself.
 */
class SearchViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val recentSearchesRepository: RecentSearchesRepository,
    private val contactsRepository: ContactsRepository,
    private val fileSearchRepository: FileSearchRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val allApps: List<AppInfo> by lazy { installedAppsRepository.getLaunchableApps() }

    /** package -> rank (0 = most used), refreshed from UsageRepository.
     * Powers Smart Search: apps you actually use more often rank higher
     * in results than a plain alphabetical/contains match would give. */
    private var usageRank: Map<String, Int> = emptyMap()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recentSearchesRepository.recentSearchesFlow.collectLatest { recent ->
                _uiState.value = _uiState.value.copy(recentSearches = recent)
            }
        }
        viewModelScope.launch {
            usageRepository.observeMostUsed().collectLatest { entities ->
                usageRank = entities.withIndex().associate { (index, entity) -> entity.packageName to index }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                query = newQuery,
                matchingApps = emptyList(),
                matchingSettings = emptyList(),
                matchingContacts = emptyList(),
                matchingFiles = emptyList(),
                calculatorResult = null
            )
            return
        }

        val appMatches = allApps.filter { it.label.contains(newQuery, ignoreCase = true) }
        val settingsMatches = SettingsCatalog.search(newQuery)
        val contactMatches = contactsRepository.search(newQuery)
        val fileMatches = fileSearchRepository.search(newQuery)
        val calcResult = SimpleCalculator.evaluateOrNull(newQuery)?.let { formatResult(it) }

        _uiState.value = _uiState.value.copy(
            query = newQuery,
            matchingApps = appMatches,
            matchingSettings = settingsMatches,
            matchingContacts = contactMatches,
            matchingFiles = fileMatches,
            calculatorResult = calcResult
        )
    }

    fun onAppLaunched(app: AppInfo) {
        installedAppsRepository.launch(app)
        recordSearch(app.label)
        viewModelScope.launch {
            usageRepository.recordLaunch(app.packageName)
        }
    }

    fun onSettingsTapped(context: Context, entry: SettingsEntry) {
        recordSearch(entry.label)
        context.startActivity(Intent(entry.intentAction))
    }

    fun onContactTapped(context: Context, contact: ContactResult) {
        recordSearch(contact.displayName)
        val lookupUri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
        context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
    }

    fun onFileTapped(context: Context, file: FileResult) {
        recordSearch(file.displayName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun onWebSearch(context: Context, query: String) {
        if (query.isBlank()) return
        recordSearch(query)
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
        }
        val resolvable = intent.resolveActivity(context.packageManager) != null
        if (resolvable) {
            context.startActivity(intent)
        } else {
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            )
            context.startActivity(fallback)
        }
    }

    fun onRecentSearchTapped(query: String) {
        onQueryChange(query)
    }

    private fun recordSearch(query: String) {
        viewModelScope.launch {
            recentSearchesRepository.addSearch(query)
        }
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.4f".format(value).trimEnd('0').trimEnd('.')
        }
    }
}
