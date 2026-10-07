package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * The center guide (Step 2.2): a soft cross that marks the middle of the screen while
 * the panel is dragged. A line burns brighter when the panel clicks into the middle on it.
 * It sits under the panel, covers the whole screen, and never takes taps.
 * With less motion asked for, it shows and hides at once with no fade.
 */
class SnapGuideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = SOFT_DP * density
        maskFilter = BlurMaskFilter(BLUR_DP * density, BlurMaskFilter.Blur.NORMAL)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = LINE_DP * density
    }
    private var color = Hud.HOLO_CYAN
    private var across: LinearGradient? = null
    private var down: LinearGradient? = null
    private var shown = 0f
    private var snappedX = false
    private var snappedY = false
    private var fade: ValueAnimator? = null

    /** The middle the panel snaps to. Until the panel says, it is the middle of this view. */
    private var middleX = Float.NaN
    private var middleY = Float.NaN

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = INVISIBLE
    }

    internal fun applyLook(look: PanelLook) {
        if (look.edge != color) {
            color = look.edge
            buildShaders(width, height)
            invalidate()
        }
    }

    /**
     * The middle of the screen between the status bar and the navigation bar, where the
     * panel snaps (Step 2.4). The cross is drawn there.
     */
    fun setMiddle(x: Float, y: Float) {
        if (x == middleX && y == middleY) return
        middleX = x
        middleY = y
        buildShaders(width, height)
        invalidate()
    }

    private fun centerX(): Float = if (middleX.isNaN()) width / 2f else middleX

    private fun centerY(): Float = if (middleY.isNaN()) height / 2f else middleY

    /** Fades the cross in. Called when a drag starts. */
    fun showGuides() {
        snappedX = false
        snappedY = false
        fadeTo(1f)
    }

    /** Fades the cross out. Called when the drag ends. */
    fun hideGuides() {
        fadeTo(0f)
    }

    /** [x] is true while the panel sits on the up and down line, [y] while on the side to side line. */
    fun setSnapped(x: Boolean, y: Boolean) {
        if (x == snappedX && y == snappedY) return
        snappedX = x
        snappedY = y
        invalidate()
    }

    private fun fadeTo(target: Float) {
        fade?.cancel()
        fade = null
        if (!ValueAnimator.areAnimatorsEnabled()) {
            setShown(target)
            return
        }
        fade = ValueAnimator.ofFloat(shown, target).apply {
            duration = FADE_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { setShown(it.animatedValue as Float) }
            start()
        }
    }

    private fun setShown(value: Float) {
        shown = value
        visibility = if (value > 0f) VISIBLE else INVISIBLE
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildShaders(w, h)
    }

    /** Each line is brightest in the middle and fades away toward the screen edges. */
    private fun buildShaders(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val clear = Hud.withAlpha(color, 0f)
        val stops = floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f)
        val colors = intArrayOf(clear, Hud.withAlpha(color, 0.55f), color, Hud.withAlpha(color, 0.55f), clear)
        // Brightest at the middle, wherever the middle is.
        val x = centerX()
        val y = centerY()
        across = LinearGradient(x - w / 2f, 0f, x + w / 2f, 0f, colors, stops, Shader.TileMode.CLAMP)
        down = LinearGradient(0f, y - h / 2f, 0f, y + h / 2f, colors, stops, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        if (shown <= 0f) return
        val centerX = centerX()
        val centerY = centerY()
        drawLine(canvas, down, centerX, 0f, centerX, height.toFloat(), snappedX)
        drawLine(canvas, across, 0f, centerY, width.toFloat(), centerY, snappedY)
        // A small ring marks the exact middle.
        val ring = RING_DP * density
        line.shader = null
        line.color = Hud.withAlpha(color, 0.8f * shown)
        canvas.drawCircle(centerX, centerY, ring, line)
        soft.shader = null
        soft.color = Hud.withAlpha(color, 0.5f * shown)
        canvas.drawCircle(centerX, centerY, ring, soft)
    }

    private fun drawLine(
        canvas: Canvas,
        shader: Shader?,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        snapped: Boolean,
    ) {
        val strength = shown * if (snapped) 1f else IDLE_SHARE
        soft.shader = shader
        soft.alpha = (255f * strength * 0.8f).toInt()
        canvas.drawLine(startX, startY, endX, endY, soft)
        line.shader = shader
        line.alpha = (255f * strength).toInt()
        canvas.drawLine(startX, startY, endX, endY, line)
    }

    private companion object {
        const val SOFT_DP = 5f
        const val BLUR_DP = 4f
        const val LINE_DP = 1f
        const val RING_DP = 7f
        const val IDLE_SHARE = 0.4f
        const val FADE_MS = 180L
    }
}
