package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * The status label (feature 11). The box turns deep OLED black at once, with very faint
 * screen lines and grain, and a status word in glowing laser letters fades in over Fade.
 *
 * The word is the typed Status text, in capitals, in the middle. Size 0 is small, 100 fills
 * the width. A long word always fits: it gets smaller. Words with spaces can go onto 2 or 3
 * lines when that lets them be bigger. The glow breathes slowly, about once every 3 seconds,
 * and holds still when the phone asks for less motion. Nothing ever blinks.
 *
 * [face] is the letter style. The app can pass the panel's own monospace font.
 */
class StatusLabelLayer(face: Typeface? = null) : OverlayLayer {
    private val letters = face ?: Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val black = Paint().apply { color = OLED }
    private val texture = Paint()
    private val band = Paint()
    private val spill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = letters
        textAlign = Paint.Align.CENTER
        letterSpacing = SPACING
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = letters
        textAlign = Paint.Align.CENTER
        letterSpacing = SPACING
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = letters
        textAlign = Paint.Align.CENTER
        letterSpacing = SPACING
    }
    private val metrics = Paint.FontMetrics()

    // The word, split into lines, and its size. Only worked out again when something changes.
    private val lines = arrayOfNulls<String>(MAX_LINES)
    private var lineCount = 0
    private var textSize = 0f
    private var widest = 0f
    private var laidText: String? = null
    private var laidWidth = 0
    private var laidHeight = 0
    private var laidSize = -1

    // Cached shaders, made again only when the size or the color changes.
    private var shadeWidth = 0
    private var shadeHeight = 0
    private var shadeColor = 0
    private var shadeWidest = -1f
    private var lineShader: Shader? = null
    private var grainShader: Shader? = null
    private var bandShader: Shader? = null
    private var spillShader: Shader? = null
    private var ruleShader: Shader? = null

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.chose(FxKeys.CAMERA_MODE, FxKeys.MODE_STATUS)

    override fun coversAll(frame: EffectFrame): Boolean = isActive(frame)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val s = frame.settings

        // The box is black at once, so the face never shows. Only the word fades in.
        canvas.drawRect(0f, 0f, w, h, black)
        layout(s.text(FxKeys.STATUS_TEXT), frame.width, frame.height, s.raw(FxKeys.STATUS_SIZE))
        val color = frame.panelColor or OPAQUE
        shaders(frame.width, frame.height, color)
        texture.shader = lineShader
        canvas.drawRect(0f, 0f, w, h, texture)
        texture.shader = grainShader
        canvas.drawRect(0f, 0f, w, h, texture)

        // A very soft lighter band rolls slowly down the screen, like an old monitor.
        val slow = if (frame.reducedMotion) CALM else 1f
        val roll = (frame.time * slow / ROLL_SECONDS) % 1f
        canvas.save()
        canvas.translate(0f, (roll * (1f + BAND_TALL) - BAND_TALL) * h)
        canvas.scale(1f, h * BAND_TALL)
        band.shader = bandShader
        canvas.drawRect(0f, 0f, w, 1f, band)
        canvas.restore()

        val fade = fadeIn(frame)
        if (fade <= 0f || lineCount == 0) return
        // A slow breath of the glow. It never goes dark, so it is never a flash.
        val breath = if (frame.reducedMotion) {
            1f
        } else {
            1f - BREATH + BREATH * (0.5f + 0.5f * sin(frame.time * TWO_PI / BREATH_SECONDS))
        }
        drawWord(canvas, w, h, color, fade, breath)
    }

    /** How far the word has faded in since status mode came on, 0 to 1. */
    private fun fadeIn(frame: EffectFrame): Float {
        if (frame.previousMode == FxKeys.MODE_STATUS) return 1f
        val length = EffectTimes.statusSeconds(frame.settings)
        return EffectMath.smooth(EffectMath.progress(frame.time, frame.modeChangedAt, length))
    }

    private fun drawWord(canvas: Canvas, w: Float, h: Float, color: Int, fade: Float, breath: Float) {
        val size = textSize
        core.textSize = size
        glow.textSize = size
        edge.textSize = size
        core.getFontMetrics(metrics)
        val step = size * LINE_GAP
        val cx = w / 2f
        // Capital letters sit from the baseline up to about this share of the size.
        val cap = size * CAP_SHARE
        val block = step * (lineCount - 1) + cap
        val firstBase = h / 2f - block / 2f + cap

        // Light spilling softly onto the black around the word.
        spill.shader = spillShader
        spill.alpha = (255 * fade * breath).toInt()
        canvas.save()
        canvas.translate(cx, h / 2f)
        canvas.scale(widest * 0.75f + size, block * 0.5f + size * 1.1f)
        canvas.drawCircle(0f, 0f, 1f, spill)
        canvas.restore()

        // Thin laser lines above and below the word.
        val ruleGap = size * RULE_GAP
        val ruleThick = maxOf(size * RULE_THICK, h * 0.0008f)
        val half = widest / 2f + size * RULE_REACH
        rule.shader = ruleShader
        rule.alpha = (255 * fade).toInt()
        rule.setShadowLayer(size * 0.08f, 0f, 0f, EffectMath.withAlpha(color, 0.9f * breath))
        val top = firstBase - cap - ruleGap
        val bottom = firstBase + step * (lineCount - 1) + ruleGap
        canvas.drawRect(cx - half, top - ruleThick / 2f, cx + half, top + ruleThick / 2f, rule)
        canvas.drawRect(cx - half, bottom - ruleThick / 2f, cx + half, bottom + ruleThick / 2f, rule)
        rule.clearShadowLayer()

        val hot = mix(color, Color.WHITE, HOT)
        glow.color = EffectMath.withAlpha(color, 0.5f * fade)
        glow.setShadowLayer(size * WIDE_GLOW, 0f, 0f, EffectMath.withAlpha(color, 0.85f * fade * breath))
        edge.color = EffectMath.withAlpha(color, 0.75f * fade)
        edge.strokeWidth = size * EDGE_WIDTH
        edge.setShadowLayer(size * NEAR_GLOW, 0f, 0f, EffectMath.withAlpha(color, fade * breath))
        core.color = EffectMath.withAlpha(hot, fade)
        for (i in 0 until lineCount) {
            val text = lines[i] ?: continue
            val y = firstBase + step * i
            canvas.drawText(text, cx, y, glow)
            canvas.drawText(text, cx, y, edge)
            canvas.drawText(text, cx, y, core)
        }
    }

    /** Splits the word into lines and picks its size. Only runs when the text or size changes. */
    private fun layout(raw: String, width: Int, height: Int, sizeSetting: Int) {
        if (raw == laidText && width == laidWidth && height == laidHeight && sizeSetting == laidSize) return
        laidText = raw
        laidWidth = width
        laidHeight = height
        laidSize = sizeSetting
        val text = raw.trim().ifEmpty { DEFAULT_TEXT }.uppercase(Locale.ROOT)
        val words = text.split(' ').filter { it.isNotEmpty() }
        val room = width * ROOM
        core.textSize = PROBE
        // The size where the words fill the width, for 1, 2, or 3 lines.
        var best = 0f
        var bestLines = 1
        for (count in 1..min(MAX_LINES, words.size)) {
            val split = splitInto(words, count)
            var wide = 0f
            for (line in split) wide = maxOf(wide, core.measureText(line))
            val fit = min(PROBE * room / wide, height * TALLEST / ((count - 1) * LINE_GAP + 1f))
            // Another line only wins when it makes the words clearly bigger.
            if (fit > best * WRAP_GAIN) {
                best = fit
                bestLines = count
            }
        }
        val split = splitInto(words, bestLines)
        lineCount = split.size
        for (i in 0 until MAX_LINES) lines[i] = split.getOrNull(i)
        val small = min(width * SMALLEST, best)
        textSize = EffectMath.lerp(small, best, sizeSetting / 100f)
        core.textSize = textSize
        widest = 0f
        for (line in split) widest = maxOf(widest, core.measureText(line))
    }

    /** The words in [count] lines of about the same length, keeping each word whole. */
    private fun splitInto(words: List<String>, count: Int): List<String> {
        if (count <= 1 || words.size <= 1) return listOf(words.joinToString(" "))
        val total = words.sumOf { it.length } + words.size - 1
        val target = total / count.toFloat()
        val out = ArrayList<String>(count)
        val now = StringBuilder()
        for ((i, word) in words.withIndex()) {
            val left = words.size - i
            val linesLeft = count - out.size
            val wouldBe = now.length + (if (now.isEmpty()) 0 else 1) + word.length
            val mustBreak = now.isNotEmpty() && linesLeft > 1 && (wouldBe > target * 1.15f || left < linesLeft)
            if (mustBreak) {
                out += now.toString()
                now.setLength(0)
            }
            if (now.isNotEmpty()) now.append(' ')
            now.append(word)
        }
        if (now.isNotEmpty()) out += now.toString()
        return out
    }

    /** Makes the screen lines, grain, band, and glow shaders. Only runs when they change. */
    private fun shaders(width: Int, height: Int, color: Int) {
        if (width == shadeWidth && height == shadeHeight && color == shadeColor && widest == shadeWidest) return
        val sizeChanged = width != shadeWidth || height != shadeHeight
        shadeWidth = width
        shadeHeight = height
        shadeColor = color
        shadeWidest = widest
        val h = height.toFloat()
        if (sizeChanged || lineShader == null) {
            // One faint line every few pixels, scaled to the picture so it looks the same on every phone.
            val pitch = maxOf(3, (h / LINE_COUNT).toInt())
            val rows = IntArray(pitch)
            rows[0] = Color.argb(LINE_ALPHA, 200, 230, 235)
            lineShader = BitmapShader(Bitmap.createBitmap(rows, 1, pitch, Bitmap.Config.ARGB_8888), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            grainShader = makeGrain()
        }
        bandShader = LinearGradient(
            0f, 0f, 0f, 1f,
            intArrayOf(0, EffectMath.withAlpha(mix(color, Color.WHITE, 0.6f), BAND_ALPHA), 0),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        spillShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(EffectMath.withAlpha(color, SPILL), EffectMath.withAlpha(color, SPILL * 0.35f), 0),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        val cx = width / 2f
        val half = widest / 2f + textSize * RULE_REACH
        ruleShader = LinearGradient(
            cx - half, 0f, cx + half, 0f,
            intArrayOf(0, EffectMath.withAlpha(color, 0.8f), mix(color, Color.WHITE, 0.7f), EffectMath.withAlpha(color, 0.8f), 0),
            floatArrayOf(0f, 0.22f, 0.5f, 0.78f, 1f), Shader.TileMode.CLAMP,
        )
    }

    private companion object {
        const val OLED = 0xFF010203.toInt()
        const val OPAQUE = 0xFF000000.toInt()
        const val DEFAULT_TEXT = "CLOAKED"
        const val TWO_PI = (PI * 2).toFloat()
        const val CALM = 0.5f

        const val SPACING = 0.14f
        const val MAX_LINES = 3
        const val LINE_GAP = 1.25f
        const val CAP_SHARE = 0.72f
        const val PROBE = 100f

        /** Size 100 fills this share of the width. Size 0 is this share of the width tall. */
        const val ROOM = 0.88f
        const val SMALLEST = 0.045f
        const val TALLEST = 0.55f
        const val WRAP_GAIN = 1.3f

        const val HOT = 0.62f
        const val WIDE_GLOW = 0.3f
        const val NEAR_GLOW = 0.07f
        const val EDGE_WIDTH = 0.035f
        const val SPILL = 0.13f

        /** The glow breathes from full to this much less and back, once every few seconds. */
        const val BREATH = 0.22f
        const val BREATH_SECONDS = 3.2f

        const val RULE_GAP = 0.42f
        const val RULE_THICK = 0.012f
        const val RULE_REACH = 0.45f

        const val LINE_COUNT = 600f
        const val LINE_ALPHA = 14
        const val ROLL_SECONDS = 7f
        const val BAND_TALL = 0.3f
        const val BAND_ALPHA = 0.035f
        const val GRAIN_TILE = 128
        const val SALT_GRAIN = 401

        fun mix(from: Int, to: Int, t: Float): Int {
            val k = EffectMath.clamp01(t)
            val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
            val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
            val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
            return Color.rgb(r, g, b)
        }

        /** A tile of very faint specks, the same every time, so the black looks like a real screen. */
        fun makeGrain(): Shader {
            val size = GRAIN_TILE
            val pixels = IntArray(size * size)
            for (i in pixels.indices) {
                val n = EffectMath.hash(i, SALT_GRAIN)
                pixels[i] = if (n > 0.82f) Color.argb((6 + 16 * (n - 0.82f) / 0.18f).toInt(), 210, 235, 240) else 0
            }
            val tile = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            return BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }
}
