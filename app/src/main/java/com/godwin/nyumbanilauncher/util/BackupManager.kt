package com.godwin.nyumbanilauncher.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/**
 * Handles writing a backup JSON file to Downloads/NyumbaniLauncher and reading one back.
 * Uses MediaStore on API 29+ (scoped storage) and falls back to direct file access below that.
 */
object BackupManager {

    private const val BACKUP_FILENAME = "nyumbani_launcher_backup.json"

    fun saveBackup(context: Context, json: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILENAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out: OutputStream ->
                    out.write(json.toByteArray())
                }
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, BACKUP_FILENAME)
                file.writeText(json)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Reads the most recent backup file previously written by [saveBackup], if any. */
    fun readBackup(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection, selection, arrayOf(BACKUP_FILENAME), null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                        resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } else null
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(dir, BACKUP_FILENAME)
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
