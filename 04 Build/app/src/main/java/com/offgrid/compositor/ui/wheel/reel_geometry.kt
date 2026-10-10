package com.offgrid.compositor.ui.wheel

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * S-1: the barrel is measured from the width it actually has.
 * A bigger font scale, or a narrower panel, shrinks the digits so "100" stays inside the reel.
 */
internal object ReelGeometry {
    const val VISIBLE_ROWS = 5
    const val ANGLE_DEGREES = 26.0
    val ANGLE_RADIANS = Math.toRadians(ANGLE_DEGREES)
    private const val DIGITS = 3f
    private const val DIGIT_EM = 0.62f

    data class Fit(
        val fontPx: Float,
        val pitchPx: Float,
        val radiusPx: Float,
        val heightPx: Int,
        val glyphFits: Boolean,
        val innerWidthPx: Float,
    )

    fun fit(widthPx: Int, maxHeightPx: Int, fontScale: Float, density: Float): Fit {
        val width = widthPx.coerceAtLeast(1)
        val scale = fontScale.coerceIn(0.5f, 2f)
        val pad = 6f * density.coerceAtLeast(0.5f)
        val inner = (width - pad * 2f).coerceAtLeast(1f)
        val maxFontByWidth = inner / (DIGITS * DIGIT_EM)
        var font = min(18f * density.coerceAtLeast(0.5f) * scale, maxFontByWidth)
        var pitch = font * 1.35f
        var height = pitch * VISIBLE_ROWS
        val limit = maxHeightPx.coerceAtLeast(1).toFloat()
        if (height > limit) {
            pitch = limit / VISIBLE_ROWS
            font = min(font, pitch / 1.35f)
            height = pitch * VISIBLE_ROWS
        }
        font = min(font, maxFontByWidth).coerceAtLeast(1f)
        val angle = ANGLE_RADIANS.toFloat()
        val radius = pitch / sin(angle)
        val glyph = font * DIGITS * DIGIT_EM
        val minTouch = 48f * density.coerceAtLeast(0.5f)
        val usedHeight = height.coerceAtLeast(minTouch).roundToInt().coerceAtLeast(1)
        return Fit(
            fontPx = font,
            pitchPx = pitch,
            radiusPx = radius,
            heightPx = if (usedHeight > maxHeightPx && maxHeightPx < Int.MAX_VALUE / 4) {
                maxHeightPx.coerceAtLeast(1)
            } else {
                usedHeight
            },
            glyphFits = glyph <= inner + 0.51f,
            innerWidthPx = inner,
        )
    }

    fun projectedY(centerY: Float, offsetNotches: Float, radiusPx: Float): Float {
        val angle = offsetNotches * ANGLE_RADIANS.toFloat()
        return centerY + sin(angle) * radiusPx
    }

    fun depth(offsetNotches: Float): Float =
        cos(offsetNotches * ANGLE_RADIANS).toFloat().coerceAtLeast(0f)

    /** The selected row stays put when the barrel is measured again. */
    fun valueAfterResize(value: Int): Int = value.coerceIn(0, 100)
}
