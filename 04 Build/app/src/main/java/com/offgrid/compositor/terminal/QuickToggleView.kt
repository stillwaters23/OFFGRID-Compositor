package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.PathInterpolator

internal class QuickToggleView(
    context: Context,
    private val microphone: Boolean,
    initialOn: Boolean,
) : View(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Hud.dp(context, 2f)
    }
    private var on = initialOn
    private var slashProgress = if (initialOn) 0f else 1f
    private var slashAnimator: ValueAnimator? = null
    var onChanged: ((Boolean) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateDescription()
        updateAppearance()
        setOnClickListener { setOn(!on, notify = true) }
    }

    fun setOn(value: Boolean, notify: Boolean = false) {
        if (on == value && slashProgress == if (value) 0f else 1f) return
        on = value
        updateDescription()
        updateAppearance()
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

    private fun updateAppearance() {
        val color = if (on) 0xFF30FF7A.toInt() else 0xFFFF2D55.toInt()
        line.color = color
        line.setShadowLayer(if (on) Hud.dp(context, 5f) else 0f, 0f, 0f, color)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val scale = Hud.dp(context, 1f)
        if (microphone) {
            canvas.drawRoundRect(
                centerX - 5f * scale,
                centerY - 12f * scale,
                centerX + 5f * scale,
                centerY + 5f * scale,
                5f * scale,
                5f * scale,
                line,
            )
            canvas.drawArc(
                centerX - 10f * scale,
                centerY - 4f * scale,
                centerX + 10f * scale,
                centerY + 13f * scale,
                0f,
                180f,
                false,
                line,
            )
            canvas.drawLine(centerX, centerY + 13f * scale, centerX, centerY + 18f * scale, line)
            canvas.drawLine(centerX - 6f * scale, centerY + 18f * scale, centerX + 6f * scale, centerY + 18f * scale, line)
        } else {
            canvas.drawRoundRect(
                centerX - 15f * scale,
                centerY - 10f * scale,
                centerX + 15f * scale,
                centerY + 10f * scale,
                4f * scale,
                4f * scale,
                line,
            )
            canvas.drawCircle(centerX, centerY, 5f * scale, line)
            canvas.drawCircle(centerX + 10f * scale, centerY - 6f * scale, 1.4f * scale, line)
        }
        if (slashProgress > 0f) {
            canvas.drawLine(
                centerX - 15f * scale,
                centerY + 15f * scale,
                centerX - 15f * scale + 30f * scale * slashProgress,
                centerY + 15f * scale - 30f * scale * slashProgress,
                line,
            )
        }
    }
}
