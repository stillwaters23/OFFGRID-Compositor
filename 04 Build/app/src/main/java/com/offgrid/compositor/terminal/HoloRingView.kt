package com.offgrid.compositor.terminal

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * The soft rotating ring around the active folder.
 * The ring is drawn once. Only the view turns, so it is cheap to animate.
 * [special] rings use the special folder color (gold in Holo Classic).
 */
internal class HoloRingView(context: Context, private val special: Boolean = false) : View(context), Styled {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dashes = DashPathEffect(floatArrayOf(Hud.dp(context, 6f), Hud.dp(context, 5f)), 0f)
    private val oval = RectF()
    private var spin: ObjectAnimator? = null
    private var shown = false
    private var ringColor = Hud.HOLO_CYAN
    private var arcColor = Hud.HOLO_GOLD

    var spinning = false
        set(value) {
            field = value
            refreshSpin()
        }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0.85f
    }

    override fun applyLook(look: PanelLook) {
        ringColor = if (special) look.specialFolder else look.folder
        arcColor = look.decor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - Hud.dp(context, 3f)
        if (radius <= 0f) return
        stroke.pathEffect = null
        stroke.strokeWidth = Hud.dp(context, 5f)
        stroke.color = Hud.withAlpha(ringColor, 0.10f)
        canvas.drawCircle(cx, cy, radius, stroke)
        stroke.pathEffect = dashes
        stroke.strokeWidth = Hud.dp(context, 1.4f)
        stroke.color = Hud.withAlpha(ringColor, 0.75f)
        canvas.drawCircle(cx, cy, radius, stroke)
        stroke.pathEffect = null
        stroke.strokeWidth = Hud.dp(context, 2.4f)
        stroke.color = Hud.withAlpha(arcColor, 0.9f)
        val arcRadius = radius - Hud.dp(context, 4f)
        oval.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)
        canvas.drawArc(oval, 20f, 48f, false, stroke)
        canvas.drawArc(oval, 200f, 48f, false, stroke)
        stroke.strokeWidth = Hud.dp(context, 0.8f)
        stroke.color = Hud.withAlpha(ringColor, 0.3f)
        canvas.drawCircle(cx, cy, radius - Hud.dp(context, 8f), stroke)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shown = isVisible
        refreshSpin()
    }

    override fun onDetachedFromWindow() {
        spin?.cancel()
        spin = null
        super.onDetachedFromWindow()
    }

    private fun refreshSpin() {
        val want = spinning && shown && ValueAnimator.areAnimatorsEnabled() && GlowClock.isLive
        if (want) {
            if (spin == null) {
                spin = ObjectAnimator.ofFloat(this, ROTATION, rotation, rotation + 360f).apply {
                    duration = TURN_MS
                    interpolator = LinearInterpolator()
                    repeatCount = 0
                    start()
                }
            }
        } else {
            spin?.cancel()
            spin = null
        }
    }

    private companion object {
        const val TURN_MS = 9_000L
    }
}
