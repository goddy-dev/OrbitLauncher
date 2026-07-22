package com.godwin.orbitlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godwin.orbitlauncher.data.repository.HapticStrength

data class SettingsUiValues(
    val amoledMode: Boolean,
    val darkMode: Boolean,
    val wheelSizeScale: Float,
    val animationSpeedScale: Float,
    val searchBarVisible: Boolean,
    val dockLabelsVisible: Boolean,
    val hapticStrength: HapticStrength,
    val wheelOnRight: Boolean
)

@Composable
fun SettingsScreen(
    values: SettingsUiValues,
    onBack: () -> Unit,
    onAmoledModeChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onWheelSizeScaleChange: (Float) -> Unit,
    onAnimationSpeedScaleChange: (Float) -> Unit,
    onSearchBarVisibleChange: (Boolean) -> Unit,
    onDockLabelsVisibleChange: (Boolean) -> Unit,
    onHapticStrengthChange: (HapticStrength) -> Unit,
    onWheelOnRightChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Settings", color = Color.White, fontSize = 20.sp)
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 20.dp)) {
                item { SectionHeader("Appearance") }
                item { SwitchRow("AMOLED (true black) background", values.amoledMode, onAmoledModeChange) }
                item { SwitchRow("Dark mode", values.darkMode, onDarkModeChange) }
                item { SwitchRow("Show search bar", values.searchBarVisible, onSearchBarVisibleChange) }
                item { SwitchRow("Show dock labels", values.dockLabelsVisible, onDockLabelsVisibleChange) }
                item { SwitchRow("Wheel on right edge", values.wheelOnRight, onWheelOnRightChange) }

                item { SectionHeader("Wheel") }
                item {
                    SliderRow(
                        label = "Wheel size",
                        value = values.wheelSizeScale,
                        range = 0.7f..1.3f,
                        onChange = onWheelSizeScaleChange
                    )
                }
                item {
                    SliderRow(
                        label = "Animation speed",
                        value = values.animationSpeedScale,
                        range = 0.5f..2f,
                        onChange = onAnimationSpeedScaleChange
                    )
                }

                item { SectionHeader("Haptics") }
                item { HapticStrengthRow(values.hapticStrength, onHapticStrengthChange) }

                item { SectionHeader("Coming later") }
                item {
                    Text(
                        text = "Icon packs and custom fonts need their own asset pipeline " +
                            "and aren't wired up yet.",
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF888888),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Color.White)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun HapticStrengthRow(current: HapticStrength, onChange: (HapticStrength) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HapticStrength.values().forEach { strength ->
            val selected = strength == current
            Text(
                text = strength.name.lowercase().replaceFirstChar { it.uppercase() },
                color = if (selected) Color.Black else Color.White,
                modifier = Modifier
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color(0x1AFFFFFF),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onChange(strength) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
