package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Roller coaster (feature 22), in two views.
 *
 * Watch: a white coaster track with a lift hill, a loop and two small hills runs across the
 * bottom of the video, over the camera. A train of 1 to 8 cars rides it. The cars slow down
 * near the tops and speed up on the drops, like a real ride. Coaster size sets how tall the
 * track is.
 *
 * Ride: the rider's own view from the front car. The rails rush toward you and swoop up,
 * down, left and right, the sky and the ground tilt in the turns, and the front of the car
 * shows at the bottom. This view covers the whole video. Coaster size sets how big the
 * swoops are.
 *
 * Coaster speed sets how fast the train goes in both views.
 */
class RollerCoasterLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bulbs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // ---- Watch view: the track plan, made again only when the size changes.
    private var planWidth = -1
    private var planHeight = -1
    private var planSize = -1
    private val trackX = FloatArray(MAX_SAMPLES)
    private val trackY = FloatArray(MAX_SAMPLES)
    private val trackS = FloatArray(MAX_SAMPLES)
    private val trackT = FloatArray(MAX_SAMPLES)
    private var samples = 0
    private var trackLength = 1f
    private var lapTime = 1f
    private var endPace = 1f
    private var unit = 1f
    private var carLength = 1f
    private val rail = Path()
    private val spine = Path()
    private val ties = Path()
    private val posts = Path()
    private val braces = Path()
    private var bulbDash: DashPathEffect? = null

    // One car, one unit long, riding on the rail at y = 0. Up is negative y.
    private val carBody = Path()
    private val carNose = Path()
    private val rider = Path()
    private val armsUp = Path()
    private val armsDown = Path()
    private val gloss = LinearGradient(0f, -0.5f, 0f, -0.06f, GLOSS_COLORS, GLOSS_STOPS, Shader.TileMode.CLAMP)
    private val headlight = RadialGradient(0f, 0f, 1f, intArrayOf(0xCCFFF6D8.toInt(), 0x55FFE6A0, 0), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)

    // Where a car is, filled by carAt().
    private var atX = 0f
    private var atY = 0f
    private var atAngle = 0f

    // ---- Ride view.
    private var rideWidth = -1
    private var rideHeight = -1
    private var sky: Shader? = null
    private var ground: Shader? = null
    private var noseShade: Shader? = null
    private var noseShine: Shader? = null
    private val sun = RadialGradient(0f, 0f, 1f, intArrayOf(0xFFFFFBEA.toInt(), 0xEEFFE2A8.toInt(), 0x55FFB070, 0), floatArrayOf(0f, 0.12f, 0.35f, 1f), Shader.TileMode.CLAMP)
    private val puff = RadialGradient(0f, 0f, 1f, intArrayOf(0xEEFFF4EC.toInt(), 0x99FFE2D2.toInt(), 0), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    private val hills = Path()
    private val stripes = Path()
    private val rows = Path()
    private val pillars = Path()
    private val beam = Path()
    private val leftRail = Path()
    private val rightRail = Path()
    private val sleepers = Path()
    private val glowRail = Path()
    private val nose = Path()
    private val noseTrim = Path()

    // The camera, filled each frame.
    private var camX = 0f
    private var camY = 0f
    private var camZ = 0f
    private var yawCos = 1f
    private var yawSin = 0f
    private var pitchCos = 1f
    private var pitchSin = 0f
    private var focal = 1f
    private var midX = 0f
    private var midY = 0f
    private var outX = 0f
    private var outY = 0f
    private var outZ = 0f

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.COASTER_ON)

    override fun coversAll(frame: EffectFrame): Boolean =
        frame.settings.on(FxKeys.COASTER_ON) && frame.settings.chose(FxKeys.COASTER_VIEW, FxKeys.COASTER_RIDE)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        if (frame.width <= 0 || frame.height <= 0) return
        if (frame.settings.chose(FxKeys.COASTER_VIEW, FxKeys.COASTER_RIDE)) drawRide(canvas, frame) else drawWatch(canvas, frame)
    }

    // ------------------------------------------------------------------ Watch view

    private fun drawWatch(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        plan(frame.width, frame.height, s.raw(FxKeys.COASTER_SIZE))
        val glowColor = frame.panelColor or OPAQUE

        // Supports first, then the track over them.
        pen.shader = null
        pen.strokeCap = Paint.Cap.BUTT
        pen.color = STEEL_SHADOW
        pen.strokeWidth = unit * POST_WIDTH * 1.9f
        canvas.drawPath(posts, pen)
        pen.strokeWidth = unit * BRACE_WIDTH * 2.2f
        canvas.drawPath(braces, pen)
        pen.color = STEEL
        pen.strokeWidth = unit * POST_WIDTH
        canvas.drawPath(posts, pen)
        pen.strokeWidth = unit * BRACE_WIDTH
        canvas.drawPath(braces, pen)
        pen.strokeCap = Paint.Cap.ROUND

        pen.color = STEEL_SHADOW
        pen.strokeWidth = unit * TIE_WIDTH * 1.8f
        canvas.drawPath(ties, pen)
        pen.color = TIE_COLOR
        pen.strokeWidth = unit * TIE_WIDTH
        canvas.drawPath(ties, pen)

        pen.color = STEEL_SHADOW
        pen.strokeWidth = unit * SPINE_WIDTH * 1.7f
        canvas.drawPath(spine, pen)
        pen.color = SPINE_COLOR
        pen.strokeWidth = unit * SPINE_WIDTH
        canvas.drawPath(spine, pen)

        // Warm bulbs along the spine. They stay lit. They never blink.
        bulbs.pathEffect = bulbDash
        bulbs.color = EffectMath.withAlpha(BULB_COLOR, 0.35f)
        bulbs.strokeWidth = unit * BULB_SIZE * 2.6f
        canvas.drawPath(spine, bulbs)
        bulbs.color = BULB_COLOR
        bulbs.strokeWidth = unit * BULB_SIZE
        canvas.drawPath(spine, bulbs)

        // A soft neon glow around the rail, in the panel's color.
        pen.color = EffectMath.withAlpha(glowColor, RAIL_GLOW_ALPHA * 0.45f)
        pen.strokeWidth = unit * RAIL_GLOW
        canvas.drawPath(rail, pen)
        pen.color = EffectMath.withAlpha(glowColor, RAIL_GLOW_ALPHA)
        pen.strokeWidth = unit * RAIL_GLOW * 0.4f
        canvas.drawPath(rail, pen)
        pen.color = STEEL_SHADOW
        pen.strokeWidth = unit * RAIL_WIDTH * 1.6f
        canvas.drawPath(rail, pen)
        pen.color = towardWhite(glowColor, RAIL_WHITE)
        pen.strokeWidth = unit * RAIL_WIDTH
        canvas.drawPath(rail, pen)

        // The train.
        val cars = 1 + (s.value(FxKeys.COASTER_CARS) * (MAX_CARS - 1) + 0.5f).toInt()
        var rate = EffectMath.lerp(PACE_SLOW, PACE_FAST, s.value(FxKeys.COASTER_SPEED))
        if (frame.reducedMotion) rate *= CALM
        val spacing = carLength * CAR_GAP
        val cycle = lapTime + (spacing * cars + unit * EXIT_ROOM) / (unit * endPace)
        val clock = frame.time * rate
        val lap = clock - floor(clock / cycle) * cycle
        val head = headDistance(lap)
        for (j in cars - 1 downTo 0) {
            val d = head - j * spacing
            if (d < 0f || d > trackLength) continue
            carAt(d)
            drawCar(canvas, j, frame.time)
        }
    }

    /** Fills with [shader] at full strength, whatever color was set before. */
    private fun shade(shader: Shader?) {
        fill.shader = shader
        fill.color = Color.BLACK
    }

    /** How far along the track the front car is [lap] seconds into a lap, at pace 1. */
    private fun headDistance(lap: Float): Float {
        if (lap >= lapTime) return trackLength + (lap - lapTime) * unit * endPace
        var lo = 0
        var hi = samples - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (trackT[mid] <= lap) lo = mid else hi = mid
        }
        val span = trackT[hi] - trackT[lo]
        val f = if (span > 0f) (lap - trackT[lo]) / span else 0f
        return trackS[lo] + (trackS[hi] - trackS[lo]) * f
    }

    /** Fills where a car is when it is [d] along the track, and its slope. */
    private fun carAt(d: Float) {
        var lo = 0
        var hi = samples - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (trackS[mid] <= d) lo = mid else hi = mid
        }
        val span = trackS[hi] - trackS[lo]
        val f = if (span > 0f) (d - trackS[lo]) / span else 0f
        atX = trackX[lo] + (trackX[hi] - trackX[lo]) * f
        atY = trackY[lo] + (trackY[hi] - trackY[lo]) * f
        // A smooth slope from a little before to a little after.
        val back = max(0, lo - 2)
        val ahead = min(samples - 1, hi + 2)
        atAngle = atan2(trackY[ahead] - trackY[back], trackX[ahead] - trackX[back])
    }

    private fun drawCar(canvas: Canvas, index: Int, time: Float) {
        canvas.save()
        canvas.translate(atX, atY)
        canvas.rotate(atAngle * DEGREES)
        canvas.scale(carLength, carLength)

        // Wheels and the frame under the car.
        fill.shader = null
        fill.color = CHASSIS
        canvas.drawRect(-0.44f, -0.11f, 0.44f, -0.05f, fill)
        canvas.drawCircle(-0.28f, -0.03f, WHEEL, fill)
        canvas.drawCircle(0.28f, -0.03f, WHEEL, fill)
        fill.color = HUB
        canvas.drawCircle(-0.28f, -0.03f, WHEEL * 0.4f, fill)
        canvas.drawCircle(0.28f, -0.03f, WHEEL * 0.4f, fill)

        // The rider sits in the car. Some ride with hands up.
        fill.color = RIDER
        canvas.drawPath(rider, fill)
        pen.shader = null
        pen.color = RIDER
        pen.strokeWidth = ARM
        val handsUp = EffectMath.hash(index, 5) < 0.6f
        if (handsUp) {
            // A small wave of the arms, slow and soft.
            canvas.save()
            canvas.rotate(4f * sin(time * 3f + index), -0.06f, -0.5f)
            canvas.drawPath(armsUp, pen)
            canvas.restore()
        } else {
            canvas.drawPath(armsDown, pen)
        }

        // The body, with a glossy shine on top.
        pen.color = CAR_EDGE
        pen.strokeWidth = CAR_OUTLINE
        val body = if (index == 0) carNose else carBody
        canvas.drawPath(body, pen)
        fill.color = CAR_COLORS[index % CAR_COLORS.size]
        canvas.drawPath(body, fill)
        shade(gloss)
        canvas.drawPath(body, fill)
        fill.shader = null
        pen.color = TRIM
        pen.strokeWidth = TRIM_WIDTH
        canvas.drawLine(-0.42f, -0.2f, if (index == 0) 0.5f else 0.44f, -0.2f, pen)

        if (index == 0) {
            shade(headlight)
            canvas.save()
            canvas.translate(0.58f, -0.2f)
            canvas.scale(0.3f, 0.3f)
            canvas.drawCircle(0f, 0f, 1f, fill)
            canvas.restore()
            fill.shader = null
            fill.color = LAMP
            canvas.drawCircle(0.53f, -0.2f, 0.04f, fill)
        }
        canvas.restore()
    }

    /** Works out the track for this size. Only runs when the size changes. */
    private fun plan(width: Int, height: Int, size: Int) {
        if (width == planWidth && height == planHeight && size == planSize) return
        planWidth = width
        planHeight = height
        planSize = size
        val w = width.toFloat()
        val h = height.toFloat()
        unit = min(w, h)
        val k = EffectMath.lerp(SCALE_MIN, SCALE_MAX, size / 100f)
        val u = unit
        val base = h - u * BASE_LIFT * k
        val hill = u * HILL_HEIGHT * k
        val radius = u * LOOP_RADIUS * k
        val drift = radius * LOOP_DRIFT
        carLength = u * CAR_LENGTH * (0.75f + 0.25f * k)
        samples = 0

        // Lead-in, flat.
        // The train starts just off the left edge and leaves just off the right edge.
        val start = -u * LEAD_IN - carLength
        var x = 0f
        addLine(start, base, x, base, 20)
        // The lift hill: a long climb, a steep drop.
        val hillWidth = u * HILL_WIDTH
        for (i in 1..HILL_STEPS) {
            val t = i.toFloat() / HILL_STEPS
            val bump = sin(PI_F * t.pow(HILL_SKEW))
            addPoint(x + hillWidth * t, base - hill * bump * bump)
        }
        x += hillWidth
        addLine(x, base, x + u * 0.03f, base, 4)
        x += u * 0.03f
        // The loop: a circle that moves forward a little as it turns, so it comes out ahead.
        for (i in 1..LOOP_STEPS) {
            val a = TWO_PI * i / LOOP_STEPS
            addPoint(x + drift * a + radius * sin(a), base - radius * LOOP_TALL * (1f - cos(a)))
        }
        x += drift * TWO_PI
        addLine(x, base, x + u * 0.04f, base, 4)
        x += u * 0.04f
        // Two small hills.
        for (n in 0..1) {
            val bw = u * if (n == 0) CAMEL_ONE_WIDTH else CAMEL_TWO_WIDTH
            val bh = u * k * if (n == 0) CAMEL_ONE else CAMEL_TWO
            for (i in 1..BUMP_STEPS) {
                val t = i.toFloat() / BUMP_STEPS
                val bump = sin(PI_F * t)
                addPoint(x + bw * t, base - bh * bump * bump)
            }
            x += bw
        }
        addLine(x, base, max(x, w) + u * LEAD_IN + carLength, base, 20)

        // Length and timing. Higher up is slower, lower down is faster.
        var top = Float.MAX_VALUE
        for (i in 0 until samples) top = min(top, trackY[i])
        trackS[0] = 0f
        trackT[0] = 0f
        for (i in 1 until samples) {
            val dx = trackX[i] - trackX[i - 1]
            val dy = trackY[i] - trackY[i - 1]
            val ds = sqrt(dx * dx + dy * dy)
            trackS[i] = trackS[i - 1] + ds
            val pace = sqrt(1f + DROP_SPEEDUP * (0.5f * (trackY[i] + trackY[i - 1]) - top) / u)
            trackT[i] = trackT[i - 1] + ds / (u * pace)
        }
        trackLength = trackS[samples - 1]
        lapTime = trackT[samples - 1]
        endPace = sqrt(1f + DROP_SPEEDUP * (trackY[samples - 1] - top) / u)

        // The rail, the spine under it and the ties between them.
        rail.reset()
        spine.reset()
        ties.reset()
        val drop = u * SPINE_DROP
        var nextTie = 0f
        for (i in 0 until samples) {
            val back = max(0, i - 1)
            val ahead = min(samples - 1, i + 1)
            var tx = trackX[ahead] - trackX[back]
            var ty = trackY[ahead] - trackY[back]
            val len = sqrt(tx * tx + ty * ty).coerceAtLeast(1e-3f)
            tx /= len
            ty /= len
            // Down, away from the riders: the tangent turned a quarter turn.
            val dx = -ty
            val dy = tx
            val sx = trackX[i] + dx * drop
            val sy = trackY[i] + dy * drop
            if (i == 0) {
                rail.moveTo(trackX[i], trackY[i])
                spine.moveTo(sx, sy)
            } else {
                rail.lineTo(trackX[i], trackY[i])
                spine.lineTo(sx, sy)
            }
            if (trackS[i] >= nextTie) {
                ties.moveTo(trackX[i], trackY[i])
                ties.lineTo(sx, sy)
                nextTie = trackS[i] + u * TIE_EVERY
            }
        }

        // Posts from the lowest part of the track down past the bottom edge, and the lattice.
        posts.reset()
        braces.reset()
        val step = u * POST_EVERY
        val count = ((w + 2f * step) / step).toInt() + 1
        var lastX = Float.NaN
        var lastTop = 0f
        for (p in 0 until count) {
            val px = -step + p * step
            val topY = lowestTrackAt(px)
            if (topY.isNaN()) {
                lastX = Float.NaN
                continue
            }
            val postTop = topY + drop
            posts.moveTo(px, postTop)
            posts.lineTo(px, h + unit)
            if (!lastX.isNaN()) {
                val shared = max(postTop, lastTop)
                var y = h
                val rung = u * RUNG_EVERY
                while (y - rung > shared) {
                    braces.moveTo(lastX, y)
                    braces.lineTo(px, y - rung)
                    braces.moveTo(lastX, y - rung)
                    braces.lineTo(px, y)
                    braces.moveTo(lastX, y - rung)
                    braces.lineTo(px, y - rung)
                    y -= rung
                }
            }
            lastX = px
            lastTop = postTop
        }
        bulbDash = DashPathEffect(floatArrayOf(0.01f, u * BULB_EVERY), 0f)
        buildCar()
    }

    /** The lowest point of the track straight above [x], or NaN if the track is not there. */
    private fun lowestTrackAt(x: Float): Float {
        var best = Float.NaN
        for (i in 1 until samples) {
            val x0 = trackX[i - 1]
            val x1 = trackX[i]
            if ((x0 <= x && x1 >= x) || (x1 <= x && x0 >= x)) {
                val f = if (x1 != x0) (x - x0) / (x1 - x0) else 0f
                val y = trackY[i - 1] + (trackY[i] - trackY[i - 1]) * f
                if (best.isNaN() || y > best) best = y
            }
        }
        return best
    }

    private fun addLine(x0: Float, y0: Float, x1: Float, y1: Float, steps: Int) {
        if (samples == 0) addPoint(x0, y0)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            addPoint(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t)
        }
    }

    private fun addPoint(x: Float, y: Float) {
        if (samples >= MAX_SAMPLES) return
        trackX[samples] = x
        trackY[samples] = y
        samples++
    }

    private fun buildCar() {
        if (!carBody.isEmpty) return
        carBody.moveTo(-0.47f, -0.07f)
        carBody.lineTo(0.42f, -0.07f)
        carBody.quadTo(0.5f, -0.07f, 0.5f, -0.16f)
        carBody.lineTo(0.48f, -0.36f)
        carBody.lineTo(0.16f, -0.36f)
        carBody.quadTo(0.1f, -0.3f, 0f, -0.3f)
        carBody.lineTo(-0.28f, -0.3f)
        carBody.lineTo(-0.36f, -0.5f)
        carBody.quadTo(-0.46f, -0.52f, -0.48f, -0.44f)
        carBody.close()

        carNose.moveTo(-0.47f, -0.07f)
        carNose.lineTo(0.5f, -0.07f)
        carNose.cubicTo(0.66f, -0.07f, 0.66f, -0.26f, 0.5f, -0.36f)
        carNose.lineTo(0.16f, -0.36f)
        carNose.quadTo(0.1f, -0.3f, 0f, -0.3f)
        carNose.lineTo(-0.28f, -0.3f)
        carNose.lineTo(-0.36f, -0.5f)
        carNose.quadTo(-0.46f, -0.52f, -0.48f, -0.44f)
        carNose.close()

        rider.addCircle(-0.06f, -0.68f, 0.11f, Path.Direction.CW)
        rider.moveTo(-0.24f, -0.3f)
        rider.lineTo(-0.22f, -0.5f)
        rider.quadTo(-0.06f, -0.6f, 0.08f, -0.5f)
        rider.lineTo(0.1f, -0.3f)
        rider.close()

        armsUp.moveTo(-0.17f, -0.5f)
        armsUp.lineTo(-0.3f, -0.95f)
        armsUp.moveTo(0.04f, -0.5f)
        armsUp.lineTo(0.16f, -0.95f)
        armsDown.moveTo(0.04f, -0.5f)
        armsDown.lineTo(0.26f, -0.4f)
    }

    // ------------------------------------------------------------------ Ride view

    private fun drawRide(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        rideLook(frame.width, frame.height)
        val calm = frame.reducedMotion
        var speed = EffectMath.lerp(RIDE_SLOW, RIDE_FAST, s.value(FxKeys.COASTER_SPEED))
        if (calm) speed *= CALM
        var swoop = EffectMath.lerp(SWOOP_MIN, SWOOP_MAX, s.value(FxKeys.COASTER_SIZE))
        if (calm) swoop *= CALM_SWOOP

        // The camera rides the track, a little above the rails, and looks a little ahead.
        val d0 = frame.time * speed
        camX = trackSide(d0, swoop)
        camY = trackHeight(d0, swoop) + EYE
        camZ = d0
        val look = d0 + LOOK_AHEAD
        val slopeX = (trackSide(look + 0.5f, swoop) - trackSide(look - 0.5f, swoop))
        val slopeY = (trackHeight(look + 0.5f, swoop) - trackHeight(look - 0.5f, swoop))
        val yaw = atan(slopeX)
        val pitch = atan(slopeY / sqrt(1f + slopeX * slopeX))
        val bend = trackSide(look + 4f, swoop) - 2f * trackSide(look, swoop) + trackSide(look - 4f, swoop)
        val roll = (-bend * ROLL_PER_BEND).coerceIn(-MAX_ROLL, MAX_ROLL)
        yawCos = cos(yaw)
        yawSin = sin(yaw)
        pitchCos = cos(pitch)
        pitchSin = sin(pitch)
        focal = h * FOCAL
        midX = w / 2f
        midY = h * HORIZON
        val shake = if (calm) 0f else h * SHAKE * sin(frame.time * SHAKE_SPEED) * min(1f, speed / RIDE_FAST)

        canvas.save()
        canvas.translate(0f, shake)
        canvas.rotate(roll, midX, midY)
        val big = max(w, h) * 1.5f

        // Sky and ground, split at the horizon.
        val horizon = midY + focal * pitchSin / pitchCos
        canvas.save()
        canvas.translate(0f, horizon)
        shade(sky)
        canvas.drawRect(-big, -big * 2f, w + big, 0.5f, fill)
        shade(ground)
        canvas.drawRect(-big, 0f, w + big, big * 2f, fill)
        fill.shader = null
        canvas.restore()

        // The sun, far away, and soft clouds.
        if (projectDirection(SUN_AZIMUTH, SUN_HEIGHT)) {
            shade(sun)
            canvas.save()
            canvas.translate(outX, outY)
            canvas.scale(h * SUN_SIZE, h * SUN_SIZE)
            canvas.drawCircle(0f, 0f, 1f, fill)
            canvas.restore()
        }
        shade(puff)
        for (c in 0 until CLOUDS) {
            val az = (EffectMath.hash(c, 81) - 0.5f) * CLOUD_SPREAD
            val el = CLOUD_LOW + CLOUD_HIGH * EffectMath.hash(c, 82)
            if (!projectDirection(az, el)) continue
            val r = h * CLOUD_SIZE * (0.6f + EffectMath.hash(c, 83))
            for (p in 0 until 3) {
                canvas.save()
                canvas.translate(outX + (p - 1) * r * 0.9f, outY - if (p == 1) r * 0.25f else 0f)
                canvas.scale(r * 1.4f, r * 0.55f)
                canvas.drawCircle(0f, 0f, 1f, fill)
                canvas.restore()
            }
        }
        fill.shader = null

        // Far hills on the horizon.
        hills.reset()
        var started = false
        for (i in 0..HILL_POINTS) {
            val az = yaw - HILL_REACH + 2f * HILL_REACH * i / HILL_POINTS
            val el = HILL_BASE + HILL_A * sin(az * 3f + 1f) + HILL_B * sin(az * 7f + 2f)
            if (!projectDirection(az, el)) continue
            if (!started) {
                hills.moveTo(outX, outY)
                started = true
            } else {
                hills.lineTo(outX, outY)
            }
        }
        if (started) {
            projectDirection(yaw + HILL_REACH, -0.02f)
            hills.lineTo(outX, outY)
            projectDirection(yaw - HILL_REACH, -0.02f)
            hills.lineTo(outX, outY)
            hills.close()
            fill.color = HILL_COLOR
            canvas.drawPath(hills, fill)
        }
        // Nearer, greener hills in front of the far ones.
        hills.reset()
        started = false
        for (i in 0..HILL_POINTS) {
            val az = yaw - HILL_REACH + 2f * HILL_REACH * i / HILL_POINTS
            val el = HILL_BASE * 0.5f + HILL_B * sin(az * 5f + 0.4f) + HILL_B * 0.6f * sin(az * 11f + 3f)
            if (!projectDirection(az, el)) continue
            if (!started) {
                hills.moveTo(outX, outY)
                started = true
            } else {
                hills.lineTo(outX, outY)
            }
        }
        if (started) {
            projectDirection(yaw + HILL_REACH, -0.02f)
            hills.lineTo(outX, outY)
            projectDirection(yaw - HILL_REACH, -0.02f)
            hills.lineTo(outX, outY)
            hills.close()
            fill.color = HILL_NEAR
            canvas.drawPath(hills, fill)
        }

        // Fields on the ground, rushing past.
        stripes.reset()
        val first = floor(camZ / STRIPE) * STRIPE
        for (n in 1..STRIPES) {
            val z = first + n * STRIPE
            addQuad(stripes, camX - FIELD_WIDE, 0f, z, camX + FIELD_WIDE, 0f, z, camX + FIELD_WIDE, 0f, z + STRIPE * 0.5f, camX - FIELD_WIDE, 0f, z + STRIPE * 0.5f)
        }
        fill.color = STRIPE_COLOR
        canvas.drawPath(stripes, fill)
        rows.reset()
        val rowStart = floor(camX / ROW_GAP) * ROW_GAP
        for (m in -ROWS..ROWS) {
            val x = rowStart + m * ROW_GAP
            if (project(x, 0f, camZ + 2f)) {
                rows.moveTo(outX, outY)
                if (project(x, 0f, camZ + FAR)) rows.lineTo(outX, outY)
            }
        }
        pen.shader = null
        pen.color = ROW_COLOR
        pen.strokeWidth = w * 0.004f
        canvas.drawPath(rows, pen)

        // Posts that hold the track up.
        pillars.reset()
        val firstPost = ceil((camZ + 2f) / POST_STEP) * POST_STEP
        for (n in 0 until POSTS_AHEAD) {
            val z = firstPost + n * POST_STEP
            val tx = trackSide(z, swoop)
            val ty = trackHeight(z, swoop) - BEAM_DROP
            addQuad(pillars, tx - POST_HALF, ty, z, tx + POST_HALF, ty, z, tx + POST_HALF, 0f, z, tx - POST_HALF, 0f, z)
        }
        fill.color = PILLAR_COLOR
        canvas.drawPath(pillars, fill)

        // The track: a dark beam, the sleepers, then the two shining rails.
        beam.reset()
        leftRail.reset()
        rightRail.reset()
        glowRail.reset()
        ribbon(beam, 0f, BEAM_HALF, -BEAM_DROP, swoop)
        ribbon(glowRail, -GAUGE, RAIL_GLOW_HALF, 0f, swoop)
        ribbon(glowRail, GAUGE, RAIL_GLOW_HALF, 0f, swoop)
        ribbon(leftRail, -GAUGE, RAIL_HALF, 0f, swoop)
        ribbon(leftRail, GAUGE, RAIL_HALF, 0f, swoop)
        ribbon(rightRail, -GAUGE, RAIL_HALF * RAIL_SHINE, RAIL_HALF * 0.5f, swoop)
        ribbon(rightRail, GAUGE, RAIL_HALF * RAIL_SHINE, RAIL_HALF * 0.5f, swoop)
        sleepers.reset()
        val firstTie = ceil((camZ + NEAR_TIE) / TIE_STEP) * TIE_STEP
        for (n in 0 until TIES_AHEAD) {
            val z = firstTie + n * TIE_STEP
            val tx = trackSide(z, swoop)
            val ty = trackHeight(z, swoop) - 0.08f
            addQuad(sleepers, tx - TIE_HALF, ty, z - TIE_DEPTH, tx + TIE_HALF, ty, z - TIE_DEPTH, tx + TIE_HALF, ty, z + TIE_DEPTH, tx - TIE_HALF, ty, z + TIE_DEPTH)
        }
        fill.color = BEAM_COLOR
        canvas.drawPath(beam, fill)
        fill.color = SLEEPER_COLOR
        canvas.drawPath(sleepers, fill)
        fill.color = EffectMath.withAlpha(frame.panelColor or OPAQUE, RIDE_GLOW_ALPHA)
        canvas.drawPath(glowRail, fill)
        // Steel rails with a bright line of light along their tops.
        fill.color = RIDE_STEEL
        canvas.drawPath(leftRail, fill)
        fill.color = RIDE_RAIL
        canvas.drawPath(rightRail, fill)
        canvas.restore()

        // The front of the car, fixed to the rider.
        drawNose(canvas, w, h)
    }

    /** A band along the track, [side] meters to the side, [half] meters wide, [lift] meters up. */
    private fun ribbon(path: Path, side: Float, half: Float, lift: Float, swoop: Float) {
        var started = false
        var count = 0
        for (k in 0..RAIL_POINTS) {
            val z = camZ + NEAR * (FAR / NEAR).pow(k.toFloat() / RAIL_POINTS)
            val x = trackSide(z, swoop) + side
            val y = trackHeight(z, swoop) + lift
            if (!project(x - half, y, z)) continue
            if (!started) path.moveTo(outX, outY) else path.lineTo(outX, outY)
            started = true
            count++
        }
        if (!started) return
        for (k in RAIL_POINTS downTo 0) {
            val z = camZ + NEAR * (FAR / NEAR).pow(k.toFloat() / RAIL_POINTS)
            val x = trackSide(z, swoop) + side
            val y = trackHeight(z, swoop) + lift
            if (!project(x + half, y, z)) continue
            path.lineTo(outX, outY)
        }
        path.close()
    }

    private fun addQuad(
        path: Path,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
    ) {
        if (!project(x0, y0, z0)) return
        val ax = outX
        val ay = outY
        if (!project(x1, y1, z1)) return
        val bx = outX
        val by = outY
        if (!project(x2, y2, z2)) return
        val cx = outX
        val cy = outY
        if (!project(x3, y3, z3)) return
        path.moveTo(ax, ay)
        path.lineTo(bx, by)
        path.lineTo(cx, cy)
        path.lineTo(outX, outY)
        path.close()
    }

    /** Puts the world point on the screen. False when it is behind the rider. */
    private fun project(x: Float, y: Float, z: Float): Boolean {
        val rx = x - camX
        val ry = y - camY
        val rz = z - camZ
        val x1 = rx * yawCos - rz * yawSin
        val z1 = rx * yawSin + rz * yawCos
        val y2 = ry * pitchCos - z1 * pitchSin
        val z2 = ry * pitchSin + z1 * pitchCos
        if (z2 < CLIP) return false
        outX = midX + focal * x1 / z2
        outY = midY - focal * y2 / z2
        outZ = z2
        return true
    }

    /** Puts a far-away direction on the screen, like the sun. [azimuth] and [elevation] in radians. */
    private fun projectDirection(azimuth: Float, elevation: Float): Boolean {
        val ce = cos(elevation)
        val rx = sin(azimuth) * ce
        val ry = sin(elevation)
        val rz = cos(azimuth) * ce
        val x1 = rx * yawCos - rz * yawSin
        val z1 = rx * yawSin + rz * yawCos
        val y2 = ry * pitchCos - z1 * pitchSin
        val z2 = ry * pitchSin + z1 * pitchCos
        if (z2 < 0.05f) return false
        outX = midX + focal * x1 / z2
        outY = midY - focal * y2 / z2
        return true
    }

    private fun drawNose(canvas: Canvas, w: Float, h: Float) {
        shade(noseShade)
        canvas.drawPath(nose, fill)
        shade(noseShine)
        canvas.drawPath(nose, fill)
        fill.shader = null
        pen.shader = null
        pen.color = NOSE_TRIM
        pen.strokeWidth = w * 0.008f
        canvas.drawPath(noseTrim, pen)
        // The padded lap bar, close to the rider, across the bottom.
        pen.color = BAR_EDGE
        pen.strokeWidth = h * BAR_THICK * 1.25f
        canvas.drawLine(w * 0.16f, h * BAR_Y, w * 0.84f, h * BAR_Y, pen)
        pen.color = BAR_COLOR
        pen.strokeWidth = h * BAR_THICK
        canvas.drawLine(w * 0.16f, h * BAR_Y, w * 0.84f, h * BAR_Y, pen)
        pen.color = BAR_SHINE
        pen.strokeWidth = h * BAR_THICK * 0.18f
        canvas.drawLine(w * 0.18f, h * (BAR_Y - BAR_THICK * 0.22f), w * 0.82f, h * (BAR_Y - BAR_THICK * 0.22f), pen)
    }

    /** Makes the sky, ground and car-front shapes for this size. Only runs when the size changes. */
    private fun rideLook(width: Int, height: Int) {
        if (width == rideWidth && height == rideHeight) return
        rideWidth = width
        rideHeight = height
        val w = width.toFloat()
        val h = height.toFloat()
        sky = LinearGradient(0f, -h * 0.75f, 0f, 0f, SKY_COLORS, SKY_STOPS, Shader.TileMode.CLAMP)
        ground = LinearGradient(0f, 0f, 0f, h * 0.6f, GROUND_COLORS, GROUND_STOPS, Shader.TileMode.CLAMP)
        noseShade = LinearGradient(0f, h * 0.84f, 0f, h, NOSE_COLORS, NOSE_STOPS, Shader.TileMode.CLAMP)
        noseShine = LinearGradient(0f, 0f, w, 0f, SHINE_COLORS, SHINE_STOPS, Shader.TileMode.CLAMP)
        nose.reset()
        nose.moveTo(-w * 0.05f, h + 2f)
        nose.lineTo(-w * 0.05f, h * 0.9f)
        nose.cubicTo(w * 0.15f, h * 0.86f, w * 0.3f, h * 0.85f, w * 0.5f, h * 0.85f)
        nose.cubicTo(w * 0.7f, h * 0.85f, w * 0.85f, h * 0.86f, w * 1.05f, h * 0.9f)
        nose.lineTo(w * 1.05f, h + 2f)
        nose.close()
        noseTrim.reset()
        noseTrim.moveTo(-w * 0.05f, h * 0.9f)
        noseTrim.cubicTo(w * 0.15f, h * 0.86f, w * 0.3f, h * 0.85f, w * 0.5f, h * 0.85f)
        noseTrim.cubicTo(w * 0.7f, h * 0.85f, w * 0.85f, h * 0.86f, w * 1.05f, h * 0.9f)
    }

    private companion object {
        const val OPAQUE = 0xFF000000.toInt()
        const val PI_F = PI.toFloat()
        const val TWO_PI = (2.0 * PI).toFloat()
        const val DEGREES = (180.0 / PI).toFloat()
        const val CALM = 0.5f
        const val CALM_SWOOP = 0.6f

        // Watch view track, in shares of the short side.
        const val MAX_SAMPLES = 900
        const val SCALE_MIN = 0.65f
        const val SCALE_MAX = 1.35f
        const val BASE_LIFT = 0.07f
        const val LEAD_IN = 0.06f
        const val HILL_HEIGHT = 0.46f
        const val HILL_WIDTH = 0.5f
        const val HILL_SKEW = 1.3f
        const val HILL_STEPS = 140
        const val LOOP_RADIUS = 0.13f
        const val LOOP_DRIFT = 0.3f
        const val LOOP_TALL = 1.12f
        const val LOOP_STEPS = 200
        const val CAMEL_ONE = 0.26f
        const val CAMEL_TWO = 0.15f
        const val CAMEL_ONE_WIDTH = 0.24f
        const val CAMEL_TWO_WIDTH = 0.2f
        const val BUMP_STEPS = 70
        const val DROP_SPEEDUP = 7f
        const val PACE_SLOW = 0.35f
        const val PACE_FAST = 1.6f
        const val EXIT_ROOM = 0.1f
        const val MAX_CARS = 8
        const val CAR_LENGTH = 0.085f
        const val CAR_GAP = 1.08f
        const val SPINE_DROP = 0.022f
        const val TIE_EVERY = 0.018f
        const val POST_EVERY = 0.075f
        const val RUNG_EVERY = 0.075f
        const val POST_WIDTH = 0.007f
        const val BRACE_WIDTH = 0.0028f
        const val TIE_WIDTH = 0.004f
        const val SPINE_WIDTH = 0.009f
        const val RAIL_WIDTH = 0.0065f
        const val RAIL_GLOW = 0.04f
        const val RAIL_GLOW_ALPHA = 0.4f
        const val RAIL_WHITE = 0.75f
        const val BULB_EVERY = 0.045f
        const val BULB_SIZE = 0.006f
        val STEEL = 0xF2F4F7FB.toInt()
        val STEEL_SHADOW = 0x8C0B1220.toInt()
        val TIE_COLOR = 0xFFB9C3D2.toInt()
        val SPINE_COLOR = 0xFFDDE4EE.toInt()
        val BULB_COLOR = 0xFFFFD58A.toInt()

        // One car, in car lengths.
        const val WHEEL = 0.075f
        const val ARM = 0.07f
        const val CAR_OUTLINE = 0.05f
        const val TRIM_WIDTH = 0.025f
        val CHASSIS = 0xFF1E2430.toInt()
        val HUB = 0xFF9AA6B8.toInt()
        val RIDER = 0xFF1A1C26.toInt()
        val CAR_EDGE = 0xCC0B0E16.toInt()
        val TRIM = 0xCCFFFFFF.toInt()
        val LAMP = 0xFFFFFBEA.toInt()
        val CAR_COLORS = intArrayOf(
            0xFFFF2E4D.toInt(), 0xFFFFC21A.toInt(), 0xFF2BF5E6.toInt(), 0xFFB04BFF.toInt(),
            0xFFFF7A1A.toInt(), 0xFF3D8BFF.toInt(), 0xFFFF3EA5.toInt(), 0xFF46E07A.toInt(),
        )
        val GLOSS_COLORS = intArrayOf(0x8CFFFFFF.toInt(), 0x26FFFFFF, 0x00000000, 0x59000000)
        val GLOSS_STOPS = floatArrayOf(0f, 0.3f, 0.55f, 1f)

        // Ride view, in meters.
        const val RIDE_SLOW = 7f
        const val RIDE_FAST = 34f
        const val SWOOP_MIN = 0.35f
        const val SWOOP_MAX = 1.6f
        const val TRACK_HEIGHT = 16f
        const val SIDE_A = 16f
        const val SIDE_B = 9f
        const val UP_A = 7f
        const val UP_B = 3.5f
        const val EYE = 1.25f
        const val LOOK_AHEAD = 3f
        const val ROLL_PER_BEND = 160f
        const val MAX_ROLL = 28f
        const val FOCAL = 0.5f
        const val HORIZON = 0.44f
        const val SHAKE = 0.0025f
        const val SHAKE_SPEED = 23f
        const val NEAR = 1.6f
        const val FAR = 160f
        const val CLIP = 0.3f
        const val RAIL_POINTS = 48
        const val GAUGE = 0.62f
        const val RAIL_HALF = 0.045f
        const val RAIL_SHINE = 0.35f
        const val RAIL_GLOW_HALF = 0.13f
        const val BEAM_HALF = 0.2f
        const val BEAM_DROP = 0.55f
        const val TIE_STEP = 1.1f
        const val NEAR_TIE = 1.2f
        const val TIE_HALF = 0.76f
        const val TIE_DEPTH = 0.08f
        const val TIES_AHEAD = 54
        const val POST_STEP = 9f
        const val POSTS_AHEAD = 12
        const val POST_HALF = 0.28f
        const val STRIPE = 12f
        const val STRIPES = 14
        const val FIELD_WIDE = 260f
        const val ROW_GAP = 7f
        const val ROWS = 14
        const val SUN_AZIMUTH = 0.42f
        const val SUN_HEIGHT = 0.1f
        const val SUN_SIZE = 0.16f
        const val CLOUDS = 6
        const val CLOUD_SPREAD = 2.6f
        const val CLOUD_LOW = 0.06f
        const val CLOUD_HIGH = 0.28f
        const val CLOUD_SIZE = 0.05f
        const val HILL_POINTS = 40
        const val HILL_REACH = 1.2f
        const val HILL_BASE = 0.02f
        const val HILL_A = 0.022f
        const val HILL_B = 0.011f
        const val RIDE_GLOW_ALPHA = 0.35f
        val RIDE_RAIL = 0xFFFFFFFF.toInt()
        val RIDE_STEEL = 0xFF9AA6B8.toInt()
        val BEAM_COLOR = 0xFF20262F.toInt()
        val SLEEPER_COLOR = 0xFF5C677C.toInt()
        val PILLAR_COLOR = 0xFFDCE3EE.toInt()
        val HILL_COLOR = 0xFF7891A6.toInt()
        val HILL_NEAR = 0xFF46624F.toInt()
        val STRIPE_COLOR = 0x1E000000
        val ROW_COLOR = 0x12FFFFFF
        val NOSE_TRIM = 0xFFE9EEF5.toInt()
        val BAR_COLOR = 0xFF15171D.toInt()
        val BAR_EDGE = 0x66000000
        val BAR_SHINE = 0x40FFFFFF
        const val BAR_Y = 0.975f
        const val BAR_THICK = 0.03f
        val SHINE_COLORS = intArrayOf(0, 0x14FFFFFF, 0x47FFFFFF, 0x14FFFFFF, 0, 0)
        val SHINE_STOPS = floatArrayOf(0f, 0.18f, 0.3f, 0.42f, 0.6f, 1f)
        val SKY_COLORS = intArrayOf(0xFF16306E.toInt(), 0xFF3F6CC4.toInt(), 0xFF8FB6EC.toInt(), 0xFFFFD7A8.toInt(), 0xFFFFC08A.toInt())
        val SKY_STOPS = floatArrayOf(0f, 0.35f, 0.7f, 0.93f, 1f)
        val GROUND_COLORS = intArrayOf(0xFF8FA58A.toInt(), 0xFF4F7A45.toInt(), 0xFF2C5228.toInt(), 0xFF173318.toInt())
        val GROUND_STOPS = floatArrayOf(0f, 0.12f, 0.45f, 1f)
        val NOSE_COLORS = intArrayOf(0xFFFF6A7E.toInt(), 0xFFE0203E.toInt(), 0xFF8C0A1E.toInt(), 0xFF3A0410.toInt())
        val NOSE_STOPS = floatArrayOf(0f, 0.15f, 0.6f, 1f)

        /** The track's side-to-side place at [d] meters along, for the ride view. */
        fun trackSide(d: Float, swoop: Float): Float =
            swoop * (SIDE_A * sin(d * 0.019f) + SIDE_B * sin(d * 0.043f + 1.3f))

        /** The track's height at [d] meters along, for the ride view. Never near the ground. */
        fun trackHeight(d: Float, swoop: Float): Float =
            TRACK_HEIGHT + swoop * (UP_A * sin(d * 0.031f + 0.4f) + UP_B * sin(d * 0.071f + 2.1f))

        fun towardWhite(color: Int, f: Float): Int {
            val r = Color.red(color) + ((255 - Color.red(color)) * f).toInt()
            val g = Color.green(color) + ((255 - Color.green(color)) * f).toInt()
            val b = Color.blue(color) + ((255 - Color.blue(color)) * f).toInt()
            return Color.rgb(r, g, b)
        }
    }
}
