package com.godwin.orbitlauncher.data.repository

import android.content.Context
import android.provider.ContactsContract

data class ContactResult(
    val id: Long,
    val displayName: String,
    val lookupKey: String
)

/**
 * Requires Manifest.permission.READ_CONTACTS to return results. Caller
 * is responsible for checking/requesting the permission first -- this
 * repository simply returns an empty list if it isn't granted (querying
 * without permission throws a SecurityException, which we catch safely).
 */
class ContactsRepository(private val context: Context) {

    fun search(query: String): List<ContactResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<ContactResult>()
        try {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.LOOKUP_KEY
            )
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)

                var count = 0
                while (cursor.moveToNext() && count < 5) {
                    results.add(
                        ContactResult(
                            id = cursor.getLong(idIdx),
                            displayName = cursor.getString(nameIdx) ?: "Unknown",
                            lookupKey = cursor.getString(lookupIdx) ?: ""
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
