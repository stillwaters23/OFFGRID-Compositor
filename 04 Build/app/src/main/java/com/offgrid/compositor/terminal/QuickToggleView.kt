package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.PathInterpolator

/**
 * Camera and microphone controls in the top bar.
 *
 * Each one is a coded power button: a black disc, a glowing ring, and a white symbol.
 * The symbol says which control it is. Nothing is loaded from a picture.
 */
internal class QuickToggleView(
    context: Context,
    private val microphone: Boolean,
    initialOn: Boolean,
) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stamp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var on = initialOn
    private var slashProgress = if (initialOn) 0f else 1f
    private var slashAnimator: ValueAnimator? = null
    private var accent = NeonPalette.CYAN_GLOW
    private var halo = NeonPalette.ELECTRIC_PURPLE
    var onChanged: ((Boolean) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateDescription()
        setOnClickListener { setOn(!on, notify = true) }
    }

    fun applyAccent(edge: Int, haloColor: Int) {
        accent = edge
        halo = haloColor
        invalidate()
    }

    fun setOn(value: Boolean, notify: Boolean = false) {
        if (on == value && slashProgress == if (value) 0f else 1f) return
        on = value
        updateDescription()
        slashAnimator?.cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            slashProgress = if (value) 0f else 1f
            invalidate()
        } else {
            slashAnimator = ValueAnimator.ofFloat(slashProgress, if (value) 0f else 1f).apply {
                duration = 160L
                interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
                addUpdateListener {
                    slashProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        if (notify) onChanged?.invoke(value)
    }

    override fun onDetachedFromWindow() {
        slashAnimator?.cancel()
        slashAnimator = null
        super.onDetachedFromWindow()
    }

    private fun updateDescription() {
        contentDescription = if (microphone) {
            if (on) "Microphone on" else "Microphone off"
        } else {
            if (on) "Camera on" else "Camera off"
        }
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height)
        if (size <= 0) return
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        val layers = BioluminescentPowerIcon.layers(size, accent, halo, resources.displayMetrics.density)
        if (layers != null) {
            BioluminescentPowerIcon.paintLayers(canvas, left, top, layers, if (on) 1f else 0.28f, stamp)
        }
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = size * BioluminescentPowerIcon.DISC_SHARE
        disc.color = Hud.INK
        canvas.drawCircle(centerX, centerY, radius, disc)
        line.color = if (on) BioluminescentPowerIcon.hotCore(accent) else Hud.withAlpha(Hud.WHITE, 0.45f)
        line.strokeWidth = radius * 0.16f
        val unit = radius / 18f
        if (microphone) drawMicrophone(canvas, centerX, centerY, unit) else drawCamera(canvas, centerX, centerY, unit)
        if (slashProgress > 0f) {
            canvas.drawLine(
                centerX - 12f * unit,
                centerY + 12f * unit,
                centerX - 12f * unit + 24f * unit * slashProgress,
                centerY + 12f * unit - 24f * unit * slashProgress,
                line,
            )
        }
    }

    private fun drawMicrophone(canvas: Canvas, centerX: Float, centerY: Float, unit: Float) {
        canvas.drawRoundRect(
            centerX - 4f * unit,
            centerY - 10f * unit,
            centerX + 4f * unit,
            centerY + 4f * unit,
            4f * unit,
            4f * unit,
            line,
        )
        canvas.drawArc(
            centerX - 8f * unit,
            centerY - 3f * unit,
            centerX + 8f * unit,
            centerY + 10f * unit,
            0f,
            180f,
            false,
            line,
        )
        canvas.drawLine(centerX, centerY + 10f * unit, centerX, centerY + 14f * unit, line)
        canvas.drawLine(centerX - 5f * unit, centerY + 14f * unit, centerX + 5f * unit, centerY + 14f * unit, line)
    }

    private fun drawCamera(canvas: Canvas, centerX: Float, centerY: Float, unit: Float) {
        canvas.drawRoundRect(
            centerX - 11f * unit,
            centerY - 7f * unit,
            centerX + 11f * unit,
            centerY + 7f * unit,
            3f * unit,
            3f * unit,
            line,
        )
        canvas.drawCircle(centerX, centerY, 3.5f * unit, line)
    }
}
