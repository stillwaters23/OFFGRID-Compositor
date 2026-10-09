package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Portal (feature 20): a swirling ring of light that opens a window in the video.
 *
 * Scene: inside the ring you see another place, a starry sky with colored clouds of gas and
 * a far planet with a ring, slowly turning.
 * Wormhole: a deep tunnel of glowing rings that sink toward a bright light far away, with
 * spiral arms turning around it.
 * Void: a pitch-black hole with a shimmering edge, and specks of light pulled in, spinning
 * faster as they fall.
 *
 * Every style has the same rim: bright swirling arcs and small crackles of light running
 * around it. The crackles change shape, but they never blink on and off.
 *
 * Portal across and Portal down place the middle (50, 50 is the center). Portal size sets the
 * radius, 8% to 45% of the width. Portal spin sets how fast it turns. Portal glow sets how big
 * and bright the rim glow is.
 */
class PortalLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Everything is drawn in portal units: the rim is a circle of radius 1 around 0, 0.
    private val window = Path().apply { addCircle(0f, 0f, 1f, Path.Direction.CW) }
    private val hole = Path().apply { addCircle(0f, 0f, HOLE, Path.Direction.CW) }
    private val oval = RectF()
    private val bolt = Path()
    private val shimmer = Path()
    private val spirals = Path()

    // Scene: the far sky.
    private val space = RadialGradient(-0.2f, -0.25f, 1.3f, SPACE_COLORS, SPACE_STOPS, Shader.TileMode.CLAMP)
    private val clouds = Array(NEBULA_COLORS.size) { blob(NEBULA_COLORS[it]) }
    private val planet = RadialGradient(PLANET_X - 0.06f, PLANET_Y - 0.07f, PLANET_R * 1.4f, PLANET_COLORS, PLANET_STOPS, Shader.TileMode.CLAMP)
    private val planetAir = RadialGradient(PLANET_X, PLANET_Y, PLANET_R * 1.5f, intArrayOf(0, 0, 0x66FFB070, 0), floatArrayOf(0f, 0.62f, 0.68f, 1f), Shader.TileMode.CLAMP)
    private val depth = RadialGradient(0f, 0f, 1f, intArrayOf(0, 0, 0x55000000, 0xCC000000.toInt()), floatArrayOf(0f, 0.6f, 0.85f, 1f), Shader.TileMode.CLAMP)
    private val starsSmall = FloatArray(STARS_SMALL * 2)
    private val starsBig = FloatArray(STARS_BIG * 2)
    private val glints = FloatArray(STARS_BIG * 8)
    private val streak = FloatArray(STREAK_STEPS * 4)

    // Wormhole: the tunnel.
    private val tunnel = RadialGradient(0f, 0f, 1f, TUNNEL_COLORS, TUNNEL_STOPS, Shader.TileMode.CLAMP)
    private val farLight = RadialGradient(0f, 0f, 1f, intArrayOf(0xFFFFFFFF.toInt(), 0xCCBFF6FF.toInt(), 0x553D8BFF, 0), floatArrayOf(0f, 0.15f, 0.45f, 1f), Shader.TileMode.CLAMP)

    // Void: the dark around the hole.
    private val pull = RadialGradient(0f, 0f, PULL_REACH, intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0x80000000.toInt(), 0x30000000, 0), floatArrayOf(0f, HOLE / PULL_REACH, 1f / PULL_REACH, 1.25f / PULL_REACH, 1f), Shader.TileMode.CLAMP)
    private val disk = RadialGradient(0f, 0f, 1.35f, intArrayOf(0, 0, 0xCCFFB347.toInt(), 0x88FF6A1A.toInt(), 0x22B3160A, 0), floatArrayOf(0f, HOLE / 1.35f, 0.92f / 1.35f, 1.05f / 1.35f, 1.2f / 1.35f, 1f), Shader.TileMode.CLAMP)

    // The rim, one look per style.
    private val rims = arrayOf(
        SweepGradient(0f, 0f, SCENE_RIM, null),
        SweepGradient(0f, 0f, WORM_RIM, null),
        SweepGradient(0f, 0f, VOID_RIM, null),
    )
    private val palettes = arrayOf(SCENE_ARCS, WORM_ARCS, VOID_ARCS)
    private var glowFor = -1
    private var glowStyle = -1
    private var rimGlow: Shader? = null
    private var glowReach = 1.5f

    init {
        for (i in 0 until STARS_SMALL) starAt(starsSmall, i, i)
        for (i in 0 until STARS_BIG) {
            starAt(starsBig, i, i + 500)
            val x = starsBig[i * 2]
            val y = starsBig[i * 2 + 1]
            val g = GLINT * (0.6f + 0.8f * EffectMath.hash(i, 43))
            glints[i * 8] = x - g
            glints[i * 8 + 1] = y
            glints[i * 8 + 2] = x + g
            glints[i * 8 + 3] = y
            glints[i * 8 + 4] = x
            glints[i * 8 + 5] = y - g
            glints[i * 8 + 6] = x
            glints[i * 8 + 7] = y + g
        }
        for (a in 0 until SPIRAL_ARMS) {
            val turn = a * TWO_PI / SPIRAL_ARMS
            for (k in 0..SPIRAL_POINTS) {
                val f = k.toFloat() / SPIRAL_POINTS
                val r = SPIRAL_IN + (1f - SPIRAL_IN) * f.pow(1.6f)
                val angle = turn + SPIRAL_TURNS * TWO_PI * (1f - f)
                val x = cos(angle) * r
                val y = sin(angle) * r
                if (k == 0) spirals.moveTo(x, y) else spirals.lineTo(x, y)
            }
        }
    }

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.PORTAL_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val style = when (s.choice(FxKeys.PORTAL_STYLE)) {
            FxKeys.PORTAL_WORMHOLE -> WORMHOLE
            FxKeys.PORTAL_VOID -> VOID
            else -> SCENE
        }
        val radius = w * EffectMath.lerp(SIZE_MIN, SIZE_MAX, s.value(FxKeys.PORTAL_SIZE))
        val cx = w * s.value(FxKeys.PORTAL_X)
        val cy = h * s.value(FxKeys.PORTAL_Y)
        var turns = EffectMath.lerp(SPIN_MIN, SPIN_MAX, s.value(FxKeys.PORTAL_SPIN))
        if (frame.reducedMotion) turns *= CALM
        val glow = s.value(FxKeys.PORTAL_GLOW)
        // The turn angle, in turns. It keeps growing, so take the part past whole turns.
        val spin = frame.time * turns
        val t = frame.time
        rimLook(style, s.raw(FxKeys.PORTAL_GLOW))

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius, radius)

        // The glow outside the rim, under everything.
        shade(rimGlow)
        fill.alpha = (255 * (GLOW_ALPHA_MIN + (1f - GLOW_ALPHA_MIN) * glow)).toInt()
        canvas.drawCircle(0f, 0f, glowReach, fill)

        when (style) {
            SCENE -> drawScene(canvas, spin, t)
            WORMHOLE -> drawWormhole(canvas, spin, t)
            else -> drawVoid(canvas, spin, t, frame.reducedMotion)
        }
        drawRim(canvas, style, spin, t, glow, frame.reducedMotion)
        canvas.restore()
        fill.shader = null
        pen.shader = null
    }

    /** Fills with [shader] at full strength, whatever color was set before. */
    private fun shade(shader: Shader?) {
        fill.shader = shader
        fill.color = Color.BLACK
    }

    // ------------------------------------------------------------------ Scene

    private fun drawScene(canvas: Canvas, spin: Float, t: Float) {
        canvas.save()
        canvas.clipPath(window)
        shade(space)
        canvas.drawCircle(0f, 0f, 1f, fill)

        // Clouds of gas, turning slowly, each at its own pace.
        canvas.save()
        canvas.rotate(spin * 360f * NEBULA_TURN)
        for (i in clouds.indices) {
            val drift = sin(t * 0.13f + i * 1.7f) * 0.04f
            shade(clouds[i])
            canvas.save()
            canvas.translate(NEBULA_X[i] + drift, NEBULA_Y[i] - drift)
            canvas.rotate(NEBULA_TILT[i])
            canvas.scale(NEBULA_RX[i], NEBULA_RY[i])
            canvas.drawCircle(0f, 0f, 1f, fill)
            canvas.restore()
        }
        canvas.restore()

        // Stars, turning a little slower than the gas, so the sky has depth.
        canvas.save()
        canvas.rotate(spin * 360f * STAR_TURN)
        dots.shader = null
        dots.color = STAR_DIM
        dots.strokeWidth = STAR_SMALL_SIZE
        canvas.drawPoints(starsSmall, dots)
        // Big stars breathe softly, less than once a second.
        val breathe = 0.8f + 0.2f * sin(t * TWINKLE)
        dots.color = EffectMath.withAlpha(STAR_GLOW, 0.22f * breathe)
        dots.strokeWidth = STAR_BIG_SIZE * 2.2f
        canvas.drawPoints(starsBig, dots)
        dots.color = EffectMath.withAlpha(STAR_GLOW, 0.75f * breathe)
        dots.strokeWidth = STAR_SMALL_SIZE * 0.6f
        canvas.drawLines(glints, dots)
        dots.color = EffectMath.withAlpha(Color.WHITE, breathe)
        dots.strokeWidth = STAR_BIG_SIZE
        canvas.drawPoints(starsBig, dots)
        canvas.restore()

        // A far planet with a ring, rocking very slowly.
        canvas.save()
        canvas.translate(0f, sin(t * 0.4f) * 0.01f)
        oval.set(PLANET_X - PLANET_R * 2f, PLANET_Y - PLANET_R * 0.45f, PLANET_X + PLANET_R * 2f, PLANET_Y + PLANET_R * 0.45f)
        canvas.save()
        canvas.rotate(PLANET_TILT, PLANET_X, PLANET_Y)
        pen.shader = null
        pen.color = RING_BACK
        pen.strokeWidth = PLANET_R * 0.12f
        canvas.drawArc(oval, 180f, 180f, false, pen)
        canvas.restore()
        shade(planetAir)
        canvas.drawCircle(PLANET_X, PLANET_Y, PLANET_R * 1.5f, fill)
        shade(planet)
        canvas.drawCircle(PLANET_X, PLANET_Y, PLANET_R, fill)
        canvas.save()
        canvas.rotate(PLANET_TILT, PLANET_X, PLANET_Y)
        pen.color = RING_FRONT
        canvas.drawArc(oval, 0f, 180f, false, pen)
        pen.color = RING_EDGE
        pen.strokeWidth = PLANET_R * 0.03f
        canvas.drawArc(oval, 0f, 180f, false, pen)
        canvas.restore()
        canvas.restore()

        // Darker toward the rim, so the window looks deep.
        shade(depth)
        canvas.drawCircle(0f, 0f, 1f, fill)
        canvas.restore()
    }

    // ------------------------------------------------------------------ Wormhole

    private fun drawWormhole(canvas: Canvas, spin: Float, t: Float) {
        canvas.save()
        canvas.clipPath(window)
        shade(tunnel)
        canvas.drawCircle(0f, 0f, 1f, fill)

        // Spiral arms turning around the far end.
        canvas.save()
        canvas.rotate(spin * 360f)
        pen.shader = null
        pen.color = SPIRAL_GLOW
        pen.strokeWidth = 0.09f
        canvas.drawPath(spirals, pen)
        pen.color = SPIRAL_LINE
        pen.strokeWidth = 0.022f
        canvas.drawPath(spirals, pen)
        canvas.restore()

        // Rings sinking into the tunnel, small and dim far away, big and bright up close.
        for (k in 0 until RINGS) {
            var z = (k.toFloat() / RINGS - spin * RING_FLOW) % 1f
            if (z < 0f) z += 1f
            val r = z.pow(RING_DEPTH)
            if (r < 0.02f) continue
            val fadeIn = EffectMath.smooth(z / 0.18f)
            val fadeOut = 1f - EffectMath.smooth((z - 0.86f) / 0.14f)
            val alpha = fadeIn * fadeOut
            if (alpha <= 0.01f) continue
            // The far end of the tunnel bends a little.
            val bend = (1f - z) * TUNNEL_BEND
            val ox = sin(t * 0.5f) * bend
            val oy = cos(t * 0.37f) * bend
            oval.set(ox - r, oy - r, ox + r, oy + r)
            val turn = spin * 360f + z * RING_TWIST
            val color = mix(RING_FAR, RING_NEAR, z)
            // A full ring, soft and then sharp, so the tunnel reads as rings going deep.
            pen.color = EffectMath.withAlpha(color, alpha * 0.22f)
            pen.strokeWidth = 0.01f + 0.05f * z
            canvas.drawOval(oval, pen)
            pen.color = EffectMath.withAlpha(color, alpha * 0.8f)
            pen.strokeWidth = 0.003f + 0.012f * z
            canvas.drawOval(oval, pen)
            // Two bright pieces on each ring, twisted by depth, so the rings swirl.
            pen.color = EffectMath.withAlpha(towardWhite(color), alpha)
            pen.strokeWidth = 0.005f + 0.016f * z
            canvas.drawArc(oval, turn, RING_ARC, false, pen)
            canvas.drawArc(oval, turn + 180f, RING_ARC, false, pen)
        }

        // The light at the far end.
        shade(farLight)
        canvas.save()
        canvas.translate(sin(t * 0.5f) * TUNNEL_BEND, cos(t * 0.37f) * TUNNEL_BEND)
        canvas.scale(FAR_LIGHT, FAR_LIGHT)
        canvas.drawCircle(0f, 0f, 1f, fill)
        canvas.restore()

        shade(depth)
        canvas.drawCircle(0f, 0f, 1f, fill)
        canvas.restore()
    }

    // ------------------------------------------------------------------ Void

    private fun drawVoid(canvas: Canvas, spin: Float, t: Float, calm: Boolean) {
        // The dark pull around the hole, then the hot glowing disk at its edge.
        shade(pull)
        canvas.drawCircle(0f, 0f, PULL_REACH, fill)
        canvas.save()
        canvas.rotate(spin * 360f * 1.5f)
        shade(disk)
        canvas.drawCircle(0f, 0f, 1.35f, fill)
        // Streaks in the disk, turning fast.
        pen.shader = null
        for (i in 0 until DISK_ARCS) {
            val r = 0.95f + 0.28f * EffectMath.hash(i, 91)
            val speed = 1f / (r * r)
            val start = EffectMath.hash(i, 92) * 360f + spin * 360f * speed * 0.8f
            oval.set(-r, -r, r, r)
            pen.color = EffectMath.withAlpha(if (i % 3 == 0) Color.WHITE else DISK_STREAK, 0.25f + 0.35f * EffectMath.hash(i, 93))
            pen.strokeWidth = 0.008f + 0.014f * EffectMath.hash(i, 94)
            canvas.drawArc(oval, start, 30f + 70f * EffectMath.hash(i, 95), false, pen)
        }
        canvas.restore()

        // Specks pulled in. They spin faster and stretch as they fall, then fade at the edge.
        for (i in 0 until SPECKS) {
            val life = SPECK_LIFE_MIN + SPECK_LIFE_EXTRA * EffectMath.hash(i, 101)
            val pace = if (calm) 0.5f else 1f
            var p = (t * pace / life + EffectMath.hash(i, 102)) % 1f
            if (p < 0f) p += 1f
            val r0 = SPECK_FAR_MIN + SPECK_FAR_EXTRA * EffectMath.hash(i, 103)
            val a0 = EffectMath.hash(i, 104) * TWO_PI
            val alpha = (0.35f + 0.65f * p) * EffectMath.smooth(p / 0.2f) * (1f - EffectMath.smooth((p - 0.88f) / 0.12f))
            if (alpha <= 0.01f) continue
            // A short curved streak along its spiral, longer as it speeds up.
            var lastX = 0f
            var lastY = 0f
            for (k in 0..STREAK_STEPS) {
                val q = (p - SPECK_STREAK * (0.4f + p) * k / STREAK_STEPS).coerceAtLeast(0f)
                val r = speckRadius(r0, q)
                val a = a0 + SWIRL * (1f / r - 1f / r0)
                val x = cos(a) * r
                val y = sin(a) * r
                if (k > 0) {
                    streak[(k - 1) * 4] = lastX
                    streak[(k - 1) * 4 + 1] = lastY
                    streak[(k - 1) * 4 + 2] = x
                    streak[(k - 1) * 4 + 3] = y
                }
                lastX = x
                lastY = y
            }
            pen.color = EffectMath.withAlpha(mix(SPECK_COOL, SPECK_HOT, p), alpha)
            pen.strokeWidth = 0.005f + 0.007f * EffectMath.hash(i, 105)
            canvas.drawLines(streak, pen)
        }

        // The hole itself: no light at all.
        fill.shader = null
        fill.color = Color.BLACK
        canvas.drawPath(hole, fill)

        // The shimmering edge: two wavy rings that slide past each other.
        for (layer in 0 until 2) {
            shimmer.reset()
            val phase = t * if (layer == 0) 1.7f else -1.3f
            for (k in 0..SHIMMER_POINTS) {
                val a = TWO_PI * k / SHIMMER_POINTS
                val r = HOLE + 0.012f + SHIMMER * (sin(a * 7f + phase) * 0.6f + sin(a * 13f - phase * 1.4f + layer) * 0.4f)
                val x = cos(a) * r
                val y = sin(a) * r
                if (k == 0) shimmer.moveTo(x, y) else shimmer.lineTo(x, y)
            }
            shimmer.close()
            pen.color = EffectMath.withAlpha(if (layer == 0) EDGE_HOT else EDGE_WARM, 0.35f)
            pen.strokeWidth = 0.05f
            canvas.drawPath(shimmer, pen)
            pen.color = EffectMath.withAlpha(if (layer == 0) Color.WHITE else EDGE_HOT, 0.85f)
            pen.strokeWidth = 0.012f
            canvas.drawPath(shimmer, pen)
        }
    }

    private fun speckRadius(r0: Float, p: Float): Float = HOLE + (r0 - HOLE) * (1f - p).pow(1.6f)

    // ------------------------------------------------------------------ Rim

    private fun drawRim(canvas: Canvas, style: Int, spin: Float, t: Float, glow: Float, calm: Boolean) {
        val palette = palettes[style]
        canvas.save()
        canvas.rotate(spin * 360f)

        // The rim band itself, colored all the way around.
        pen.shader = rims[style]
        pen.color = Color.BLACK
        pen.strokeWidth = RIM_WIDTH * 2.6f
        pen.alpha = 90
        canvas.drawCircle(0f, 0f, 1f, pen)
        pen.alpha = 255
        pen.strokeWidth = RIM_WIDTH
        canvas.drawCircle(0f, 0f, 1f, pen)
        pen.shader = null
        pen.color = EffectMath.withAlpha(Color.WHITE, 0.75f)
        pen.strokeWidth = RIM_WIDTH * 0.3f
        canvas.drawCircle(0f, 0f, 1f - RIM_WIDTH * 0.15f, pen)

        // Swirling arcs: each turns at its own pace, some inside the rim, some just outside.
        for (i in 0 until SWIRLS) {
            val r = SWIRL_IN + SWIRL_SPREAD * EffectMath.hash(i, 11)
            val pace = 0.6f + 0.9f * EffectMath.hash(i, 12)
            val start = EffectMath.hash(i, 13) * 360f + spin * 360f * (pace - 1f)
            val sweep = 35f + 110f * EffectMath.hash(i, 14)
            val color = palette[i % palette.size]
            oval.set(-r, -r, r, r)
            pen.color = EffectMath.withAlpha(color, 0.22f + 0.2f * glow)
            pen.strokeWidth = 0.05f
            canvas.drawArc(oval, start, sweep, false, pen)
            pen.color = EffectMath.withAlpha(color, 0.9f)
            pen.strokeWidth = 0.008f + 0.008f * EffectMath.hash(i, 15)
            canvas.drawArc(oval, start, sweep, false, pen)
        }
        canvas.restore()

        // Crackles of electric light that run around the rim. They always show; only their shape jumps.
        val tick = floor(t * if (calm) CRACKLE_CALM else CRACKLE_RATE).toInt()
        for (b in 0 until BOLTS) {
            bolt.reset()
            val center = TWO_PI * b / BOLTS + spin * TWO_PI * BOLT_PACE + sin(t * 0.6f + b) * 0.3f
            for (k in 0..BOLT_POINTS) {
                val f = k.toFloat() / BOLT_POINTS
                val a = center + (f - 0.5f) * BOLT_SPAN
                // The ends sit on the rim; the middle jumps in and out.
                val reach = sin(f * PI_F)
                val r = 1f + (EffectMath.hash(k + b * 31 + tick * 977, 21) - 0.5f) * BOLT_JAG * reach
                val x = cos(a) * r
                val y = sin(a) * r
                if (k == 0) bolt.moveTo(x, y) else bolt.lineTo(x, y)
            }
            val color = palette[(b + 1) % palette.size]
            pen.color = EffectMath.withAlpha(color, 0.25f)
            pen.strokeWidth = 0.04f
            canvas.drawPath(bolt, pen)
            pen.color = EffectMath.withAlpha(color, 0.85f)
            pen.strokeWidth = 0.01f
            canvas.drawPath(bolt, pen)
            pen.color = Color.WHITE
            pen.strokeWidth = 0.0035f
            canvas.drawPath(bolt, pen)
        }

        // Sparks thrown off the rim, sliding out along the spin and fading.
        for (i in 0 until SPARKS) {
            val life = 0.7f + 0.8f * EffectMath.hash(i, 31)
            var p = (t / life + EffectMath.hash(i, 32)) % 1f
            if (p < 0f) p += 1f
            val a = EffectMath.hash(i, 33) * TWO_PI + spin * TWO_PI
            val out = 1f + 0.35f * p
            val ahead = a + 0.5f * p
            val x = cos(ahead) * out
            val y = sin(ahead) * out
            val back = a + 0.5f * (p - 0.08f).coerceAtLeast(0f)
            val bo = 1f + 0.35f * (p - 0.08f).coerceAtLeast(0f)
            pen.color = EffectMath.withAlpha(palette[i % palette.size], (1f - p) * 0.9f)
            pen.strokeWidth = 0.012f
            canvas.drawLine(cos(back) * bo, sin(back) * bo, x, y, pen)
        }
    }

    /** Makes the rim glow for this style and glow setting. Only runs when they change. */
    private fun rimLook(style: Int, glowRaw: Int) {
        if (style == glowStyle && glowRaw == glowFor) return
        glowStyle = style
        glowFor = glowRaw
        val glow = glowRaw / 100f
        val reach = 1.12f + 0.7f * glow
        glowReach = reach
        val color = GLOW_COLORS[style]
        rimGlow = RadialGradient(
            0f,
            0f,
            reach,
            intArrayOf(0, 0, EffectMath.withAlpha(color, 0.45f), EffectMath.withAlpha(color, 0.9f), EffectMath.withAlpha(color, 0.35f), EffectMath.withAlpha(color, 0.1f), 0),
            floatArrayOf(0f, 0.8f / reach, 0.95f / reach, 1f / reach, (1f + (reach - 1f) * 0.25f) / reach, (1f + (reach - 1f) * 0.6f) / reach, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    private companion object {
        const val SCENE = 0
        const val WORMHOLE = 1
        const val VOID = 2

        const val PI_F = PI.toFloat()
        const val TWO_PI = (2.0 * PI).toFloat()
        const val SIZE_MIN = 0.08f
        const val SIZE_MAX = 0.45f
        const val SPIN_MIN = 0.03f
        const val SPIN_MAX = 0.6f
        const val CALM = 0.5f
        const val GLOW_ALPHA_MIN = 0.35f

        // The rim.
        const val RIM_WIDTH = 0.035f
        const val SWIRLS = 9
        const val SWIRL_IN = 0.94f
        const val SWIRL_SPREAD = 0.14f
        const val BOLTS = 3
        const val BOLT_POINTS = 22
        const val BOLT_SPAN = 0.9f
        const val BOLT_JAG = 0.1f
        const val BOLT_PACE = 1.4f
        const val CRACKLE_RATE = 12f
        const val CRACKLE_CALM = 4f
        const val SPARKS = 14

        // Scene.
        const val STARS_SMALL = 90
        const val STARS_BIG = 14
        const val STAR_SMALL_SIZE = 0.012f
        const val STAR_BIG_SIZE = 0.02f
        const val GLINT = 0.05f
        const val STAR_TURN = 0.12f
        const val NEBULA_TURN = 0.2f
        const val TWINKLE = 3.1f
        const val PLANET_X = 0.36f
        const val PLANET_Y = 0.34f
        const val PLANET_R = 0.15f
        const val PLANET_TILT = -16f
        val STAR_DIM = 0xCCE8F0FF.toInt()
        val STAR_GLOW = 0xFFBFD8FF.toInt()
        val RING_BACK = 0x99D9B98A.toInt()
        val RING_FRONT = 0xDDE8CDA0.toInt()
        val RING_EDGE = 0x66FFFFFF
        val SPACE_COLORS = intArrayOf(0xFF2A1260.toInt(), 0xFF150A3C.toInt(), 0xFF080520.toInt(), 0xFF020108.toInt())
        val SPACE_STOPS = floatArrayOf(0f, 0.35f, 0.75f, 1f)
        val NEBULA_COLORS = intArrayOf(0x99FF3EA5.toInt(), 0x802BF5E6.toInt(), 0x993D6BFF.toInt(), 0x999B4BFF.toInt(), 0x66FF8A3D)
        val NEBULA_X = floatArrayOf(-0.3f, 0.25f, 0.1f, -0.15f, 0.45f)
        val NEBULA_Y = floatArrayOf(-0.2f, -0.35f, 0.15f, 0.4f, 0.05f)
        val NEBULA_RX = floatArrayOf(0.75f, 0.55f, 0.85f, 0.6f, 0.4f)
        val NEBULA_RY = floatArrayOf(0.35f, 0.28f, 0.4f, 0.3f, 0.22f)
        val NEBULA_TILT = floatArrayOf(-30f, 20f, -10f, 35f, 60f)
        val PLANET_COLORS = intArrayOf(0xFFFFE2B0.toInt(), 0xFFE58A4A.toInt(), 0xFF7A2E18.toInt(), 0xFF1E0A06.toInt())
        val PLANET_STOPS = floatArrayOf(0f, 0.3f, 0.65f, 1f)

        // Wormhole.
        const val RINGS = 16
        const val RING_FLOW = 0.6f
        const val RING_DEPTH = 1.9f
        const val RING_TWIST = 260f
        const val RING_ARC = 55f
        const val TUNNEL_BEND = 0.12f
        const val FAR_LIGHT = 0.42f
        const val SPIRAL_ARMS = 5
        const val SPIRAL_POINTS = 48
        const val SPIRAL_IN = 0.03f
        const val SPIRAL_TURNS = 0.9f
        val SPIRAL_GLOW = 0x263D8BFF
        val SPIRAL_LINE = 0x6E8FC8FF
        val RING_FAR = 0xFF8FF3FF.toInt()
        val RING_NEAR = 0xFF9B5BFF.toInt()
        val TUNNEL_COLORS = intArrayOf(0xFF2A6CFF.toInt(), 0xFF1A2A8C.toInt(), 0xFF120A4A.toInt(), 0xFF07041C.toInt(), 0xFF030210.toInt())
        val TUNNEL_STOPS = floatArrayOf(0f, 0.12f, 0.35f, 0.7f, 1f)

        // Void.
        const val HOLE = 0.86f
        const val PULL_REACH = 1.7f
        const val DISK_ARCS = 12
        const val SPECKS = 46
        const val SPECK_LIFE_MIN = 2.2f
        const val SPECK_LIFE_EXTRA = 2.4f
        const val SPECK_FAR_MIN = 1.15f
        const val SPECK_FAR_EXTRA = 1.1f
        const val SPECK_STREAK = 0.09f
        const val STREAK_STEPS = 5
        const val SWIRL = 2.4f
        const val SHIMMER = 0.01f
        const val SHIMMER_POINTS = 96
        val DISK_STREAK = 0xFFFFC870.toInt()
        val SPECK_COOL = 0xFFE8F0FF.toInt()
        val SPECK_HOT = 0xFFFFB347.toInt()
        val EDGE_HOT = 0xFFFFD58A.toInt()
        val EDGE_WARM = 0xFFFF7A2E.toInt()

        val SCENE_RIM = intArrayOf(0xFF2BF5E6.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF3EA5.toInt(), 0xFF9B4BFF.toInt(), 0xFF2BF5E6.toInt())
        val WORM_RIM = intArrayOf(0xFF3D8BFF.toInt(), 0xFF8FF3FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF9B5BFF.toInt(), 0xFF3D8BFF.toInt())
        val VOID_RIM = intArrayOf(0xFFFF7A1A.toInt(), 0xFFFFD58A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF4A1A.toInt(), 0xFFFF7A1A.toInt())
        val SCENE_ARCS = intArrayOf(0xFF2BF5E6.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF3EA5.toInt(), 0xFFB98CFF.toInt())
        val WORM_ARCS = intArrayOf(0xFF8FF3FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF6A8CFF.toInt(), 0xFFB98CFF.toInt())
        val VOID_ARCS = intArrayOf(0xFFFFD58A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF8A3D.toInt(), 0xFFFFB347.toInt())
        val GLOW_COLORS = intArrayOf(0xFF2BF5E6.toInt(), 0xFF5A8CFF.toInt(), 0xFFFF8A2E.toInt())

        /** A soft round cloud of [color] that fades to nothing at radius 1. */
        fun blob(color: Int): Shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(color, EffectMath.withAlpha(color, Color.alpha(color) / 255f * 0.45f), 0),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )

        /** Puts star [i] somewhere inside the window, the same place every time. */
        fun starAt(into: FloatArray, i: Int, seed: Int) {
            val r = sqrt(EffectMath.hash(seed, 41)) * 0.97f
            val a = EffectMath.hash(seed, 42) * TWO_PI
            into[i * 2] = cos(a) * r
            into[i * 2 + 1] = sin(a) * r
        }

        fun towardWhite(color: Int): Int = mix(color, Color.WHITE, 0.55f)

        fun mix(a: Int, b: Int, f: Float): Int {
            val t = EffectMath.clamp01(f)
            val r = Color.red(a) + ((Color.red(b) - Color.red(a)) * t).toInt()
            val g = Color.green(a) + ((Color.green(b) - Color.green(a)) * t).toInt()
            val bl = Color.blue(a) + ((Color.blue(b) - Color.blue(a)) * t).toInt()
            return Color.rgb(r, g, bl)
        }
    }
}
