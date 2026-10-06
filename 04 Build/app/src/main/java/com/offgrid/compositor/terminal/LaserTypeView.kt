package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd
import kotlin.math.sqrt

/**
 * Laser typewriter text, per 03 Design/References/Grok Bot Laser Typewriter UI Spec.md.
 *
 * The whole text is laid out once, so words never jump to a new line while typing.
 * Only the letters typed so far are drawn. Nothing is drawn for the letters still to come,
 * so no ghost text shows behind the laser. Tap the text to finish typing at once.
 *
 * Laser Jet Bright (Step 2.2): typed letters get a bright core and layers of glow.
 * The glow is built only from letters already typed, one line at a time, so the glow
 * of a letter still to come can never show either. Finished lines are kept in a picture,
 * so only the line being typed is redrawn each frame.
 *
 * The cursor (Step 2.3) is one solid, opaque block in one color, white at the start.
 * Its soft glow is optional, off at the start, and stays off when the phone asks for
 * less motion. It blinks in hard steps, not a fade.
 */
internal class LaserTypeView(context: Context) : AppCompatTextView(context) {
    /** Letters per second. */
    var speed = 80f

    /** False shows the full text at once with a still cursor (reduce motion). */
    var animateTyping = true

    /** The cursor's one solid color. */
    var cursorColor = Hud.WHITE
        set(value) {
            field = value
            invalidate()
        }

    /** 0 to 1. How strong the Laser Jet Bright glow around the typed letters is. 0 is plain text. */
    var glowStrength = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** 0 to 1. The optional soft glow around the cursor. 0 is a solid block only. */
    var cursorGlow = 0f
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
    private val cursorPaint = Paint()
    private val cursorGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cursorGlowRadius = 0f
    private val metrics = Paint.FontMetrics()

    /** The paint for laser lines. Its glow changes for each pass. */
    private val laserPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    /** Finished lines, already drawn with their glow. */
    private var glowCache: Bitmap? = null
    private var glowCanvas: Canvas? = null
    private var cachedLines = 0
    private var cacheKey = 0
    private val lineLayouts = SparseArray<StaticLayout>()

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
        dropGlowCache()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val textLayout = layout ?: return
        val total = text.length
        val shown = shownCount.coerceIn(0, total)
        paint.color = currentTextColor
        paint.drawableState = drawableState
        if (glowStrength > 0f) {
            drawLaser(canvas, textLayout, shown, total)
        } else {
            canvas.save()
            canvas.translate(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
            if (shown >= total) {
                textLayout.draw(canvas)
            } else if (shown > 0) {
                drawTyped(canvas, textLayout, shown)
            }
            canvas.restore()
        }
        if (caretOn) drawCursor(canvas, textLayout, shown)
    }

    /**
     * Plain text, with no glow. Lines above the cursor are drawn whole.
     * The cursor's line is drawn up to the cursor. Everything after it is never drawn.
     */
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

    /**
     * Laser Jet Bright text. Each typed line is laid out on its own, from typed letters only,
     * at the same place the full text puts it. Finished lines come from the picture.
     */
    private fun drawLaser(canvas: Canvas, textLayout: Layout, shown: Int, total: Int) {
        if (shown <= 0) return
        laserPaint.set(paint)
        val cursorLine = textLayout.getLineForOffset(shown)
        val finished = if (shown >= total) textLayout.lineCount else cursorLine
        val cache = glowCacheFor(textLayout)
        val cacheCanvas = glowCanvas
        if (cache != null && cacheCanvas != null) {
            while (cachedLines < finished) {
                drawLine(cacheCanvas, textLayout, cachedLines, textLayout.getLineEnd(cachedLines))
                cachedLines++
            }
            canvas.drawBitmap(cache, 0f, 0f, null)
        } else {
            for (line in 0 until finished) drawLine(canvas, textLayout, line, textLayout.getLineEnd(line))
        }
        if (shown < total && shown > textLayout.getLineStart(cursorLine)) {
            drawLine(canvas, textLayout, cursorLine, shown)
        }
    }

    /** Draws letters [start of line, end) of one line, with the glow passes and then the bright core. */
    private fun drawLine(target: Canvas, textLayout: Layout, line: Int, end: Int) {
        val start = textLayout.getLineStart(line)
        if (end <= start) return
        val single = lineLayout(textLayout, line, start, end)
        val dx = totalPaddingLeft + textLayout.getLineLeft(line)
        val dy = totalPaddingTop + textLayout.getLineBaseline(line) - single.getLineBaseline(0).toFloat()
        val color = currentTextColor
        val size = laserPaint.textSize
        // The glow climbs fast at low strengths, so the default already burns harsh and bright.
        val power = sqrt(glowStrength)
        target.save()
        target.translate(dx, dy)
        for (pass in GLOW_PASSES) {
            laserPaint.color = color
            laserPaint.setShadowLayer(size * pass[0], 0f, 0f, Hud.withAlpha(color, pass[1] * power))
            single.draw(target)
        }
        laserPaint.clearShadowLayer()
        laserPaint.color = Hud.lighten(color, CORE_LIGHTEN * power)
        single.draw(target)
        target.restore()
    }

    /** A layout of just the typed part of one line. Finished lines are kept. */
    private fun lineLayout(textLayout: Layout, line: Int, start: Int, end: Int): StaticLayout {
        val finished = end == textLayout.getLineEnd(line)
        if (finished) lineLayouts.get(line)?.let { return it }
        val built = StaticLayout.Builder.obtain(text, start, end, laserPaint, LINE_WIDTH)
            .setIncludePad(false)
            .build()
        if (finished) lineLayouts.put(line, built)
        return built
    }

    /** The picture of finished lines. It starts over when the text, its size, or its colors change. */
    private fun glowCacheFor(textLayout: Layout): Bitmap? {
        if (width <= 0 || height <= 0) return null
        var key = System.identityHashCode(text)
        key = key * 31 + System.identityHashCode(textLayout)
        key = key * 31 + paint.textSize.toBits()
        key = key * 31 + System.identityHashCode(paint.typeface)
        key = key * 31 + currentTextColor
        key = key * 31 + glowStrength.toBits()
        key = key * 31 + (PanelLook.current?.label ?: 0)
        key = key * 31 + width
        key = key * 31 + height
        var cache = glowCache
        if (cache == null || cache.width != width || cache.height != height) {
            cache?.recycle()
            cache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            glowCache = cache
            glowCanvas = Canvas(cache)
            cacheKey = key
            cachedLines = 0
            lineLayouts.clear()
        } else if (key != cacheKey) {
            cache.eraseColor(0)
            cacheKey = key
            cachedLines = 0
            lineLayouts.clear()
        }
        return cache
    }

    private fun dropGlowCache() {
        glowCache?.recycle()
        glowCache = null
        glowCanvas = null
        cachedLines = 0
        lineLayouts.clear()
    }

    /** One solid block in one color. A soft glow is drawn around it only when Cursor glow is above 0. */
    private fun drawCursor(canvas: Canvas, textLayout: Layout, shown: Int) {
        val line = textLayout.getLineForOffset(shown)
        val x = textLayout.getPrimaryHorizontal(shown)
        val baseline = textLayout.getLineBaseline(line).toFloat()
        paint.getFontMetrics(metrics)
        val left = totalPaddingLeft + x
        val top = totalPaddingTop + baseline + metrics.ascent
        val bottom = totalPaddingTop + baseline + metrics.descent
        val blockWidth = paint.measureText(CARET)
        if (cursorGlow > 0f && ValueAnimator.areAnimatorsEnabled()) {
            val radius = (textSize * (0.15f + 0.45f * cursorGlow)).coerceAtLeast(1f)
            if (radius != cursorGlowRadius) {
                cursorGlowRadius = radius
                cursorGlowPaint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.OUTER)
            }
            cursorGlowPaint.color = Hud.withAlpha(cursorColor, 0.9f * cursorGlow)
            canvas.drawRect(left, top, left + blockWidth, bottom, cursorGlowPaint)
        }
        cursorPaint.color = cursorColor or OPAQUE
        canvas.drawRect(left, top, left + blockWidth, bottom, cursorPaint)
    }

    private companion object {
        const val CARET = "█"
        const val BLINK_ON_MS = 530L
        const val BLINK_OFF_MS = 450L
        const val IDLE_BLINK_MS = 1_800L
        const val OPAQUE = 0xFF000000.toInt()
        const val LINE_WIDTH = 100_000

        /** How much brighter than the text color the core of each letter burns. */
        const val CORE_LIGHTEN = 0.35f

        /** Each glow pass: blur size as a share of the text size, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(0.55f, 0.45f),
            floatArrayOf(0.22f, 0.75f),
            floatArrayOf(0.07f, 0.95f),
        )
    }
}
