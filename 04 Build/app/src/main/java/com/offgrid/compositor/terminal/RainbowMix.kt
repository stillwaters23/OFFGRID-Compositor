package com.offgrid.compositor.terminal

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Six typed numbers mix into one color, like light: Red, Yellow, Green, Blue, Indigo, Violet.
 * Each is 0 to 100. Red and Green make Yellow. Blue and Yellow make white.
 * If the mix is brighter than the screen can show, the whole mix is scaled down,
 * so Red 100 plus Yellow 100 gives orange instead of plain yellow.
 *
 * Colors are packed as 0xAARRGGBB, like Android colors. This file uses no Android code,
 * so its math can be checked on any computer.
 */
internal object RainbowMix {
    val NAMES = listOf("Red", "Yellow", "Green", "Blue", "Indigo", "Violet")

    /** Each of the six colors at full strength, for the little dots next to the numbers. */
    val BASE_COLORS = intArrayOf(
        0xFFFF0000.toInt(),
        0xFFFFFF00.toInt(),
        0xFF00FF00.toInt(),
        0xFF0000FF.toInt(),
        0xFF5500FF.toInt(),
        0xFFBF00FF.toInt(),
    )

    private const val INDIGO_RED = 1f / 3f
    private const val VIOLET_RED = 0.75f

    /** Each base at 100: red, green, blue amounts from 0 to 1. Indigo is #5500FF. Violet is #BF00FF. */
    private val BASES = arrayOf(
        floatArrayOf(1f, 0f, 0f),
        floatArrayOf(1f, 1f, 0f),
        floatArrayOf(0f, 1f, 0f),
        floatArrayOf(0f, 0f, 1f),
        floatArrayOf(INDIGO_RED, 0f, 1f),
        floatArrayOf(VIOLET_RED, 0f, 1f),
    )

    fun mix(weights: IntArray): Int {
        var r = 0f
        var g = 0f
        var b = 0f
        for (index in BASES.indices) {
            val amount = (weights.getOrElse(index) { 0 }.coerceIn(0, 100)) / 100f
            r += BASES[index][0] * amount
            g += BASES[index][1] * amount
            b += BASES[index][2] * amount
        }
        val top = maxOf(r, g, b)
        if (top > 1f) {
            r /= top
            g /= top
            b /= top
        }
        return rgb((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt())
    }

    /** The six numbers that make [color], as closely as whole numbers allow. */
    fun split(color: Int): IntArray {
        var r = (color shr 16 and 0xFF) / 255f
        var g = (color shr 8 and 0xFF) / 255f
        var b = (color and 0xFF) / 255f
        val yellow = minOf(r, g)
        r -= yellow
        g -= yellow
        var red = 0f
        var green = 0f
        var blue = 0f
        var indigo = 0f
        var violet = 0f
        when {
            g > 0f -> {
                green = g
                blue = b
            }
            r > 0f && b > 0f -> {
                val ratio = r / b
                when {
                    ratio >= VIOLET_RED -> {
                        violet = b
                        red = r - VIOLET_RED * b
                    }
                    ratio >= INDIGO_RED -> {
                        violet = (r - INDIGO_RED * b) / (VIOLET_RED - INDIGO_RED)
                        indigo = b - violet
                    }
                    else -> {
                        indigo = r / INDIGO_RED
                        blue = b - indigo
                    }
                }
            }
            else -> {
                red = r
                blue = b
            }
        }
        return floatArrayOf(red, yellow, green, blue, indigo, violet)
            .map { (it * 100f).roundToInt().coerceIn(0, 100) }
            .toIntArray()
    }

    /** Reads "#00FF66" or "00FF66". Anything else gives null. */
    fun parseHex(text: String): Int? {
        val digits = text.trim().removePrefix("#")
        if (digits.length != 6 || digits.any { it.digitToIntOrNull(16) == null }) return null
        return (0xFF000000.toInt() or digits.toInt(16))
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)

    fun hex(color: Int): String = String.format(Locale.US, "#%06X", color and 0x00FFFFFF)

    fun encode(weights: IntArray): String = weights.joinToString(",")

    fun decode(text: String?): IntArray? {
        val parts = text?.split(",") ?: return null
        if (parts.size != BASES.size) return null
        val numbers = parts.map { it.trim().toIntOrNull() ?: return null }
        return numbers.map { it.coerceIn(0, 100) }.toIntArray()
    }
}
