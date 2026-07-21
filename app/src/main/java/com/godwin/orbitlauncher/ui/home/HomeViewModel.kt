package com.godwin.orbitlauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godwin.orbitlauncher.data.local.PinnedAppDao
import com.godwin.orbitlauncher.data.local.PinnedAppEntity
import com.godwin.orbitlauncher.data.repository.InstalledAppsRepository
import com.godwin.orbitlauncher.data.repository.UsageRepository
import com.godwin.orbitlauncher.domain.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Slot keys for the 4-app dock, per the spec: Phone, Messages, Browser, Camera defaults. */
object DockSlots {
    const val COUNT = 4
    fun key(index: Int) = "dock_$index"
}

/** Slot keys for the Favorite Ring (premium feature): 6-8 pinned favorites. */
object RingSlots {
    const val COUNT = 8
    fun key(index: Int) = "ring_$index"
}

data class HomeUiState(
    val dockApps: List<AppInfo?> = List(DockSlots.COUNT) { null },
    val allApps: List<AppInfo> = emptyList(),
    /** Apps ordered for the wheel: favorites first, then remaining apps
     * ranked by usage frequency (adaptive wheel), then alphabetically. */
    val wheelApps: List<AppInfo> = emptyList(),
    val favoritePackages: Set<String> = emptySet()
)

class HomeViewModel(
    private val pinnedAppDao: PinnedAppDao,
    private val installedAppsRepository: InstalledAppsRepository,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Kept up to date from the pinned-apps flow so onToggleFavorite can
     * find/free a ring slot without an extra one-off Room query. */
    private var latestPinnedList: List<PinnedAppEntity> = emptyList()

    init {
        val allApps = installedAppsRepository.getLaunchableApps()
        _uiState.value = _uiState.value.copy(allApps = allApps)

        viewModelScope.launch {
            combine(
                pinnedAppDao.observeAll(),
                usageRepository.observeMostUsed()
            ) { pinnedList, usageList -> pinnedList to usageList }
                .collectLatest { (pinnedList, usageList) ->
                    latestPinnedList = pinnedList
                    val pinnedBySlot = pinnedList.associateBy { it.slotKey }

                    val dockApps = List(DockSlots.COUNT) { index ->
                        val entity = pinnedBySlot[DockSlots.key(index)]
                        entity?.let { e -> allApps.firstOrNull { it.packageName == e.packageName } }
                    }

                    val favoriteEntities = (0 until RingSlots.COUNT)
                        .mapNotNull { pinnedBySlot[RingSlots.key(it)] }
                    val favoritePackages = favoriteEntities.map { it.packageName }.toSet()
                    val favoriteApps = favoriteEntities
                        .mapNotNull { e -> allApps.firstOrNull { it.packageName == e.packageName } }

                    val usageRank = usageList.associate { it.packageName to it.launchCount }
                    val remaining = allApps
                        .filter { it.packageName !in favoritePackages }
                        .sortedWith(
                            compareByDescending<AppInfo> { usageRank[it.packageName] ?: 0 }
                                .thenBy { it.label.lowercase() }
                        )

                    _uiState.value = _uiState.value.copy(
                        dockApps = dockApps,
                        wheelApps = favoriteApps + remaining,
                        favoritePackages = favoritePackages
                    )
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

    /** Toggles [app] in the Favorite Ring: removes it if already pinned,
     * otherwise adds it to the first free ring slot (no-op if the ring
     * of [RingSlots.COUNT] favorites is already full). */
    fun onToggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            val existingSlot = latestPinnedList.firstOrNull {
                it.slotKey.startsWith("ring_") && it.packageName == app.packageName
            }
            if (existingSlot != null) {
                pinnedAppDao.clearSlot(existingSlot.slotKey)
            } else {
                val occupiedRingKeys = latestPinnedList
                    .filter { it.slotKey.startsWith("ring_") }
                    .map { it.slotKey }
                    .toSet()
                val freeIndex = (0 until RingSlots.COUNT).firstOrNull {
                    RingSlots.key(it) !in occupiedRingKeys
                }
                if (freeIndex != null) {
                    pinnedAppDao.upsert(
                        PinnedAppEntity(
                            slotKey = RingSlots.key(freeIndex),
                            packageName = app.packageName,
                            activityClassName = app.activityClassName
                        )
                    )
                }
                // Ring full: silently ignore rather than bumping an
                // existing favorite, so the action stays predictable.
            }
        }
    }

    fun onLaunchApp(app: AppInfo) {
        installedAppsRepository.launch(app)
        viewModelScope.launch {
            usageRepository.recordLaunch(app.packageName)
        }
    }
}
