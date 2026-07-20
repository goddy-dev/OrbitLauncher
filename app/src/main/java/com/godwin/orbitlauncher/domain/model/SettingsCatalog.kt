package com.godwin.orbitlauncher.domain.model

data class SettingsEntry(
    val label: String,
    val intentAction: String
)

object SettingsCatalog {
    val entries = listOf(
        SettingsEntry("Wi-Fi", android.provider.Settings.ACTION_WIFI_SETTINGS),
        SettingsEntry("Bluetooth", android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingsEntry("Display", android.provider.Settings.ACTION_DISPLAY_SETTINGS),
        SettingsEntry("Sound", android.provider.Settings.ACTION_SOUND_SETTINGS),
        SettingsEntry("Battery", android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS),
        SettingsEntry("Apps", android.provider.Settings.ACTION_APPLICATION_SETTINGS),
        SettingsEntry("Date and time", android.provider.Settings.ACTION_DATE_SETTINGS),
        SettingsEntry("Security", android.provider.Settings.ACTION_SECURITY_SETTINGS),
        SettingsEntry("Accessibility", android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingsEntry("Location", android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingsEntry("Storage", android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingsEntry("Airplane mode", android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        SettingsEntry("NFC", android.provider.Settings.ACTION_NFC_SETTINGS),
        SettingsEntry("All settings", android.provider.Settings.ACTION_SETTINGS)
    )

    fun search(query: String): List<SettingsEntry> =
        entries.filter { it.label.contains(query, ignoreCase = true) }
}
