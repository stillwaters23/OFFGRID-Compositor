package com.offgrid.compositor

import android.content.Intent

/**
 * The clips the AR Video Deck can show. Picking uses the system document picker (SAF),
 * so the app does not ask for video-library access and does not scan Download.
 *
 * Clip ids are short on purpose. Phone 2 sends an id, not a long file address.
 */
internal class ArDeckCatalog {
    data class Clip(val id: String, val name: String, val uri: String)

    private val clips = ArrayList<Clip>()
    var index: Int = -1
        private set

    @Synchronized
    fun add(name: String, uri: String): Clip {
        require(isLocal(uri)) { "AR clips must be local files" }
        val cleanName = name.trim().ifEmpty { "clip" }.take(NAME_MAX)
        val clip = Clip("c${clips.size + 1}", cleanName, uri)
        clips += clip
        index = clips.lastIndex
        return clip
    }

    /** Puts back the clip that was playing, after the app starts again. */
    @Synchronized
    fun restore(id: String, uri: String) {
        if (id.isBlank() || !isLocal(uri) || clips.any { it.id == id }) return
        clips += Clip(id, id, uri)
        index = clips.lastIndex
    }

    @Synchronized
    fun current(): Clip? = clips.getOrNull(index)

    @Synchronized
    fun next(): Clip? {
        if (clips.isEmpty()) return null
        index = (index + 1) % clips.size
        return clips[index]
    }

    /** A catalog id, or a local content/file address. Anything else is ignored. */
    @Synchronized
    fun resolve(idOrUri: String): String? {
        if (idOrUri.isBlank()) return null
        clips.firstOrNull { it.id == idOrUri }?.let { return it.uri }
        return idOrUri.takeIf { isLocal(it) }
    }

    @Synchronized
    fun clips(): List<Clip> = clips.toList()

    companion object {
        const val NAME_MAX = 40
        const val PLAY = "ar_play"
        const val PAUSE = "ar_pause"
        const val NEXT = "ar_next"

        /** Saved with the panel, not sent as a remote field. Holds the current local address. */
        const val URI_PREF = "ar.deck.uri"

        fun isLocal(uri: String): Boolean =
            uri.startsWith("content://") || uri.startsWith("file://")

        /** System file picker. Read access is for the chosen file only. */
        fun pickIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }
}
