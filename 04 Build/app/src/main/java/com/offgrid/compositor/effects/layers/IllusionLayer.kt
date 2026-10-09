package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Eye tricks (feature 27), drawn see-through over the camera. Nothing here flashes.
 *
 * Tunnel: rings of the video's own shape sink into the middle, so the box looks like a deep hole.
 * Triangle: a big impossible triangle with three shaded faces that slowly turns and breathes.
 * Stairs: a never-ending staircase loop with a small glowing dot that climbs forever.
 * Drift: rings of stepped dark and light pieces that seem to turn even while still. They also
 * turn for real, very slowly.
 */
class IllusionLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(Color.WHITE, EffectMath.withAlpha(DOT_COLOR, 0.85f), EffectMath.withAlpha(DOT_COLOR, 0.25f), 0),
            floatArrayOf(0f, 0.18f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(Color.BLACK, EffectMath.withAlpha(Color.BLACK, 0.7f), 0),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(EffectMath.withAlpha(AURA_COLOR, 0.32f), EffectMath.withAlpha(AURA_COLOR, 0.1f), 0),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    private val band = Path()
    private val ring = Path()
    private val shape = Path()
    private val box = RectF()
    private val twist = Matrix()
    private val spin = Matrix()

    /** Face shaders for the solid shapes: top, right side, left side, in unit space. */
    private val faceShaders = arrayOf(
        LinearGradient(0f, -0.5f, 0f, 0.5f, TOP_LIGHT, TOP_DARK, Shader.TileMode.CLAMP),
        LinearGradient(0f, -0.5f, 0f, 0.5f, SIDE_LIGHT, SIDE_DARK, Shader.TileMode.CLAMP),
        LinearGradient(0f, -0.5f, 0f, 0.5f, SHADE_LIGHT, SHADE_DARK, Shader.TileMode.CLAMP),
    )

    private val triangle = Solid.penroseTriangle()
    private val stairs = Solid.penroseStairs()
    private val driftShaders = Array(DRIFT_RINGS) { k -> driftShader(k) }

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.ILLUSION_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        if (frame.width <= 0 || frame.height <= 0) return
        val size = s.value(FxKeys.ILLUSION_SIZE)
        var speed = SPEED_MIN + (1f - SPEED_MIN) * s.value(FxKeys.ILLUSION_SPEED)
        if (frame.reducedMotion) speed *= 0.5f
        val strength = s.value(FxKeys.ILLUSION_STRENGTH)
        when {
            s.chose(FxKeys.ILLUSION_KIND, FxKeys.ILLUSION_TRIANGLE) -> drawTriangle(canvas, frame, size, speed, strength)
            s.chose(FxKeys.ILLUSION_KIND, FxKeys.ILLUSION_STAIRS) -> drawStairs(canvas, frame, size, speed, strength)
            s.chose(FxKeys.ILLUSION_KIND, FxKeys.ILLUSION_DRIFT) -> drawDrift(canvas, frame, size, speed, strength)
            else -> drawTunnel(canvas, frame, size, speed, strength)
        }
    }

    /** Rings shaped like the video sink into the middle, dark and light in turn, with thin glowing edges. */
    private fun drawTunnel(canvas: Canvas, frame: EffectFrame, size: Float, speed: Float, strength: Float) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val reach = TUNNEL_MIN + (TUNNEL_MAX - TUNNEL_MIN) * size
        val halfW = w / 2f * reach
        val halfH = h / 2f * reach
        val flow = frame.time * speed * TUNNEL_RATE
        val base = floor(flow)
        val phase = flow - base
        val dark = TUNNEL_DARK_MIN + (1f - TUNNEL_DARK_MIN) * strength
        val edgeColor = frame.panelColor
        val line = min(w, h) * TUNNEL_LINE
        for (k in -1 until TUNNEL_RINGS) {
            val d0 = k + phase
            val d1 = d0 + 1f
            val s0 = min(1f, TUNNEL_STEP.pow(d0))
            val s1 = TUNNEL_STEP.pow(d1)
            val deep = EffectMath.clamp01(d0 / TUNNEL_RINGS)
            band.rewind()
            addRing(band, cx, cy, halfW * s0, halfH * s0, d0)
            addRing(band, cx, cy, halfW * s1, halfH * s1, d1)
            band.fillType = Path.FillType.EVEN_ODD
            val darkBand = ((k - base.toInt()) and 1) == 0
            fill.shader = null
            fill.color = if (darkBand) {
                EffectMath.withAlpha(TUNNEL_INK, dark * TUNNEL_DARK)
            } else {
                EffectMath.withAlpha(Color.WHITE, (TUNNEL_LIGHT_MIN + TUNNEL_LIGHT * strength) * (1f - deep))
            }
            canvas.drawPath(band, fill)
            // A thin glowing edge on the outside of each ring, dimmer as it goes deeper.
            if (d0 >= 0f) {
                ring.rewind()
                addRing(ring, cx, cy, halfW * s0, halfH * s0, d0)
                val fadeIn = EffectMath.clamp01(d0)
                val lit = (1f - deep) * (1f - deep) * fadeIn * (EDGE_MIN + (1f - EDGE_MIN) * strength)
                pen.strokeWidth = line * EDGE_GLOW
                pen.color = EffectMath.withAlpha(edgeColor, EDGE_GLOW_ALPHA * lit)
                canvas.drawPath(ring, pen)
                pen.strokeWidth = line
                pen.color = EffectMath.withAlpha(edgeColor, lit)
                canvas.drawPath(ring, pen)
            }
        }
        // The far end of the tunnel is a dark hole.
        val hole = min(halfW, halfH) * HOLE_SIZE
        holePaint.alpha = (255 * (HOLE_MIN + (1f - HOLE_MIN) * strength)).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(hole, hole)
        canvas.drawCircle(0f, 0f, 1f, holePaint)
        canvas.restore()
    }

    /** Adds one rounded ring shape, turned a little more the deeper it is, so the tunnel twists. */
    private fun addRing(path: Path, cx: Float, cy: Float, halfW: Float, halfH: Float, depth: Float) {
        box.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        val corner = min(halfW, halfH) * TUNNEL_CORNER
        twist.setRotate(depth * TWIST_DEGREES, cx, cy)
        shape.rewind()
        shape.addRoundRect(box, corner, corner, Path.Direction.CW)
        path.addPath(shape, twist)
    }

    private fun drawTriangle(canvas: Canvas, frame: EffectFrame, size: Float, speed: Float, strength: Float) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val unit = w * (SOLID_MIN + (SOLID_MAX - SOLID_MIN) * size)
        val breathe = 1f + BREATHE * sin((2f * PI).toFloat() * frame.time / (BREATHE_SECONDS / speed))
        val angle = frame.time * speed * TRIANGLE_DEGREES
        drawSolid(canvas, triangle, w / 2f, h * SOLID_Y, unit * breathe, angle, strength)
    }

    private fun drawStairs(canvas: Canvas, frame: EffectFrame, size: Float, speed: Float, strength: Float) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val unit = w * (SOLID_MIN + (SOLID_MAX - SOLID_MIN) * size)
        val cx = w / 2f
        val cy = h * SOLID_Y
        drawSolid(canvas, stairs, cx, cy, unit, 0f, strength)

        // The dot hops up one step at a time, round and round, and never gets any higher.
        val steps = stairs.path.size / 2 - 1
        val climb = frame.time * speed * STEPS_PER_SECOND
        val dotR = unit * DOT_SIZE
        for (ghost in DOT_TRAIL downTo 0) {
            val at = climb - ghost * DOT_TRAIL_GAP
            if (at < 0f) continue
            val whole = floor(at)
            val i = whole.toInt() % steps
            val f = at - whole
            val hop = HOP * sin(PI.toFloat() * EffectMath.smooth(f))
            val x = EffectMath.lerp(stairs.path[2 * i], stairs.path[2 * i + 2], EffectMath.smooth(f))
            val y = EffectMath.lerp(stairs.path[2 * i + 1], stairs.path[2 * i + 3], EffectMath.smooth(f)) - hop
            val fade = if (ghost == 0) 1f else (1f - ghost / (DOT_TRAIL + 1f)) * GHOST_ALPHA
            val r = dotR * (if (ghost == 0) 1f else GHOST_SIZE) * DOT_GLOW
            glowPaint.alpha = (255 * fade).toInt()
            canvas.save()
            canvas.translate(cx + x * unit, cy + y * unit)
            canvas.scale(r, r)
            canvas.drawCircle(0f, 0f, 1f, glowPaint)
            canvas.restore()
        }
    }

    /**
     * Draws a solid made of flat faces, back to front, at full strength inside one see-through
     * layer, so faces that overlap do not show through each other.
     */
    private fun drawSolid(canvas: Canvas, solid: Solid, cx: Float, cy: Float, unit: Float, degrees: Float, strength: Float) {
        val reach = unit * SOLID_ROOM
        val alpha = SOLID_ALPHA_MIN + (SOLID_ALPHA_MAX - SOLID_ALPHA_MIN) * strength
        canvas.save()
        canvas.translate(cx, cy)
        auraPaint.alpha = (255 * alpha).toInt()
        canvas.save()
        canvas.scale(reach, reach)
        canvas.drawCircle(0f, 0f, 1f, auraPaint)
        canvas.restore()
        canvas.saveLayerAlpha(-reach, -reach, reach, reach, (255 * alpha).toInt())
        canvas.rotate(degrees)
        canvas.scale(unit, unit)
        pen.strokeWidth = SOLID_EDGE
        pen.color = SOLID_EDGE_COLOR
        for (i in solid.faces.indices) {
            fill.shader = faceShaders[solid.shades[i]]
            canvas.drawPath(solid.faces[i], fill)
            canvas.drawPath(solid.faces[i], pen)
        }
        fill.shader = null
        canvas.restore()
        canvas.restore()
    }

    private fun drawDrift(canvas: Canvas, frame: EffectFrame, size: Float, speed: Float, strength: Float) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        val outer = w * (DRIFT_MIN + (DRIFT_MAX - DRIFT_MIN) * size)
        val inner = outer * DRIFT_HOLE
        val width = (outer - inner) / DRIFT_RINGS
        val alpha = (255 * (DRIFT_ALPHA_MIN + (1f - DRIFT_ALPHA_MIN) * strength)).toInt()
        val turn = frame.time * speed * DRIFT_DEGREES
        canvas.save()
        canvas.translate(w / 2f, h / 2f)
        pen.strokeCap = Paint.Cap.BUTT
        for (k in 0 until DRIFT_RINGS) {
            val r = inner + (k + 0.5f) * width
            // Rings take turns: one seems to turn one way, the next the other way. The real turn agrees.
            val way = if (k % 2 == 0) 1f else -1f
            spin.setRotate(way * turn + k * DRIFT_STAGGER)
            driftShaders[k].setLocalMatrix(spin)
            pen.shader = driftShaders[k]
            pen.alpha = alpha
            pen.strokeWidth = width * DRIFT_FILL
            canvas.drawCircle(0f, 0f, r, pen)
        }
        pen.shader = null
        pen.strokeCap = Paint.Cap.ROUND
        fill.color = EffectMath.withAlpha(Color.BLACK, alpha / 255f)
        canvas.drawCircle(0f, 0f, inner * DRIFT_CORE, fill)
        pen.strokeWidth = min(w, h) * TUNNEL_LINE
        pen.color = EffectMath.withAlpha(frame.panelColor, alpha / 255f)
        canvas.drawCircle(0f, 0f, inner * DRIFT_CORE, pen)
        canvas.restore()
    }

    /** One drift ring: black, dark, white, light, over and over, with hard edges. Odd rings run backward. */
    private fun driftShader(k: Int): Shader {
        val r = DRIFT_HOLE + (k + 0.5f) * (1f - DRIFT_HOLE) / DRIFT_RINGS
        val width = (1f - DRIFT_HOLE) / DRIFT_RINGS
        val pieces = max(DRIFT_PIECES_MIN, (2f * PI.toFloat() * r / (width * DRIFT_PIECE_LENGTH)).roundToInt())
        val order = if (k % 2 == 0) DRIFT_ORDER else DRIFT_ORDER_BACK
        val colors = IntArray(pieces * order.size * 2)
        val stops = FloatArray(colors.size)
        var at = 0f
        var n = 0
        for (p in 0 until pieces) {
            for (c in order.indices) {
                val next = at + DRIFT_SHARES[c] / pieces
                colors[n] = order[c]
                stops[n] = at
                colors[n + 1] = order[c]
                stops[n + 1] = min(1f, next)
                n += 2
                at = next
            }
        }
        stops[stops.size - 1] = 1f
        return SweepGradient(0f, 0f, colors, stops)
    }

    /**
     * A solid drawn as flat faces in unit space, back to front, 1 unit wide and centered.
     * [path] is an optional walk across the top, as x and y pairs.
     */
    private class Solid(val faces: Array<Path>, val shades: IntArray, val path: FloatArray) {
        companion object {
            /** The impossible triangle: three bars, each meeting the next at a true corner. */
            fun penroseTriangle(): Solid {
                val n = TRIANGLE_SIDE
                val boxes = arrayOf(
                    floatArrayOf(0f, n, 0f, 1f, 0f, 1f),
                    floatArrayOf(n - 1f, n, 0f, n, 0f, 1f),
                    floatArrayOf(n - 1f, n, n - 1f, n, 0f, n),
                )
                return build(boxes, TRIANGLE_TURN, FloatArray(0))
            }

            /**
             * The never-ending stairs. Steps go 6 one way, 6 the next, then 2 and 2 back. They rise all
             * the way round, and the drawing still meets where it started.
             */
            fun penroseStairs(): Solid {
                val moves = intArrayOf(1, 0, 0, 1, -1, 0, 0, -1)
                val counts = STAIR_RUNS
                val total = counts.sum()
                val rise = (counts[0] - counts[2]).toFloat() / total
                val cells = ArrayList<FloatArray>()
                var x = 0
                var y = 0
                for (side in counts.indices) {
                    for (step in 0 until counts[side]) {
                        cells += floatArrayOf(x.toFloat(), y.toFloat(), cells.size * rise)
                        x += moves[2 * side]
                        y += moves[2 * side + 1]
                    }
                }
                // The walk across the step tops, ending where it began.
                val walk = FloatArray((total + 1) * 2)
                for (i in 0..total) {
                    val c = cells[i % total]
                    val z = if (i == total) total * rise else c[2]
                    val shift = if (i == total) (counts[0] - counts[2]).toFloat() else 0f
                    walk[2 * i] = projectX(c[0] + shift + 0.5f, c[1] + shift + 0.5f)
                    walk[2 * i + 1] = projectY(c[0] + shift + 0.5f, c[1] + shift + 0.5f, z)
                }
                cells.sortWith(compareBy<FloatArray> { it[0] + it[1] }.thenBy { it[2] })
                val boxes = Array(cells.size) { i ->
                    val c = cells[i]
                    floatArrayOf(c[0], c[0] + 1f, c[1], c[1] + 1f, c[2] - STEP_HEIGHT, c[2])
                }
                return build(boxes, 0f, walk)
            }

            /** Projects each box's three seen faces, turns them, then scales all to 1 unit wide. */
            private fun build(boxes: Array<FloatArray>, degrees: Float, walk: FloatArray): Solid {
                val points = ArrayList<FloatArray>()
                val shades = ArrayList<Int>()
                for (b in boxes) {
                    val x0 = b[0]
                    val x1 = b[1]
                    val y0 = b[2]
                    val y1 = b[3]
                    val z0 = b[4]
                    val z1 = b[5]
                    // Right side (x), left side (y), then top (z). The top is drawn last so it sits on the sides.
                    points += face(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1)
                    shades += 1
                    points += face(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1)
                    shades += 2
                    points += face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1)
                    shades += 0
                }
                val turn = Matrix().apply { setRotate(degrees) }
                for (p in points) turn.mapPoints(p)
                turn.mapPoints(walk)
                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (p in points) {
                    for (i in p.indices step 2) {
                        minX = min(minX, p[i])
                        maxX = max(maxX, p[i])
                        minY = min(minY, p[i + 1])
                        maxY = max(maxY, p[i + 1])
                    }
                }
                val span = max(maxX - minX, maxY - minY)
                val midX = (minX + maxX) / 2f
                val midY = (minY + maxY) / 2f
                val faces = Array(points.size) { i ->
                    val p = points[i]
                    Path().apply {
                        moveTo((p[0] - midX) / span, (p[1] - midY) / span)
                        for (j in 2 until p.size step 2) lineTo((p[j] - midX) / span, (p[j + 1] - midY) / span)
                        close()
                    }
                }
                for (i in walk.indices step 2) {
                    walk[i] = (walk[i] - midX) / span
                    walk[i + 1] = (walk[i + 1] - midY) / span
                }
                return Solid(faces, shades.toIntArray(), walk)
            }

            private fun face(vararg c: Float): FloatArray {
                val out = FloatArray(8)
                for (i in 0 until 4) {
                    out[2 * i] = projectX(c[3 * i], c[3 * i + 1])
                    out[2 * i + 1] = projectY(c[3 * i], c[3 * i + 1], c[3 * i + 2])
                }
                return out
            }

            /** Even (isometric) view: x goes down right, y goes down left, z goes straight up. */
            fun projectX(x: Float, y: Float): Float = (x - y) * ISO_X

            fun projectY(x: Float, y: Float, z: Float): Float = (x + y) * ISO_Y - z
        }
    }

    private companion object {
        const val SPEED_MIN = 0.15f

        const val TUNNEL_MIN = 0.45f
        const val TUNNEL_MAX = 1.1f
        const val TUNNEL_RINGS = 18
        const val TUNNEL_STEP = 0.82f
        const val TUNNEL_RATE = 2f
        const val TUNNEL_CORNER = 0.1f
        const val TWIST_DEGREES = 1.2f
        const val TUNNEL_INK = 0xFF02080A.toInt()
        const val TUNNEL_DARK_MIN = 0.3f
        const val TUNNEL_DARK = 0.85f
        const val TUNNEL_LIGHT_MIN = 0.03f
        const val TUNNEL_LIGHT = 0.1f
        const val TUNNEL_LINE = 0.003f
        const val EDGE_GLOW = 4f
        const val EDGE_GLOW_ALPHA = 0.18f
        const val EDGE_MIN = 0.35f
        const val HOLE_SIZE = 0.55f
        const val HOLE_MIN = 0.35f

        const val SOLID_MIN = 0.35f
        const val SOLID_MAX = 0.95f
        const val SOLID_Y = 0.5f
        const val SOLID_ROOM = 0.75f
        const val SOLID_ALPHA_MIN = 0.3f
        const val SOLID_ALPHA_MAX = 0.95f
        const val SOLID_EDGE = 0.004f
        const val SOLID_EDGE_COLOR = 0xE6031014.toInt()
        const val BREATHE = 0.04f
        const val BREATHE_SECONDS = 4f
        const val TRIANGLE_DEGREES = 8f
        const val TRIANGLE_SIDE = 5f
        const val TRIANGLE_TURN = 30f
        const val ISO_X = 0.8660254f
        const val ISO_Y = 0.5f

        val STAIR_RUNS = intArrayOf(6, 6, 2, 2)
        const val STEP_HEIGHT = 1.5f
        const val STEPS_PER_SECOND = 4f
        const val DOT_SIZE = 0.016f
        const val DOT_GLOW = 4f
        const val DOT_TRAIL = 4
        const val DOT_TRAIL_GAP = 0.12f
        const val GHOST_ALPHA = 0.5f
        const val GHOST_SIZE = 0.7f
        const val HOP = 0.035f
        const val DOT_COLOR = 0xFF2BF5E6.toInt()
        const val AURA_COLOR = 0xFF2BF5E6.toInt()

        const val TOP_LIGHT = 0xFFF4FFFD.toInt()
        const val TOP_DARK = 0xFFBDEBE6.toInt()
        const val SIDE_LIGHT = 0xFF45D6CA.toInt()
        const val SIDE_DARK = 0xFF16857E.toInt()
        const val SHADE_LIGHT = 0xFF14404A.toInt()
        const val SHADE_DARK = 0xFF061A1F.toInt()

        const val DRIFT_RINGS = 6
        const val DRIFT_MIN = 0.18f
        const val DRIFT_MAX = 0.6f
        const val DRIFT_HOLE = 0.18f
        const val DRIFT_FILL = 0.92f
        const val DRIFT_CORE = 0.92f
        const val DRIFT_PIECES_MIN = 8
        const val DRIFT_PIECE_LENGTH = 1.7f
        const val DRIFT_DEGREES = 6f
        const val DRIFT_STAGGER = 7f
        const val DRIFT_ALPHA_MIN = 0.3f

        /** Black, dark teal, white, light teal: the steps that make the eye see drift. */
        val DRIFT_ORDER = intArrayOf(0xFF000000.toInt(), 0xFF1B5A60.toInt(), 0xFFFFFFFF.toInt(), 0xFFA6EDE6.toInt())
        val DRIFT_ORDER_BACK = intArrayOf(0xFF000000.toInt(), 0xFFA6EDE6.toInt(), 0xFFFFFFFF.toInt(), 0xFF1B5A60.toInt())

        /** How much of each piece each step takes: big black and white, thin dark and light. */
        val DRIFT_SHARES = floatArrayOf(0.4f, 0.1f, 0.4f, 0.1f)
    }
}
