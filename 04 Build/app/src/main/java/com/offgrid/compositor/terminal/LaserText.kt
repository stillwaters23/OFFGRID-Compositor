package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import kotlin.math.sqrt

/**
 * A title in Laser Jet Bright: a bright core with harsh layers of glow around it.
 * Used for the panel title, the small tag, folder headings, and section labels.
 * Titles are never typed letter by letter, so the whole title glows at once.
 * Words always fit (see [FitTextView]). The panel title stays on one line and gets smaller if needed.
 */
internal class LaserTitleView(context: Context) : FitTextView(context) {
    private var laserColor = Hud.WHITE
    private var glowColor: Int? = null
    private var glowStrength = 1f
    private var glowBoost = 0f

    /**
     * [color] is the laser color. The core burns brighter than it. [glow] colors the glow,
     * or null to glow in the laser color. [strength] 0 is plain text. [boost] above 0 spreads
     * the glow wider and brighter, for Glow strength above 50.
     */
    fun setLaser(color: Int, glow: Int?, strength: Float, boost: Float = 0f) {
        laserColor = color
        glowColor = glow
        // The glow climbs fast at low strengths, so the default already burns harsh and bright.
        glowStrength = sqrt(strength.coerceIn(0f, 1f))
        glowBoost = if (glowStrength > 0f) boost.coerceIn(0f, 1f) else 0f
        setTextColor(if (glowStrength > 0f) Hud.lighten(color, CORE_LIGHTEN * glowStrength + CORE_BOOST * glowBoost) else color)
        // The widest glow is also set the normal way, so the text is not clipped before the glow ends.
        val wide = GLOW_PASSES[0]
        if (glowStrength > 0f) {
            setShadowLayer(textSize * wide[0] * spread(), 0f, 0f, Hud.withAlpha(glow ?: color, alphaOf(wide)))
        } else {
            setShadowLayer(0f, 0f, 0f, 0)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (glowStrength <= 0f) {
            super.onDraw(canvas)
            return
        }
        val glow = glowColor ?: laserColor
        val size = textSize
        val spread = spread()
        for (pass in GLOW_PASSES) {
            paint.setShadowLayer(size * pass[0] * spread, 0f, 0f, Hud.withAlpha(glow, alphaOf(pass)))
            super.onDraw(canvas)
        }
        paint.clearShadowLayer()
        super.onDraw(canvas)
    }

    private fun spread(): Float = 1f + GLOW_SPREAD * glowBoost

    private fun alphaOf(pass: FloatArray): Float = minOf(1f, pass[1] * glowStrength + GLOW_LIFT * glowBoost)

    private companion object {
        const val CORE_LIGHTEN = 0.35f
        const val CORE_BOOST = 0.2f
        const val GLOW_SPREAD = 0.8f
        const val GLOW_LIFT = 0.25f

        /** Each glow pass: blur size as a share of the text size, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(0.6f, 0.55f),
            floatArrayOf(0.25f, 0.85f),
            floatArrayOf(0.08f, 1f),
        )
    }
}
