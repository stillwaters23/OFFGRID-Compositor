package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectSnapshot
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.Fired
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Laser writing (feature 31). A laser writes the words in flowing script, one letter at a time.
 * A white-hot dot burns at the tip and throws a few sparks. The fresh line glows hot, then
 * cools to the laser color. The words stay a while, then fade out.
 *
 * The letter shapes are worked out once for each text and size, and kept until they change.
 */
class LaserWriteLayer : OverlayLayer {
    /** One closed line of a letter, with its own ruler so part of it can be drawn. */
    private class Piece(val measure: PathMeasure, val length: Float, val start: Float)

    /** One letter: its outline, its lines in drawing order, and its place in the text. */
    private class Glyph(val index: Int, val outline: Path, val pieces: Array<Piece>, val length: Float)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = scriptFace()
        style = Paint.Style.FILL
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val spark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val partial = Path()
    private val pos = FloatArray(2)

    private var glyphs: Array<Glyph> = emptyArray()
    private var glyphAt = IntArray(0)
    private var allOutline = Path()
    private var fontSize = 1f
    private var cacheText: String? = null
    private var cacheWidth = 0
    private var cacheHeight = 0
    private var cacheSize = -1

    private var tipShader: Shader? = null
    private var tipColor = 0

    override fun isActive(frame: EffectFrame): Boolean {
        val fired = lastWrite(frame) ?: return false
        val since = frame.time - fired.at
        return since >= 0f && since < EffectTimes.writeSeconds(frame.settings, textOf(fired, frame.settings))
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val fired = lastWrite(frame) ?: return
        val s = frame.settings
        val text = textOf(fired, s)
        val t = frame.time - fired.at
        if (t < 0f || frame.width <= 0 || frame.height <= 0) return
        layout(text, frame.width, frame.height, s.raw(FxKeys.WRITE_SIZE))
        if (glyphs.isEmpty()) return

        val letter = EffectTimes.writeLetterSeconds(s)
        val writeEnd = text.length * letter
        val stayEnd = writeEnd + EffectTimes.writeStaySeconds(s)
        val fade = if (t <= stayEnd) 1f else 1f - EffectMath.smooth((t - stayEnd) / EffectTimes.WRITE_FADE_SECONDS)
        if (fade <= 0f) return
        val color = EffectMath.hueColor(s.value(FxKeys.WRITE_HUE), LASER_SATURATION, LASER_BRIGHTNESS)
        val glow = s.value(FxKeys.WRITE_GLOW)
        val core = max(MIN_CORE_PX, fontSize * CORE_SHARE)
        val cool = if (frame.reducedMotion) COOL_SECONDS * 2f else COOL_SECONDS

        // Once every letter has cooled, the whole text is one shape and draws in one go.
        if (t > writeEnd + cool * ALL_COOL_TIMES) {
            drawLine(canvas, allOutline, color, 0f, glow, core, fade, filled = true)
            return
        }
        for (i in glyphs.indices) {
            val g = glyphs[i]
            val start = g.index * letter
            if (t < start) break
            val p = (t - start) / letter
            if (p >= 1f) {
                val heat = exp(-(t - start - letter) / cool)
                drawLine(canvas, g.outline, color, heat, glow, core, fade, filled = true)
            } else {
                val reach = p * g.length
                partial.rewind()
                for (piece in g.pieces) {
                    if (reach <= piece.start) break
                    piece.measure.getSegment(0f, min(piece.length, reach - piece.start), partial, true)
                }
                drawLine(canvas, partial, color, 1f, glow, core, fade, filled = false)
                if (tipAt(g, p)) drawTip(canvas, color, glow, core, fade)
            }
        }
        if (!frame.reducedMotion && t < writeEnd) drawSparks(canvas, t, letter, writeEnd, color, core, fade)
    }

    /** One line in its glow passes. [heat] 1 is white hot, 0 is cooled to [color]. */
    private fun drawLine(
        canvas: Canvas,
        path: Path,
        color: Int,
        heat: Float,
        glow: Float,
        core: Float,
        fade: Float,
        filled: Boolean,
    ) {
        if (filled) {
            fill.color = EffectMath.withAlpha(color, FILL_ALPHA * fade * (1f - FILL_HEAT_DIM * heat))
            canvas.drawPath(path, fill)
        }
        if (glow > 0f) {
            val glowColor = mix(color, Color.WHITE, GLOW_WHITE * heat)
            val spread = GLOW_SPREAD_MIN + (1f - GLOW_SPREAD_MIN) * glow
            for (pass in GLOW_PASSES) {
                line.strokeWidth = core * (1f + (pass[0] - 1f) * spread) * (1f + HOT_WIDEN * heat)
                line.color = EffectMath.withAlpha(glowColor, pass[1] * glow * (COOL_GLOW + (1f - COOL_GLOW) * heat) * fade)
                canvas.drawPath(path, line)
            }
        }
        line.strokeWidth = core
        line.color = EffectMath.withAlpha(mix(color, Color.WHITE, CORE_WHITE + (1f - CORE_WHITE) * heat), fade)
        canvas.drawPath(path, line)
    }

    private fun drawTip(canvas: Canvas, color: Int, glow: Float, core: Float, fade: Float) {
        if (tipShader == null || tipColor != color) {
            tipShader = RadialGradient(
                0f,
                0f,
                1f,
                intArrayOf(Color.WHITE, EffectMath.withAlpha(mix(color, Color.WHITE, 0.4f), 0.9f), EffectMath.withAlpha(color, 0.3f), 0),
                floatArrayOf(0f, 0.16f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            tipColor = color
        }
        tip.shader = tipShader
        tip.alpha = (255 * fade).toInt()
        val radius = core * TIP_GLOW * (TIP_GLOW_MIN + (1f - TIP_GLOW_MIN) * glow)
        canvas.save()
        canvas.translate(pos[0], pos[1])
        canvas.scale(radius, radius)
        canvas.drawCircle(0f, 0f, 1f, tip)
        canvas.restore()
        tip.shader = null
        tip.color = EffectMath.withAlpha(Color.WHITE, fade)
        canvas.drawCircle(pos[0], pos[1], core * TIP_CORE, tip)
    }

    /** Sparks thrown from where the tip was a moment ago. Each spark is a pure function of time. */
    private fun drawSparks(canvas: Canvas, t: Float, letter: Float, writeEnd: Float, color: Int, core: Float, fade: Float) {
        val newest = floor(t * SPARK_RATE).toInt()
        val life = SPARK_COUNT / SPARK_RATE
        for (k in 0 until SPARK_COUNT) {
            val n = newest - k
            val born = n / SPARK_RATE
            if (born < 0f || born >= writeEnd) continue
            val slot = (born / letter).toInt()
            if (slot < 0 || slot >= glyphAt.size) continue
            val gi = glyphAt[slot]
            if (gi < 0) continue
            if (!tipAt(glyphs[gi], born / letter - slot)) continue
            val age = t - born
            val angle = -PI.toFloat() / 2f + (EffectMath.hash(n, SALT_ANGLE) - 0.5f) * SPARK_SPREAD
            val speed = fontSize * (SPARK_SPEED_MIN + SPARK_SPEED_RANGE * EffectMath.hash(n, SALT_SPEED))
            val gravity = fontSize * SPARK_GRAVITY
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val tail = max(0f, age - SPARK_TAIL_SECONDS)
            val x1 = pos[0] + vx * age
            val y1 = pos[1] + vy * age + 0.5f * gravity * age * age
            val x0 = pos[0] + vx * tail
            val y0 = pos[1] + vy * tail + 0.5f * gravity * tail * tail
            val left = 1f - age / life
            spark.strokeWidth = core * SPARK_WIDTH * SPARK_GLOW
            spark.color = EffectMath.withAlpha(color, SPARK_GLOW_ALPHA * left * fade)
            canvas.drawLine(x0, y0, x1, y1, spark)
            spark.strokeWidth = core * SPARK_WIDTH
            spark.color = EffectMath.withAlpha(mix(Color.WHITE, color, 0.6f * (1f - left)), left * fade)
            canvas.drawLine(x0, y0, x1, y1, spark)
        }
    }

    /** Puts the tip of [glyph] at [progress] (0 to 1) into [pos]. False if the letter has no lines. */
    private fun tipAt(glyph: Glyph, progress: Float): Boolean {
        if (glyph.pieces.isEmpty()) return false
        val reach = EffectMath.clamp01(progress) * glyph.length
        for (piece in glyph.pieces) {
            if (reach <= piece.start + piece.length) {
                piece.measure.getPosTan(reach - piece.start, pos, null)
                return true
            }
        }
        val last = glyph.pieces[glyph.pieces.size - 1]
        last.measure.getPosTan(last.length, pos, null)
        return true
    }

    /** Works out the letter shapes for [text], wrapped to at most three lines in the upper third. */
    private fun layout(text: String, width: Int, height: Int, size: Int) {
        if (text == cacheText && width == cacheWidth && height == cacheHeight && size == cacheSize) return
        cacheText = text
        cacheWidth = width
        cacheHeight = height
        cacheSize = size

        val maxWidth = width * MAX_WIDTH_SHARE
        var px = height * (MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * size / 100f)
        var lines = wrap(text, px, maxWidth)
        var tries = 0
        while ((lines == null || lines.size > MAX_LINES) && tries < SHRINK_TRIES) {
            px *= SHRINK_STEP
            lines = wrap(text, px, maxWidth)
            tries++
        }
        textPaint.textSize = px
        fontSize = px
        val rows = lines ?: listOf(0 until text.length)
        val metrics = textPaint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent) * LINE_GAP
        val block = lineHeight * rows.size
        val top = max(height * TOP_MARGIN, height * CENTER_Y - block / 2f)

        val list = ArrayList<Glyph>()
        val all = Path()
        glyphAt = IntArray(text.length) { -1 }
        for (r in rows.indices) {
            val start = rows[r].first
            val end = rows[r].last + 1
            if (end <= start) continue
            val left = (width - textPaint.measureText(text, start, end)) / 2f
            val baseline = top + r * lineHeight - metrics.ascent
            for (glyph in lineGlyphs(text, start, end, left, baseline)) {
                glyphAt[glyph.index] = list.size
                list += glyph
                all.addPath(glyph.outline)
            }
        }
        glyphs = list.toTypedArray()
        allOutline = all
    }

    /**
     * The letters of one line. The whole line is shaped at once, so joins and spacing match
     * real script. Each closed line of the outline then goes to the letter it sits over.
     */
    private fun lineGlyphs(text: String, start: Int, end: Int, left: Float, baseline: Float): List<Glyph> {
        val edges = FloatArray(end - start + 1) { k -> left + textPaint.measureText(text, start, start + k) }
        val outline = Path()
        textPaint.getTextPath(text, start, end, left, baseline, outline)
        val parts = Array(end - start) { ArrayList<Path>() }
        val bounds = RectF()
        val measure = PathMeasure(outline, false)
        do {
            val length = measure.length
            if (length > MIN_PIECE_PX) {
                val piece = Path()
                measure.getSegment(0f, length, piece, true)
                piece.computeBounds(bounds, true)
                parts[letterAt(text, start, edges, bounds.centerX())] += piece
            }
        } while (measure.nextContour())

        val out = ArrayList<Glyph>()
        for (k in parts.indices) {
            if (parts[k].isEmpty()) continue
            val shape = Path()
            val pieces = ArrayList<Piece>()
            var offset = 0f
            for (piece in parts[k]) {
                val pm = PathMeasure(piece, false)
                pieces += Piece(pm, pm.length, offset)
                offset += pm.length
                shape.addPath(piece)
            }
            out += Glyph(start + k, shape, pieces.toTypedArray(), offset)
        }
        return out
    }

    /** Which letter (counted from [start]) is at [x], skipping spaces. */
    private fun letterAt(text: String, start: Int, edges: FloatArray, x: Float): Int {
        var k = 0
        while (k < edges.size - 2 && x >= edges[k + 1]) k++
        if (!text[start + k].isWhitespace()) return k
        var best = k
        var gap = Float.MAX_VALUE
        for (j in 0 until edges.size - 1) {
            if (text[start + j].isWhitespace()) continue
            val mid = (edges[j] + edges[j + 1]) / 2f
            if (abs(mid - x) < gap) {
                gap = abs(mid - x)
                best = j
            }
        }
        return best
    }

    /** Splits [text] into lines no wider than [maxWidth]. Null if one word alone is too wide. */
    private fun wrap(text: String, px: Float, maxWidth: Float): List<IntRange>? {
        textPaint.textSize = px
        val rows = ArrayList<IntRange>()
        for (para in paragraphs(text)) {
            var lineStart = -1
            var lineEnd = -1
            var i = para.first
            while (i <= para.last) {
                while (i <= para.last && text[i] == ' ') i++
                if (i > para.last) break
                var j = i
                while (j <= para.last && text[j] != ' ') j++
                if (textPaint.measureText(text, i, j) > maxWidth) return null
                if (lineStart < 0) {
                    lineStart = i
                    lineEnd = j
                } else if (textPaint.measureText(text, lineStart, j) <= maxWidth) {
                    lineEnd = j
                } else {
                    rows += lineStart until lineEnd
                    lineStart = i
                    lineEnd = j
                }
                i = j
            }
            if (lineStart >= 0) rows += lineStart until lineEnd
        }
        return rows
    }

    /** The parts of [text] between line breaks, as index ranges. */
    private fun paragraphs(text: String): List<IntRange> {
        val out = ArrayList<IntRange>()
        var at = 0
        while (at <= text.length) {
            val stop = text.indexOf('\n', at).let { if (it < 0) text.length else it }
            out += at until stop
            at = stop + 1
        }
        return out
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        val k = EffectMath.clamp01(t)
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
        return Color.rgb(r, g, b)
    }

    private companion object {
        const val DEFAULT_TEXT = "OFFGRID"

        /** Saturation and brightness that make hue 50 the laser teal, #2BF5E6. */
        const val LASER_SATURATION = 0.82f
        const val LASER_BRIGHTNESS = 0.96f

        /** Text height: 6% to 18% of the video height. */
        const val MIN_HEIGHT = 0.06f
        const val MAX_HEIGHT = 0.18f
        const val MAX_WIDTH_SHARE = 0.88f
        const val MAX_LINES = 3
        const val SHRINK_STEP = 0.9f
        const val SHRINK_TRIES = 24
        const val LINE_GAP = 0.92f
        const val CENTER_Y = 0.22f
        const val TOP_MARGIN = 0.05f
        const val MIN_PIECE_PX = 0.5f

        const val CORE_SHARE = 0.03f
        const val MIN_CORE_PX = 1.5f
        const val CORE_WHITE = 0.5f
        const val GLOW_WHITE = 0.5f
        const val HOT_WIDEN = 0.35f
        const val COOL_GLOW = 0.6f
        const val GLOW_SPREAD_MIN = 0.4f
        const val FILL_ALPHA = 0.16f
        const val FILL_HEAT_DIM = 0.7f

        /** How long a fresh line takes to cool most of the way, in seconds. */
        const val COOL_SECONDS = 0.55f
        const val ALL_COOL_TIMES = 8f

        const val TIP_GLOW = 9f
        const val TIP_GLOW_MIN = 0.55f
        const val TIP_CORE = 1.35f

        const val SPARK_COUNT = 10
        const val SPARK_RATE = 45f
        const val SPARK_SPREAD = 2.8f
        const val SPARK_SPEED_MIN = 0.5f
        const val SPARK_SPEED_RANGE = 1.3f
        const val SPARK_GRAVITY = 7f
        const val SPARK_TAIL_SECONDS = 0.04f
        const val SPARK_WIDTH = 0.8f
        const val SPARK_GLOW = 3.5f
        const val SPARK_GLOW_ALPHA = 0.35f
        const val SALT_ANGLE = 311
        const val SALT_SPEED = 317

        /** Each glow pass: width as a multiple of the core, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(9f, 0.06f),
            floatArrayOf(5f, 0.12f),
            floatArrayOf(2.6f, 0.3f),
        )

        /** A flowing script face. Phones without one get a slanted serif. */
        fun scriptFace(): Typeface {
            val script = Typeface.create("cursive", Typeface.NORMAL)
            return if (script == Typeface.DEFAULT) Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC) else script
        }

        /** The newest laser writing, found without making a list iterator. */
        fun lastWrite(frame: EffectFrame): Fired? {
            val fired = frame.fired
            for (i in fired.size - 1 downTo 0) {
                if (fired[i].kind == Fire.LASER_WRITE) return fired[i]
            }
            return null
        }

        fun textOf(fired: Fired, s: EffectSnapshot): String =
            fired.arg?.takeIf { it.isNotBlank() } ?: s.text(FxKeys.WRITE_TEXT).ifBlank { DEFAULT_TEXT }
    }
}
