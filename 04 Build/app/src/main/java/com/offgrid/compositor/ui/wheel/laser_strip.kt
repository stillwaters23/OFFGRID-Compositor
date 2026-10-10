package com.offgrid.compositor.ui.wheel

import kotlin.math.roundToInt

/**
 * One laser across the reels: a white-hot core, then colored bloom that fades out.
 * Dimmer 0 draws nothing. The strip is a straight line, so it stays continuous through the gap.
 */
internal object LaserStrip {
    const val DEFAULT_COLOR = 0xFF2BF5E6.toInt()
    const val CORE_MIN_CHANNEL = 240
    private const val BLOOM_PEAK = 0.62

    data class Stroke(val color: Int, val widthPx: Float, val core: Boolean)

    fun channel(color: Int, shift: Int): Int = color shr shift and 0xFF

    fun withAlpha(color: Int, alpha: Double): Int {
        val a = (alpha.coerceIn(0.0, 1.0) * 255.0).roundToInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun coreArgb(dimmer: Int): Int {
        if (dimmer <= 0) return 0
        val alpha = dimmer.coerceIn(0, 100) / 100.0
        return withAlpha(0xFFFFFF, alpha)
    }

    fun spreadPx(density: Float): Float = 14f * density.coerceAtLeast(0.5f)

    fun bloomArgb(laser: Int, dimmer: Int, distancePx: Float, spreadPx: Float): Int {
        if (dimmer <= 0 || spreadPx <= 0f) return 0
        val falloff = (1.0 - distancePx / spreadPx).coerceIn(0.0, 1.0)
        val alpha = (dimmer.coerceIn(0, 100) / 100.0) * falloff * BLOOM_PEAK
        return withAlpha(laser, alpha)
    }

    fun strokes(laser: Int, dimmer: Int, density: Float): List<Stroke> {
        if (dimmer <= 0) return emptyList()
        val spread = spreadPx(density)
        val bloom = (4 downTo 1).map { layer ->
            val distance = spread * layer / 5f
            Stroke(
                color = bloomArgb(laser, dimmer, distance, spread),
                widthPx = spread * 2f * layer / 4f,
                core = false,
            )
        }
        val core = Stroke(coreArgb(dimmer), 1.7f * density.coerceAtLeast(0.5f), core = true)
        val edge = Stroke(
            color = withAlpha(laser, 0.85 * dimmer.coerceIn(0, 100) / 100.0),
            widthPx = 0.7f * density.coerceAtLeast(0.5f),
            core = false,
        )
        return bloom + core + edge
    }

    /** The colored glow mixed onto the wheel, sampled just off the white core. */
    fun bloomBackground(background: Int, laser: Int, dimmer: Int, density: Float = 1f): Int {
        val spread = spreadPx(density)
        val bloom = bloomArgb(laser, dimmer, distancePx = 8f * density.coerceAtLeast(0.5f), spreadPx = spread)
        return sourceOver(background, bloom)
    }

    fun sourceOver(background: Int, foreground: Int): Int {
        val alpha = channel(foreground, 24) / 255.0
        fun mix(shift: Int): Int {
            val front = channel(foreground, shift)
            val back = channel(background, shift)
            return (front * alpha + back * (1.0 - alpha)).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** True when the strip crosses the empty space between two reels. */
    fun coversGap(stripLeft: Float, stripRight: Float, gapLeft: Float, gapRight: Float): Boolean =
        gapRight > gapLeft && stripLeft <= gapLeft && stripRight >= gapRight

    fun saturation(color: Int): Double {
        val red = channel(color, 16)
        val green = channel(color, 8)
        val blue = channel(color, 0)
        val high = maxOf(red, green, blue)
        val low = minOf(red, green, blue)
        return if (high == 0) 0.0 else (high - low).toDouble() / high
    }
}
