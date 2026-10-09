package com.offgrid.compositor.terminal

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Saved looks (final build, M4): the theme, colors, glow, cursor, toggle style, and size,
 * under a name Royalty picks. Save, load, rename, and delete. Presets live in their own
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

    /** Saves the look on screen now as [name]. A preset with the same name is replaced. */
    fun save(name: String): Result {
        val clean = name.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        val names = names()
        if (clean !in names && names.size >= MAX_PRESETS) return Result.FULL
        val look = JSONObject()
        panel.lookEntries().forEach { (key, value) -> look.put(key, value) }
        prefs.edit {
            putString(PRESET + clean, look.toString())
            putString(NAMES, JSONArray(if (clean in names) names else names + clean).toString())
        }
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
        panel.replaceLook(entries)
        return Result.DONE
    }

    fun rename(from: String, to: String): Result {
        val clean = to.trim()
        if (clean.isEmpty()) return Result.NO_NAME
        val names = names()
        if (from !in names) return Result.MISSING
        if (clean == from) return Result.DONE
        if (clean in names) return Result.TAKEN
        val saved = prefs.getString(PRESET + from, null) ?: return Result.MISSING
        prefs.edit {
            remove(PRESET + from)
            putString(PRESET + clean, saved)
            putString(NAMES, JSONArray(names.map { if (it == from) clean else it }).toString())
        }
        return Result.DONE
    }

    fun delete(name: String): Result {
        val names = names()
        if (name !in names) return Result.MISSING
        prefs.edit {
            remove(PRESET + name)
            putString(NAMES, JSONArray(names - name).toString())
        }
        return Result.DONE
    }

    enum class Result { DONE, REPLACED, NO_NAME, TAKEN, MISSING, FULL }

    companion object {
        const val MAX_PRESETS = 20
        const val NAME_MAX = 24
        private const val FILE_NAME = "look_presets"
        private const val NAMES = "names"
        private const val PRESET = "preset."
    }
}
