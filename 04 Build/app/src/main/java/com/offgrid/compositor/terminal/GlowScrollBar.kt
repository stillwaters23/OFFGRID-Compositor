package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import android.animation.ValueAnimator

internal class GlowScrollBar(context: Context, private val scrollView: ScrollView) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26FFFFFF }
    private val bloom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = NeonPalette.CYAN_GLOW
        setShadowLayer(Hud.dp(context, 9f), 0f, 0f, NeonPalette.ELECTRIC_PURPLE)
    }
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = NeonPalette.BLACKLIGHT_WHITE }
    private val trackRect = RectF()
    private val thumbRect = RectF()
    private var dragging = false
    private var touchOffset = 0f
    private val touchWidth = Hud.dp(context, 24f)

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0f
        visibility = INVISIBLE
        scrollView.setOnScrollChangeListener { _, _, _, _, _ -> reveal() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = scrollView.getChildAt(0)?.height ?: 0
        if (height <= 0 || total <= height) return
        val trackWidth = Hud.dp(context, 3f)
        val thumbWidth = Hud.dp(context, 8f)
        val left = if (layoutDirection == LAYOUT_DIRECTION_RTL) 0f else width - thumbWidth
        trackRect.set(left + (thumbWidth - trackWidth) / 2f, 0f, left + (thumbWidth + trackWidth) / 2f, height.toFloat())
        val thumbHeight = PanelGeometry.scrollThumbHeight(height, total, Hud.dp(context, 32f))
        val top = PanelGeometry.scrollThumbTop(height, total, scrollView.scrollY, thumbHeight)
        thumbRect.set(left, top, left + thumbWidth, top + thumbHeight)
        canvas.drawRoundRect(trackRect, trackWidth, trackWidth, track)
        canvas.drawRoundRect(thumbRect, thumbWidth / 2f, thumbWidth / 2f, bloom)
        canvas.drawRoundRect(thumbRect, thumbWidth / 2f, thumbWidth / 2f, thumb)
    }

    private fun reveal() {
        animate().cancel()
        if (visibility != VISIBLE) visibility = VISIBLE
        if (ValueAnimator.areAnimatorsEnabled()) {
            animate().alpha(1f).setDuration(120L).start()
        } else {
            alpha = 1f
        }
        removeCallbacks(hideThumb)
        postDelayed(hideThumb, 900L)
        invalidate()
    }

    private val hideThumb = Runnable {
        if (ValueAnimator.areAnimatorsEnabled()) {
            animate().alpha(0f).setDuration(240L).withEndAction { visibility = INVISIBLE }.start()
        } else {
            alpha = 0f
            visibility = INVISIBLE
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val inZone = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            event.x <= touchWidth
        } else {
            event.x >= width - touchWidth
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!inZone) return false
                dragging = true
                touchOffset = event.y - thumbRect.top
                reveal()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                val childHeight = scrollView.getChildAt(0)?.height ?: height
                val thumbHeight = PanelGeometry.scrollThumbHeight(height, childHeight, Hud.dp(context, 32f))
                val trackTravel = (height - thumbHeight).coerceAtLeast(1f)
                val top = (event.y - touchOffset).coerceIn(0f, trackTravel)
                scrollView.scrollTo(0, ((childHeight - height) * top / trackTravel).toInt())
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                postDelayed(hideThumb, 900L)
                return true
            }
        }
        return false
    }
}
