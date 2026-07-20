package com.godwin.orbitlauncher.ui.search.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.godwin.orbitlauncher.data.repository.ContactResult
import com.godwin.orbitlauncher.data.repository.FileResult
import com.godwin.orbitlauncher.domain.model.AppInfo
import com.godwin.orbitlauncher.domain.model.SettingsEntry
import com.godwin.orbitlauncher.ui.search.SearchUiState

@Composable
fun SearchResultsPanel(
    uiState: SearchUiState,
    onAppTap: (AppInfo) -> Unit,
    onSettingsTap: (SettingsEntry) -> Unit,
    onContactTap: (ContactResult) -> Unit,
    onFileTap: (FileResult) -> Unit,
    onWebSearchTap: (String) -> Unit,
    onRecentTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showRecent = uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()
    val showResults = uiState.query.isNotBlank()

    if (!showRecent && !showResults) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141414)
    ) {
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {

            if (showRecent) {
                item {
                    SectionLabel("Recent")
                }
                items(uiState.recentSearches) { recent ->
                    ResultRow(
                        icon = Icons.Filled.History,
                        label = recent,
                        onClick = { onRecentTap(recent) }
                    )
                }
            }

            if (showResults) {
                uiState.calculatorResult?.let { result ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "= $result",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (uiState.matchingApps.isNotEmpty()) {
                    item { SectionLabel("Apps") }
                    items(uiState.matchingApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppTap(app) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Text(
                                text = app.label,
                                color = Color.White,
                                modifier = Modifier.padding(start = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (uiState.matchingContacts.isNotEmpty()) {
                    item { SectionLabel("Contacts") }
                    items(uiState.matchingContacts) { contact ->
                        ResultRow(
                            icon = Icons.Filled.Contacts,
                            label = contact.displayName,
                            onClick = { onContactTap(contact) }
                        )
                    }
                }

                if (uiState.matchingSettings.isNotEmpty()) {
                    item { SectionLabel("Settings") }
                    items(uiState.matchingSettings) { entry ->
                        ResultRow(
                            icon = Icons.Filled.Settings,
                            label = entry.label,
                            onClick = { onSettingsTap(entry) }
                        )
                    }
                }

                if (uiState.matchingFiles.isNotEmpty()) {
                    item { SectionLabel("Files") }
                    items(uiState.matchingFiles) { file ->
                        ResultRow(
                            icon = Icons.Filled.Description,
                            label = file.displayName,
                            onClick = { onFileTap(file) }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWebSearchTap(uiState.query) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Search the web for \"${uiState.query}\"",
                            color = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(start = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF888888),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun ResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
