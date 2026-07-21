package com.godwin.orbitlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "orbit_settings")

/**
 * Central store for the customization options in the spec: wheel size,
 * transparency, blur, animation speed, AMOLED mode, dark/light, etc.
 * Only a couple of keys are wired up in Phase 1 — more are added as each
 * later phase implements the feature that reads them.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val WHEEL_SIZE_SCALE = floatPreferencesKey("wheel_size_scale") // 0.5..1.5
        val ANIMATION_SPEED_SCALE = floatPreferencesKey("animation_speed_scale") // 0.5..2.0
        val ONE_HANDED_MODE = booleanPreferencesKey("one_handed_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you_enabled")
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
}
