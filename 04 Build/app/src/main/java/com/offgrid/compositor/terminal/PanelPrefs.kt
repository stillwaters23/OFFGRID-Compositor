package com.offgrid.compositor.terminal

import android.content.Context
import androidx.core.content.edit

/**
 * Saves the panel's typed numbers, toggles, colors, size, and place on the screen.
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

    fun text(key: String): String? = prefs.getString(TEXT + key, null)

    fun hasColor(key: String): Boolean = prefs.contains(COLOR + key)

    fun color(key: String, default: Int): Int = prefs.getInt(COLOR + key, default)

    /** Saves many values in one write, so a theme lands all at once. */
    fun write(block: Writer.() -> Unit) {
        prefs.edit { Writer(this).block() }
    }

    class Writer(private val editor: android.content.SharedPreferences.Editor) {
        fun number(key: String, value: Int) {
            editor.putInt(NUMBER + key, value.coerceIn(0, 100))
        }

        fun toggle(key: String, value: Boolean) {
            editor.putBoolean(TOGGLE + key, value)
        }

        fun text(key: String, value: String?) {
            if (value == null) editor.remove(TEXT + key) else editor.putString(TEXT + key, value)
        }

        fun color(key: String, value: Int) {
            editor.putInt(COLOR + key, value)
        }

        fun removeNumber(key: String) {
            editor.remove(NUMBER + key)
        }

        fun removeToggle(key: String) {
            editor.remove(TOGGLE + key)
        }

        fun removeColor(key: String) {
            editor.remove(COLOR + key)
        }
    }

    fun hasNumber(key: String): Boolean = prefs.contains(NUMBER + key)

    fun hasToggle(key: String): Boolean = prefs.contains(TOGGLE + key)

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

    /** The panel's width and height in dp, if Royalty resized it. */
    fun size(): Pair<Float, Float>? {
        if (!prefs.contains(SIZE_W) || !prefs.contains(SIZE_H)) {
            return null
        }
        return prefs.getFloat(SIZE_W, 0f) to prefs.getFloat(SIZE_H, 0f)
    }

    fun setSize(widthDp: Float, heightDp: Float) {
        prefs.edit {
            putFloat(SIZE_W, widthDp)
            putFloat(SIZE_H, heightDp)
        }
    }

    /** Puts every setting back to the start. The private test markers stay. */
    fun resetAll() {
        val keep = TOGGLE + TEST_PREFIX
        prefs.edit {
            for (key in prefs.all.keys) {
                if (!key.startsWith(keep)) remove(key)
            }
        }
    }

    companion object {
        const val TEST_PREFIX = "test."
        private const val FILE_NAME = "terminal_panel"
        private const val NUMBER = "number."
        private const val TOGGLE = "toggle."
        private const val TEXT = "text."
        private const val COLOR = "color."
        private const val MINIMIZED = "minimized"
        private const val ACTIVE_FOLDER = "active_folder"
        private const val POSITION_X = "position_x"
        private const val POSITION_Y = "position_y"
        private const val SIZE_W = "size_w_dp"
        private const val SIZE_H = "size_h_dp"
    }
}
