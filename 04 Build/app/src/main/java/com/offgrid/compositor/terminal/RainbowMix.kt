package com.offgrid.compositor.terminal

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Nine typed numbers mix into one color: Red, Orange, Yellow, Green, Blue, Indigo, Violet
 * (ROYGBIV), then White and Black. Each is 0 to 100.
 *
 * The seven colors and White mix like light: Red and Green make Yellow, Blue and Yellow make
 * white, and White makes any color paler. If the mix is brighter than the screen can show,
 * the whole mix is scaled down, so Red 100 plus Yellow 100 gives orange instead of plain yellow.
 * Black works last, like a dimmer: Black 50 makes the color half as bright, and 100 is black.
 *
 * Colors are packed as 0xAARRGGBB, like Android colors. This file uses no Android code,
 * so its math can be checked on any computer.
 */
internal object RainbowMix {
    val NAMES = listOf("Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "White", "Black")

    /** Each of the nine at full strength, for the little dots next to the numbers. */
    val BASE_COLORS = intArrayOf(
        0xFFFF0000.toInt(),
        0xFFFF8000.toInt(),
        0xFFFFFF00.toInt(),
        0xFF00FF00.toInt(),
        0xFF0000FF.toInt(),
        0xFF5500FF.toInt(),
        0xFFBF00FF.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt(),
    )

    /** Where White and Black sit in the list. */
    const val WHITE = 7
    const val BLACK = 8

    private const val ORANGE_GREEN = 0.5f
    private const val INDIGO_RED = 1f / 3f
    private const val VIOLET_RED = 0.75f

    /** Each light at 100: red, green, blue amounts from 0 to 1. Black is not a light. */
    private val LIGHTS = arrayOf(
        floatArrayOf(1f, 0f, 0f),
        floatArrayOf(1f, ORANGE_GREEN, 0f),
        floatArrayOf(1f, 1f, 0f),
        floatArrayOf(0f, 1f, 0f),
        floatArrayOf(0f, 0f, 1f),
        floatArrayOf(INDIGO_RED, 0f, 1f),
        floatArrayOf(VIOLET_RED, 0f, 1f),
        floatArrayOf(1f, 1f, 1f),
    )

    /** Step 2.1 and 2.2 saved six numbers: Red, Yellow, Green, Blue, Indigo, Violet. */
    private val OLD_SIX = intArrayOf(0, 2, 3, 4, 5, 6)

    fun mix(weights: IntArray): Int {
        var r = 0f
        var g = 0f
        var b = 0f
        for (index in LIGHTS.indices) {
            val amount = amount(weights, index)
            r += LIGHTS[index][0] * amount
            g += LIGHTS[index][1] * amount
            b += LIGHTS[index][2] * amount
        }
        val top = maxOf(r, g, b)
        if (top > 1f) {
            r /= top
            g /= top
            b /= top
        }
        val dim = 1f - amount(weights, BLACK)
        return rgb((r * dim * 255f).roundToInt(), (g * dim * 255f).roundToInt(), (b * dim * 255f).roundToInt())
    }

    private fun amount(weights: IntArray, index: Int): Float = weights.getOrElse(index) { 0 }.coerceIn(0, 100) / 100f

    /** The nine numbers that make [color], as closely as whole numbers allow. */
    fun split(color: Int): IntArray {
        var r = (color shr 16 and 0xFF) / 255f
        var g = (color shr 8 and 0xFF) / 255f
        var b = (color and 0xFF) / 255f
        val out = FloatArray(NAMES.size)
        val top = maxOf(r, g, b)
        if (top <= 0f) {
            out[BLACK] = 1f
            return whole(out)
        }
        // Black is how far the color is from full brightness. The rest is light.
        out[BLACK] = 1f - top
        r /= top
        g /= top
        b /= top
        // White is the gray part that all three share.
        val white = minOf(r, g, b)
        out[WHITE] = white
        r -= white
        g -= white
        b -= white
        when {
            b <= 0f -> splitWarm(r, g, out)
            g <= 0f -> splitPurple(r, b, out)
            else -> {
                out[3] = g
                out[4] = b
            }
        }
        return whole(out)
    }

    /** Red, Orange, Yellow, and Green, for colors with no blue in them. */
    private fun splitWarm(r: Float, g: Float, out: FloatArray) {
        when {
            r <= 0f -> out[3] = g
            g <= r * ORANGE_GREEN -> {
                out[1] = g / ORANGE_GREEN
                out[0] = r - out[1]
            }
            g <= r -> {
                // Between orange and yellow.
                out[1] = (r - g) / (1f - ORANGE_GREEN)
                out[2] = r - out[1]
            }
            else -> {
                out[2] = r
                out[3] = g - r
            }
        }
    }

    /** Red, Violet, Indigo, and Blue, for colors with no green in them. */
    private fun splitPurple(r: Float, b: Float, out: FloatArray) {
        if (b <= 0f) {
            out[0] = r
            return
        }
        val ratio = r / b
        when {
            ratio >= VIOLET_RED -> {
                out[6] = b
                out[0] = r - VIOLET_RED * b
            }
            ratio >= INDIGO_RED -> {
                out[6] = (r - INDIGO_RED * b) / (VIOLET_RED - INDIGO_RED)
                out[5] = b - out[6]
            }
            else -> {
                out[5] = r / INDIGO_RED
                out[4] = b - out[5]
            }
        }
    }

    private fun whole(values: FloatArray): IntArray =
        values.map { (it * 100f).roundToInt().coerceIn(0, 100) }.toIntArray()

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

    /** Reads nine saved numbers. Six numbers saved by Step 2.1 and 2.2 still load, with Orange, White, and Black at 0. */
    fun decode(text: String?): IntArray? {
        val parts = text?.split(",") ?: return null
        val numbers = parts.map { it.trim().toIntOrNull()?.coerceIn(0, 100) ?: return null }
        return when (numbers.size) {
            NAMES.size -> numbers.toIntArray()
            OLD_SIX.size -> IntArray(NAMES.size).also { nine -> OLD_SIX.forEachIndexed { old, new -> nine[new] = numbers[old] } }
            else -> null
        }
    }
}
