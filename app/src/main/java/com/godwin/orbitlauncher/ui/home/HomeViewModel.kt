package com.godwin.orbitlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godwin.orbitlauncher.data.local.PinnedAppDao
import com.godwin.orbitlauncher.data.local.PinnedAppEntity
import com.godwin.orbitlauncher.data.repository.InstalledAppsRepository
import com.godwin.orbitlauncher.domain.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Slot keys for the 4-app dock, per the spec: Phone, Messages, Browser, Camera defaults. */
object DockSlots {
    const val COUNT = 4
    fun key(index: Int) = "dock_$index"
}

data class HomeUiState(
    val dockApps: List<AppInfo?> = List(DockSlots.COUNT) { null },
    val allApps: List<AppInfo> = emptyList()
)

class HomeViewModel(
    private val pinnedAppDao: PinnedAppDao,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val allApps = installedAppsRepository.getLaunchableApps()
        _uiState.value = _uiState.value.copy(allApps = allApps)

        viewModelScope.launch {
            pinnedAppDao.observeAll().collectLatest { pinnedList ->
                val pinnedBySlot = pinnedList.associateBy { it.slotKey }
                val dockApps = List(DockSlots.COUNT) { index ->
                    val entity = pinnedBySlot[DockSlots.key(index)]
                    entity?.let { allApps.firstOrNull { app -> app.packageName == it.packageName } }
                }
                _uiState.value = _uiState.value.copy(dockApps = dockApps)
            }
        }
    }

    fun onDockSlotAssigned(slotIndex: Int, app: AppInfo) {
        viewModelScope.launch {
            pinnedAppDao.upsert(
                PinnedAppEntity(
                    slotKey = DockSlots.key(slotIndex),
                    packageName = app.packageName,
                    activityClassName = app.activityClassName
                )
            )
        }
    }

    fun onLaunchApp(app: AppInfo) {
        installedAppsRepository.launch(app)
    }
}
