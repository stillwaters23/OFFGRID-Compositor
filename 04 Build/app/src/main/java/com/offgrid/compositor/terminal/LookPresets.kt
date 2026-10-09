package com.offgrid.compositor.terminal

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Saved panel profiles include the look, position, camera mode, and active effect settings.
 * Profiles can be saved, loaded, renamed, duplicated, and archived. They live in their own
 * file on the phone, so Reset all never deletes them.
 */
internal class LookPresets(context: Context, private val panel: PanelPrefs) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** The preset names, oldest first. */
    fun names(): List<String> {
        val saved = prefs.getString(NAMES, null) ?: return emptyList()
        return try {
            val list = JSONArray(saved)
            (0 until list.length()).map { list.getString(it) }
        } catch (error: JSONException) {
            emptyList()
        }
    }

    fun archivedNames(): List<String> {
        val saved = prefs.getString(ARCHIVED_NAMES, null) ?: return emptyList()
        return try {
            val list = JSONArray(saved)
            (0 until list.length()).map { list.getString(it) }
        } catch (error: JSONException) {
            emptyList()
        }
    }

    /** Saves the profile now. Existing names require an explicit overwrite confirmation. */
    fun save(name: String, overwrite: Boolean = false): Result {
        val clean = name.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        if (clean.length > NAME_MAX || clean.any { it.isISOControl() }) return Result.INVALID_NAME
        val names = names()
        if (clean in names && !overwrite) return Result.TAKEN
        if (clean !in names && names.size >= MAX_PRESETS) return Result.FULL
        val look = JSONObject()
        panel.profileEntries().forEach { (key, value) -> look.put(key, value) }
        saveJson(clean, look, names, clean in names)
        return if (clean in names) Result.REPLACED else Result.DONE
    }

    /** Puts preset [name]'s look back. Settings it doesn't hold go back to the start. */
    fun load(name: String): Result {
        val saved = prefs.getString(PRESET + name, null) ?: return Result.MISSING
        val look = try {
            JSONObject(saved)
        } catch (error: JSONException) {
            return Result.MISSING
        }
        val entries = HashMap<String, Any>()
        for (key in look.keys()) entries[key] = look.get(key)
        panel.replaceProfile(entries)
        return Result.DONE
    }

    fun rename(from: String, to: String): Result {
        val clean = to.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        val names = names()
        if (from !in names) return Result.MISSING
        if (clean == from) return Result.DONE
        if (clean.length > NAME_MAX || clean.any { it.isISOControl() }) return Result.INVALID_NAME
        if (clean in names || clean in archivedNames()) return Result.TAKEN
        val saved = prefs.getString(PRESET + from, null) ?: return Result.MISSING
        prefs.edit {
            remove(PRESET + from)
            putString(PRESET + clean, saved)
            putString(NAMES, JSONArray(names.map { if (it == from) clean else it }).toString())
        }
        return Result.DONE
    }

    fun archive(name: String): Result {
        val names = names()
        if (name !in names) return Result.MISSING
        prefs.edit {
            putString(NAMES, JSONArray(names - name).toString())
            putString(ARCHIVED_NAMES, JSONArray(archivedNames() + name).toString())
        }
        return Result.DONE
    }

    fun restore(name: String): Result {
        val archived = archivedNames()
        if (name !in archived) return Result.MISSING
        val names = names()
        if (name in names) return Result.TAKEN
        if (names.size >= MAX_PRESETS) return Result.FULL
        prefs.edit {
            putString(NAMES, JSONArray(names + name).toString())
            putString(ARCHIVED_NAMES, JSONArray(archived - name).toString())
        }
        return Result.DONE
    }

    fun duplicate(from: String, to: String): Result {
        val clean = to.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        if (clean.length > NAME_MAX || clean.any { it.isISOControl() }) return Result.INVALID_NAME
        val names = names()
        if (from !in names) return Result.MISSING
        if (clean in names || clean in archivedNames()) return Result.TAKEN
        if (names.size >= MAX_PRESETS) return Result.FULL
        val saved = prefs.getString(PRESET + from, null) ?: return Result.MISSING
        val json = try {
            JSONObject(saved)
        } catch (error: JSONException) {
            return Result.BAD_FILE
        }
        saveJson(clean, json, names, replacing = false)
        return Result.DONE
    }

    fun exportJson(name: String): String? = prefs.getString(PRESET + name, null)

    fun importJson(name: String, serialized: String, overwrite: Boolean = false): Result {
        val clean = name.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        if (clean.length > NAME_MAX || clean.any { it.isISOControl() }) return Result.INVALID_NAME
        if (serialized.length > MAX_IMPORT_LENGTH) return Result.BAD_FILE
        val profile = try {
            JSONObject(serialized)
        } catch (error: JSONException) {
            return Result.BAD_FILE
        }
        if (profile.length() == 0) return Result.BAD_FILE
        val entries = HashMap<String, Any>()
        for (key in profile.keys()) {
            if (!panel.isProfileEntryAllowed(key)) return Result.BAD_FILE
            val value = profile.opt(key)
            when {
                key.startsWith("toggle.") -> if (value is Boolean) entries[key] = value else return Result.BAD_FILE
                key.startsWith("text.") -> if (value is String) entries[key] = value else return Result.BAD_FILE
                key == "position_x" || key == "position_y" ||
                    key.startsWith("number.") || key.startsWith("color.") ->
                    if (value is Number) entries[key] = value else return Result.BAD_FILE
                else -> return Result.BAD_FILE
            }
        }
        val names = names()
        if ((clean in names || clean in archivedNames()) && !overwrite) return Result.TAKEN
        if (clean !in names && names.size >= MAX_PRESETS) return Result.FULL
        saveJson(clean, JSONObject().apply { entries.forEach { (key, value) -> put(key, value) } }, names, clean in names)
        return if (clean in names) Result.REPLACED else Result.DONE
    }

    private fun saveJson(name: String, json: JSONObject, names: List<String>, replacing: Boolean) {
        prefs.edit {
            putString(PRESET + name, json.toString())
            putString(NAMES, JSONArray(if (replacing) names else names + name).toString())
            putString(ARCHIVED_NAMES, JSONArray(archivedNames() - name).toString())
        }
    }

    enum class Result { DONE, REPLACED, NO_NAME, TAKEN, MISSING, FULL, INVALID_NAME, BAD_FILE }

    companion object {
        const val MAX_PRESETS = 20
        const val NAME_MAX = 24
        const val MAX_IMPORT_LENGTH = 65_536
        private const val FILE_NAME = "look_presets"
        private const val NAMES = "names"
        private const val ARCHIVED_NAMES = "archived_names"
        private const val PRESET = "preset."
    }
}
