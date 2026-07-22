package com.godwin.orbitlauncher.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun QuickSettingsMenu(
    oneHandedMode: Boolean,
    materialYouEnabled: Boolean,
    onChangeWallpaper: () -> Unit,
    onToggleOneHanded: () -> Unit,
    onToggleMaterialYou: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF1A1A1A)
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeWallpaper)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFFAAAAAA))
                Text("Change wallpaper", color = Color.White, modifier = Modifier.padding(start = 12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PanTool, contentDescription = null, tint = Color(0xFFAAAAAA))
                    Text("One-handed mode", color = Color.White, modifier = Modifier.padding(start = 12.dp))
                }
                Switch(
                    checked = oneHandedMode,
                    onCheckedChange = { onToggleOneHanded() },
                    colors = SwitchDefaults.colors()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(0xFFAAAAAA))
                    Text("Material You colors", color = Color.White, modifier = Modifier.padding(start = 12.dp))
                }
                Switch(
                    checked = materialYouEnabled,
                    onCheckedChange = { onToggleMaterialYou() },
                    colors = SwitchDefaults.colors()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = Color(0xFFAAAAAA))
                Text("More settings", color = Color.White, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
