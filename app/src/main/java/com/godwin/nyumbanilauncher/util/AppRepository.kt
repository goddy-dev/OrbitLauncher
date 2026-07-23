package com.godwin.nyumbanilauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.godwin.nyumbanilauncher.model.AppInfo

/**
 * Wraps PackageManager queries so the rest of the app never touches it directly.
 */
object AppRepository {

    fun getAllLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        return resolved.map { info ->
            AppInfo(
                packageName = info.activityInfo.packageName,
                activityName = info.activityInfo.name,
                label = info.loadLabel(pm).toString()
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun findByKey(context: Context, key: String): AppInfo? {
        val (pkg, activity) = key.split("/", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else return null
        }
        return getAllLaunchableApps(context).firstOrNull {
            it.packageName == pkg && it.activityName == activity
        }
    }

    fun launch(context: Context, app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = android.content.ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // App may have been uninstalled since the layout was saved; fail silently.
        }
    }
}
