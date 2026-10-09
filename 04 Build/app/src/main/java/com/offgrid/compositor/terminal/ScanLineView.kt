package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * A soft glowing line that slowly sweeps down the panel, then waits, then sweeps again.
 * It takes the edge color. It never takes taps.
 */
internal class ScanLineView(context: Context) : View(context), Styled {
    private val band = Paint()
    private var sweep: ValueAnimator? = null
    private var shown = false
    private var color = Hud.HOLO_CYAN

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        if (look.edge != color) {
            color = look.edge
            buildShader(width, height)
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildShader(w, h)
    }

    private fun buildShader(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val clear = Hud.withAlpha(color, 0f)
        val vertical = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(clear, Hud.withAlpha(color, 0.10f), Hud.withAlpha(color, 0.45f), Hud.withAlpha(color, 0.10f), clear),
            floatArrayOf(0f, 0.38f, 0.5f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        val sides = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(0x00FFFFFF, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0f, 0.15f, 0.85f, 1f),
            Shader.TileMode.CLAMP,
        )
        band.shader = ComposeShader(vertical, sides, PorterDuff.Mode.DST_IN)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), band)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shown = isVisible
        if (isVisible) start() else stop()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    private fun start() {
        if (sweep != null || !shown) return
        val travel = (parent as? View)?.height ?: 0
        if (travel == 0 || height == 0) {
            post { start() }
            return
        }
        // The line runs past the bottom on purpose. That gap is the rest between sweeps.
        val from = -height.toFloat()
        val to = travel * 1.45f
        sweep = ValueAnimator.ofFloat(from, to).apply {
            duration = SWEEP_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { translationY = it.animatedValue as Float }
            start()
        }
    }

    private fun stop() {
        sweep?.cancel()
        sweep = null
    }

    private companion object {
        const val SWEEP_MS = 6_500L
    }
}
