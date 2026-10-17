package com.offgrid.live

import android.content.Context
import androidx.core.content.edit

internal class OverlayPrefs(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var x: Float
        get() = preferences.getFloat(X, Float.NaN)
        set(value) = preferences.edit { putFloat(X, value) }

    var y: Float
        get() = preferences.getFloat(Y, Float.NaN)
        set(value) = preferences.edit { putFloat(Y, value) }

    var widthPercent: Int
        get() = preferences.getInt(WIDTH, 40).coerceIn(0, 100)
        set(value) = preferences.edit { putInt(WIDTH, value.coerceIn(0, 100)) }

    var heightPercent: Int
        get() = preferences.getInt(HEIGHT, 35).coerceIn(0, 100)
        set(value) = preferences.edit { putInt(HEIGHT, value.coerceIn(0, 100)) }

    var isBubble: Boolean
        get() = preferences.getBoolean(BUBBLE, false)
        set(value) = preferences.edit { putBoolean(BUBBLE, value) }

    var fullScreen: Boolean
        get() = preferences.getBoolean(FULL_SCREEN, false)
        set(value) = preferences.edit { putBoolean(FULL_SCREEN, value) }

    private companion object {
        const val FILE_NAME = "live_overlay"
        const val X = "x"
        const val Y = "y"
        const val WIDTH = "width_percent"
        const val HEIGHT = "height_percent"
        const val BUBBLE = "bubble"
        const val FULL_SCREEN = "full_screen"
    }
}
