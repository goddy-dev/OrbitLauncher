package com.godwin.nyumbanilauncher.model

/**
 * A single slot on the home screen grid. It is either:
 *  - a shortcut to one installed app (appKey set, folder items empty), or
 *  - a folder containing multiple app keys (folderName set, folderApps non-empty)
 *
 * This single model keeps backup/restore simple: the whole home layout is
 * just a list of GridItem, serialized to JSON.
 */
data class GridItem(
    var position: Int,
    var appKey: String? = null,
    var folderName: String? = null,
    var folderApps: MutableList<String> = mutableListOf()
) {
    val isFolder: Boolean get() = folderName != null
}
