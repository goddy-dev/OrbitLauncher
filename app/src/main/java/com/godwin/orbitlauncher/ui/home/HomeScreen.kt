package com.godwin.orbitlauncher.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godwin.orbitlauncher.data.notifications.OrbitNotificationListenerService
import com.godwin.orbitlauncher.ui.home.components.AppPickerDialog
import com.godwin.orbitlauncher.ui.home.components.ClockAndDate
import com.godwin.orbitlauncher.ui.home.components.DockRow
import com.godwin.orbitlauncher.ui.home.components.DrawerButton
import com.godwin.orbitlauncher.ui.home.components.UniversalSearchBar
import com.godwin.orbitlauncher.ui.search.SearchViewModel
import com.godwin.orbitlauncher.ui.search.SearchViewModelFactory
import com.godwin.orbitlauncher.ui.search.components.SearchResultsPanel
import java.util.Locale

/**
 * Layout order matches the spec: clock/date up top, search bar below it,
 * empty center (wallpaper shows through -- nothing rendered there on
 * purpose), then drawer button, then the 4-app dock at the bottom.
 *
 * Phase 4 adds the full search system: apps, settings, contacts, files,
 * calculator, web fallback, recent searches, and voice input. Contacts
 * and file search need runtime permissions, requested once here via
 * LaunchedEffect; if denied, those sections simply stay empty rather
 * than blocking the rest of search.
 */
@Composable
fun HomeScreen(
    onOpenWheel: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory()),
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()
    val notifyingPackages by OrbitNotificationListenerService.activePackages.collectAsState()
    val context = LocalContext.current

    var pickerSlotIndex by remember { mutableIntStateOf(-1) }

    // Request contacts + media permissions once, silently, so search
    // results simply appear if granted and stay empty if not.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { /* no-op: repositories check grants themselves */ }
    )
    LaunchedEffect(Unit) {
        val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val needed = (arrayOf(Manifest.permission.READ_CONTACTS) + mediaPermissions).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Voice search: launches the system speech recognizer, feeds the
    // recognized text back into the search query on success.
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) {
            searchViewModel.onQueryChange(text)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        ClockAndDate(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        UniversalSearchBar(
            query = searchState.query,
            onQueryChange = { searchViewModel.onQueryChange(it) },
            onMicClick = {
                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Orbit Launcher")
                }
                voiceLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SearchResultsPanel(
            uiState = searchState,
            onAppTap = { app -> searchViewModel.onAppLaunched(app) },
            onSettingsTap = { entry -> searchViewModel.onSettingsTapped(context, entry) },
            onContactTap = { contact -> searchViewModel.onContactTapped(context, contact) },
            onFileTap = { file -> searchViewModel.onFileTapped(context, file) },
            onWebSearchTap = { query -> searchViewModel.onWebSearch(context, query) },
            onRecentTap = { query -> searchViewModel.onRecentSearchTapped(query) }
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
            notifyingPackages = notifyingPackages,
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
