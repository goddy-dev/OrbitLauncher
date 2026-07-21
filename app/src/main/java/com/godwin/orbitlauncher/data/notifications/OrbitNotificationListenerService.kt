package com.godwin.orbitlauncher.data.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Requires the user to manually grant "Notification access" in system
 * settings (Android does not allow this to be requested via a normal
 * runtime permission dialog). Once granted, tracks which package names
 * currently have an active notification so dock/wheel icons can show a
 * small dot. If access is never granted, [activePackages] simply stays
 * empty and dots never appear -- no crash, no forced setup.
 */
class OrbitNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        _activePackages.value = _activePackages.value + sbn.packageName
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val stillActive = activeNotifications
            ?.any { it.packageName == sbn.packageName } ?: false
        if (!stillActive) {
            _activePackages.value = _activePackages.value - sbn.packageName
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _activePackages.value = activeNotifications?.map { it.packageName }?.toSet() ?: emptySet()
    }

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())
        val activePackages: StateFlow<Set<String>> = _activePackages.asStateFlow()
    }
}
