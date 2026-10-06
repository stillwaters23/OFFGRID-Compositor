package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
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
import kotlin.math.sqrt

/**
 * The one toggle used everywhere in the panel, so all toggles look the same.
 * The art is the dark red OFF and green ON toggle from 03 Design/References.
 *
 * Liquid inside the glass (Step 2.2): the glowing plasma painted inside each ball slowly
 * flows around, the outer part faster than the middle. It is the art's own red or green
 * light that moves, not a colored layer on top. The glass rim and the power icon are drawn
 * again on top and never move. The white glints are light on the glass, so they are laid
 * back on top as light and stay still while the liquid flows under them.
 * A soft swish of brighter light turns with it. Nothing moves when the phone asks for
 * less motion, and Liquid swirl off shows the plain art.
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
    private var tintColor = 0
    private var swirlStrength = 0f
    private var swirlSpeed = 0.3f
    private var shown = false
    private var ticking = false
    private val liquidPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val currentPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        blendMode = BlendMode.SCREEN
    }
    private val ringMask = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.DST_IN }
    private val swishPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.SCREEN }
    private val topPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val glintPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        blendMode = BlendMode.SCREEN
    }
    private val liquidMatrix = Matrix()
    private var ringSize = 0f
    private val offSwish = SwishCache()
    private val onSwish = SwishCache()

    /** The swish shader for one side, kept until its color or size changes. */
    private class SwishCache {
        var key = 0
        var shader: Shader? = null
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
        tintColor = look.toggleGlow
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
        val picture = art.off.picture
        val scale = minOf(width / picture.width.toFloat(), height / picture.height.toFloat())
        val drawW = picture.width * scale
        val drawH = picture.height * scale
        target.set((width - drawW) / 2f, (height - drawH) / 2f, (width + drawW) / 2f, (height + drawH) / 2f)
        val seconds = if (GlowClock.motionAllowed) GlowClock.seconds else 0f
        if (mix < 1f) drawSide(canvas, art.off, offSwish, OFF_ORB, scale, 1f - mix, seconds)
        if (mix > 0f) drawSide(canvas, art.on, onSwish, ON_ORB, scale, mix, seconds)
    }

    /** One side of the art (OFF or ON), with its liquid when the swirl is on. */
    private fun drawSide(
        canvas: Canvas,
        side: Side,
        swish: SwishCache,
        orb: FloatArray,
        scale: Float,
        weight: Float,
        seconds: Float,
    ) {
        val alpha = (weight * 255f).toInt()
        artPaint.alpha = alpha
        if (!swirlOn) {
            canvas.drawBitmap(side.picture, null, target, artPaint)
            return
        }
        // The ball with its glints lifted off. They go back on top at the end, as light.
        canvas.drawBitmap(side.liquid, null, target, artPaint)
        val cx = target.left + orb[0] * scale
        val cy = target.top + orb[1] * scale
        val radius = orb[2] * scale
        val turn = seconds * (12f + 60f * swirlSpeed)
        drawLiquid(canvas, side.shader, cx, cy, radius, scale, alpha, turn)
        drawSwish(canvas, side.color, swish, cx, cy, radius, weight, turn)
        topPaint.alpha = alpha
        canvas.drawBitmap(side.still, null, target, topPaint)
        glintPaint.alpha = alpha
        canvas.drawBitmap(side.glints, null, target, glintPaint)
    }

    /**
     * The art's own plasma, turned around the ball's center. The middle stays still and the
     * outer part moves, then a second, gentler current turns the other way and mixes in.
     */
    private fun drawLiquid(
        canvas: Canvas,
        shader: BitmapShader,
        cx: Float,
        cy: Float,
        radius: Float,
        scale: Float,
        alpha: Int,
        turn: Float,
    ) {
        val inner = radius * INNER_GLASS
        val saved = canvas.saveLayer(cx - inner, cy - inner, cx + inner, cy + inner, null)
        liquidMatrix.setScale(scale, scale)
        liquidMatrix.postTranslate(target.left, target.top)
        liquidMatrix.postRotate(turn, cx, cy)
        shader.setLocalMatrix(liquidMatrix)
        liquidPaint.shader = shader
        liquidPaint.alpha = alpha
        canvas.drawCircle(cx, cy, inner, liquidPaint)
        liquidMatrix.setScale(scale * 1.07f, scale * 1.07f, 0f, 0f)
        liquidMatrix.postTranslate(target.left - (cx - target.left) * 0.07f, target.top - (cy - target.top) * 0.07f)
        liquidMatrix.postRotate(-turn * 0.65f, cx, cy)
        shader.setLocalMatrix(liquidMatrix)
        currentPaint.shader = shader
        currentPaint.alpha = (alpha * 0.45f * swirlStrength).toInt()
        canvas.drawCircle(cx, cy, inner, currentPaint)
        if (ringSize != inner) {
            ringSize = inner
            ringMask.shader = RadialGradient(
                0f, 0f, inner,
                intArrayOf(0x00000000, 0x00000000, 0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
                floatArrayOf(0f, 0.42f, 0.66f, 0.9f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        // Keep only the ring that flows. The still middle and the rim come from the art itself.
        canvas.save()
        canvas.translate(cx, cy)
        canvas.drawCircle(0f, 0f, inner, ringMask)
        canvas.restore()
        canvas.restoreToCount(saved)
    }

    /** A soft swish of brighter light in the ball's own color, a little tinted by Toggle glow color. */
    private fun drawSwish(
        canvas: Canvas,
        orbColor: Int,
        cache: SwishCache,
        cx: Float,
        cy: Float,
        radius: Float,
        weight: Float,
        turn: Float,
    ) {
        val inner = radius * INNER_GLASS
        val key = (orbColor * 31 + tintColor) * 31 + inner.toBits()
        if (cache.key != key || cache.shader == null) {
            cache.key = key
            val light = Hud.lighten(Hud.blend(orbColor, tintColor, TINT_SHARE), 0.35f)
            val sweep = SweepGradient(
                0f, 0f,
                intArrayOf(0x00000000, Hud.withAlpha(light, 0.9f), 0x00000000, 0x00000000, Hud.withAlpha(light, 0.5f), 0x00000000),
                floatArrayOf(0f, 0.12f, 0.3f, 0.55f, 0.66f, 0.82f),
            )
            // The swish fades out well before the rim, so it never draws a ring along the glass.
            val fade = RadialGradient(
                0f, 0f, inner,
                intArrayOf(0x00000000, 0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
                floatArrayOf(0.25f, 0.48f, 0.66f, 0.84f),
                Shader.TileMode.CLAMP,
            )
            cache.shader = ComposeShader(sweep, fade, PorterDuff.Mode.DST_IN)
        }
        swishPaint.shader = cache.shader
        swishPaint.alpha = (255f * weight * swirlStrength * 0.8f).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(turn * 1.6f)
        canvas.drawCircle(0f, 0f, inner, swishPaint)
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

    /**
     * One side of the toggle art, split into the layers the liquid needs.
     * [liquid] is the art with the power icon and the white glints lifted off the ball, so
     * nothing but plasma turns. [still] is the rim of the ball, everything outside it, and the
     * power icon. [glints] is the white light on the glass, laid back on top as light.
     */
    private class Side(val picture: Bitmap, orb: FloatArray) {
        val liquid: Bitmap
        val still: Bitmap
        val glints: Bitmap
        val shader: BitmapShader

        /** The ball's own glow color, made a little brighter, for the swish. */
        val color: Int

        init {
            val w = picture.width
            val h = picture.height
            val source = IntArray(w * h)
            picture.getPixels(source, 0, w, 0, 0, w, h)
            val cx = orb[0]
            val cy = orb[1]
            val radius = orb[2]
            val fill = plasmaColor(source, w, h, cx, cy, radius)
            val liquidPixels = IntArray(w * h)
            val stillPixels = IntArray(w * h)
            val glintPixels = IntArray(w * h)
            // How much each pixel is glint. Softened below, because light on glass is smooth.
            val shine = FloatArray(w * h)
            val glass = FloatArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val index = y * w + x
                    val color = source[index]
                    val dx = x - cx
                    val dy = y - cy
                    val distance = sqrt(dx * dx + dy * dy)
                    val r = Color.red(color) / 255f
                    val g = Color.green(color) / 255f
                    val b = Color.blue(color) / 255f
                    val whitest = minOf(r, g, b)
                    val inside = distance < radius
                    val rim = smooth(radius * 0.8f, radius * 0.9f, distance)
                    val icon = if (distance < radius * 0.55f) smooth(0.8f, 0.95f, maxOf(r, g, b)) else 0f
                    val glint = if (inside) smooth(0.4f, 0.85f, whitest) else 0f
                    val alpha = Color.alpha(color)
                    stillPixels[index] = withAlpha(color, alpha * maxOf(rim, icon))
                    shine[index] = glint * alpha / 255f
                    glass[index] = 1f - maxOf(rim, icon)
                    liquidPixels[index] = if (inside) {
                        // Under a glint is plasma of the same hue. Pure white glints take the ball's color.
                        val lift = if (whitest < 0.97f) 1f / (1f - whitest) else 0f
                        val under = Color.rgb(
                            ((r - whitest) * lift * 255f).toInt().coerceIn(0, 255),
                            ((g - whitest) * lift * 255f).toInt().coerceIn(0, 255),
                            ((b - whitest) * lift * 255f).toInt().coerceIn(0, 255),
                        )
                        val plasma = Hud.blend(under, fill, smooth(0.75f, 0.95f, whitest))
                        val lifted = Hud.blend(Hud.blend(color or OPAQUE, plasma, glint), fill, icon)
                        withAlpha(lifted, alpha.toFloat())
                    } else {
                        color
                    }
                }
            }
            soften(shine, w, h, SOFT_PX)
            soften(shine, w, h, SOFT_PX)
            val light = Hud.lighten(fill, GLINT_WHITE) and 0x00FFFFFF
            for (index in glintPixels.indices) {
                val strength = (shine[index] * glass[index] * 255f).toInt().coerceIn(0, 255)
                glintPixels[index] = (strength shl 24) or light
            }
            liquid = Bitmap.createBitmap(liquidPixels, w, h, Bitmap.Config.ARGB_8888)
            still = Bitmap.createBitmap(stillPixels, w, h, Bitmap.Config.ARGB_8888)
            glints = Bitmap.createBitmap(glintPixels, w, h, Bitmap.Config.ARGB_8888)
            shader = BitmapShader(liquid, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            color = brighter(fill)
        }

        private companion object {
            const val OPAQUE = 0xFF000000.toInt()

            /** How far the glints are softened, in art pixels. The art is 472 by 224. */
            const val SOFT_PX = 3

            /** Glints are nearly white light with a touch of the ball's own color. */
            const val GLINT_WHITE = 0.8f

            /** A box blur across, then down. Two passes look like a soft round blur. */
            fun soften(values: FloatArray, w: Int, h: Int, reach: Int) {
                val line = FloatArray(maxOf(w, h))
                for (y in 0 until h) {
                    for (x in 0 until w) line[x] = values[y * w + x]
                    blurLine(line, w, reach) { x, value -> values[y * w + x] = value }
                }
                for (x in 0 until w) {
                    for (y in 0 until h) line[y] = values[y * w + x]
                    blurLine(line, h, reach) { y, value -> values[y * w + x] = value }
                }
            }

            /** A running sum, so each pixel costs the same however wide the blur is. */
            inline fun blurLine(line: FloatArray, size: Int, reach: Int, put: (Int, Float) -> Unit) {
                var sum = 0f
                var count = 0
                for (k in 0..minOf(reach, size - 1)) {
                    sum += line[k]
                    count++
                }
                for (i in 0 until size) {
                    put(i, sum / count)
                    val enter = i + reach + 1
                    val leave = i - reach
                    if (enter < size) {
                        sum += line[enter]
                        count++
                    }
                    if (leave >= 0) {
                        sum -= line[leave]
                        count--
                    }
                }
            }

            fun smooth(edge0: Float, edge1: Float, x: Float): Float {
                val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                return t * t * (3f - 2f * t)
            }

            fun withAlpha(color: Int, alpha: Float): Int =
                (alpha.toInt().coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

            /** The plasma's usual color inside the ball, leaving out the icon and the glints. */
            fun plasmaColor(pixels: IntArray, w: Int, h: Int, cx: Float, cy: Float, radius: Float): Int {
                var red = 0f
                var green = 0f
                var blue = 0f
                var total = 0f
                val reach = radius * 0.75f
                val left = (cx - reach).toInt().coerceAtLeast(0)
                val right = (cx + reach).toInt().coerceAtMost(w - 1)
                val top = (cy - reach).toInt().coerceAtLeast(0)
                val bottom = (cy + reach).toInt().coerceAtMost(h - 1)
                for (y in top..bottom) {
                    for (x in left..right) {
                        val dx = x - cx
                        val dy = y - cy
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance > reach) continue
                        val color = pixels[y * w + x]
                        val r = Color.red(color) / 255f
                        val g = Color.green(color) / 255f
                        val b = Color.blue(color) / 255f
                        val icon = if (distance < radius * 0.55f) smooth(0.8f, 0.95f, maxOf(r, g, b)) else 0f
                        val weight = 1f - maxOf(icon, smooth(0.35f, 0.8f, minOf(r, g, b)))
                        red += r * weight
                        green += g * weight
                        blue += b * weight
                        total += weight
                    }
                }
                if (total <= 0f) return Hud.WHITE
                return Color.rgb(
                    (red / total * 255f).toInt().coerceIn(0, 255),
                    (green / total * 255f).toInt().coerceIn(0, 255),
                    (blue / total * 255f).toInt().coerceIn(0, 255),
                )
            }

            fun brighter(color: Int): Int {
                val top = maxOf(Color.red(color), Color.green(color), Color.blue(color)).coerceAtLeast(1)
                val boost = 255f / top
                return Color.rgb(
                    (Color.red(color) * boost).toInt().coerceAtMost(255),
                    (Color.green(color) * boost).toInt().coerceAtMost(255),
                    (Color.blue(color) * boost).toInt().coerceAtMost(255),
                )
            }
        }
    }

    /** The two toggle pictures, OFF and ON, split once and kept for every toggle. */
    private class Art(val on: Side, val off: Side) {
        companion object {
            private var loaded: Art? = null

            fun load(resources: Resources): Art {
                loaded?.let { return it }
                val art = Art(
                    on = Side(BitmapFactory.decodeResource(resources, R.drawable.hud_toggle_on), ON_ORB),
                    off = Side(BitmapFactory.decodeResource(resources, R.drawable.hud_toggle_off), OFF_ORB),
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

        /** How much of the ball, from its center, is glass the liquid can move in. */
        const val INNER_GLASS = 0.9f

        /** How much the Toggle glow color tints the swish. The ball keeps its own red or green. */
        const val TINT_SHARE = 0.3f

        /** Where the orb sits in the 472 by 224 art: x, y, radius. Measured from the reference images. */
        val OFF_ORB = floatArrayOf(122f, 114f, 82f)
        val ON_ORB = floatArrayOf(354f, 115f, 81f)
    }
}
