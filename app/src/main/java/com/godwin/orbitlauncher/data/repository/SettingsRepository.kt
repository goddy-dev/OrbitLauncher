package com.godwin.orbitlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "orbit_settings")

/**
 * Haptic feedback intensity. OFF skips haptics entirely, LIGHT uses
 * subtle feedback types, STRONG uses more pronounced ones -- read by
 * the wheel and other interactive components via [SettingsRepository].
 */
enum class HapticStrength { OFF, LIGHT, STRONG }

/**
 * Central store for the customization options in the spec: wheel size,
 * transparency, blur, animation speed, AMOLED mode, dark/light, accent
 * colors, labels, search bar visibility, haptic strength, wheel
 * position, etc. Icon packs and custom fonts are deferred -- both need
 * their own asset-parsing pipeline and would be shallow stubs here.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val WHEEL_SIZE_SCALE = floatPreferencesKey("wheel_size_scale") // 0.5..1.5
        val ANIMATION_SPEED_SCALE = floatPreferencesKey("animation_speed_scale") // 0.5..2.0
        val ONE_HANDED_MODE = booleanPreferencesKey("one_handed_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SEARCH_BAR_VISIBLE = booleanPreferencesKey("search_bar_visible")
        val DOCK_LABELS_VISIBLE = booleanPreferencesKey("dock_labels_visible")
        val HAPTIC_STRENGTH = intPreferencesKey("haptic_strength")
        val WHEEL_ON_RIGHT = booleanPreferencesKey("wheel_on_right")
    }

    val amoledModeFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.AMOLED_MODE] ?: true }

    val wheelSizeScaleFlow: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.WHEEL_SIZE_SCALE] ?: 1.0f }

    val animationSpeedScaleFlow: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.ANIMATION_SPEED_SCALE] ?: 1.0f }

    val oneHandedModeFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ONE_HANDED_MODE] ?: false }

    val materialYouFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.MATERIAL_YOU] ?: false }

    val darkModeFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.DARK_MODE] ?: true }

    val searchBarVisibleFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.SEARCH_BAR_VISIBLE] ?: true }

    val dockLabelsVisibleFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.DOCK_LABELS_VISIBLE] ?: true }

    val hapticStrengthFlow: Flow<HapticStrength> =
        context.settingsDataStore.data.map {
            when (it[Keys.HAPTIC_STRENGTH] ?: 1) {
                0 -> HapticStrength.OFF
                2 -> HapticStrength.STRONG
                else -> HapticStrength.LIGHT
            }
        }

    val wheelOnRightFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.WHEEL_ON_RIGHT] ?: true }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    }

    suspend fun setWheelSizeScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.WHEEL_SIZE_SCALE] = scale }
    }

    suspend fun setAnimationSpeedScale(scale: Float) {
        context.settingsDataStore.edit { it[Keys.ANIMATION_SPEED_SCALE] = scale }
    }

    suspend fun setOneHandedMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONE_HANDED_MODE] = enabled }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MATERIAL_YOU] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setSearchBarVisible(visible: Boolean) {
        context.settingsDataStore.edit { it[Keys.SEARCH_BAR_VISIBLE] = visible }
    }

    suspend fun setDockLabelsVisible(visible: Boolean) {
        context.settingsDataStore.edit { it[Keys.DOCK_LABELS_VISIBLE] = visible }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        val value = when (strength) {
            HapticStrength.OFF -> 0
            HapticStrength.LIGHT -> 1
            HapticStrength.STRONG -> 2
        }
        context.settingsDataStore.edit { it[Keys.HAPTIC_STRENGTH] = value }
    }

    suspend fun setWheelOnRight(onRight: Boolean) {
        context.settingsDataStore.edit { it[Keys.WHEEL_ON_RIGHT] = onRight }
    }
}
