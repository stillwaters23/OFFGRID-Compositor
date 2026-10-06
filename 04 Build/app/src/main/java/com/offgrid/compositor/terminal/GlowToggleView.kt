package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Switch
import com.offgrid.compositor.R

/**
 * The one toggle used everywhere in the panel, so all toggles look the same.
 * The art is the dark red OFF and green ON toggle from 03 Design/References,
 * drawn exactly as it is.
 *
 * Step 2.2 tried a liquid swirl inside the ball. It did not look truly inside the art,
 * so, as Royalty asked, the swirl was removed and the original pictures are kept.
 * The only motion is a short fade between OFF and ON, and none when the phone asks
 * for less motion.
 */
internal class GlowToggleView(context: Context) : View(context), Styled {
    var isOn = false
        private set

    var onToggled: ((Boolean) -> Unit)? = null

    private var mix = 0f
    private var fade: ValueAnimator? = null
    private var tick = true
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val target = RectF()

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener { setOn(!isOn, fromUser = true) }
        stateDescription = context.getString(R.string.terminal_off)
    }

    fun setOn(on: Boolean, fromUser: Boolean = false) {
        if (on == isOn && fromUser) return
        isOn = on
        stateDescription = context.getString(if (on) R.string.terminal_on else R.string.terminal_off)
        val end = if (on) 1f else 0f
        fade?.cancel()
        if (fromUser && ValueAnimator.areAnimatorsEnabled()) {
            fade = ValueAnimator.ofFloat(mix, end).apply {
                duration = FADE_MS
                addUpdateListener {
                    mix = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            mix = end
            invalidate()
        }
        if (fromUser) {
            if (tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onToggled?.invoke(on)
        }
    }

    /** Only the tick follows the look. The pictures never change. */
    override fun applyLook(look: PanelLook) {
        tick = look.tick
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(Hud.dpi(context, WIDTH_DP), widthMeasureSpec),
            resolveSize(Hud.dpi(context, HEIGHT_DP), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val art = Art.load(resources)
        val scale = minOf(width / art.off.width.toFloat(), height / art.off.height.toFloat())
        val drawW = art.off.width * scale
        val drawH = art.off.height * scale
        target.set((width - drawW) / 2f, (height - drawH) / 2f, (width + drawW) / 2f, (height + drawH) / 2f)
        if (mix < 1f) {
            paint.alpha = ((1f - mix) * 255f).toInt()
            canvas.drawBitmap(art.off, null, target, paint)
        }
        if (mix > 0f) {
            paint.alpha = (mix * 255f).toInt()
            canvas.drawBitmap(art.on, null, target, paint)
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Switch::class.java.name
        info.isCheckable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            info.setChecked(
                if (isOn) AccessibilityNodeInfo.CHECKED_STATE_TRUE else AccessibilityNodeInfo.CHECKED_STATE_FALSE,
            )
        } else {
            @Suppress("DEPRECATION")
            info.isChecked = isOn
        }
    }

    override fun onDetachedFromWindow() {
        fade?.cancel()
        fade = null
        super.onDetachedFromWindow()
    }

    private class Art(val on: Bitmap, val off: Bitmap) {
        companion object {
            private var loaded: Art? = null

            fun load(resources: Resources): Art {
                loaded?.let { return it }
                val art = Art(
                    on = BitmapFactory.decodeResource(resources, R.drawable.hud_toggle_on),
                    off = BitmapFactory.decodeResource(resources, R.drawable.hud_toggle_off),
                )
                loaded = art
                return art
            }
        }
    }

    private companion object {
        const val WIDTH_DP = 96f
        const val HEIGHT_DP = 48f
        const val FADE_MS = 180L
    }
}
