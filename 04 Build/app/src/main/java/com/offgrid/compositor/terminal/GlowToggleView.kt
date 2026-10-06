package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Switch
import com.offgrid.compositor.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * The one toggle used everywhere in the panel, so all toggles look the same.
 * The art is the dark red OFF and green ON toggle from 03 Design/References.
 *
 * Inside the orb, a liquid bioluminescent swirl moves: three soft blobs of light drift
 * and a bright swish turns. It stops when the phone asks for reduced motion.
 */
internal class GlowToggleView(context: Context) : View(context), Styled {
    var isOn = false
        private set

    var onToggled: ((Boolean) -> Unit)? = null

    private var mix = 0f
    private var fade: ValueAnimator? = null
    private val artPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val target = RectF()

    private var swirlOn = false
    private var swirlColor = 0
    private var swirlStrength = 0f
    private var swirlSpeed = 0.3f
    private var shown = false
    private var ticking = false
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val swishPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.14f
        blendMode = BlendMode.PLUS
    }

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
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onToggled?.invoke(on)
        }
    }

    override fun applyLook(look: PanelLook) {
        val color = look.toggleGlow
        if (color != swirlColor) {
            swirlColor = color
            blobPaint.shader = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(color, Hud.withAlpha(color, 0.35f), Hud.withAlpha(color, 0f)),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            swishPaint.shader = SweepGradient(
                0f, 0f,
                intArrayOf(Hud.withAlpha(color, 0f), Hud.withAlpha(color, 0.95f), Hud.withAlpha(color, 0f)),
                floatArrayOf(0f, 0.16f, 0.34f),
            )
        }
        swirlStrength = look.toggleGlowStrength
        swirlOn = look.toggleSwirl && swirlStrength > 0f
        swirlSpeed = look.swirlSpeed
        refreshClock()
        invalidate()
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
            artPaint.alpha = ((1f - mix) * 255f).toInt()
            canvas.drawBitmap(art.off, null, target, artPaint)
        }
        if (mix > 0f) {
            artPaint.alpha = (mix * 255f).toInt()
            canvas.drawBitmap(art.on, null, target, artPaint)
        }
        if (swirlOn) {
            val seconds = if (GlowClock.motionAllowed) GlowClock.seconds else 0f
            if (mix < 1f) drawSwirl(canvas, OFF_ORB, scale, 1f - mix, seconds)
            if (mix > 0f) drawSwirl(canvas, ON_ORB, scale, mix, seconds)
        }
    }

    /** The liquid light inside one orb. [orb] is x, y, and radius in the art's pixels. */
    private fun drawSwirl(canvas: Canvas, orb: FloatArray, scale: Float, weight: Float, seconds: Float) {
        val cx = target.left + orb[0] * scale
        val cy = target.top + orb[1] * scale
        val radius = orb[2] * scale * 0.9f
        val pace = 0.35f + swirlSpeed * 2.4f
        val time = seconds * pace
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius, radius)
        for (index in BLOBS.indices) {
            val blob = BLOBS[index]
            val angle = time * blob[0] + blob[1]
            val drift = 0.38f + 0.08f * sin(time * 1.7f + blob[1])
            canvas.save()
            canvas.translate(cos(angle) * drift, sin(angle) * drift)
            canvas.scale(blob[2], blob[2])
            blobPaint.alpha = (255f * swirlStrength * weight * blob[3]).toInt().coerceIn(0, 255)
            canvas.drawCircle(0f, 0f, 1f, blobPaint)
            canvas.restore()
        }
        canvas.rotate(time * 140f)
        swishPaint.alpha = (255f * swirlStrength * weight * 0.8f).toInt().coerceIn(0, 255)
        canvas.drawCircle(0f, 0f, 0.68f, swishPaint)
        canvas.restore()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shown = isVisible
        refreshClock()
    }

    override fun onDetachedFromWindow() {
        fade?.cancel()
        fade = null
        shown = false
        refreshClock()
        super.onDetachedFromWindow()
    }

    private fun refreshClock() {
        val want = shown && swirlOn && GlowClock.motionAllowed
        if (want && !ticking) {
            ticking = true
            GlowClock.join(this)
        } else if (!want && ticking) {
            ticking = false
            GlowClock.leave(this)
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

        /** Where the orb sits in the 472 by 224 art: x, y, radius. Measured from the reference images. */
        val OFF_ORB = floatArrayOf(122f, 114f, 82f)
        val ON_ORB = floatArrayOf(354f, 115f, 81f)

        /** Each blob: turn speed, start angle, size, brightness. */
        val BLOBS = arrayOf(
            floatArrayOf(1.3f, 0f, 0.62f, 0.85f),
            floatArrayOf(-0.9f, 2.1f, 0.55f, 0.7f),
            floatArrayOf(2.1f, 4.2f, 0.45f, 0.6f),
        )
    }
}
