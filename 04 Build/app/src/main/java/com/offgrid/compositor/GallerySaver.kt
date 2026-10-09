package com.offgrid.compositor

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Puts a finished video in the phone's gallery, in Movies › OFFGRID (final build, M4).
 * Android lets an app add its own videos there without any permission. Nothing uploads.
 */
internal object GallerySaver {
    /** Copies [file] into the gallery. Returns the name it was saved under, or null if it failed. Call off the main thread. */
    fun save(context: Context, file: File, kind: String = "Video"): String? {
        if (!file.exists() || file.length() == 0L) return null
        val name = fileName(kind, Date())
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$FOLDER")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = try {
            resolver.insert(collection, values)
        } catch (error: Exception) {
            Log.e(TAG, "The gallery did not take the video", error)
            null
        } ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IllegalStateException("No gallery file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            name
        } catch (error: Exception) {
            Log.e(TAG, "Saving to the gallery failed", error)
            resolver.delete(uri, null, null)
            null
        }
    }

    /** Title Case with spaces, like "OFFGRID Video 2026-10-09 14.03.22.mp4". */
    fun fileName(kind: String, at: Date): String =
        "OFFGRID $kind ${SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US).format(at)}.mp4"

    private const val FOLDER = "OFFGRID"
    private const val TAG = "OffgridGallery"
}
