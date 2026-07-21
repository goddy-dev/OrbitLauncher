package com.godwin.orbitlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.godwin.orbitlauncher.di.AppGraph

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(
            pinnedAppDao = AppGraph.database.pinnedAppDao(),
            installedAppsRepository = AppGraph.installedAppsRepository,
            usageRepository = AppGraph.usageRepository
        ) as T
    }
}
