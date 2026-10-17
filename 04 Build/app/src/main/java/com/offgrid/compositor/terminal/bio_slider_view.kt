package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * OFFGRID's own slider (Phase 0.2 placeholder for Phase 2.4).
 *
 * Royalty approved one slider class for the whole app, and this is it. The CI slider guard bans
 * the stock Android and Material sliders and allows only this name. Phase 2.4 gives it its real
 * look, touch handling, detents, haptics, and accessibility range info.
 *
 * Today it only keeps a value from 0 to 100 and draws a hairline track with a dot, so the class
 * exists for the guard and compiles. Nothing in the panel tree uses it yet. Every number in the
 * panel is still typed.
 */
internal class BioSliderView(context: Context) : View(context) {
    /** The value from 0 to 100. Setting it redraws the view. */
    var value = 0
        set(next) {
            val clamped = next.coerceIn(0, MAX_VALUE)
            if (clamped == field) return
            field = clamped
            invalidate()
            onValueChanged?.invoke(clamped)
        }

    var onValueChanged: ((Int) -> Unit)? = null

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = TRACK_COLOR
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = DOT_COLOR
    }

    override fun onDraw(canvas: Canvas) {
        val midY = height / 2f
        val inset = paddingStart.toFloat() + DOT_RADIUS
        val span = (width - paddingEnd - DOT_RADIUS - inset).coerceAtLeast(0f)
        canvas.drawLine(inset, midY, inset + span, midY, track)
        canvas.drawCircle(inset + span * value / MAX_VALUE, midY, DOT_RADIUS, dot)
    }

    private companion object {
        const val MAX_VALUE = 100
        const val DOT_RADIUS = 6f
        const val TRACK_COLOR = 0x66FFFFFF
        const val DOT_COLOR = 0xFFFFFFFF.toInt()
    }
}
