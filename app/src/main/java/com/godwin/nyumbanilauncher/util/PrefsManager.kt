package com.godwin.nyumbanilauncher.util

import android.content.Context
import com.godwin.nyumbanilauncher.model.GridItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Central place for reading/writing everything the user can customize:
 * grid dimensions, accent color, background color, and the home layout itself.
 */
class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ---- Grid customization ----

    var columns: Int
        get() = prefs.getInt(KEY_COLUMNS, DEFAULT_COLUMNS)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value.coerceIn(3, 8)).apply()

    var rows: Int
        get() = prefs.getInt(KEY_ROWS, DEFAULT_ROWS)
        set(value) = prefs.edit().putInt(KEY_ROWS, value.coerceIn(3, 10)).apply()

    // ---- Theme customization ----

    var accentColor: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT)
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    var backgroundColor: Int
        get() = prefs.getInt(KEY_BG_COLOR, DEFAULT_BG)
        set(value) = prefs.edit().putInt(KEY_BG_COLOR, value).apply()

    var iconLabelsVisible: Boolean
        get() = prefs.getBoolean(KEY_LABELS_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_LABELS_VISIBLE, value).apply()

    // ---- Gesture customization (which app/action each swipe triggers) ----

    var swipeUpAction: String
        get() = prefs.getString(KEY_SWIPE_UP, ACTION_OPEN_DRAWER) ?: ACTION_OPEN_DRAWER
        set(value) = prefs.edit().putString(KEY_SWIPE_UP, value).apply()

    var swipeDownAction: String
        get() = prefs.getString(KEY_SWIPE_DOWN, ACTION_NOTIFICATIONS) ?: ACTION_NOTIFICATIONS
        set(value) = prefs.edit().putString(KEY_SWIPE_DOWN, value).apply()

    // ---- Home layout persistence ----

    fun saveLayout(items: List<GridItem>) {
        prefs.edit().putString(KEY_LAYOUT, gson.toJson(items)).apply()
    }

    fun loadLayout(): MutableList<GridItem> {
        val json = prefs.getString(KEY_LAYOUT, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<GridItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /** Serializes every customizable setting + the layout into one JSON blob for backup. */
    fun exportAll(): String {
        val export = BackupData(
            columns = columns,
            rows = rows,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            iconLabelsVisible = iconLabelsVisible,
            swipeUpAction = swipeUpAction,
            swipeDownAction = swipeDownAction,
            layout = loadLayout()
        )
        return gson.toJson(export)
    }

    /** Restores everything from a JSON blob previously produced by [exportAll]. */
    fun importAll(json: String): Boolean {
        return try {
            val data = gson.fromJson(json, BackupData::class.java)
            columns = data.columns
            rows = data.rows
            accentColor = data.accentColor
            backgroundColor = data.backgroundColor
            iconLabelsVisible = data.iconLabelsVisible
            swipeUpAction = data.swipeUpAction
            swipeDownAction = data.swipeDownAction
            saveLayout(data.layout)
            true
        } catch (e: Exception) {
            false
        }
    }

    data class BackupData(
        val columns: Int,
        val rows: Int,
        val accentColor: Int,
        val backgroundColor: Int,
        val iconLabelsVisible: Boolean,
        val swipeUpAction: String,
        val swipeDownAction: String,
        val layout: List<GridItem>
    )

    companion object {
        private const val PREFS_NAME = "nyumbani_launcher_prefs"

        private const val KEY_COLUMNS = "columns"
        private const val KEY_ROWS = "rows"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_LABELS_VISIBLE = "labels_visible"
        private const val KEY_SWIPE_UP = "swipe_up_action"
        private const val KEY_SWIPE_DOWN = "swipe_down_action"
        private const val KEY_LAYOUT = "home_layout"

        const val DEFAULT_COLUMNS = 4
        const val DEFAULT_ROWS = 5
        const val DEFAULT_ACCENT = 0xFF6750A4.toInt()
        const val DEFAULT_BG = 0xFF121212.toInt()

        const val ACTION_OPEN_DRAWER = "open_drawer"
        const val ACTION_NONE = "none"
        const val ACTION_NOTIFICATIONS = "notifications"
        const val ACTION_SEARCH = "search"
    }
}
