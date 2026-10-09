package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

/**
 * The security scanner radar (features 32 and 34). A round scope sits in the middle of the
 * video, with range rings, a crosshair, and a beam that sweeps round with a fading trail.
 *
 * In Face mode with a face in view, blips lock onto the eyes, nose, and mouth, with corner
 * brackets round the face and the words FACE LOCK. With no face, or in Room mode, blips are
 * spread round the room. They light up when the beam passes and fade after.
 *
 * Mirror plus scan uses its own speed and blip count, and adds a small SCAN ON tag.
 */
class RadarLayer : OverlayLayer {
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trail = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val blip = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = LABEL_SPACING
    }
    private val metrics = Paint.FontMetrics()
    private val ticks = Path()
    private val bracket = Path()
    private val box = RectF()
    private val turn = Matrix()

    private var shaderColor = 0
    private var discShader: Shader? = null
    private var sweepShader: Shader? = null
    private var blipShader: Shader? = null
    private var tickRadius = -1f
    private var tickX = 0f
    private var tickY = 0f

    override fun isActive(frame: EffectFrame): Boolean =
        frame.settings.on(FxKeys.RADAR_ON) || frame.settings.on(FxKeys.MIRROR_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val mirror = s.on(FxKeys.MIRROR_ON)
        val speed = s.value(if (mirror) FxKeys.MIRROR_SPEED else FxKeys.RADAR_SPEED)
        var lap = SLOW_TURN - (SLOW_TURN - FAST_TURN) * speed
        if (frame.reducedMotion) lap *= 2f
        val color = EffectMath.hueColor(s.value(FxKeys.RADAR_HUE))
        val glow = s.value(FxKeys.RADAR_GLOW)
        val radius = w * (MIN_SIZE + (MAX_SIZE - MIN_SIZE) * s.value(FxKeys.RADAR_SIZE)) / 2f
        val cx = w / 2f
        val cy = h / 2f
        val turnPart = frame.time / lap - floor(frame.time / lap)
        // The beam starts straight up and goes round clockwise.
        val beam = (turnPart * 2f * PI).toFloat() - HALF_PI
        shaders(color)

        // The dark glass of the scope, then the fading wedge behind the beam.
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius, radius)
        canvas.drawCircle(0f, 0f, 1f, disc)
        canvas.restore()
        turn.setRotate(Math.toDegrees(beam.toDouble()).toFloat())
        turn.postTranslate(cx, cy)
        sweepShader?.setLocalMatrix(turn)
        trail.alpha = (255 * (TRAIL_MIN + (1f - TRAIL_MIN) * glow)).toInt()
        canvas.drawCircle(cx, cy, radius, trail)

        drawScope(canvas, cx, cy, radius, color, glow, w)
        drawBeam(canvas, cx, cy, radius, beam, color, glow, w)

        val face = frame.face
        val faceMode = !s.chose(FxKeys.RADAR_MODE, FxKeys.RADAR_ROOM)
        if (face != null && (faceMode || mirror)) {
            val fx = w * if (s.on(FxKeys.FACE_FLIP_X)) 1f - face.x else face.x
            val fy = h * face.y
            val size = w * face.size
            val count = if (mirror) mirrorBlips(s.value(FxKeys.MIRROR_BLIPS)) else FACE_POINTS
            drawFaceBlips(canvas, frame.time, fx, fy, size, count, cx, cy, beam, color, w)
            drawLock(canvas, frame.time, fx, fy, size, color, glow, w, h)
        } else {
            val count = if (mirror) mirrorBlips(s.value(FxKeys.MIRROR_BLIPS)) else ROOM_SLOTS
            drawRoomBlips(canvas, frame.time, count, mirror, cx, cy, radius, beam, color, w)
        }
        if (mirror) drawScanTag(canvas, color, glow, w)
    }

    private fun drawScope(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, glow: Float, w: Float) {
        val fine = w * FINE_LINE
        // Range rings.
        pen.strokeWidth = fine
        pen.color = EffectMath.withAlpha(color, RING_ALPHA)
        for (i in 1 until RINGS) canvas.drawCircle(cx, cy, radius * i / RINGS, pen)
        // Crosshair.
        pen.color = EffectMath.withAlpha(color, CROSS_ALPHA)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, pen)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, pen)
        // Tick marks round the rim.
        if (radius != tickRadius || cx != tickX || cy != tickY) buildTicks(cx, cy, radius)
        pen.color = EffectMath.withAlpha(color, TICK_ALPHA)
        canvas.drawPath(ticks, pen)
        // The rim, with its glow.
        val rim = w * RIM_LINE
        for (pass in GLOW_PASSES) {
            pen.strokeWidth = rim * (1f + (pass[0] - 1f) * glow)
            pen.color = EffectMath.withAlpha(color, pass[1] * glow)
            canvas.drawCircle(cx, cy, radius, pen)
        }
        pen.strokeWidth = rim
        pen.color = mix(color, Color.WHITE, CORE_WHITE)
        canvas.drawCircle(cx, cy, radius, pen)
        // The small center mark.
        dot.color = mix(color, Color.WHITE, CORE_WHITE)
        canvas.drawCircle(cx, cy, rim * CENTER_DOT, dot)
    }

    private fun drawBeam(canvas: Canvas, cx: Float, cy: Float, radius: Float, beam: Float, color: Int, glow: Float, w: Float) {
        val ex = cx + cos(beam) * radius
        val ey = cy + sin(beam) * radius
        val core = w * BEAM_LINE
        for (pass in GLOW_PASSES) {
            pen.strokeWidth = core * (1f + (pass[0] - 1f) * glow)
            pen.color = EffectMath.withAlpha(color, pass[1] * (BEAM_GLOW_MIN + glow))
            canvas.drawLine(cx, cy, ex, ey, pen)
        }
        pen.strokeWidth = core
        pen.color = mix(color, Color.WHITE, BEAM_WHITE)
        canvas.drawLine(cx, cy, ex, ey, pen)
    }

    /** Blips spread round the room. Each one moves to a new place now and then, while it is dark. */
    private fun drawRoomBlips(
        canvas: Canvas,
        time: Float,
        count: Int,
        mirror: Boolean,
        cx: Float,
        cy: Float,
        radius: Float,
        beam: Float,
        color: Int,
        w: Float,
    ) {
        for (i in 0 until count) {
            val period = BLIP_LIFE_MIN + BLIP_LIFE_RANGE * EffectMath.hash(i, SALT_LIFE)
            val local = time / period + EffectMath.hash(i, SALT_OFFSET)
            val cycle = floor(local).toInt()
            val phase = local - cycle
            // The first 8 always show. The rest come and go, so the room has 8 to 14.
            if (!mirror && i >= ALWAYS_BLIPS && EffectMath.hash(i * PRIME_A + cycle, SALT_SHOW) > SOMETIMES) continue
            val seed = i * PRIME_B + cycle
            val angle = (2f * PI).toFloat() * EffectMath.hash(seed, SALT_ANGLE) + (EffectMath.hash(i, SALT_DRIFT) - 0.5f) * DRIFT * phase
            val r = radius * (BLIP_RING_MIN + BLIP_RING_RANGE * EffectMath.hash(seed, SALT_RING))
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            val edges = EffectMath.smooth(phase / BLIP_EDGE) * EffectMath.smooth((1f - phase) / BLIP_EDGE)
            val lit = litBy(beam, angle)
            drawBlip(canvas, x, y, lit * edges, color, w)
        }
    }

    /** Blips locked onto the face: eyes, nose, and mouth first, then more round the face. */
    private fun drawFaceBlips(
        canvas: Canvas,
        time: Float,
        fx: Float,
        fy: Float,
        size: Float,
        count: Int,
        cx: Float,
        cy: Float,
        beam: Float,
        color: Int,
        w: Float,
    ) {
        for (i in 0 until count) {
            val x: Float
            val y: Float
            if (i < FEATURES.size) {
                x = fx + FEATURES[i][0] * size
                y = fy + FEATURES[i][1] * size
            } else {
                val a = (2f * PI).toFloat() * EffectMath.hash(i, SALT_ANGLE) + time * FACE_DRIFT * (EffectMath.hash(i, SALT_DRIFT) - 0.5f)
                val r = size * (FACE_RING_MIN + FACE_RING_RANGE * EffectMath.hash(i, SALT_RING))
                x = fx + cos(a) * r
                y = fy + FACE_DROP * size + sin(a) * r * FACE_TALL
            }
            val angle = atan2(y - cy, x - cx)
            drawBlip(canvas, x, y, LOCKED_MIN + (1f - LOCKED_MIN) * litBy(beam, angle), color, w)
        }
    }

    private fun drawLock(canvas: Canvas, time: Float, fx: Float, fy: Float, size: Float, color: Int, glow: Float, w: Float, h: Float) {
        // The brackets breathe gently, about once every two seconds.
        val breathe = 1f + BREATHE * sin((2f * PI).toFloat() * time / BREATHE_SECONDS)
        val halfW = size * BRACKET_W * breathe
        val halfH = size * BRACKET_H * breathe
        val top = fy + FACE_DROP * size - halfH
        val bottom = fy + FACE_DROP * size + halfH
        val left = fx - halfW
        val right = fx + halfW
        val arm = halfW * ARM
        bracket.rewind()
        bracket.moveTo(left, top + arm)
        bracket.lineTo(left, top)
        bracket.lineTo(left + arm, top)
        bracket.moveTo(right - arm, top)
        bracket.lineTo(right, top)
        bracket.lineTo(right, top + arm)
        bracket.moveTo(right, bottom - arm)
        bracket.lineTo(right, bottom)
        bracket.lineTo(right - arm, bottom)
        bracket.moveTo(left + arm, bottom)
        bracket.lineTo(left, bottom)
        bracket.lineTo(left, bottom - arm)
        val core = w * BRACKET_LINE
        for (pass in GLOW_PASSES) {
            pen.strokeWidth = core * (1f + (pass[0] - 1f) * glow)
            pen.color = EffectMath.withAlpha(color, pass[1] * glow)
            canvas.drawPath(bracket, pen)
        }
        pen.strokeWidth = core
        pen.color = mix(color, Color.WHITE, CORE_WHITE)
        canvas.drawPath(bracket, pen)

        label.textSize = w * LABEL_SIZE
        val above = top - label.textSize * LABEL_GAP
        val baseline = if (above - label.textSize > h * EDGE_ROOM) above else bottom + label.textSize * (LABEL_GAP + 1f)
        drawTag(canvas, FACE_LOCK, fx, baseline, 0f, color, glow)
    }

    private fun drawScanTag(canvas: Canvas, color: Int, glow: Float, w: Float) {
        label.textSize = w * TAG_SIZE
        val textWidth = label.measureText(SCAN_ON)
        val pad = label.textSize * TAG_PAD
        val left = w * TAG_MARGIN
        val top = w * TAG_MARGIN
        val dotRoom = label.textSize * DOT_ROOM
        label.getFontMetrics(metrics)
        val baseline = top + pad - metrics.ascent
        drawTag(canvas, SCAN_ON, left + pad + dotRoom + textWidth / 2f, baseline, dotRoom, color, glow)
        dot.color = mix(color, Color.WHITE, CORE_WHITE)
        canvas.drawCircle(left + pad + dotRoom * 0.4f, baseline - label.textSize * 0.36f, label.textSize * 0.22f, dot)
    }

    /** Words in a small dark box with a thin glowing edge, centered on [x]. [extra] leaves room on the left. */
    private fun drawTag(canvas: Canvas, text: String, x: Float, baseline: Float, extra: Float, color: Int, glow: Float) {
        val half = label.measureText(text) / 2f
        val pad = label.textSize * TAG_PAD
        label.getFontMetrics(metrics)
        box.set(x - half - pad - extra, baseline + metrics.ascent - pad, x + half + pad, baseline + metrics.descent + pad)
        val corner = label.textSize * TAG_CORNER
        dot.color = EffectMath.withAlpha(Color.BLACK, TAG_BACK)
        canvas.drawRoundRect(box, corner, corner, dot)
        pen.strokeWidth = label.textSize * TAG_EDGE
        pen.color = EffectMath.withAlpha(color, TAG_EDGE_ALPHA)
        canvas.drawRoundRect(box, corner, corner, pen)
        label.color = mix(color, Color.WHITE, LABEL_WHITE)
        label.setShadowLayer(label.textSize * LABEL_GLOW * (0.3f + glow), 0f, 0f, color)
        canvas.drawText(text, x, baseline, label)
        label.clearShadowLayer()
    }

    private fun drawBlip(canvas: Canvas, x: Float, y: Float, lit: Float, color: Int, w: Float) {
        if (lit <= MIN_LIT) return
        val core = w * BLIP_SIZE
        blip.alpha = (255 * EffectMath.clamp01(lit)).toInt()
        canvas.save()
        canvas.translate(x, y)
        val r = core * BLIP_GLOW
        canvas.scale(r, r)
        canvas.drawCircle(0f, 0f, 1f, blip)
        canvas.restore()
        dot.color = EffectMath.withAlpha(mix(color, Color.WHITE, BLIP_WHITE), lit)
        canvas.drawCircle(x, y, core * (BLIP_CORE_MIN + (1f - BLIP_CORE_MIN) * lit), dot)
    }

    /** How bright a blip at [angle] is: 1 just as the beam passes, then fading. */
    private fun litBy(beam: Float, angle: Float): Float {
        val full = (2f * PI).toFloat()
        var behind = (beam - angle) % full
        if (behind < 0f) behind += full
        return exp(-(behind / full) / FADE_TURNS)
    }

    private fun buildTicks(cx: Float, cy: Float, radius: Float) {
        tickRadius = radius
        tickX = cx
        tickY = cy
        ticks.rewind()
        for (k in 0 until TICKS) {
            val a = (2f * PI * k / TICKS).toFloat()
            val len = if (k % LONG_TICK_EVERY == 0) LONG_TICK else SHORT_TICK
            val c = cos(a)
            val sn = sin(a)
            ticks.moveTo(cx + c * radius, cy + sn * radius)
            ticks.lineTo(cx + c * radius * (1f - len), cy + sn * radius * (1f - len))
        }
    }

    /** Rebuilds the color shaders only when the color changes. */
    private fun shaders(color: Int) {
        if (discShader != null && shaderColor == color) return
        shaderColor = color
        val deep = mix(color, Color.BLACK, DISC_DARK)
        discShader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(EffectMath.withAlpha(deep, DISC_CENTER), EffectMath.withAlpha(deep, DISC_MID), EffectMath.withAlpha(deep, DISC_EDGE)),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        disc.shader = discShader
        sweepShader = SweepGradient(
            0f,
            0f,
            intArrayOf(0, 0, EffectMath.withAlpha(color, 0.05f), EffectMath.withAlpha(color, 0.16f), EffectMath.withAlpha(color, 0.42f)),
            floatArrayOf(0f, 1f - TRAIL_SHARE, 1f - TRAIL_SHARE * 0.55f, 1f - TRAIL_SHARE * 0.18f, 1f),
        )
        trail.shader = sweepShader
        blipShader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(EffectMath.withAlpha(mix(color, Color.WHITE, 0.5f), 0.95f), EffectMath.withAlpha(color, 0.45f), 0),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP,
        )
        blip.shader = blipShader
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        val k = EffectMath.clamp01(t)
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
        return Color.rgb(r, g, b)
    }

    private companion object {
        const val FACE_LOCK = "FACE LOCK"
        const val SCAN_ON = "SCAN ON"
        const val HALF_PI = (PI / 2).toFloat()

        /** Scope size: 30% to 95% of the video width. */
        const val MIN_SIZE = 0.3f
        const val MAX_SIZE = 0.95f

        /** One turn: 6 seconds slow, 1 second fast. */
        const val SLOW_TURN = 6f
        const val FAST_TURN = 1f

        const val RINGS = 4
        const val TICKS = 72
        const val LONG_TICK_EVERY = 6
        const val LONG_TICK = 0.07f
        const val SHORT_TICK = 0.03f
        const val FINE_LINE = 0.0018f
        const val RIM_LINE = 0.0035f
        const val BEAM_LINE = 0.003f
        const val BRACKET_LINE = 0.005f
        const val CENTER_DOT = 1.6f
        const val RING_ALPHA = 0.42f
        const val CROSS_ALPHA = 0.3f
        const val TICK_ALPHA = 0.6f
        const val CORE_WHITE = 0.35f
        const val BEAM_WHITE = 0.6f
        const val BEAM_GLOW_MIN = 0.35f
        const val TRAIL_MIN = 0.55f
        const val TRAIL_SHARE = 0.24f
        const val DISC_DARK = 0.88f
        const val DISC_CENTER = 0.16f
        const val DISC_MID = 0.24f
        const val DISC_EDGE = 0.42f

        const val ROOM_SLOTS = 14
        const val ALWAYS_BLIPS = 8
        const val SOMETIMES = 0.6f
        const val BLIP_LIFE_MIN = 14f
        const val BLIP_LIFE_RANGE = 14f
        const val BLIP_EDGE = 0.06f
        const val BLIP_RING_MIN = 0.16f
        const val BLIP_RING_RANGE = 0.76f
        const val DRIFT = 0.5f
        const val FADE_TURNS = 0.45f
        const val BLIP_SIZE = 0.011f
        const val BLIP_GLOW = 4.2f
        const val BLIP_CORE_MIN = 0.55f
        const val BLIP_WHITE = 0.7f
        const val MIN_LIT = 0.01f
        const val PRIME_A = 131
        const val PRIME_B = 977
        const val SALT_LIFE = 11
        const val SALT_OFFSET = 13
        const val SALT_SHOW = 17
        const val SALT_ANGLE = 19
        const val SALT_RING = 23
        const val SALT_DRIFT = 29

        const val FACE_POINTS = 5
        const val LOCKED_MIN = 0.5f
        const val FACE_DRIFT = 0.25f
        const val FACE_RING_MIN = 0.3f
        const val FACE_RING_RANGE = 0.22f
        const val FACE_TALL = 1.3f
        const val FACE_DROP = 0.08f
        const val BRACKET_W = 0.68f
        const val BRACKET_H = 0.92f
        const val ARM = 0.32f
        const val BREATHE = 0.02f
        const val BREATHE_SECONDS = 2f

        const val LABEL_SIZE = 0.034f
        const val LABEL_SPACING = 0.16f
        const val LABEL_GAP = 0.9f
        const val LABEL_WHITE = 0.6f
        const val LABEL_GLOW = 0.35f
        const val EDGE_ROOM = 0.03f
        const val TAG_SIZE = 0.03f
        const val TAG_MARGIN = 0.05f
        const val TAG_PAD = 0.45f
        const val TAG_CORNER = 0.3f
        const val TAG_EDGE = 0.07f
        const val TAG_EDGE_ALPHA = 0.85f
        const val TAG_BACK = 0.55f
        const val DOT_ROOM = 0.9f

        /** Each glow pass: width as a multiple of the line, then strength. Wide, then tight. */
        val GLOW_PASSES = arrayOf(
            floatArrayOf(8f, 0.07f),
            floatArrayOf(4.5f, 0.13f),
            floatArrayOf(2.4f, 0.3f),
        )

        /** Eyes, nose, mouth corners, then forehead, chin, and cheeks, as face widths from the middle. */
        val FEATURES = arrayOf(
            floatArrayOf(-0.2f, -0.1f),
            floatArrayOf(0.2f, -0.1f),
            floatArrayOf(0f, 0.12f),
            floatArrayOf(-0.15f, 0.32f),
            floatArrayOf(0.15f, 0.32f),
            floatArrayOf(0f, -0.42f),
            floatArrayOf(0f, 0.56f),
            floatArrayOf(-0.36f, 0.12f),
            floatArrayOf(0.36f, 0.12f),
        )

        /** Mirror plus scan: 3 to 20 blips. */
        fun mirrorBlips(value: Float): Int = 3 + (17f * value + 0.5f).toInt()
    }
}
