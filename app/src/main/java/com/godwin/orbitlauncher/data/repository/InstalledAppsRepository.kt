package com.godwin.orbitlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.godwin.orbitlauncher.domain.model.AppInfo

interface InstalledAppsRepository {
    fun getLaunchableApps(): List<AppInfo>
    fun launch(app: AppInfo)
}

class InstalledAppsRepositoryImpl(
    private val context: Context
) : InstalledAppsRepository {

    override fun getLaunchableApps(): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityClassName = resolveInfo.activityInfo.name,
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override fun launch(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = android.content.ComponentName(app.packageName, app.activityClassName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
