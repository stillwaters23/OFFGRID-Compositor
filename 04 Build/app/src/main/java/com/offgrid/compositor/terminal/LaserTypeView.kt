package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd

/**
 * Laser typewriter text, per 03 Design/References/Grok Bot Laser Typewriter UI Spec.md.
 *
 * The whole text is laid out once and the letters not typed yet are see-through,
 * so words never jump to a new line while typing. A block caret sits at the end.
 * It blinks in hard steps, not a fade. Tap the text to finish typing at once.
 */
internal class LaserTypeView(context: Context) : AppCompatTextView(context) {
    /** Letters per second. */
    var speed = 80f

    /** False shows the full text at once with a still caret (reduce motion). */
    var animateTyping = true

    private var hidden = ForegroundColorSpan(Color.TRANSPARENT)
    private var shownCount = 0
    private var typing: ValueAnimator? = null
    private var caretOn = true
    private var blinking = false
    private var blinkUntil = 0L
    private var attachedAndShown = false
    private val caretPaint = Paint().apply { color = Hud.TEXT }
    private val metrics = Paint.FontMetrics()

    private val blink = object : Runnable {
        override fun run() {
            if (!blinking) return
            val now = SystemClock.uptimeMillis()
            if (typing == null && now >= blinkUntil) {
                blinking = false
                caretOn = true
                invalidate()
                return
            }
            caretOn = !caretOn
            invalidate()
            postDelayed(this, if (caretOn) BLINK_ON_MS else BLINK_OFF_MS)
        }
    }

    init {
        typeface = Hud.mono(context)
        setTextColor(Hud.TEXT)
        setLineSpacing(0f, 1.12f)
        // An opaque shadow color follows each letter's own alpha, so hidden letters cast no shadow.
        setShadowLayer(Hud.dp(context, 3f), 0f, 0f, Hud.INK)
        setOnClickListener { finishNow() }
    }

    fun applySystemTextColor() {
        setTextColor(Hud.TEXT)
        caretPaint.color = Hud.TEXT
        invalidate()
    }

    /** Types [text] letter by letter. */
    fun type(text: CharSequence) {
        val old = typing
        typing = null
        old?.cancel()
        val reduceMotion = !animateTyping || !ValueAnimator.areAnimatorsEnabled()
        if (reduceMotion || text.isEmpty()) {
            setText(text, BufferType.SPANNABLE)
            shownCount = text.length
            stopBlink(caretStays = true)
            return
        }
        val start = SpannableString(text)
        hidden = ForegroundColorSpan(Color.TRANSPARENT)
        start.setSpan(hidden, 0, start.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setText(start, BufferType.SPANNABLE)
        shownCount = 0
        val total = text.length
        typing = ValueAnimator.ofInt(0, total).apply {
            duration = (total / speed.coerceAtLeast(1f) * 1000f).toLong().coerceAtLeast(1L)
            interpolator = LinearInterpolator()
            addUpdateListener { reveal(it.animatedValue as Int) }
            doOnEnd {
                if (typing === it) {
                    typing = null
                    reveal(total)
                    blinkUntil = SystemClock.uptimeMillis() + IDLE_BLINK_MS
                }
            }
            start()
        }
        startBlink()
    }

    /** Shows all the text right away. */
    fun finishNow() {
        val running = typing ?: return
        typing = null
        running.cancel()
        reveal(text.length)
        blinkUntil = SystemClock.uptimeMillis() + IDLE_BLINK_MS
    }

    private fun reveal(count: Int) {
        val spannable = text as? Spannable ?: return
        var end = count.coerceIn(0, spannable.length)
        if (end in 1 until spannable.length && Character.isHighSurrogate(spannable[end - 1])) {
            end++
        }
        if (end == shownCount && end != spannable.length) return
        shownCount = end
        if (end >= spannable.length) {
            spannable.removeSpan(hidden)
        } else {
            spannable.setSpan(hidden, end, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        invalidate()
    }

    private fun startBlink() {
        removeCallbacks(blink)
        blinking = true
        caretOn = true
        invalidate()
        if (attachedAndShown) {
            postDelayed(blink, BLINK_ON_MS)
        }
    }

    private fun stopBlink(caretStays: Boolean) {
        removeCallbacks(blink)
        blinking = false
        caretOn = caretStays
        invalidate()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        attachedAndShown = isVisible
        if (!isVisible) {
            finishNow()
            stopBlink(caretStays = true)
        } else if (typing != null) {
            startBlink()
        }
    }

    override fun onDetachedFromWindow() {
        val running = typing
        typing = null
        running?.cancel()
        removeCallbacks(blink)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!caretOn) return
        val textLayout = layout ?: return
        val offset = shownCount.coerceIn(0, text.length)
        val line = textLayout.getLineForOffset(offset)
        val x = textLayout.getPrimaryHorizontal(offset)
        val baseline = textLayout.getLineBaseline(line).toFloat()
        paint.getFontMetrics(metrics)
        val left = totalPaddingLeft + x
        val top = totalPaddingTop + baseline + metrics.ascent
        val bottom = totalPaddingTop + baseline + metrics.descent
        val width = paint.measureText(CARET)
        canvas.drawRect(left, top, left + width, bottom, caretPaint)
    }

    private companion object {
        const val CARET = "█"
        const val BLINK_ON_MS = 530L
        const val BLINK_OFF_MS = 450L
        const val IDLE_BLINK_MS = 1_800L
    }
}
