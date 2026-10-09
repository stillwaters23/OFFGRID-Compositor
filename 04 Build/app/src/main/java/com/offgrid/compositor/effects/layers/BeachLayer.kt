package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * The beach (feature 24). A calm ocean beach fills the whole box, in place of the camera.
 * No people, only the sea, the sky, the sun and a few far gulls.
 *
 * Waves roll in from the far line of the sea, break into white foam, and wash up the sand,
 * then slide back and leave the sand wet for a moment. Waves sets how fast and how big.
 * Brightness goes from dusk (0, the sun on the sea line, the first stars) to bright noon (100).
 *
 * Everything is worked out from the time and the settings, so a frame always looks the same.
 */
class BeachLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val plain = Paint().apply { style = Paint.Style.FILL }
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val grain = Paint().apply { style = Paint.Style.FILL }
    private val path = Path()
    private val ripples = Path()
    private val edge = FloatArray(SEGMENTS + 1)
    private val wet = FloatArray(SEGMENTS + 1)

    // Cached look. It is only made again when the size or brightness changes.
    private var lookWidth = 0
    private var lookHeight = 0
    private var lookBright = -1
    private var skyShader: Shader? = null
    private var seaShader: Shader? = null
    private var sandShader: Shader? = null
    private var dampShader: Shader? = null
    private var shallowShader: Shader? = null
    private var glowShader: Shader? = null
    private var sunShader: Shader? = null
    private var shineShader: Shader? = null
    private var hazeShader: Shader? = null
    private var cloudShader: Shader? = null
    private var shadeShader: Shader? = null
    private var grainShader: Shader? = null

    private var horizon = 0f
    private var breakLine = 0f
    private var shore = 0f
    private var sunX = 0f
    private var sunY = 0f
    private var sunRadius = 0f
    private var glowRadius = 0f
    private var bright = 0f
    private var crestColor = 0
    private var faceColor = 0
    private var foamColor = 0
    private var wetColor = 0
    private var glintColor = 0
    private var gullColor = 0
    private var landColor = 0
    private var starAmount = 0f
    private var shineAmount = 0f

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.BEACH_ON)

    override fun coversAll(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.BEACH_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val s = frame.settings
        look(frame.width, frame.height, s.raw(FxKeys.BEACH_BRIGHTNESS))
        val waves = s.value(FxKeys.BEACH_WAVES)
        // Less motion asked for: everything moves at half speed.
        val t = if (frame.reducedMotion) frame.time * CALM else frame.time
        val period = EffectMath.lerp(SLOW_PERIOD, FAST_PERIOD, waves)

        drawSky(canvas, w, h, t)
        drawSea(canvas, w, h, t, waves, period)
        drawShore(canvas, w, h, t, waves, period)
        drawGulls(canvas, w, h, t)

        plain.shader = shadeShader
        canvas.drawRect(0f, 0f, w, h, plain)
        plain.shader = null
    }

    private fun drawSky(canvas: Canvas, w: Float, h: Float, t: Float) {
        plain.shader = skyShader
        canvas.drawRect(0f, 0f, w, horizon + 1f, plain)
        plain.shader = null

        // The first stars come out at dusk. They glow softly and never blink.
        if (starAmount > 0f) {
            for (i in 0 until STARS) {
                val x = EffectMath.hash(i, SALT_STAR_X) * w
                val y = EffectMath.hash(i, SALT_STAR_Y) * EffectMath.hash(i, SALT_STAR_Y + 1) * horizon * STAR_REACH
                val glow = 0.55f + 0.45f * sin(t * TWO_PI / (STAR_PERIOD + 3f * EffectMath.hash(i, SALT_STAR_T)) + i)
                val big = EffectMath.hash(i, SALT_STAR_S)
                fill.color = EffectMath.withAlpha(Color.WHITE, starAmount * glow * (0.35f + 0.65f * big) * (1f - y / horizon))
                canvas.drawCircle(x, y, w * (STAR_SIZE + STAR_SIZE * big), fill)
            }
        }

        // Long thin clouds drift slowly over the sea line.
        soft.shader = cloudShader
        for (i in 0 until SKY_CLOUDS) {
            val span = w * (0.35f + 0.4f * EffectMath.hash(i, SALT_CLOUD_W))
            val run = w + span * 2f
            val x = ((EffectMath.hash(i, SALT_CLOUD_X) * run + t * w * CLOUD_DRIFT * (0.6f + 0.4f * EffectMath.hash(i, SALT_CLOUD_V))) % run) - span
            val y = horizon * (0.5f + 0.42f * EffectMath.hash(i, SALT_CLOUD_Y))
            val tall = h * (0.006f + 0.01f * EffectMath.hash(i, SALT_CLOUD_H))
            soft.alpha = (255 * (0.45f + 0.45f * EffectMath.hash(i, SALT_CLOUD_A))).toInt()
            for (k in 0 until 3) {
                val dx = (k - 1) * span * 0.32f
                val dy = (k - 1) * (k - 1) * tall * 0.6f
                canvas.save()
                canvas.translate(x + dx, y + dy)
                canvas.scale(span * (0.55f - 0.12f * (k - 1) * (k - 1)), tall * (1.2f - 0.3f * (k - 1) * (k - 1)))
                canvas.drawCircle(0f, 0f, 1f, soft)
                canvas.restore()
            }
        }
        soft.alpha = 255

        // The sun and its glow, kept above the sea line.
        canvas.save()
        canvas.clipRect(0f, 0f, w, horizon)
        soft.shader = glowShader
        canvas.drawCircle(sunX, sunY, glowRadius, soft)
        soft.shader = sunShader
        canvas.drawCircle(sunX, sunY, sunRadius, soft)
        canvas.restore()
        soft.shader = null

        // A low far headland on the left, faded by the haze.
        path.reset()
        path.moveTo(0f, horizon - h * LAND_TALL)
        path.cubicTo(w * 0.08f, horizon - h * LAND_TALL * 1.15f, w * 0.15f, horizon - h * LAND_TALL * 0.55f, w * 0.24f, horizon - h * LAND_TALL * 0.35f)
        path.cubicTo(w * 0.29f, horizon - h * LAND_TALL * 0.25f, w * 0.33f, horizon - h * 0.002f, w * 0.38f, horizon)
        path.lineTo(0f, horizon)
        path.close()
        fill.color = landColor
        canvas.drawPath(path, fill)
    }

    private fun drawSea(canvas: Canvas, w: Float, h: Float, t: Float, waves: Float, period: Float) {
        plain.shader = seaShader
        canvas.drawRect(0f, horizon, w, breakLine + h * 0.01f, plain)
        // A soft haze right on the sea line.
        plain.shader = hazeShader
        canvas.drawRect(0f, horizon - h * HAZE, w, horizon + h * HAZE, plain)
        plain.shader = null

        // The sun's path of light on the water.
        val depth = breakLine - horizon
        soft.shader = shineShader
        soft.alpha = (255 * shineAmount).toInt()
        canvas.save()
        canvas.clipRect(0f, horizon, w, breakLine)
        canvas.translate(sunX, horizon + depth * 0.42f)
        canvas.scale(w * SHINE_WIDE, depth * 0.62f)
        canvas.drawCircle(0f, 0f, 1f, soft)
        canvas.restore()
        soft.alpha = 255
        soft.shader = null

        // Rows of swell roll in toward the shore. Far rows are thin and close together.
        // Each row gets new broken tops every time it starts again at the far line.
        val travel = period * ROW_TRAVEL
        for (i in 0 until ROWS) {
            val u = i / ROWS.toFloat() + t / travel
            val pass = floor(u).toInt()
            val d = u - pass
            val dd = d * d
            val y = horizon + depth * dd
            val fade = EffectMath.smooth(d / 0.12f) * (1f - EffectMath.smooth((d - 0.86f) / 0.14f))
            if (fade <= 0.01f) continue
            val seed = i + pass * ROWS
            val thick = h * (0.0005f + 0.0032f * dd * (0.5f + waves))
            val wave = h * 0.003f * dd * (0.4f + waves)
            // The dark face of the swell, all the way across.
            buildSwell(w, y + thick * 2.2f, wave, seed)
            line.color = EffectMath.withAlpha(faceColor, fade * (0.12f + 0.16f * waves) * (0.4f + 0.6f * d))
            line.strokeWidth = thick * 3.2f
            canvas.drawPath(path, line)
            // Its light top, in a few long broken pieces.
            buildRow(w, y, wave, seed)
            line.color = EffectMath.withAlpha(crestColor, fade * (0.16f + 0.22f * waves))
            line.strokeWidth = thick
            canvas.drawPath(path, line)
        }

        // Little sparkles in the sun's path. Each one glows up and down slowly.
        for (i in 0 until GLINTS) {
            val d = EffectMath.hash(i, SALT_GLINT_D)
            val near = d * d
            val y = horizon + depth * (0.02f + 0.95f * near)
            val spread = w * (0.03f + 0.17f * near) + sunRadius * 0.4f
            val side = EffectMath.hash(i, SALT_GLINT_X) * 2f - 1f
            val x = sunX + side * side * side * spread * 1.6f + sin(t * 0.6f + i) * w * 0.004f
            val rate = 0.35f + 0.5f * EffectMath.hash(i, SALT_GLINT_T)
            val pulse = 0.5f + 0.5f * sin(t * TWO_PI * rate + i * 2.3f)
            val a = shineAmount * pulse * pulse * pulse * (1f - 0.6f * kotlin.math.abs(side))
            if (a < 0.02f) continue
            val half = w * (0.004f + 0.014f * near) * (0.5f + EffectMath.hash(i, SALT_GLINT_S))
            val tall = h * (0.0006f + 0.0008f * near)
            fill.color = EffectMath.withAlpha(glintColor, a)
            canvas.drawOval(x - half, y - tall, x + half, y + tall, fill)
        }
    }

    /** The light tops of one row of swell: a few long, gently bending pieces. */
    private fun buildRow(w: Float, y: Float, wave: Float, seed: Int) {
        path.reset()
        val bend = TWO_PI * (1.5f + 2f * EffectMath.hash(seed, SALT_ROW_BEND)) / w
        val phase = EffectMath.hash(seed, SALT_ROW_PHASE) * TWO_PI
        val pieces = 2 + (EffectMath.hash(seed, SALT_ROW_GAP) * 3f).toInt()
        for (j in 0 until pieces) {
            val x0 = w * (-0.1f + 1.1f * EffectMath.hash(seed * 7 + j, SALT_ROW_SHIFT))
            val x1 = x0 + w * (0.12f + 0.4f * EffectMath.hash(seed * 7 + j, SALT_ROW_LEN))
            path.moveTo(x0, y + wave * sin(x0 * bend + phase))
            val xm = (x0 + x1) / 2f
            path.quadTo(xm, y + wave * sin(xm * bend + phase) - wave * 0.6f, x1, y + wave * sin(x1 * bend + phase))
        }
    }

    /** The dark face of one row of swell, a soft wavy line all the way across. */
    private fun buildSwell(w: Float, y: Float, wave: Float, seed: Int) {
        path.reset()
        val bend = TWO_PI * (1.5f + 2f * EffectMath.hash(seed, SALT_ROW_BEND)) / w
        val phase = EffectMath.hash(seed, SALT_ROW_PHASE) * TWO_PI
        val step = w / SWELL_STEPS
        for (j in 0..SWELL_STEPS) {
            val x = j * step
            val yy = y + wave * sin(x * bend + phase)
            if (j == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
        }
    }

    private fun drawShore(canvas: Canvas, w: Float, h: Float, t: Float, waves: Float, period: Float) {
        // Dry sand, with a fine grain, and darker damp sand near the water.
        plain.shader = sandShader
        canvas.drawRect(0f, breakLine, w, h, plain)
        plain.shader = dampShader
        canvas.drawRect(0f, breakLine, w, shore + h * DAMP_BELOW, plain)
        plain.shader = null
        grain.shader = grainShader
        canvas.drawRect(0f, breakLine, w, h, grain)
        // Soft wind ripples in the dry sand: a dark side and a lit side.
        line.strokeWidth = h * 0.003f
        line.color = EffectMath.withAlpha(wetColor, RIPPLE_ALPHA)
        canvas.drawPath(ripples, line)
        canvas.save()
        canvas.translate(0f, -h * 0.0025f)
        line.color = EffectMath.withAlpha(foamColor, RIPPLE_ALPHA * (0.5f + 0.5f * bright))
        line.strokeWidth = h * 0.0018f
        canvas.drawPath(ripples, line)
        canvas.restore()

        val reach = h * (MIN_REACH + (MAX_REACH - MIN_REACH) * waves)
        val curl = h * (0.01f + 0.016f * waves)
        val rest = shore - h * REST_BACK
        val step = w / SEGMENTS
        val mid = t / period - TILT * 0.5f
        val midCycle = floor(mid)
        val midP = mid - midCycle

        // Where the water ends, and how far the last wave wet the sand.
        for (i in 0..SEGMENTS) {
            val x = i * step
            val u = t / period - TILT * x / w
            val cycle = floor(u)
            val p = u - cycle
            val c = cycle.toInt()
            val lap = h * 0.003f * sin(x * 0.011f + t * 0.9f)
            val restY = rest + lap
            val shape = scallop(x, w, c) * curl * sin(PI.toFloat() * EffectMath.clamp01((p - 0.22f) / 0.75f))
            val far = reach * (0.7f + 0.3f * EffectMath.hash(c, SALT_REACH))
            // The wavy outline grows and shrinks smoothly with the wash, so the edge never jumps.
            edge[i] = shape + when {
                p < UP_START -> restY
                p < UP_END -> EffectMath.lerp(restY, shore + far, EffectMath.easeOut((p - UP_START) / (UP_END - UP_START)))
                else -> EffectMath.lerp(shore + far, restY, EffectMath.smooth((p - UP_END) / (BACK_END - UP_END)))
            }
            val last = if (p >= UP_END) c else c - 1
            val lastFar = reach * (0.7f + 0.3f * EffectMath.hash(last, SALT_REACH))
            wet[i] = shore + lastFar + scallop(x, w, last) * curl * WET_SHAPE
        }

        // The wet mark from the last wave dries out slowly.
        val age = if (midP >= UP_END) midP - UP_END else midP + 1f - UP_END
        val damp = WET_ALPHA * (1f - EffectMath.smooth(age / DRY_CYCLES))
        if (damp > 0.01f) {
            path.reset()
            path.moveTo(0f, rest)
            for (i in 0..SEGMENTS) path.lineTo(i * step, wet[i])
            path.lineTo(w, rest)
            path.close()
            // A soft outer edge first, then the wet mark itself.
            canvas.save()
            canvas.translate(0f, h * WET_FEATHER)
            fill.color = EffectMath.withAlpha(wetColor, damp * 0.35f)
            canvas.drawPath(path, fill)
            canvas.restore()
            fill.color = EffectMath.withAlpha(wetColor, damp)
            canvas.drawPath(path, fill)
            // A thin line of old foam left at the top of the wash.
            edgePath(wet, step, 0f)
            line.color = EffectMath.withAlpha(foamColor, damp * 0.55f)
            line.strokeWidth = h * 0.0016f
            canvas.drawPath(path, line)
        }

        // The thin sheet of water over the sand, clear at its edge.
        path.reset()
        path.moveTo(0f, breakLine - 1f)
        path.lineTo(w, breakLine - 1f)
        for (i in SEGMENTS downTo 0) path.lineTo(i * step, edge[i])
        path.close()
        fill.shader = shallowShader
        fill.color = Color.WHITE
        canvas.drawPath(path, fill)
        fill.shader = null

        drawFoam(canvas, w, h, t, waves, midP, midCycle.toInt(), step)
    }

    private fun drawFoam(canvas: Canvas, w: Float, h: Float, t: Float, waves: Float, p: Float, cycle: Int, step: Float) {
        val surf = 0.55f + 0.45f * waves
        // The break line's shape changes smoothly with the time, not with each wave.
        val roll = cycle + p
        // Foam lace on the water between the break line and the edge. Its shape only drifts
        // slowly with the time, so it never jumps from one wave to the next.
        val lace = 0.35f + 0.65f * EffectMath.smooth(p / 0.1f) * (1f - EffectMath.smooth((p - 0.35f) / 0.6f))
        line.strokeWidth = h * 0.0014f
        for (k in 1..LACE_LINES) {
            val share = k / (LACE_LINES + 1f)
            path.reset()
            var drawing = false
            for (i in 0..SEGMENTS) {
                val x = i * step
                val gap = EffectMath.hash(i / 3 + k * 17, SALT_LACE) < 0.3f
                if (gap) {
                    drawing = false
                    continue
                }
                val y = EffectMath.lerp(breakLine + h * 0.012f, edge[i], share) +
                    h * 0.004f * sin(x * (0.02f + 0.006f * k) + k * 1.7f + t * 0.4f)
                if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
                drawing = true
            }
            line.color = EffectMath.withAlpha(foamColor, 0.32f * lace * surf * (1f - share * 0.4f))
            canvas.drawPath(path, line)
        }

        // The break line: a band of white water where each wave tumbles over.
        val burst = 0.4f + 0.6f * EffectMath.smooth(p / 0.06f) * (1f - EffectMath.smooth((p - 0.1f) / 0.5f))
        path.reset()
        for (i in 0..SEGMENTS) {
            val x = i * step
            val y = breakLine - h * 0.005f * surf * (1f + sin(x * 0.017f + roll * 2.1f + t * 0.3f) * 0.6f + sin(x * 0.051f + roll) * 0.4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        for (i in SEGMENTS downTo 0) {
            val x = i * step
            val y = breakLine + h * (0.004f + 0.014f * burst) * surf * (1f + 0.5f * sin(x * 0.029f + roll * 1.7f))
            path.lineTo(x, y)
        }
        path.close()
        fill.color = EffectMath.withAlpha(foamColor, 0.3f * burst)
        canvas.drawPath(path, fill)
        path.reset()
        for (i in 0..SEGMENTS) {
            val x = i * step
            val y = breakLine - h * 0.005f * surf * (1f + sin(x * 0.017f + roll * 2.1f + t * 0.3f) * 0.6f + sin(x * 0.051f + roll) * 0.4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        line.color = EffectMath.withAlpha(foamColor, 0.2f * burst)
        line.strokeWidth = h * 0.014f * surf
        canvas.drawPath(path, line)
        line.color = EffectMath.withAlpha(foamColor, 0.85f * burst)
        line.strokeWidth = h * 0.004f * surf
        canvas.drawPath(path, line)

        // The next wave rising before it breaks: a dark face with a bright top.
        val rise = EffectMath.smooth((p - 0.68f) / 0.12f) * (1f - EffectMath.smooth((p - 0.96f) / 0.04f))
        if (rise > 0.01f) {
            val climb = EffectMath.smooth((p - 0.68f) / 0.32f)
            val y = breakLine - h * 0.04f * (1f - climb)
            path.reset()
            for (i in 0..SEGMENTS) {
                val x = i * step
                val yy = y - h * 0.003f * sin(x * 0.013f + cycle * 1.3f)
                if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
            }
            canvas.save()
            canvas.translate(0f, h * 0.006f * surf)
            line.color = EffectMath.withAlpha(faceColor, 0.32f * rise * surf)
            line.strokeWidth = h * 0.016f * surf
            canvas.drawPath(path, line)
            canvas.restore()
            line.color = EffectMath.withAlpha(foamColor, 0.75f * rise * surf)
            line.strokeWidth = h * 0.003f * surf
            canvas.drawPath(path, line)
        }

        // The rush of foam from the break line in toward the sand.
        if (p < BORE_END) {
            val k = EffectMath.smooth(p / BORE_END)
            val a = EffectMath.smooth(p / 0.04f) * (1f - EffectMath.smooth((p - BORE_END * 0.7f) / (BORE_END * 0.3f)))
            path.reset()
            for (i in 0..SEGMENTS) {
                val x = i * step
                val y = EffectMath.lerp(breakLine + h * 0.006f, edge[i], k) + h * 0.005f * sin(x * 0.023f + cycle * 3.1f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            line.color = EffectMath.withAlpha(foamColor, 0.2f * a * surf)
            line.strokeWidth = h * 0.014f * surf
            canvas.drawPath(path, line)
            line.color = EffectMath.withAlpha(foamColor, 0.7f * a * surf)
            line.strokeWidth = h * 0.004f * surf
            canvas.drawPath(path, line)
        }

        // The foam at the water's edge, brightest as the wave runs up the sand.
        val run = 0.3f + 0.7f * EffectMath.smooth((p - 0.24f) / 0.08f) * (1f - EffectMath.smooth((p - UP_END) / 0.35f))
        edgePath(edge, step, 0f)
        line.color = EffectMath.withAlpha(foamColor, 0.22f * run)
        line.strokeWidth = h * 0.012f
        canvas.drawPath(path, line)
        edgePath(edge, step, -h * 0.003f)
        line.color = EffectMath.withAlpha(foamColor, 0.9f * run)
        line.strokeWidth = h * 0.0035f
        canvas.drawPath(path, line)

        // Small bubbles of foam just behind the edge. New ones for each wave, so they fade
        // out before the next wave and never jump.
        val fizz = EffectMath.smooth(p / 0.1f) * (1f - EffectMath.smooth((p - 0.8f) / 0.18f))
        for (i in 0 until BUBBLES) {
            if (fizz <= 0.01f) break
            val at = EffectMath.hash(i + cycle * BUBBLES, SALT_BUBBLE)
            val k = (at * SEGMENTS).toInt().coerceIn(0, SEGMENTS - 1)
            val f = at * SEGMENTS - k
            val y = EffectMath.lerp(edge[k], edge[k + 1], f) - h * (0.006f + 0.02f * EffectMath.hash(i, SALT_BUBBLE + 1))
            val r = h * (0.0012f + 0.0022f * EffectMath.hash(i, SALT_BUBBLE + 2))
            fill.color = EffectMath.withAlpha(foamColor, fizz * (0.25f + 0.45f * run) * EffectMath.hash(i, SALT_BUBBLE + 3))
            canvas.drawCircle(at * w, y, r, fill)
        }
    }

    private fun edgePath(ys: FloatArray, step: Float, lift: Float) {
        path.reset()
        path.moveTo(0f, ys[0] + lift)
        for (i in 1..SEGMENTS) {
            val x0 = (i - 1) * step
            val xm = x0 + step / 2f
            path.quadTo(x0, ys[i - 1] + lift, xm, (ys[i - 1] + ys[i]) / 2f + lift)
        }
        path.lineTo(SEGMENTS * step, ys[SEGMENTS] + lift)
    }

    /** A far gull or two, gliding with a slow flap now and then. */
    private fun drawGulls(canvas: Canvas, w: Float, h: Float, t: Float) {
        line.color = gullColor
        for (i in 0 until GULLS) {
            val speed = GULL_SPEED * (0.7f + 0.6f * EffectMath.hash(i, SALT_GULL_V))
            val run = w * 1.5f
            val x = (EffectMath.hash(i, SALT_GULL_X) * run + t * w * speed) % run - w * 0.25f
            val y = horizon * (0.38f + 0.4f * EffectMath.hash(i, SALT_GULL_Y)) + sin(t * 0.5f + i * 2f) * h * 0.006f
            val size = w * (0.012f + 0.014f * EffectMath.hash(i, SALT_GULL_S))
            // Flap for a while, then glide with the wings held up.
            val cycle = frac(t / GULL_CYCLE + EffectMath.hash(i, SALT_GULL_F))
            val flap = if (cycle < 0.45f) sin(cycle / 0.45f * TWO_PI * 3f) else 0.35f
            line.strokeWidth = size * 0.13f
            path.reset()
            val tip = -flap * size * 0.45f
            path.moveTo(x - size, y + tip)
            path.quadTo(x - size * 0.45f, y - size * 0.35f - tip * 0.3f, x, y)
            path.quadTo(x + size * 0.45f, y - size * 0.35f - tip * 0.3f, x + size, y + tip)
            canvas.drawPath(path, line)
        }
    }

    /** A wavy outline for the wash, different for every wave. Goes from about -1 to 1. */
    private fun scallop(x: Float, w: Float, cycle: Int): Float {
        val a = EffectMath.hash(cycle, SALT_SCALLOP_A) * TWO_PI
        val b = EffectMath.hash(cycle, SALT_SCALLOP_B) * TWO_PI
        val c = EffectMath.hash(cycle, SALT_SCALLOP_C) * TWO_PI
        val u = x / w * TWO_PI
        // Rounded tongues of water with sharper points between them, like real wash.
        val tongues = 1f - 2f * kotlin.math.abs(sin(u * 1.7f + b))
        return 0.4f * sin(u * 1.1f + a) + 0.42f * tongues + 0.18f * sin(u * 5.3f + c)
    }

    /** Makes the colors and shaders for this size and brightness. Only runs when they change. */
    private fun look(width: Int, height: Int, brightness: Int) {
        if (width == lookWidth && height == lookHeight && brightness == lookBright) return
        lookWidth = width
        lookHeight = height
        lookBright = brightness
        val w = width.toFloat()
        val h = height.toFloat()
        val b = brightness / 100f
        bright = b
        horizon = h * HORIZON
        breakLine = h * BREAK_LINE
        shore = h * SHORE

        val skyTop = tone(SKY_TOP, b)
        val skyMid = tone(SKY_MID, b)
        val skyLow = tone(SKY_LOW, b)
        val seaFar = tone(SEA_FAR, b)
        val seaMid = tone(SEA_MID, b)
        val seaNear = tone(SEA_NEAR, b)
        val sandWet = tone(SAND_WET, b)
        val sandNear = tone(SAND_NEAR, b)
        val sandFar = tone(SAND_LOW, b)
        val sun = tone(SUN, b)
        val glow = tone(SUN_GLOW, b)
        foamColor = tone(FOAM, b)
        crestColor = mix(skyLow, foamColor, 0.35f)
        faceColor = mix(seaMid, Color.BLACK, 0.35f)
        wetColor = mix(sandWet, Color.BLACK, 0.08f)
        glintColor = tone(GLINT, b)
        gullColor = tone(GULL, b)
        landColor = mix(mix(skyLow, seaFar, 0.55f), skyTop, 0.15f)
        starAmount = 1f - EffectMath.smooth(b / STARS_UNTIL)
        shineAmount = EffectMath.lerp(0.95f, 0.55f, EffectMath.smooth((b - 0.45f) / 0.55f))

        sunX = w * EffectMath.lerp(0.64f, 0.6f, b)
        sunRadius = w * EffectMath.lerp(0.085f, 0.05f, b)
        sunY = if (b < GOLDEN) {
            EffectMath.lerp(horizon + sunRadius * 0.3f, horizon - h * 0.11f, b / GOLDEN)
        } else {
            EffectMath.lerp(horizon - h * 0.11f, h * 0.085f, (b - GOLDEN) / (1f - GOLDEN))
        }
        glowRadius = w * EffectMath.lerp(0.95f, 0.7f, b)

        skyShader = LinearGradient(
            0f, 0f, 0f, horizon,
            intArrayOf(skyTop, skyMid, skyLow), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        seaShader = LinearGradient(
            0f, horizon, 0f, breakLine,
            intArrayOf(seaFar, seaMid, seaNear), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        hazeShader = LinearGradient(
            0f, horizon - h * HAZE, 0f, horizon + h * HAZE,
            intArrayOf(EffectMath.withAlpha(skyLow, 0f), EffectMath.withAlpha(skyLow, 0.7f), EffectMath.withAlpha(skyLow, 0f)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        sandShader = LinearGradient(
            0f, breakLine, 0f, h,
            intArrayOf(sandWet, sandNear, sandFar), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP,
        )
        dampShader = LinearGradient(
            0f, breakLine, 0f, shore + h * DAMP_BELOW,
            intArrayOf(EffectMath.withAlpha(wetColor, 0.7f), EffectMath.withAlpha(wetColor, 0.3f), EffectMath.withAlpha(wetColor, 0f)),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP,
        )
        val maxEdge = shore + h * MAX_REACH
        shallowShader = LinearGradient(
            0f, breakLine, 0f, maxEdge,
            intArrayOf(seaNear, EffectMath.withAlpha(mix(seaNear, foamColor, 0.12f), 0.7f), EffectMath.withAlpha(mix(seaNear, skyLow, 0.35f), 0.22f)),
            floatArrayOf(0f, 0.4f, 0.85f), Shader.TileMode.CLAMP,
        )
        glowShader = RadialGradient(
            sunX, sunY, glowRadius,
            intArrayOf(EffectMath.withAlpha(glow, 0.85f), EffectMath.withAlpha(glow, 0.35f), EffectMath.withAlpha(glow, 0.1f), 0),
            floatArrayOf(0f, 0.18f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        sunShader = RadialGradient(
            sunX, sunY, sunRadius,
            intArrayOf(mix(sun, Color.WHITE, 0.7f), mix(sun, Color.WHITE, 0.3f), sun, EffectMath.withAlpha(sun, 0f)),
            floatArrayOf(0f, 0.6f, 0.9f, 1f), Shader.TileMode.CLAMP,
        )
        shineShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(EffectMath.withAlpha(glow, 0.55f), EffectMath.withAlpha(glow, 0.2f), 0),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        val cloud = tone(CLOUD, b)
        cloudShader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(EffectMath.withAlpha(cloud, 0.75f), EffectMath.withAlpha(cloud, 0.3f), 0),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        shadeShader = RadialGradient(
            w / 2f, h * 0.45f, h * 0.72f,
            intArrayOf(0, 0, EffectMath.withAlpha(Color.BLACK, 0.12f + 0.2f * (1f - b))),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        ripples.reset()
        for (k in 0 until RIPPLES) {
            val share = (k + 0.5f) / RIPPLES
            val y = shore + h * MAX_REACH * 0.6f + (h - shore - h * MAX_REACH * 0.6f) * share * share
            val tilt = h * (0.01f + 0.02f * EffectMath.hash(k, SALT_RIPPLE))
            val x0 = w * (-0.2f + 0.5f * EffectMath.hash(k, SALT_RIPPLE + 1))
            val x1 = x0 + w * (0.5f + 0.6f * EffectMath.hash(k, SALT_RIPPLE + 2))
            ripples.moveTo(x0, y)
            ripples.cubicTo(x0 + (x1 - x0) * 0.3f, y - tilt, x0 + (x1 - x0) * 0.6f, y + tilt * 0.6f, x1, y - tilt * 0.4f)
        }
        if (grainShader == null) grainShader = makeGrain()
        grain.alpha = (255 * (0.5f + 0.3f * b)).toInt()
    }

    private companion object {
        const val TWO_PI = (PI * 2).toFloat()
        const val CALM = 0.5f

        /** Where things sit, as shares of the height. */
        const val HORIZON = 0.4f
        const val BREAK_LINE = 0.6f
        const val SHORE = 0.69f
        const val HAZE = 0.012f
        const val LAND_TALL = 0.014f
        const val DAMP_BELOW = 0.03f

        /** Waves: one wave every 9 seconds at 0, every 3.5 seconds at 100. */
        const val SLOW_PERIOD = 9f
        const val FAST_PERIOD = 3.5f
        const val ROWS = 22
        const val ROW_TRAVEL = 3f
        const val SWELL_STEPS = 16

        /** How far up the sand a wave runs, as shares of the height. */
        const val MIN_REACH = 0.035f
        const val MAX_REACH = 0.11f
        const val REST_BACK = 0.018f
        const val TILT = 0.06f
        const val UP_START = 0.26f
        const val UP_END = 0.5f
        const val BACK_END = 0.97f
        const val BORE_END = 0.3f
        const val WET_SHAPE = 0.95f
        const val WET_ALPHA = 0.32f
        const val WET_FEATHER = 0.005f
        const val DRY_CYCLES = 1.1f
        const val SEGMENTS = 64
        const val LACE_LINES = 4

        const val GLINTS = 54
        const val SHINE_WIDE = 0.2f
        const val STARS = 46
        const val STAR_REACH = 0.85f
        const val STAR_SIZE = 0.0012f
        const val STAR_PERIOD = 4f
        const val STARS_UNTIL = 0.32f
        const val SKY_CLOUDS = 4
        const val CLOUD_DRIFT = 0.004f
        const val GULLS = 3
        const val GULL_SPEED = 0.018f
        const val GULL_CYCLE = 7f

        /** Brightness 0 is dusk, this is golden late afternoon, 1 is noon. */
        const val GOLDEN = 0.45f

        const val SALT_STAR_X = 101
        const val SALT_STAR_Y = 102
        const val SALT_STAR_T = 104
        const val SALT_STAR_S = 105
        const val SALT_CLOUD_W = 111
        const val SALT_CLOUD_X = 112
        const val SALT_CLOUD_V = 113
        const val SALT_CLOUD_Y = 114
        const val SALT_CLOUD_H = 115
        const val SALT_CLOUD_A = 116
        const val SALT_ROW_SHIFT = 121
        const val SALT_ROW_BEND = 122
        const val SALT_ROW_PHASE = 123
        const val SALT_ROW_GAP = 124
        const val SALT_ROW_LEN = 125
        const val SALT_GLINT_D = 131
        const val SALT_GLINT_X = 132
        const val SALT_GLINT_T = 133
        const val SALT_GLINT_S = 134
        const val SALT_REACH = 141
        const val SALT_SCALLOP_A = 142
        const val SALT_SCALLOP_B = 143
        const val SALT_SCALLOP_C = 144
        const val SALT_LACE = 145
        const val SALT_GULL_V = 151
        const val SALT_GULL_X = 152
        const val SALT_GULL_Y = 153
        const val SALT_GULL_S = 154
        const val SALT_GULL_F = 155
        const val SALT_GRAIN = 161

        // Each color is dusk, golden, then noon.
        val SKY_TOP = intArrayOf(0xFF0A0E2E.toInt(), 0xFF2E4F8E.toInt(), 0xFF1766C8.toInt())
        val SKY_MID = intArrayOf(0xFF3B2B5E.toInt(), 0xFF8EA9CF.toInt(), 0xFF57A3E6.toInt())
        val SKY_LOW = intArrayOf(0xFFEE7A42.toInt(), 0xFFFFD39C.toInt(), 0xFFCDE6F5.toInt())
        val SUN = intArrayOf(0xFFFF6A2C.toInt(), 0xFFFFDC96.toInt(), 0xFFFFFDF2.toInt())
        val SUN_GLOW = intArrayOf(0xFFFF6F2E.toInt(), 0xFFFFC77E.toInt(), 0xFFFFF7DC.toInt())
        val CLOUD = intArrayOf(0xFFB0587A.toInt(), 0xFFFFE4C8.toInt(), 0xFFFFFFFF.toInt())
        val SEA_FAR = intArrayOf(0xFF4A3554.toInt(), 0xFF6A88A4.toInt(), 0xFF4F9CC4.toInt())
        val SEA_MID = intArrayOf(0xFF161C38.toInt(), 0xFF1F5878.toInt(), 0xFF0C6A94.toInt())
        val SEA_NEAR = intArrayOf(0xFF1F3C4E.toInt(), 0xFF2C8A8E.toInt(), 0xFF1FB4B6.toInt())
        val FOAM = intArrayOf(0xFFD8BFCB.toInt(), 0xFFFFF6E8.toInt(), 0xFFFFFFFF.toInt())
        val GLINT = intArrayOf(0xFFFFA25A.toInt(), 0xFFFFEDC0.toInt(), 0xFFFFFFFF.toInt())
        val SAND_WET = intArrayOf(0xFF3C302F.toInt(), 0xFF9C7D5B.toInt(), 0xFFC4A374.toInt())
        val SAND_NEAR = intArrayOf(0xFF5E4A42.toInt(), 0xFFD9B98A.toInt(), 0xFFEFDAAE.toInt())
        val SAND_LOW = intArrayOf(0xFF4C3B35.toInt(), 0xFFE7CA9C.toInt(), 0xFFF7E7C2.toInt())
        val GULL = intArrayOf(0xFF17121E.toInt(), 0xFF3A3133.toInt(), 0xFF4A4C55.toInt())

        fun frac(x: Float): Float = x - floor(x)

        /** The color for [brightness] 0 to 1 from a dusk, golden, noon set. */
        fun tone(set: IntArray, brightness: Float): Int = if (brightness < GOLDEN) {
            mix(set[0], set[1], brightness / GOLDEN)
        } else {
            mix(set[1], set[2], (brightness - GOLDEN) / (1f - GOLDEN))
        }

        fun mix(from: Int, to: Int, t: Float): Int {
            val k = EffectMath.clamp01(t)
            val a = Color.alpha(from) + ((Color.alpha(to) - Color.alpha(from)) * k).toInt()
            val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
            val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
            val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
            return Color.argb(a, r, g, b)
        }

        /** A small tile of soft sand grain: light and dark specks, the same every time. */
        fun makeGrain(): Shader {
            val size = GRAIN_TILE
            val pixels = IntArray(size * size)
            for (i in pixels.indices) {
                val n = EffectMath.hash(i, SALT_GRAIN)
                pixels[i] = when {
                    n < 0.12f -> Color.argb((40 + 60 * n / 0.12f).toInt(), 60, 45, 30)
                    n > 0.9f -> Color.argb((30 + 50 * (n - 0.9f) / 0.1f).toInt(), 255, 250, 235)
                    else -> 0
                }
            }
            val tile = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            return BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }

        const val GRAIN_TILE = 96
        const val RIPPLES = 9
        const val BUBBLES = 34
        const val SALT_BUBBLE = 181
        const val RIPPLE_ALPHA = 0.1f
        const val SALT_RIPPLE = 171
    }
}
