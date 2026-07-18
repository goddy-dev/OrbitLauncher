package com.godwin.orbitlauncher.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Per spec: this button does NOT open a normal app drawer. In Phase 3 it
 * triggers the retractable radial "Orbit Wheel" on the left edge instead.
 * For now it's a tappable placeholder so the layout is complete.
 */
@Composable
fun DrawerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0x1AFFFFFF)
    ) {
        Icon(
            imageVector = Icons.Filled.Apps,
            contentDescription = "Open app wheel",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(6.dp)
        )
    }
}
