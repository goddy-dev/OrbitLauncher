package com.godwin.orbitlauncher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godwin.orbitlauncher.ui.home.components.AppPickerDialog
import com.godwin.orbitlauncher.ui.home.components.ClockAndDate
import com.godwin.orbitlauncher.ui.home.components.DockRow
import com.godwin.orbitlauncher.ui.home.components.DrawerButton
import com.godwin.orbitlauncher.ui.home.components.UniversalSearchBar

/**
 * Layout order matches the spec: clock/date up top, search bar below it,
 * empty center (wallpaper shows through -- nothing rendered there on
 * purpose), then drawer button, then the 4-app dock at the bottom.
 */
@Composable
fun HomeScreen(
    onOpenWheel: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var pickerSlotIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        ClockAndDate(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        UniversalSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // Empty center: intentionally blank so the wallpaper shows through,
        // per spec. Nothing should be rendered in this space.
        Spacer(modifier = Modifier.weight(1f))

        DrawerButton(
            onClick = onOpenWheel,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(12.dp))

        DockRow(
            dockApps = uiState.dockApps,
            onSlotTap = { index ->
                uiState.dockApps.getOrNull(index)?.let { viewModel.onLaunchApp(it) }
            },
            onSlotLongPress = { index -> pickerSlotIndex = index }
        )
    }

    if (pickerSlotIndex >= 0) {
        AppPickerDialog(
            apps = uiState.allApps,
            onDismiss = { pickerSlotIndex = -1 },
            onAppSelected = { app ->
                viewModel.onDockSlotAssigned(pickerSlotIndex, app)
                pickerSlotIndex = -1
            }
        )
    }
}
