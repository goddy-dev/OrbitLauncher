package com.godwin.orbitlauncher.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.godwin.orbitlauncher.di.AppGraph

class SearchViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return SearchViewModel(
            installedAppsRepository = AppGraph.installedAppsRepository,
            recentSearchesRepository = AppGraph.recentSearchesRepository,
            contactsRepository = AppGraph.contactsRepository,
            fileSearchRepository = AppGraph.fileSearchRepository,
            usageRepository = AppGraph.usageRepository
        ) as T
    }
}
