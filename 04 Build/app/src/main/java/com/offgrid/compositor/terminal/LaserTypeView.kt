package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.text.Layout
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd

/**
 * Laser typewriter text, per 03 Design/References/Grok Bot Laser Typewriter UI Spec.md.
 *
 * The whole text is laid out once, so words never jump to a new line while typing.
 * Only the letters typed so far are drawn. Nothing is drawn for the letters still to come,
 * so no ghost text shows behind the laser. A bright block cursor sits at the end. It blinks
 * in hard steps, not a fade. Tap the text to finish typing at once.
 */
internal class LaserTypeView(context: Context) : AppCompatTextView(context) {
    /** Letters per second. */
    var speed = 80f

    /** False shows the full text at once with a still cursor (reduce motion). */
    var animateTyping = true

    /** The cursor's glow color. Its middle burns near white. */
    var cursorColor = Hud.HOLO_LASER
        set(value) {
            field = value
            invalidate()
        }

    /** 0 to 1. How far the cursor's light spreads. */
    var cursorGlow = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var shownCount = 0
    private var typing: ValueAnimator? = null
    private var caretOn = true
    private var blinking = false
    private var blinkUntil = 0L
    private var attachedAndShown = false
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metrics = Paint.FontMetrics()
    private var glowRadius = 0f

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
        setLineSpacing(0f, 1.12f)
        setOnClickListener { finishNow() }
    }

    /** Types [text] letter by letter. */
    fun type(text: CharSequence) {
        val old = typing
        typing = null
        old?.cancel()
        setText(text, BufferType.SPANNABLE)
        val reduceMotion = !animateTyping || !ValueAnimator.areAnimatorsEnabled()
        if (reduceMotion || text.isEmpty()) {
            shownCount = text.length
            stopBlink(caretStays = true)
            return
        }
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
        val full = text
        var end = count.coerceIn(0, full.length)
        if (end in 1 until full.length && Character.isHighSurrogate(full[end - 1])) {
            end++
        }
        if (end == shownCount) return
        shownCount = end
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

    private fun updateGlowPaint() {
        val radius = (textSize * 0.55f).coerceAtLeast(1f)
        if (radius != glowRadius) {
            glowRadius = radius
            glowPaint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
    }

    /**
     * Draws only the typed letters. Lines above the cursor are drawn whole.
     * The cursor's line is drawn up to the cursor. Everything after it is never drawn.
     */
    override fun onDraw(canvas: Canvas) {
        val textLayout = layout ?: return
        val total = text.length
        val shown = shownCount.coerceIn(0, total)
        paint.color = currentTextColor
        paint.drawableState = drawableState
        canvas.save()
        canvas.translate(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
        if (shown >= total) {
            textLayout.draw(canvas)
        } else if (shown > 0) {
            drawTyped(canvas, textLayout, shown)
        }
        canvas.restore()
        if (caretOn) drawCursor(canvas, textLayout, shown)
    }

    private fun drawTyped(canvas: Canvas, textLayout: Layout, shown: Int) {
        val line = textLayout.getLineForOffset(shown)
        val lineTop = textLayout.getLineTop(line).toFloat()
        val right = width.toFloat()
        if (line > 0) {
            canvas.save()
            canvas.clipRect(-right, -right, right * 2f, lineTop)
            textLayout.draw(canvas)
            canvas.restore()
        }
        val cursorX = textLayout.getPrimaryHorizontal(shown)
        if (cursorX > 0f) {
            canvas.save()
            canvas.clipRect(-right, lineTop, cursorX, textLayout.getLineBottom(line).toFloat())
            textLayout.draw(canvas)
            canvas.restore()
        }
    }

    /** A laser-jet bright block: a wide soft glow, a bright body, and a white-hot core. */
    private fun drawCursor(canvas: Canvas, textLayout: Layout, shown: Int) {
        val line = textLayout.getLineForOffset(shown)
        val x = textLayout.getPrimaryHorizontal(shown)
        val baseline = textLayout.getLineBaseline(line).toFloat()
        paint.getFontMetrics(metrics)
        val left = totalPaddingLeft + x
        val top = totalPaddingTop + baseline + metrics.ascent
        val bottom = totalPaddingTop + baseline + metrics.descent
        val width = paint.measureText(CARET)
        updateGlowPaint()
        if (cursorGlow > 0f) {
            glowPaint.color = Hud.withAlpha(cursorColor, 0.95f * cursorGlow)
            val spread = glowRadius * 0.35f
            canvas.drawRect(left - spread, top - spread, left + width + spread, bottom + spread, glowPaint)
        }
        bodyPaint.color = Hud.lighten(cursorColor, 0.35f)
        canvas.drawRect(left, top, left + width, bottom, bodyPaint)
        corePaint.color = Hud.withAlpha(Hud.WHITE, 0.9f)
        val inset = width * 0.28f
        val height = bottom - top
        canvas.drawRect(left + inset, top + height * 0.14f, left + width - inset, bottom - height * 0.14f, corePaint)
    }

    private companion object {
        const val CARET = "█"
        const val BLINK_ON_MS = 530L
        const val BLINK_OFF_MS = 450L
        const val IDLE_BLINK_MS = 1_800L
    }
}
