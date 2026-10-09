package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import java.nio.ByteBuffer
import java.text.BreakIterator
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Emoji rain (feature 2): the emojis from Emoji set fall from the top to the bottom of the
 * video, with a little sway and spin, on top of everything.
 *
 * Emoji density sets how many are on screen, 6 to 60. Emoji speed sets how long one takes to
 * fall the whole way, about 6 seconds down to 1.2 seconds. Emoji size sets how big they are,
 * about 4% to 14% of the width. Some fall a little smaller and slower, as if farther away.
 *
 * Each emoji loops with its own start, so the same time always draws the same picture.
 */
class EmojiRainLayer : OverlayLayer {
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { color = Color.BLACK }
    private val metrics = Paint.FontMetrics()
    private val softDot: Bitmap = makeSoftDot()
    private val spot = RectF()
    private var setText: String? = null
    private var glyphs: Array<String> = emptyArray()
    private var metricsSize = -1f
    private var middle = 0f

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.EMOJI_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        useSet(s.text(FxKeys.EMOJI_SET))
        val kinds = glyphs.size
        if (kinds == 0) return

        val count = COUNT_MIN + ((COUNT_MAX - COUNT_MIN) * s.value(FxKeys.EMOJI_DENSITY) + 0.5f).toInt()
        val calm = if (frame.reducedMotion) CALM else 1f
        val fall = EffectMath.lerp(FALL_SLOW, FALL_FAST, s.value(FxKeys.EMOJI_SPEED)) / calm
        val size = w * EffectMath.lerp(SIZE_MIN, SIZE_MAX, s.value(FxKeys.EMOJI_SIZE))
        val textSize = min(size * NEAR_SCALE, MAX_TEXT_PX)
        if (textSize != metricsSize) {
            metricsSize = textSize
            glyph.textSize = textSize
            glyph.getFontMetrics(metrics)
            middle = -(metrics.ascent + metrics.descent) / 2f
        }
        val time = frame.time

        // Far ones first, so near ones fall in front of them.
        for (i in 0 until count) {
            val depth = (i + 0.5f) / count
            val one = size * EffectMath.lerp(FAR_SCALE, NEAR_SCALE, depth) * (0.92f + 0.16f * EffectMath.hash(i, SALT_SIZE))
            val oneFall = fall * EffectMath.lerp(FAR_SLOWER, NEAR_FASTER, depth) * (0.9f + 0.2f * EffectMath.hash(i, SALT_FALL))
            val run = time / oneFall + EffectMath.hash(i, SALT_START)
            val cycle = floor(run).toInt()
            val p = run - cycle
            val y = -one + p * (h + 2f * one)

            // A new place across for every trip down, spread out with the golden ratio.
            val lane = frac(i * GOLDEN + LANE_JITTER * EffectMath.hash(i + cycle * 7919, SALT_LANE))
            val phase = EffectMath.hash(i, SALT_PHASE) * TWO_PI
            val swayTime = time / (SWAY_SECONDS + SWAY_SPREAD * EffectMath.hash(i, SALT_SWAY))
            val x = one * 0.5f + lane * (w - one) +
                sin(swayTime * TWO_PI + phase) * w * SWAY_SHARE * (0.6f + 0.4f * depth) * calm

            var turn = sin(time / (ROCK_SECONDS + EffectMath.hash(i, SALT_ROCK)) * TWO_PI + phase * 1.7f) * ROCK_DEGREES * calm
            if (EffectMath.hash(i, SALT_SPIN) < SPIN_SHARE) {
                val way = if (EffectMath.hash(i, SALT_WAY) < 0.5f) -1f else 1f
                turn += way * time * SPIN_DEGREES_PER_SECOND * calm
            }
            val pick = (EffectMath.hash(i * 977 + cycle, SALT_PICK) * kinds).toInt().coerceIn(0, kinds - 1)
            val alpha = EffectMath.lerp(FAR_ALPHA, 1f, depth)

            // A soft shadow just below each emoji, so it reads on bright and busy pictures.
            val shadow = one * SHADOW_SIZE
            spot.set(x - shadow / 2f, y + one * SHADOW_DROP - shadow / 2f, x + shadow / 2f, y + one * SHADOW_DROP + shadow / 2f)
            shade.alpha = (255f * SHADOW_ALPHA * alpha).toInt()
            canvas.drawBitmap(softDot, null, spot, shade)

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(turn)
            val k = one / textSize
            canvas.scale(k, k)
            glyph.alpha = (255f * alpha).toInt()
            canvas.drawText(glyphs[pick], 0f, middle, glyph)
            canvas.restore()
        }
    }

    /** Splits the typed set into whole emojis (a heart with its color mark counts as one). */
    private fun useSet(typed: String) {
        if (typed == setText) return
        setText = typed
        glyphs = split(typed).ifEmpty { split(DEFAULT_SET) }.toTypedArray()
    }

    private companion object {
        const val DEFAULT_SET = "❤️🔥✨😂🎉💎"
        const val MAX_KINDS = 32

        /** How many are on screen at Emoji density 0 and 100. */
        const val COUNT_MIN = 6
        const val COUNT_MAX = 60

        /** Seconds to fall the whole way at Emoji speed 0 and 100. */
        const val FALL_SLOW = 6f
        const val FALL_FAST = 1.2f

        /** Size as a share of the width at Emoji size 0 and 100. */
        const val SIZE_MIN = 0.04f
        const val SIZE_MAX = 0.14f

        /** Emoji text stays at or under this size. Bigger ones are scaled up instead. */
        const val MAX_TEXT_PX = 200f

        /** Less motion: half the speed, half the sway and spin. */
        const val CALM = 0.5f

        /** Far ones are smaller, slower, and a little see-through. */
        const val FAR_SCALE = 0.72f
        const val NEAR_SCALE = 1.22f
        const val FAR_SLOWER = 1.3f
        const val NEAR_FASTER = 0.82f
        const val FAR_ALPHA = 0.82f

        const val GOLDEN = 0.618034f
        const val LANE_JITTER = 0.3f
        const val SWAY_SECONDS = 2.2f
        const val SWAY_SPREAD = 1.6f
        const val SWAY_SHARE = 0.022f
        const val ROCK_SECONDS = 1.8f
        const val ROCK_DEGREES = 18f
        const val SPIN_SHARE = 0.3f
        const val SPIN_DEGREES_PER_SECOND = 70f

        const val SHADOW_SIZE = 1.35f
        const val SHADOW_DROP = 0.1f
        const val SHADOW_ALPHA = 0.3f

        const val SALT_SIZE = 11
        const val SALT_FALL = 12
        const val SALT_START = 13
        const val SALT_LANE = 14
        const val SALT_PHASE = 15
        const val SALT_SWAY = 16
        const val SALT_ROCK = 17
        const val SALT_SPIN = 18
        const val SALT_WAY = 19
        const val SALT_PICK = 20

        const val TWO_PI = (Math.PI * 2.0).toFloat()
        const val DOT_PIXELS = 48

        fun frac(x: Float): Float = x - floor(x)

        fun split(typed: String): List<String> {
            val out = ArrayList<String>()
            val walk = BreakIterator.getCharacterInstance()
            walk.setText(typed)
            var start = walk.first()
            var end = walk.next()
            while (end != BreakIterator.DONE && out.size < MAX_KINDS) {
                val piece = typed.substring(start, end)
                if (piece.isNotBlank() && piece != "," && piece != "️") out += piece
                start = end
                end = walk.next()
            }
            return out
        }

        /** A soft round dot made once. It takes the paint's color. */
        fun makeSoftDot(): Bitmap {
            val n = DOT_PIXELS
            val bytes = ByteArray(n * n)
            val edge = exp(-3.5f)
            for (y in 0 until n) {
                for (x in 0 until n) {
                    val dx = (x + 0.5f) / n * 2f - 1f
                    val dy = (y + 0.5f) / n * 2f - 1f
                    val r2 = dx * dx + dy * dy
                    val a = if (r2 >= 1f) 0f else (exp(-3.5f * r2) - edge) / (1f - edge)
                    bytes[y * n + x] = (a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
            }
            val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
            return bitmap
        }
    }
}
