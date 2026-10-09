package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.SweepGradient
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Glow border (feature 17): a band of light runs around the edge of the video, like a comet
 * chasing its own tail. A dim outline stays lit on the whole edge.
 *
 * Border speed sets one lap: 8 seconds at 0, 1.5 seconds at 100. Border thickness sets the
 * line, from 0.4% to 3% of the width. Border glow sets how big and bright the glow is.
 * Border color is the panel color, white, a rainbow, or one hue.
 *
 * The edge is a box with round corners. Its plan is worked out once per size, and the comet
 * is drawn as short straight pieces and arcs along it, so nothing is made while drawing.
 */
class GlowBorderLayer : OverlayLayer {
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowDot: Bitmap = makeGlowDot()
    private val spot = RectF()
    private val arcBox = RectF()
    private val edgePath = Path()
    private val tailPath = Path()
    private val measure = PathMeasure()
    private val rainbowTurn = Matrix()
    private var rainbow: SweepGradient? = null
    private val point = FloatArray(2)

    // The edge plan: 4 straight sides and 4 corner arcs, in order, clockwise from the top left.
    private var planWidth = -1
    private var planHeight = -1
    private var planInset = -1f
    private var perimeter = 1f
    private var corner = 1f
    private val pieceStart = FloatArray(PIECES)
    private val pieceLength = FloatArray(PIECES)
    private val pieceX = FloatArray(PIECES)
    private val pieceY = FloatArray(PIECES)

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.BORDER_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0) return
        val unit = min(w, h).toFloat()
        val thick = unit * EffectMath.lerp(THICK_MIN, THICK_MAX, s.value(FxKeys.BORDER_THICKNESS))
        val glow = (s.value(FxKeys.BORDER_GLOW) + frame.beat * BEAT_GLOW_BOOST).coerceAtMost(1f)
        var lap = EffectMath.lerp(LAP_SLOW, LAP_FAST, s.value(FxKeys.BORDER_SPEED))
        if (frame.reducedMotion) lap *= CALM_SLOWER
        plan(w, h, thick / 2f, unit)

        val choice = s.choice(FxKeys.BORDER_COLOR)
        val isRainbow = choice == FxKeys.COLOR_RAINBOW
        val base = when (choice) {
            FxKeys.COLOR_WHITE -> WHITE_LIGHT
            FxKeys.COLOR_HUE -> EffectMath.hueColor(s.value(FxKeys.BORDER_HUE))
            else -> frame.panelColor or OPAQUE
        }
        val hueStart = frac(frame.time * HUE_DRIFT)

        drawOutline(canvas, frame, thick, glow, base, isRainbow, hueStart)

        // The comet. Its head moves at a steady pace. The tail fades and thins behind it.
        val head = frac(frame.time / lap) * perimeter
        val tail = perimeter * TAIL_SHARE

        // Soft glow first, as overlapping soft dots along the tail.
        val glowSize = thick * (GLOW_BASE + GLOW_EXTRA * glow) + unit * GLOW_UNIT * glow
        val spacing = max(glowSize * DOT_SPACING, unit * MIN_STEP)
        val dots = (tail / spacing).toInt()
        val glowStrength = GLOW_ALPHA_MIN + (1f - GLOW_ALPHA_MIN) * glow
        for (k in dots downTo 0) {
            val back = k * spacing
            val f = 1f - back / tail
            pointAt(head - back)
            val color = if (isRainbow) rainbowAt(hueStart, f) else base
            drawDot(canvas, point[0], point[1], glowSize * (0.45f + 0.55f * f), color, DOT_ALPHA * glowStrength * f.pow(1.7f))
        }

        // The bright line: layers that all end at the head, longest first. They add up to a
        // smooth fade with no seams, thick and white hot near the head, thin and faint at the end.
        for (m in 0 until LAYERS) {
            val f = 1f - (LAYERS - m - 0.5f) / LAYERS
            val color = if (isRainbow) rainbowAt(hueStart, f) else base
            core.color = EffectMath.withAlpha(color, LAYER_ALPHA[m])
            core.strokeWidth = thick * (0.38f + 0.62f * sqrt(f))
            drawTail(canvas, head, tail * (LAYERS - m) / LAYERS)
        }
        val headColor = if (isRainbow) rainbowAt(hueStart, 1f) else base
        for (m in HEAT_LENGTH.indices) {
            core.color = EffectMath.withAlpha(towardWhite(headColor, HEAT_WHITE[m]), HEAT_ALPHA[m])
            core.strokeWidth = thick * HEAT_WIDTH[m]
            drawTail(canvas, head, tail * HEAT_LENGTH[m])
        }

        // The head: a wide spill of light, a colored halo, and a small white-hot center.
        pointAt(head)
        drawDot(canvas, point[0], point[1], unit * SPILL_SIZE * (0.4f + glow), headColor, SPILL_ALPHA * glowStrength)
        drawDot(canvas, point[0], point[1], glowSize * HEAD_HALO, headColor, HALO_ALPHA * glowStrength)
        drawDot(canvas, point[0], point[1], thick * HEAD_CORE + unit * 0.012f, towardWhite(headColor, 0.85f), 1f)
    }

    /** The dim outline on the whole edge, with a faint glow inward. */
    private fun drawOutline(
        canvas: Canvas,
        frame: EffectFrame,
        thick: Float,
        glow: Float,
        base: Int,
        isRainbow: Boolean,
        hueStart: Float,
    ) {
        if (isRainbow) {
            val ring = rainbow ?: SweepGradient(frame.width / 2f, frame.height / 2f, RAINBOW, null).also { rainbow = it }
            rainbowTurn.setRotate(hueStart * 360f, frame.width / 2f, frame.height / 2f)
            ring.setLocalMatrix(rainbowTurn)
            outline.shader = ring
            outline.color = Color.WHITE
        } else {
            outline.shader = null
            outline.color = base
        }
        for (i in WASH_WIDTH.indices) {
            outline.strokeWidth = thick * WASH_WIDTH[i] * (0.6f + glow)
            outline.alpha = (255f * WASH_ALPHA * (0.4f + glow)).toInt()
            canvas.drawPath(edgePath, outline)
        }
        outline.strokeWidth = thick * OUTLINE_WIDTH
        outline.alpha = (255f * (OUTLINE_ALPHA + OUTLINE_GLOW_ALPHA * glow)).toInt()
        canvas.drawPath(edgePath, outline)
        outline.shader = null
    }

    /** Works out the edge plan for this size: where each side and corner starts and how long it is. */
    private fun plan(w: Int, h: Int, inset: Float, unit: Float) {
        if (w == planWidth && h == planHeight && inset == planInset) return
        planWidth = w
        planHeight = h
        planInset = inset
        rainbow = null
        val left = inset
        val top = inset
        val right = w - inset
        val bottom = h - inset
        corner = min(unit * CORNER_SHARE, min(right - left, bottom - top) / 2f).coerceAtLeast(1f)
        val r = corner
        val across = (right - left - 2f * r).coerceAtLeast(0f)
        val down = (bottom - top - 2f * r).coerceAtLeast(0f)
        val arc = (Math.PI / 2.0).toFloat() * r
        // Straight sides keep their start point. Arcs keep their middle point.
        setPiece(0, across, left + r, top)
        setPiece(1, arc, right - r, top + r)
        setPiece(2, down, right, top + r)
        setPiece(3, arc, right - r, bottom - r)
        setPiece(4, across, right - r, bottom)
        setPiece(5, arc, left + r, bottom - r)
        setPiece(6, down, left, bottom - r)
        setPiece(7, arc, left + r, top + r)
        var run = 0f
        for (i in 0 until PIECES) {
            pieceStart[i] = run
            run += pieceLength[i]
        }
        perimeter = run.coerceAtLeast(1f)
        // The same loop as a path, in the same order, for the outline and the comet's line.
        edgePath.reset()
        edgePath.moveTo(left + r, top)
        edgePath.lineTo(right - r, top)
        arcBox.set(right - 2f * r, top, right, top + 2f * r)
        edgePath.arcTo(arcBox, -90f, 90f, false)
        edgePath.lineTo(right, bottom - r)
        arcBox.set(right - 2f * r, bottom - 2f * r, right, bottom)
        edgePath.arcTo(arcBox, 0f, 90f, false)
        edgePath.lineTo(left + r, bottom)
        arcBox.set(left, bottom - 2f * r, left + 2f * r, bottom)
        edgePath.arcTo(arcBox, 90f, 90f, false)
        edgePath.lineTo(left, top + r)
        arcBox.set(left, top, left + 2f * r, top + 2f * r)
        edgePath.arcTo(arcBox, 180f, 90f, false)
        edgePath.close()
        measure.setPath(edgePath, true)
    }

    private fun setPiece(i: Int, length: Float, x: Float, y: Float) {
        pieceLength[i] = length
        pieceX[i] = x
        pieceY[i] = y
    }

    private fun pieceAt(d: Float): Int {
        for (i in PIECES - 1 downTo 1) if (d >= pieceStart[i]) return i
        return 0
    }

    /** Puts the spot [d] along the edge into [point]. */
    private fun pointAt(d: Float) {
        val at = wrap(d)
        val i = pieceAt(at)
        val u = (at - pieceStart[i]).coerceIn(0f, pieceLength[i])
        if (i % 2 == 0) {
            point[0] = pieceX[i] + SIDE_X[i / 2] * u
            point[1] = pieceY[i] + SIDE_Y[i / 2] * u
        } else {
            val angle = Math.toRadians(ARC_START[i / 2].toDouble()) + u / corner
            point[0] = pieceX[i] + corner * cos(angle).toFloat()
            point[1] = pieceY[i] + corner * sin(angle).toFloat()
        }
    }

    /** Strokes the edge from [length] behind [head] up to [head], going round the corner of the loop if needed. */
    private fun drawTail(canvas: Canvas, head: Float, length: Float) {
        val from = head - length
        tailPath.reset()
        if (from >= 0f) {
            measure.getSegment(from, head, tailPath, true)
        } else {
            measure.getSegment(from + perimeter, perimeter, tailPath, true)
            measure.getSegment(0f, head, tailPath, false)
        }
        canvas.drawPath(tailPath, core)
    }

    private fun drawDot(canvas: Canvas, x: Float, y: Float, size: Float, color: Int, alpha: Float) {
        if (alpha <= 0.004f || size <= 0f) return
        val half = size / 2f
        spot.set(x - half, y - half, x + half, y + half)
        dot.color = EffectMath.withAlpha(color, alpha)
        canvas.drawBitmap(glowDot, null, spot, dot)
    }

    private fun wrap(d: Float): Float {
        val m = d % perimeter
        return if (m < 0f) m + perimeter else m
    }

    private fun rainbowAt(hueStart: Float, f: Float): Int = EffectMath.hueColor(hueStart + (1f - f) * RAINBOW_SPAN)

    private companion object {
        const val PIECES = 8
        const val OPAQUE = 0xFF000000.toInt()
        const val WHITE_LIGHT = 0xFFEEF8FF.toInt()

        /** Line thickness as a share of the short side: 0.4% to 3%. */
        const val THICK_MIN = 0.004f
        const val THICK_MAX = 0.03f

        /** Seconds for one lap at Border speed 0 and 100. */
        const val LAP_SLOW = 8f
        const val LAP_FAST = 1.5f

        /** Less motion: laps take twice as long. */
        const val CALM_SLOWER = 2f
        const val BEAT_GLOW_BOOST = 0.6f

        const val CORNER_SHARE = 0.045f

        /** The comet's tail, as a share of the whole edge. */
        const val TAIL_SHARE = 0.3f

        const val MIN_STEP = 0.006f

        /** The line is this many layers, each a little shorter, that add up to a smooth fade. */
        const val LAYERS = 24

        /** Each layer's strength, worked out so the line's strength behind the head is f^1.25. */
        val LAYER_ALPHA = FloatArray(LAYERS).also { alpha ->
            var before = 0f
            for (m in 0 until LAYERS) {
                val f = 1f - (LAYERS - m - 0.5f) / LAYERS
                val want = f.pow(1.25f)
                alpha[m] = 1f - (1f - want) / (1f - before)
                before = want
            }
        }

        /** Short white-hot layers right behind the head. */
        val HEAT_LENGTH = floatArrayOf(0.12f, 0.06f, 0.025f)
        val HEAT_WIDTH = floatArrayOf(0.8f, 0.66f, 0.55f)
        val HEAT_WHITE = floatArrayOf(0.3f, 0.6f, 0.9f)
        val HEAT_ALPHA = floatArrayOf(0.45f, 0.6f, 0.85f)

        const val GLOW_BASE = 3f
        const val GLOW_EXTRA = 9f
        const val GLOW_UNIT = 0.05f
        const val GLOW_ALPHA_MIN = 0.35f
        const val DOT_SPACING = 0.22f
        const val DOT_ALPHA = 0.42f

        const val SPILL_SIZE = 0.42f
        const val SPILL_ALPHA = 0.22f
        const val HEAD_HALO = 1.5f
        const val HALO_ALPHA = 0.75f
        const val HEAD_CORE = 2.6f

        const val OUTLINE_WIDTH = 0.5f
        const val OUTLINE_ALPHA = 0.28f
        const val OUTLINE_GLOW_ALPHA = 0.17f
        /** The faint inner glow: many wide, very faint strokes, so it fades smoothly inward. */
        val WASH_WIDTH = floatArrayOf(20f, 15f, 11f, 8f, 5.5f, 3.5f, 2f)
        const val WASH_ALPHA = 0.016f

        /** Rainbow hue turns slowly with time, and the tail spans most of the wheel. */
        const val HUE_DRIFT = 0.06f
        const val RAINBOW_SPAN = 0.75f
        val RAINBOW = intArrayOf(
            0xFFFF3B3B.toInt(), 0xFFFFD23B.toInt(), 0xFF3BFF6E.toInt(), 0xFF3BE8FF.toInt(),
            0xFF5B3BFF.toInt(), 0xFFFF3BE1.toInt(), 0xFFFF3B3B.toInt(),
        )

        /** Each straight side's direction, top, right, bottom, left. */
        val SIDE_X = floatArrayOf(1f, 0f, -1f, 0f)
        val SIDE_Y = floatArrayOf(0f, 1f, 0f, -1f)

        /** Each corner arc's start angle in degrees: top right, bottom right, bottom left, top left. */
        val ARC_START = floatArrayOf(-90f, 0f, 90f, 180f)

        const val DOT_PIXELS = 64

        fun frac(x: Float): Float = x - floor(x)

        /** Mixes [color] toward white by [amount] from 0 to 1. */
        fun towardWhite(color: Int, amount: Float): Int {
            val a = amount.coerceIn(0f, 1f)
            val r = Color.red(color) + ((255 - Color.red(color)) * a).toInt()
            val g = Color.green(color) + ((255 - Color.green(color)) * a).toInt()
            val b = Color.blue(color) + ((255 - Color.blue(color)) * a).toInt()
            return Color.rgb(r, g, b)
        }

        /** A soft round glow made once: bright in the middle, gone at the edge. It takes the paint's color. */
        fun makeGlowDot(): Bitmap {
            val n = DOT_PIXELS
            val bytes = ByteArray(n * n)
            val edgeFalloff = exp(-4.2f)
            for (y in 0 until n) {
                for (x in 0 until n) {
                    val dx = (x + 0.5f) / n * 2f - 1f
                    val dy = (y + 0.5f) / n * 2f - 1f
                    val r2 = dx * dx + dy * dy
                    val a = if (r2 >= 1f) 0f else (exp(-4.2f * r2) - edgeFalloff) / (1f - edgeFalloff)
                    bytes[y * n + x] = (a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
                }
            }
            val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ALPHA_8)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
            return bitmap
        }
    }
}
