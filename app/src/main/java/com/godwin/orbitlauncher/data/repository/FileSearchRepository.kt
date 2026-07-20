package com.godwin.orbitlauncher.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class FileResult(
    val displayName: String,
    val uri: Uri,
    val mimeType: String?
)

/**
 * Requires storage/media read permission (varies by API level -- see
 * AndroidManifest). Returns an empty list if permission isn't granted,
 * same pattern as ContactsRepository.
 */
class FileSearchRepository(private val context: Context) {

    fun search(query: String): List<FileResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<FileResult>()
        try {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 5) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx)
                    results.add(
                        FileResult(
                            displayName = name,
                            uri = ContentUris.withAppendedId(collection, id),
                            mimeType = mime
                        )
                    )
                    count++
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted -- return whatever we have (empty).
        }
        return results
    }
}
