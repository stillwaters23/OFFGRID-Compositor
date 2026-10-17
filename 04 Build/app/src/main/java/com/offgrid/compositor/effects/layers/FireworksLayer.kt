package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fireworks (feature 18): when fired, rockets climb from the bottom and burst into rings of
 * sparks that fall and fade. Each burst has its own colors: gold, teal, magenta, or white.
 *
 * Fireworks count sets 1 to 8 bursts, spread over the first 40% of the time. Fireworks size
 * sets each burst's reach, 10% to 45% of the width. Fireworks duration sets how long it lasts
 * (see [EffectTimes.fireworksSeconds]). The light adds up where sparks cross, but there is
 * never a flash over the whole picture.
 */
class FireworksLayer : OverlayLayer {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        blendMode = BlendMode.PLUS
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS }
    private val glowDot: Bitmap = makeGlowDot()
    private val spot = RectF()
    private val lines = FloatArray(MAX_RING * 4)
    private val heads = FloatArray(MAX_RING * 2)
    private val outX = FloatArray(MAX_RING)
    private val outY = FloatArray(MAX_RING)
    private val sags = FloatArray(MAX_RING)

    /** The last spot worked out by [sparkAt] or [rocketAt]. */
    private var atX = 0f
    private var atY = 0f

    override fun isActive(frame: EffectFrame): Boolean {
        val since = frame.since(Fire.FIREWORKS) ?: return false
        return since >= 0f && since < EffectTimes.fireworksSeconds(frame.settings)
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val fired = frame.last(Fire.FIREWORKS) ?: return
        val s = frame.settings
        val total = EffectTimes.fireworksSeconds(s)
        val t = frame.time - fired.at
        if (t < 0f || t >= total) return
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val unit = min(w, h)
        val seed = fired.id.toInt()
        val bursts = 1 + (7f * s.value(FxKeys.FIREWORKS_COUNT) + 0.5f).toInt()
        val sizeValue = s.value(FxKeys.FIREWORKS_SIZE)
        val reach = w * EffectMath.lerp(REACH_MIN, REACH_MAX, sizeValue)
        val rise = (total * RISE_SHARE).coerceIn(RISE_MIN, RISE_MAX)
        val spread = total * STAGGER_SHARE
        val turn = EffectMath.hash(seed, SALT_PLACE)
        val calm = if (frame.reducedMotion) CALM else 1f

        for (k in 0 until bursts) {
            val jitter = if (k == 0) 0f else (EffectMath.hash(seed * 31 + k, SALT_START) - 0.5f) * 0.5f
            val start = if (bursts == 1) 0f else (spread * (k + jitter) / (bursts - 1)).coerceIn(0f, spread)
            if (t < start) continue
            // Bursts spread across the top part, each in a new spot.
            val bx = w * (EDGE_ROOM + (1f - 2f * EDGE_ROOM) * frac(turn + k * GOLDEN))
            val by = h * (HIGH + (LOW - HIGH) * EffectMath.hash(seed * 17 + k, SALT_HEIGHT))
            val radius = reach * (0.82f + 0.26f * EffectMath.hash(seed * 13 + k, SALT_SIZE))
            val look = LOOKS[((EffectMath.hash(seed, SALT_LOOK) * LOOKS.size).toInt() + k) % LOOKS.size]
            val since = t - start
            val startX = bx + (EffectMath.hash(seed * 7 + k, SALT_DRIFT) - 0.5f) * w * DRIFT
            if (since < rise) {
                drawRocket(canvas, since, rise, startX, h * 1.03f, bx, by, unit, look[0], seed * 5 + k)
            } else {
                val life = (total - start - rise).coerceAtLeast(MIN_LIFE)
                drawBurst(canvas, since - rise, life, bx, by, radius, unit, sizeValue, calm, seed * 101 + k, look)
            }
        }
    }

    /** Where a rocket is at [u] from 0 (launch) to 1 (burst point), into [atX] and [atY]. */
    private fun rocketAt(u: Float, x0: Float, y0: Float, x1: Float, y1: Float, unit: Float) {
        atX = EffectMath.lerp(x0, x1, u) + sin(u * WOBBLE_TURNS) * unit * WOBBLE
        atY = EffectMath.lerp(y0, y1, u)
    }

    /** A rocket climbing to its burst point: a hot head, a tapering trail, and a few falling embers. */
    private fun drawRocket(
        canvas: Canvas,
        since: Float,
        rise: Float,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        unit: Float,
        colors: IntArray,
        key: Int,
    ) {
        val q = (since / rise).coerceIn(0f, 1f)
        // It slows as it climbs: fast at the bottom, nearly still at the top.
        val u = 1f - (1f - q) * (1f - q)
        val tail = TRAIL_MIN + TRAIL_EXTRA * (1f - q)
        val hot = colors[1]
        val width = unit * ROCKET_WIDTH

        // Embers dropped along the way: each one appears when the head passes, then falls and fades.
        for (j in 0 until EMBERS) {
            val eu = (j + 0.5f + 0.5f * (EffectMath.hash(key * 13 + j, SALT_EMBER) - 0.5f)) / EMBERS
            if (eu > u) break
            val passed = 1f - sqrt(1f - eu)
            val age = (q - passed) * rise
            if (age > EMBER_SECONDS) continue
            rocketAt(eu, x0, y0, x1, y1, unit)
            val life = 1f - age / EMBER_SECONDS
            val side = (EffectMath.hash(key * 17 + j, SALT_EMBER + 1) - 0.5f) * unit * 0.03f
            drawDot(canvas, atX + side * (1f - life), atY + unit * EMBER_FALL * age * age, width * 3f, colors[0], 0.55f * life)
        }

        // The trail: short pieces from faint and thin at the end to bright and thick at the head.
        rocketAt((u - tail).coerceAtLeast(0f), x0, y0, x1, y1, unit)
        var px = atX
        var py = atY
        line.strokeCap = Paint.Cap.BUTT
        for (j in 1..TRAIL_PIECES) {
            val f = j / TRAIL_PIECES.toFloat()
            rocketAt((u - tail * (1f - f)).coerceAtLeast(0f), x0, y0, x1, y1, unit)
            line.color = EffectMath.withAlpha(mix(colors[0], hot, f), f * f)
            line.strokeWidth = width * (0.3f + 0.7f * f)
            canvas.drawLine(px, py, atX, atY, line)
            px = atX
            py = atY
        }
        line.strokeCap = Paint.Cap.ROUND
        drawDot(canvas, px, py, width * 9f, colors[0], 0.4f)
        drawDot(canvas, px, py, width * 3.2f, hot, 0.95f)
    }

    private fun drawBurst(
        canvas: Canvas,
        e: Float,
        life: Float,
        bx: Float,
        by: Float,
        radius: Float,
        unit: Float,
        sizeValue: Float,
        calm: Float,
        key: Int,
        look: Array<IntArray>,
    ) {
        val p = (e / life).coerceIn(0f, 1f)
        val fade = (1f - p).pow(FADE_POWER)
        if (fade <= 0.003f) return
        val open = (life * OPEN_SHARE).coerceIn(OPEN_MIN, OPEN_MAX)
        val width = unit * (CORE_MIN + CORE_EXTRA * sizeValue) * (1f - 0.4f * p)
        val heat = exp(-e / HEAT_SECONDS)

        // A soft light where it burst, only around the burst, rising and falling smoothly.
        val bloom = BLOOM_ALPHA * calm * EffectMath.smooth(e / BLOOM_RISE) * exp(-e / BLOOM_FALL)
        drawDot(canvas, bx, by, radius * BLOOM_SIZE, look[0][0], bloom)
        drawDot(canvas, bx, by, radius * 0.35f, look[0][1], bloom * 0.8f)

        val count = RING_MIN + (EffectMath.hash(key, SALT_COUNT) * (RING_MAX - RING_MIN + 1)).toInt().coerceAtMost(RING_MAX - RING_MIN)
        drawRing(canvas, count, e, life, open, bx, by, radius, OUTER_SPEED, OUTER_SPREAD, width, fade, heat, key, look[0])
        drawRing(canvas, count / 2, e, life, open, bx, by, radius, INNER_SPEED, INNER_SPREAD, width * 0.85f, fade, heat, key + 7, look[1])
    }

    /**
     * One ring of sparks. Each spark is a streak that tapers from a hot head back to a faint
     * tail, with a soft glow at the head. Every pass is one batch for the whole ring.
     */
    private fun drawRing(
        canvas: Canvas,
        count: Int,
        e: Float,
        life: Float,
        open: Float,
        bx: Float,
        by: Float,
        radius: Float,
        speed: Float,
        speedSpread: Float,
        width: Float,
        fade: Float,
        heat: Float,
        key: Int,
        colors: IntArray,
    ) {
        val n = count.coerceIn(1, MAX_RING)
        val twist = EffectMath.hash(key, SALT_TWIST) * TWO_PI
        for (j in 0 until n) {
            val angle = twist + TWO_PI * (j + 0.6f * (EffectMath.hash(key * 59 + j, SALT_ANGLE) - 0.5f)) / n
            // A ball of sparks seen from the side looks like a disk: some come toward you and look shorter.
            val tilt = EffectMath.hash(key * 61 + j, SALT_TILT)
            val reach = radius * (speed + speedSpread * (EffectMath.hash(key * 67 + j, SALT_SPEED) - 0.5f)) * sqrt(1f - TILT * tilt * tilt)
            outX[j] = cos(angle) * reach
            outY[j] = sin(angle) * reach
            sags[j] = radius * SAG * (1f + 0.4f * EffectMath.hash(key * 71 + j, SALT_SAG))
            sparkAt(e, life, open, bx, by, j)
            heads[j * 2] = atX
            heads[j * 2 + 1] = atY
        }
        val main = colors[0]
        val hot = colors[1]
        // Right after the burst the sparks are all in one spot, and their light would pile up into
        // a hard white disk. They grow to full strength as they spread apart.
        val spread = radius * speed * (1f - exp(-e / open)) / (1f - exp(-life / open))
        val glow = width * GLOW_SIZE
        val glowFade = fade * (spread / (glow * GLOW_ROOM)).coerceIn(0f, 1f)
        val headFade = fade * (spread / (width * HEAD_ROOM)).coerceIn(MIN_HEAD, 1f)
        for (pass in STREAK_SECONDS.indices) {
            val back = (e - STREAK_SECONDS[pass]).coerceAtLeast(0f)
            for (j in 0 until n) {
                sparkAt(back, life, open, bx, by, j)
                lines[j * 4] = atX
                lines[j * 4 + 1] = atY
                lines[j * 4 + 2] = heads[j * 2]
                lines[j * 4 + 3] = heads[j * 2 + 1]
            }
            val color = if (pass == STREAK_SECONDS.size - 1) mix(main, hot, 0.45f + 0.55f * heat) else main
            line.color = EffectMath.withAlpha(color, STREAK_ALPHA[pass] * headFade)
            line.strokeWidth = width * STREAK_WIDTH[pass]
            canvas.drawLines(lines, 0, n * 4, line)
        }
        line.color = EffectMath.withAlpha(hot, headFade)
        line.strokeWidth = width * HEAD_WIDTH
        canvas.drawPoints(heads, 0, n * 2, line)
        if (glowFade <= 0.004f) return
        dot.color = EffectMath.withAlpha(main, GLOW_ALPHA * glowFade)
        for (j in 0 until n) {
            spot.set(heads[j * 2] - glow, heads[j * 2 + 1] - glow, heads[j * 2] + glow, heads[j * 2 + 1] + glow)
            canvas.drawBitmap(glowDot, null, spot, dot)
        }
    }

    /** Where spark [j] is [e] seconds after the burst: it flies out fast, slows down, and sags as it falls. */
    private fun sparkAt(e: Float, life: Float, open: Float, bx: Float, by: Float, j: Int) {
        val out = (1f - exp(-e / open)) / (1f - exp(-life / open))
        val fall = e / life
        atX = bx + outX[j] * out
        atY = by + outY[j] * out + sags[j] * fall * fall
    }

    private fun drawDot(canvas: Canvas, x: Float, y: Float, size: Float, color: Int, alpha: Float) {
        if (alpha <= 0.004f || size <= 0f) return
        val half = size / 2f
        spot.set(x - half, y - half, x + half, y + half)
        dot.color = EffectMath.withAlpha(color, alpha)
        canvas.drawBitmap(glowDot, null, spot, dot)
    }

    private companion object {
        /** Burst reach as a share of the width, at Fireworks size 0 and 100. */
        const val REACH_MIN = 0.10f
        const val REACH_MAX = 0.45f

        /** Bursts start within the first 40% of the time. */
        const val STAGGER_SHARE = 0.4f
        const val RISE_SHARE = 0.2f
        const val RISE_MIN = 0.16f
        const val RISE_MAX = 0.5f
        const val MIN_LIFE = 0.25f

        /** Less motion: the light at each burst is half as strong. */
        const val CALM = 0.5f

        /** Where bursts happen: away from the side edges, in the top half. */
        const val EDGE_ROOM = 0.2f
        const val HIGH = 0.13f
        const val LOW = 0.42f
        const val DRIFT = 0.12f
        const val GOLDEN = 0.618034f

        const val ROCKET_WIDTH = 0.0055f
        const val TRAIL_MIN = 0.04f
        const val TRAIL_EXTRA = 0.2f
        const val TRAIL_PIECES = 10
        const val WOBBLE = 0.005f
        const val WOBBLE_TURNS = 11f
        const val EMBERS = 7
        const val EMBER_SECONDS = 0.35f
        const val EMBER_FALL = 0.5f

        /** Sparks per ring. The inner ring has half as many, so a burst has 45 to 78. */
        const val RING_MIN = 30
        const val RING_MAX = 52
        const val MAX_RING = 64
        const val OUTER_SPEED = 0.92f
        const val OUTER_SPREAD = 0.16f
        const val INNER_SPEED = 0.5f
        const val INNER_SPREAD = 0.2f
        const val TILT = 0.55f
        const val SAG = 0.38f

        /** How fast sparks open: a share of the burst's life, kept between these seconds. */
        const val OPEN_SHARE = 0.26f
        const val OPEN_MIN = 0.08f
        const val OPEN_MAX = 0.42f
        const val FADE_POWER = 1.35f
        const val HEAT_SECONDS = 0.16f

        /** Each streak pass: how far back in seconds it reaches, how wide, how strong. Long and faint to short and hot. */
        val STREAK_SECONDS = floatArrayOf(0.3f, 0.13f, 0.045f)
        val STREAK_WIDTH = floatArrayOf(0.45f, 0.75f, 1f)
        val STREAK_ALPHA = floatArrayOf(0.3f, 0.5f, 1f)
        const val HEAD_WIDTH = 1.15f
        const val GLOW_SIZE = 4.5f
        const val GLOW_ALPHA = 0.3f
        const val GLOW_ROOM = 4f
        const val HEAD_ROOM = 8f
        const val MIN_HEAD = 0.12f

        const val CORE_MIN = 0.0028f
        const val CORE_EXTRA = 0.0028f

        const val BLOOM_SIZE = 1.3f
        const val BLOOM_ALPHA = 0.28f
        const val BLOOM_RISE = 0.06f
        const val BLOOM_FALL = 0.22f

        const val SALT_PLACE = 31
        const val SALT_START = 32
        const val SALT_HEIGHT = 33
        const val SALT_SIZE = 34
        const val SALT_LOOK = 35
        const val SALT_DRIFT = 36
        const val SALT_COUNT = 37
        const val SALT_TWIST = 38
        const val SALT_ANGLE = 39
        const val SALT_TILT = 40
        const val SALT_SPEED = 41
        const val SALT_SAG = 42
        const val SALT_EMBER = 43

        const val TWO_PI = (Math.PI * 2.0).toFloat()
        const val DOT_PIXELS = 64

        const val GOLD = 0xFFFFB534.toInt()
        const val GOLD_HOT = 0xFFFFF1C9.toInt()
        const val TEAL = 0xFF2BF5E6.toInt()
        const val TEAL_HOT = 0xFFDAFFFC.toInt()
        const val MAGENTA = 0xFFFF2FCF.toInt()
        const val MAGENTA_HOT = 0xFFFFDAF6.toInt()
        const val SILVER = 0xFFB9CCFF.toInt()
        const val SILVER_HOT = 0xFFFFFFFF.toInt()

        /** Each burst's outer ring colors, then inner ring colors: main color, then hot color. */
        val LOOKS = arrayOf(
            arrayOf(intArrayOf(GOLD, GOLD_HOT), intArrayOf(SILVER, SILVER_HOT)),
            arrayOf(intArrayOf(TEAL, TEAL_HOT), intArrayOf(SILVER, SILVER_HOT)),
            arrayOf(intArrayOf(MAGENTA, MAGENTA_HOT), intArrayOf(GOLD, GOLD_HOT)),
            arrayOf(intArrayOf(SILVER, SILVER_HOT), intArrayOf(TEAL, TEAL_HOT)),
            arrayOf(intArrayOf(GOLD, GOLD_HOT), intArrayOf(MAGENTA, MAGENTA_HOT)),
            arrayOf(intArrayOf(TEAL, TEAL_HOT), intArrayOf(MAGENTA, MAGENTA_HOT)),
        )

        fun frac(x: Float): Float = x - floor(x)

        /** Mixes from color [a] to color [b] by [t] from 0 to 1. */
        fun mix(a: Int, b: Int, t: Float): Int {
            val k = t.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
                (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
                (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt(),
            )
        }

        /** A soft round glow made once. It takes the paint's color. */
        fun makeGlowDot(): Bitmap {
            val n = DOT_PIXELS
            val bytes = ByteArray(n * n)
            val edge = exp(-4.2f)
            for (y in 0 until n) {
                for (x in 0 until n) {
                    val dx = (x + 0.5f) / n * 2f - 1f
                    val dy = (y + 0.5f) / n * 2f - 1f
                    val r2 = dx * dx + dy * dy
                    val a = if (r2 >= 1f) 0f else (exp(-4.2f * r2) - edge) / (1f - edge)
                    bytes[y * n + x] = (a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
            }
            val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
            return bitmap
        }
    }
}
