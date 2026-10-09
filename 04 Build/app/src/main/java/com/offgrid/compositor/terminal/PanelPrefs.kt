package com.offgrid.compositor.terminal

import android.content.Context
import androidx.core.content.edit

/**
 * Saves the panel's typed numbers, toggles, colors, size, and place on the screen.
 * Only plain settings live here. Never a key, password, or code.
 */
internal class PanelPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    init {
        migrate()
    }

    /**
     * Settings from an older build are let go once, so the final build's start look shows
     * when it is installed over an older one (final build, Rex's review). That means plain
     * OLED black, Pure White toggles, and Width and Height 100. The private test marks stay.
     * Step 2.2 already let go of the Step 2.1 size and place the same way.
     */
    private fun migrate() {
        val savedTheme = prefs.getString(TEXT + "base_theme", null)
        prefs.edit {
            remove(TEXT + "side_mode")
            remove("system_color.side_text")
            remove("custom.side_text")
            remove("role.side_text")
            remove("mix.side_text")
            if (savedTheme != "system") putString(TEXT + "base_theme", "system")
        }
        val was = prefs.getInt(LAYOUT, 0)
        if (was >= LAYOUT_NOW) return
        prefs.edit {
            for (key in prefs.all.keys) {
                if (!kept(key)) remove(key)
            }
            putInt(LAYOUT, LAYOUT_NOW)
        }
    }

    /** The settings version on the phone. The final build is 3. */
    val layoutVersion: Int get() = prefs.getInt(LAYOUT, 0)

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

    /** Every saved look setting (theme, colors, glow, cursor, toggle style, size), for presets. */
    fun lookEntries(): Map<String, Any> {
        val out = HashMap<String, Any>()
        for ((key, value) in prefs.all) {
            if (isLook(key) && value != null) out[key] = value
        }
        return out
    }

    /** A profile includes look, position, camera mode, and typed effect settings, but not test marks. */
    fun profileEntries(): Map<String, Any> {
        val out = HashMap<String, Any>()
        for ((key, value) in prefs.all) {
            if (value != null && isProfileKey(key)) out[key] = value
        }
        return out
    }

    /** Swaps the whole saved look for [entries] in one write. Effects, marks, and place stay. */
    fun replaceLook(entries: Map<String, Any>) {
        prefs.edit {
            for (key in prefs.all.keys) {
                if (isLook(key)) remove(key)
            }
            for ((key, value) in entries) {
                if (!isLook(key)) continue
                when {
                    key.startsWith(TOGGLE) -> putBoolean(key, value as? Boolean ?: continue)
                    key.startsWith(TEXT) -> putString(key, value as? String ?: continue)
                    else -> putInt(key, (value as? Number)?.toInt() ?: continue)
                }
            }
        }
    }

    fun replaceProfile(entries: Map<String, Any>) {
        prefs.edit {
            for (key in prefs.all.keys) if (isProfileKey(key)) remove(key)
            for ((key, value) in entries) {
                if (!isProfileKey(key)) continue
                when {
                    key == POSITION_X || key == POSITION_Y ->
                        putFloat(key, ((value as? Number)?.toFloat() ?: continue).coerceIn(0f, 1f))
                    key.startsWith(NUMBER) ->
                        putInt(key, ((value as? Number)?.toInt() ?: continue).coerceIn(0, 100))
                    key.startsWith(COLOR) ->
                        putInt(key, (value as? Number)?.toInt() ?: continue)
                    key.startsWith(TOGGLE) ->
                        putBoolean(key, value as? Boolean ?: continue)
                    key.startsWith(TEXT) ->
                        putString(key, (value as? String)?.takeIf { it.length <= MAX_PROFILE_TEXT } ?: continue)
                }
            }
        }
    }

    private fun isLook(key: String): Boolean {
        val name = when {
            key.startsWith(NUMBER) -> key.removePrefix(NUMBER)
            key.startsWith(TOGGLE) -> key.removePrefix(TOGGLE)
            key.startsWith(TEXT) -> key.removePrefix(TEXT)
            key.startsWith(COLOR) -> key.removePrefix(COLOR)
            else -> return false
        }
        return !name.startsWith(TEST_PREFIX) && !name.startsWith(EFFECT_PREFIX) && name != DIAMOND &&
            !name.startsWith(FOLDER_PREFIX)
    }

    private fun isProfileKey(key: String): Boolean {
        if (key == POSITION_X || key == POSITION_Y) return true
        val name = when {
            key.startsWith(NUMBER) -> key.removePrefix(NUMBER)
            key.startsWith(TOGGLE) -> key.removePrefix(TOGGLE)
            key.startsWith(TEXT) -> key.removePrefix(TEXT)
            key.startsWith(COLOR) -> key.removePrefix(COLOR)
            else -> return false
        }
        if (name.startsWith(TEST_PREFIX)) return false
        if (key.startsWith(TEXT)) {
            return name == "base_theme" || name == "theme_mode" || name == "toggle_style" ||
                name.startsWith("fx.") || name.startsWith("role.") || name.startsWith("mix.")
        }
        return isLook(key) || name.startsWith("fx.")
    }

    fun isProfileEntryAllowed(key: String): Boolean = isProfileKey(key)

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

    /**
     * Puts every setting back to the start. The private test markers and results stay, and so
     * does the camera mode, so a reset never shows Royalty's face by surprise.
     */
    fun resetAll() {
        prefs.edit {
            for (key in prefs.all.keys) {
                if (!kept(key)) remove(key)
            }
        }
    }

    /** Settings no reset or settings move ever removes. */
    private fun kept(key: String): Boolean =
        key.startsWith(TOGGLE + TEST_PREFIX) || key.startsWith(TEXT + TEST_PREFIX) ||
            key == TEXT + KEEP_CAMERA_MODE || key == LAYOUT

    companion object {
        const val TEST_PREFIX = "test."

        /** Effect settings start with this. They are not part of a look preset. */
        private const val EFFECT_PREFIX = "fx."

        /** The Step 1 test diamond and the folder view settings are not part of a look either. */
        private const val DIAMOND = "test_diamond"
        private const val FOLDER_PREFIX = "folder"

        /** The camera mode's key (FxKeys.CAMERA_MODE). Resets never change it. */
        const val KEEP_CAMERA_MODE = "fx.camera.mode"
        private const val FILE_NAME = "terminal_panel"
        private const val NUMBER = "number."
        private const val TOGGLE = "toggle."
        private const val TEXT = "text."
        private const val COLOR = "color."
        private const val MINIMIZED = "minimized"
        private const val ACTIVE_FOLDER = "active_folder"
        private const val POSITION_X = "position_x"
        private const val POSITION_Y = "position_y"
        private const val MAX_PROFILE_TEXT = 256
        const val LAYOUT = "layout_version"

        /** 2 was Step 2.2 to 2.4. 3 is the final build. */
        const val LAYOUT_NOW = 3
    }
}
