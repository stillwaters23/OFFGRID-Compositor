package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Red sweep (feature 29): a red velvet carpet with gold trim rolls in from the left across the
 * video. The roll at its front end turns as it goes and lays the carpet flat behind it. Then
 * the carpet keeps sliding along, with a soft sheen moving over it, for the whole time. In the
 * last moments it rolls back up and leaves to the left.
 *
 * Sweep speed sets how fast it rolls and slides. Sweep width sets the carpet's height, 15% to
 * 80% of the video, a little below the middle. Sweep length sets how long it stays
 * (see [EffectTimes.sweepSeconds]). No words, no logos.
 */
class RedSweepLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaded = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val stitch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val velvet = LinearGradient(0f, 0f, 0f, 1f, VELVET_COLORS, VELVET_STOPS, Shader.TileMode.CLAMP)
    private val gold = LinearGradient(0f, 0f, 0f, 1f, GOLD_COLORS, GOLD_STOPS, Shader.TileMode.CLAMP)
    private val sheen = LinearGradient(0f, 0f, 1f, 0f, SHEEN_COLORS, SHEEN_STOPS, Shader.TileMode.CLAMP)
    private val fade = LinearGradient(0f, 0f, 1f, 0f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    private val fadeDown = LinearGradient(0f, 0f, 0f, 1f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
    private val roll = LinearGradient(0f, 0f, 1f, 0f, ROLL_COLORS, ROLL_STOPS, Shader.TileMode.CLAMP)
    private val rollShade = LinearGradient(0f, 0f, 1f, 0f, SHADE_COLORS, SHADE_STOPS, Shader.TileMode.CLAMP)
    private val pileTile: Bitmap = makePile()
    private val pile = BitmapShader(pileTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val rollMatrix = Matrix()
    private val body = Path()
    private val oval = RectF()
    private val stitches = FloatArray(MAX_STITCHES * 4)

    override fun isActive(frame: EffectFrame): Boolean {
        val since = frame.since(Fire.RED_SWEEP) ?: return false
        return since >= 0f && since < EffectTimes.sweepSeconds(frame.settings)
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val since = frame.since(Fire.RED_SWEEP) ?: return
        val s = frame.settings
        val total = EffectTimes.sweepSeconds(s)
        if (since < 0f || since >= total) return
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val unit = min(w, h)
        val calm = if (frame.reducedMotion) CALM else 1f
        val speed = s.value(FxKeys.SWEEP_SPEED)

        // The carpet band: its height, a little below the middle.
        val bandHeight = h * EffectMath.lerp(BAND_MIN, BAND_MAX, s.value(FxKeys.SWEEP_WIDTH))
        val top = (h * BAND_MIDDLE - bandHeight / 2f).coerceIn(0f, (h - bandHeight).coerceAtLeast(0f))
        val bottom = min(h, top + bandHeight)
        val band = bottom - top
        val radius = min(unit * ROLL_SHARE, band * ROLL_BAND_SHARE)

        // Timing: roll in, slide, roll back out at the end.
        val outTime = min(OUT_SECONDS, total * OUT_MOST)
        val inTime = (EffectMath.lerp(IN_SLOW, IN_FAST, speed) / calm).coerceAtMost((total - outTime) * IN_MOST)
        val slideStart = inTime
        val slideEnd = total - outTime
        val slide = unit * EffectMath.lerp(SLIDE_SLOW, SLIDE_FAST, speed) * calm
        val offset = slide * slideDistance(since - slideStart, slideEnd - slideStart)

        val rollX: Float
        val rollRadius: Float
        val turned: Float
        val farLeft = -radius * 1.4f
        val farRight = w + radius * 1.4f
        when {
            since < slideStart -> {
                val p = since / inTime
                rollX = EffectMath.lerp(farLeft, farRight, 1f - (1f - p).pow(IN_EASE))
                rollRadius = EffectMath.lerp(radius, radius * THIN_ROLL, p)
                turned = (rollX - farLeft) / rollRadius
            }
            since < slideEnd -> {
                rollX = Float.NaN
                rollRadius = radius * THIN_ROLL
                turned = 0f
            }
            else -> {
                val q = ((since - slideEnd) / outTime).coerceIn(0f, 1f)
                rollX = EffectMath.lerp(farRight, farLeft, q.pow(OUT_EASE))
                rollRadius = EffectMath.lerp(radius * THIN_ROLL, radius, q)
                turned = (rollX - farRight) / rollRadius
            }
        }
        val rollShown = !rollX.isNaN()
        val carpetRight = if (rollShown) rollX else w

        drawCarpet(canvas, frame, unit, top, bottom, carpetRight, offset, calm)
        if (rollShown) {
            drawRoll(canvas, unit, top, bottom, rollX, rollRadius, turned)
        }
    }

    /** The flat carpet from the left edge to [right], with its shadow, pile, trim, stitches, and sheen. */
    private fun drawCarpet(
        canvas: Canvas,
        frame: EffectFrame,
        unit: Float,
        top: Float,
        bottom: Float,
        right: Float,
        offset: Float,
        calm: Float,
    ) {
        if (right <= 0f) return
        val band = bottom - top
        // A soft shadow under and over the carpet, so it sits on top of the picture.
        val drop = unit * DROP_SHARE
        canvas.save()
        canvas.translate(0f, bottom)
        canvas.scale(1f, drop)
        fill.shader = fadeDown
        fill.alpha = (255 * DROP_ALPHA).toInt()
        canvas.drawRect(0f, 0f, right, 1f, fill)
        canvas.restore()
        canvas.save()
        canvas.translate(0f, top)
        canvas.scale(1f, -drop * 0.5f)
        fill.alpha = (255 * DROP_ALPHA * 0.6f).toInt()
        canvas.drawRect(0f, 0f, right, 1f, fill)
        canvas.restore()
        fill.alpha = 255

        canvas.save()
        canvas.clipRect(0f, top, right, bottom)

        // Velvet: deep red, lit from above.
        canvas.save()
        canvas.translate(0f, top)
        canvas.scale(1f, band)
        fill.shader = velvet
        canvas.drawRect(0f, 0f, right, 1f, fill)
        canvas.restore()

        // The pile: fine fibers that slide with the carpet.
        val tile = pileTile.width.toFloat()
        val shift = offset - floor(offset / tile) * tile
        canvas.save()
        canvas.translate(shift, top)
        fill.shader = pile
        canvas.drawRect(-shift - tile, 0f, right - shift, band, fill)
        canvas.restore()
        fill.shader = null

        // Gold trim near each long edge: a dark edge, a wide gold line, a thin gold line, then stitches.
        val inset = band * TRIM_INSET
        val wide = (band * TRIM_WIDE).coerceIn(unit * TRIM_MIN, unit * TRIM_MAX)
        val thin = wide * THIN_SHARE
        fill.color = EDGE_DARK
        canvas.drawRect(0f, top, right, top + max(1f, wide * 0.35f), fill)
        canvas.drawRect(0f, bottom - max(1f, wide * 0.35f), right, bottom, fill)
        drawGold(canvas, right, top + inset, wide)
        drawGold(canvas, right, bottom - inset - wide, wide)
        drawGold(canvas, right, top + inset + wide + wide * GAP_SHARE, thin)
        drawGold(canvas, right, bottom - inset - wide - wide * GAP_SHARE - thin, thin)
        val stitchUp = top + inset + wide + wide * GAP_SHARE + thin + wide * STITCH_GAP
        val stitchDown = bottom - inset - wide - wide * GAP_SHARE - thin - wide * STITCH_GAP
        if (stitchDown - stitchUp > wide * 4f) {
            drawStitches(canvas, unit, band, right, stitchUp, offset, wide)
            drawStitches(canvas, unit, band, right, stitchDown, offset, wide)
        }

        // A soft sheen that sweeps along the velvet again and again.
        val sheenWidth = max(unit * SHEEN_SHARE, band * 0.6f)
        val sheenTime = frame.time * calm / SHEEN_SECONDS
        val sheenX = (sheenTime - floor(sheenTime)) * (frame.width + sheenWidth * 2f + band * SHEEN_SLANT) - sheenWidth - band * SHEEN_SLANT
        canvas.save()
        canvas.translate(sheenX, top)
        canvas.skew(SHEEN_SLANT, 0f)
        canvas.scale(sheenWidth, 1f)
        fill.shader = sheen
        canvas.drawRect(0f, 0f, 1f, band, fill)
        canvas.restore()
        fill.shader = null

        canvas.restore()
    }

    /** A gold line [thick] high at [y], shiny across its middle. */
    private fun drawGold(canvas: Canvas, right: Float, y: Float, thick: Float) {
        canvas.save()
        canvas.translate(0f, y)
        canvas.scale(1f, thick)
        fill.shader = gold
        canvas.drawRect(0f, 0f, right, 1f, fill)
        canvas.restore()
        fill.shader = null
    }

    /** A row of small gold stitches that slides with the carpet. */
    private fun drawStitches(canvas: Canvas, unit: Float, band: Float, right: Float, y: Float, offset: Float, wide: Float) {
        val step = max(unit * STITCH_STEP, band * STITCH_BAND)
        val first = offset - floor(offset / step) * step - step
        var n = 0
        var x = first
        while (x < right && n < MAX_STITCHES) {
            stitches[n * 4] = x
            stitches[n * 4 + 1] = y
            stitches[n * 4 + 2] = x + step * STITCH_LENGTH
            stitches[n * 4 + 3] = y
            n++
            x += step
        }
        stitch.color = STITCH_GOLD
        stitch.strokeWidth = max(1.5f, wide * STITCH_WIDTH)
        canvas.drawLines(stitches, 0, n * 4, stitch)
    }

    /** The rolled carpet at [x]: a turning velvet roll with the spiral of layers on top. */
    private fun drawRoll(canvas: Canvas, unit: Float, top: Float, bottom: Float, x: Float, r: Float, turned: Float) {
        val tilt = r * CAP_TILT
        // Shadows: on the carpet to its left, and on the picture to its right.
        canvas.save()
        canvas.translate(x - r * 0.4f, top)
        canvas.scale(-r * 1.8f, bottom - top)
        fill.shader = fade
        fill.alpha = (255 * NEAR_SHADOW).toInt()
        canvas.drawRect(0f, 0f, 1f, 1f, fill)
        canvas.restore()
        canvas.save()
        canvas.translate(x + r * 0.6f, top + tilt)
        canvas.scale(r * 1.3f, bottom - top)
        fill.alpha = (255 * FAR_SHADOW).toInt()
        canvas.drawRect(0f, 0f, 1f, 1f, fill)
        canvas.restore()
        fill.shader = null
        fill.alpha = 255

        // The roll's body: straight sides with a rounded bottom, as seen from a little above.
        body.reset()
        body.moveTo(x - r, top)
        body.lineTo(x - r, bottom)
        oval.set(x - r, bottom - tilt, x + r, bottom + tilt)
        body.arcTo(oval, 180f, -180f, false)
        body.lineTo(x + r, top)
        body.close()
        rollMatrix.setScale(2f * r, 1f)
        rollMatrix.postTranslate(x - r, 0f)
        roll.setLocalMatrix(rollMatrix)
        rollShade.setLocalMatrix(rollMatrix)
        shaded.shader = roll
        canvas.drawPath(body, shaded)

        // Seams that turn with the roll, so you can see it rolling.
        stroke.color = SEAM
        stroke.strokeWidth = max(1f, r * 0.05f)
        for (k in 0 until SEAMS) {
            val angle = turned + k * TWO_PI / SEAMS
            val facing = cos(angle)
            if (facing <= 0.05f) continue
            val sx = x + r * sin(angle)
            stroke.alpha = (255 * SEAM_ALPHA * facing).toInt()
            canvas.drawLine(sx, top + tilt * facing, sx, bottom + tilt * facing, stroke)
        }

        // The gold trim wraps around the roll, so it shows as curves.
        val band = bottom - top
        val inset = band * TRIM_INSET
        val wide = (band * TRIM_WIDE).coerceIn(unit * TRIM_MIN, unit * TRIM_MAX)
        stroke.color = TRIM_GOLD
        stroke.alpha = 255
        stroke.strokeWidth = wide
        drawFrontCurve(canvas, x, r, tilt, top + inset + wide / 2f)
        drawFrontCurve(canvas, x, r, tilt, bottom - inset - wide / 2f)
        stroke.strokeWidth = wide * THIN_SHARE
        drawFrontCurve(canvas, x, r, tilt, top + inset + wide + wide * GAP_SHARE)
        drawFrontCurve(canvas, x, r, tilt, bottom - inset - wide - wide * GAP_SHARE)

        // Round light and shade over everything on the body.
        shaded.shader = rollShade
        canvas.drawPath(body, shaded)
        shaded.shader = null

        // The top: the spiral of rolled layers, seen from a little above.
        oval.set(x - r, top - tilt, x + r, top + tilt)
        fill.color = CAP_DARK
        canvas.drawOval(oval, fill)
        stroke.strokeWidth = max(1f, r * 0.045f)
        for (k in 0 until LAYERS) {
            val share = 1f - (k + 0.5f) / LAYERS * 0.82f
            oval.set(x - r * share, top - tilt * share, x + r * share, top + tilt * share)
            stroke.color = if (k % 2 == 0) LAYER_LIGHT else TRIM_GOLD
            stroke.alpha = if (k % 2 == 0) 255 else 150
            canvas.drawOval(oval, stroke)
        }
        oval.set(x - r * 0.14f, top - tilt * 0.14f, x + r * 0.14f, top + tilt * 0.14f)
        fill.color = CORE_DARK
        canvas.drawOval(oval, fill)
        // A thin light on the front rim of the top.
        oval.set(x - r, top - tilt, x + r, top + tilt)
        stroke.color = RIM_LIGHT
        stroke.alpha = 200
        stroke.strokeWidth = max(1f, r * 0.04f)
        canvas.drawArc(oval, 20f, 140f, false, stroke)
        stroke.alpha = 255
    }

    /** The front half of a ring around the roll at height [y]. */
    private fun drawFrontCurve(canvas: Canvas, x: Float, r: Float, tilt: Float, y: Float) {
        oval.set(x - r, y - tilt, x + r, y + tilt)
        canvas.drawArc(oval, 0f, 180f, false, stroke)
    }

    private companion object {
        /** Carpet height as a share of the video at Sweep width 0 and 100, and where its middle is. */
        const val BAND_MIN = 0.15f
        const val BAND_MAX = 0.8f
        const val BAND_MIDDLE = 0.57f

        /** The roll's radius: a share of the short side, but never more than this share of the band. */
        const val ROLL_SHARE = 0.075f
        const val ROLL_BAND_SHARE = 0.2f

        /** The roll gets thinner as it lays carpet down. */
        const val THIN_ROLL = 0.72f
        const val CAP_TILT = 0.28f

        /** Seconds to roll in at Sweep speed 0 and 100. */
        const val IN_SLOW = 1.8f
        const val IN_FAST = 0.6f
        const val IN_MOST = 0.6f
        const val IN_EASE = 1.6f

        /** Rolling back out takes the last 0.8 seconds. */
        const val OUT_SECONDS = 0.8f
        const val OUT_MOST = 0.3f
        const val OUT_EASE = 1.4f

        /** How fast the carpet slides, as a share of the short side per second. */
        const val SLIDE_SLOW = 0.12f
        const val SLIDE_FAST = 0.8f
        const val SPEED_UP = 0.6f
        const val SLOW_DOWN = 0.35f

        /** Less motion: half speed. */
        const val CALM = 0.5f

        const val DROP_SHARE = 0.035f
        const val DROP_ALPHA = 0.42f
        const val NEAR_SHADOW = 0.5f
        const val FAR_SHADOW = 0.32f

        const val TRIM_INSET = 0.06f
        const val TRIM_WIDE = 0.018f
        const val TRIM_MIN = 0.0045f
        const val TRIM_MAX = 0.014f
        const val THIN_SHARE = 0.42f
        const val GAP_SHARE = 0.9f
        const val STITCH_GAP = 1.6f
        const val STITCH_STEP = 0.028f
        const val STITCH_BAND = 0.045f
        const val STITCH_LENGTH = 0.5f
        const val STITCH_WIDTH = 0.42f
        const val MAX_STITCHES = 160

        const val SHEEN_SHARE = 0.35f
        const val SHEEN_SECONDS = 3.2f
        const val SHEEN_SLANT = -0.45f

        const val SEAMS = 7
        const val SEAM_ALPHA = 0.45f
        const val LAYERS = 5

        const val TWO_PI = (Math.PI * 2.0).toFloat()
        const val PILE_SIZE = 256

        const val EDGE_DARK = 0xFF2A0007.toInt()
        const val TRIM_GOLD = 0xFFE2B04A.toInt()
        const val STITCH_GOLD = 0xD9E9BE5C.toInt()
        const val SEAM = 0xFF3A0510.toInt()
        const val CAP_DARK = 0xFF4A0612.toInt()
        const val CORE_DARK = 0xFF1A0004.toInt()
        const val LAYER_LIGHT = 0xFF9E1A30.toInt()
        const val RIM_LIGHT = 0xFFFF8A9A.toInt()

        val VELVET_COLORS = intArrayOf(
            0xFF3E0009.toInt(), 0xFF730A1B.toInt(), 0xFFB5172F.toInt(), 0xFFA6122A.toInt(),
            0xFF7E0A1D.toInt(), 0xFF3A0008.toInt(),
        )
        val VELVET_STOPS = floatArrayOf(0f, 0.07f, 0.34f, 0.6f, 0.9f, 1f)

        val GOLD_COLORS = intArrayOf(0xFF7A5210.toInt(), 0xFFFBE08A.toInt(), 0xFFD9A93E.toInt(), 0xFF8C6016.toInt())
        val GOLD_STOPS = floatArrayOf(0f, 0.38f, 0.62f, 1f)

        val SHEEN_COLORS = intArrayOf(0x00FFFFFF, 0x14FFE6EA, 0x33FFF0F2, 0x14FFE6EA, 0x00FFFFFF)
        val SHEEN_STOPS = floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f)

        val ROLL_COLORS = intArrayOf(
            0xFF5E0614.toInt(), 0xFF9A1027.toInt(), 0xFFC5203C.toInt(), 0xFFB0162F.toInt(),
            0xFF7A0A1C.toInt(), 0xFF4A0410.toInt(),
        )
        val ROLL_STOPS = floatArrayOf(0f, 0.2f, 0.38f, 0.55f, 0.8f, 1f)

        val SHADE_COLORS = intArrayOf(
            0xC0000000.toInt(), 0x30000000, 0x00000000, 0x30FFD0D8, 0x00000000, 0x40000000, 0xC8000000.toInt(),
        )
        val SHADE_STOPS = floatArrayOf(0f, 0.2f, 0.32f, 0.4f, 0.5f, 0.75f, 1f)

        /**
         * How far the carpet has slid [t] seconds into a slide of [length] seconds, in seconds of full speed.
         * It speeds up at the start and slows down at the end, so it never jumps.
         */
        fun slideDistance(t: Float, length: Float): Float {
            if (length <= 0f || t <= 0f) return 0f
            var up = SPEED_UP
            var down = SLOW_DOWN
            if (up + down > length) {
                val k = length / (up + down)
                up *= k
                down *= k
            }
            val at = min(t, length)
            return when {
                at < up -> at * at / (2f * up)
                at < length - down -> up / 2f + (at - up)
                else -> {
                    val left = length - at
                    up / 2f + (length - down - up) + down / 2f - left * left / (2f * down)
                }
            }
        }

        /** Fine velvet fibers: short light and dark streaks, made once. They tile with no seams. */
        fun makePile(): Bitmap {
            val n = PILE_SIZE
            val pixels = IntArray(n * n)
            for (y in 0 until n) {
                val shift = (EffectMath.hash(y, 3) * n).toInt()
                for (x in 0 until n) {
                    val streak = EffectMath.hash(((x + shift) % n) / 8 + y * 977, 5) - 0.5f
                    val grain = EffectMath.hash(x + y * n, 7) - 0.5f
                    val v = streak * 0.75f + grain * 0.5f
                    pixels[y * n + x] = if (v >= 0f) {
                        Color.argb((v * 2f * 22f).toInt().coerceIn(0, 255), 255, 214, 222)
                    } else {
                        Color.argb((-v * 2f * 46f).toInt().coerceIn(0, 255), 0, 0, 0)
                    }
                }
            }
            return Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
        }
    }
}
