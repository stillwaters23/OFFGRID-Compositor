package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.sqrt

/**
 * A title in Laser Jet Bright: a bright core with harsh layers of glow around it.
 * Used for the panel title, the small tag, folder headings, and section labels.
 * Titles are never typed letter by letter, so the whole title glows at once.
 */
internal class LaserTitleView(context: Context) : AppCompatTextView(context) {
    private var laserColor = Hud.WHITE
    private var glowColor: Int? = null
    private var glowStrength = 1f

    /**
     * [color] is the laser color. The core burns brighter than it. [glow] colors the glow,
     * or null to glow in the laser color. [strength] 0 is plain text.
     */
    fun setLaser(color: Int, glow: Int?, strength: Float) {
        laserColor = color
        glowColor = glow
        // The glow climbs fast at low strengths, so the default already burns harsh and bright.
        glowStrength = sqrt(strength.coerceIn(0f, 1f))
        setTextColor(if (glowStrength > 0f) Hud.lighten(color, CORE_LIGHTEN * glowStrength) else color)
        // The widest glow is also set the normal way, so the text is not clipped before the glow ends.
        val wide = GLOW_PASSES[0]
        if (glowStrength > 0f) {
            setShadowLayer(textSize * wide[0], 0f, 0f, Hud.withAlpha(glow ?: color, wide[1] * glowStrength))
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
        for (pass in GLOW_PASSES) {
            paint.setShadowLayer(size * pass[0], 0f, 0f, Hud.withAlpha(glow, pass[1] * glowStrength))
            super.onDraw(canvas)
        }
        paint.clearShadowLayer()
        super.onDraw(canvas)
    }

    private companion object {
        const val CORE_LIGHTEN = 0.35f

        /** Each glow pass: blur size as a share of the text size, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(0.6f, 0.55f),
            floatArrayOf(0.25f, 0.85f),
            floatArrayOf(0.08f, 1f),
        )
    }
}
