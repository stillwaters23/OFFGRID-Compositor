package com.offgrid.compositor.terminal

import android.content.Context
import androidx.core.content.edit

/**
 * Saves the panel's typed numbers, toggles, and place on the screen.
 * Only plain settings live here. Never a key, password, or code.
 */
internal class PanelPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun number(key: String, default: Int): Int {
        return prefs.getInt(NUMBER + key, default).coerceIn(0, 100)
    }

    fun setNumber(key: String, value: Int) {
        prefs.edit { putInt(NUMBER + key, value.coerceIn(0, 100)) }
    }

    fun toggle(key: String, default: Boolean): Boolean = prefs.getBoolean(TOGGLE + key, default)

    fun setToggle(key: String, value: Boolean) {
        prefs.edit { putBoolean(TOGGLE + key, value) }
    }

    fun choice(key: String, default: String): String = prefs.getString(CHOICE + key, default) ?: default

    fun setChoice(key: String, value: String) {
        prefs.edit { putString(CHOICE + key, value) }
    }

    fun color(key: String, default: Int): Int = prefs.getInt(COLOR + key, default)

    fun setColor(key: String, value: Int) {
        prefs.edit { putInt(COLOR + key, value) }
    }

    var minimized: Boolean
        get() = prefs.getBoolean(MINIMIZED, false)
        set(value) = prefs.edit { putBoolean(MINIMIZED, value) }

    var activeFolder: String?
        get() = prefs.getString(ACTIVE_FOLDER, null)
        set(value) = prefs.edit { putString(ACTIVE_FOLDER, value) }

    /** Where the panel sits, as a share of the screen width and height. */
    fun position(): Pair<Float, Float>? {
        if (!prefs.contains(POSITION_X) || !prefs.contains(POSITION_Y)) {
            return null
        }
        return prefs.getFloat(POSITION_X, 0f) to prefs.getFloat(POSITION_Y, 0f)
    }

    fun setPosition(xShare: Float, yShare: Float) {
        prefs.edit {
            putFloat(POSITION_X, xShare)
            putFloat(POSITION_Y, yShare)
        }
    }

    private companion object {
        const val FILE_NAME = "terminal_panel"
        const val NUMBER = "number."
        const val TOGGLE = "toggle."
        const val CHOICE = "choice."
        const val COLOR = "color."
        const val MINIMIZED = "minimized"
        const val ACTIVE_FOLDER = "active_folder"
        const val POSITION_X = "position_x"
        const val POSITION_Y = "position_y"
    }
}
