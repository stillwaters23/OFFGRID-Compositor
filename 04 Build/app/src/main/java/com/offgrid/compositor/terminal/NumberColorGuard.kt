package com.offgrid.compositor.terminal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal object NumberColorGuard {
    const val MIN_CONTRAST = 4.5
    const val DARK_BACKGROUND = 0xFF1B1B1B.toInt()
    const val LIGHT_BACKGROUND = 0xFFFFFFFF.toInt()

    fun accepts(color: Int, background: Int): Boolean = contrastRatio(color, background) >= MIN_CONTRAST

    fun visibleColor(color: Int, background: Int): Int {
        if (accepts(color, background)) return color
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        return if (contrastRatio(black, background) >= contrastRatio(white, background)) black else white
    }

    fun contrastRatio(foreground: Int, background: Int): Double {
        val fg = luminance(composite(foreground, background))
        val bg = luminance(background)
        return (max(fg, bg) + 0.05) / (min(fg, bg) + 0.05)
    }

    fun luminance(color: Int): Double {
        fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        val red = linear(color shr 16 and 0xFF)
        val green = linear(color shr 8 and 0xFF)
        val blue = linear(color and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun composite(foreground: Int, background: Int): Int {
        val alpha = (foreground ushr 24 and 0xFF) / 255.0
        fun channel(shift: Int): Int {
            val front = foreground shr shift and 0xFF
            val back = background shr shift and 0xFF
            return (front * alpha + back * (1.0 - alpha)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
