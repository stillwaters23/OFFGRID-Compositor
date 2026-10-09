package com.offgrid.compositor.effects

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The glass pieces for Shatter (feature 36), as plain math (final build, M4).
 *
 * The picture is cut into triangle pieces on an uneven grid. Cracks spread out from the
 * impact spot, the pieces fall and tumble away, stay gone for a beat, then fly back the way
 * they fell and fit together again. Places are shares of the picture from 0 to 1, left to
 * right and top to bottom.
 */
class ShatterMesh {
    /** The number of triangle pieces. */
    var count = 0
        private set

    /** Each piece's three corners where they belong: x0, y0, x1, y1, x2, y2. */
    var home = FloatArray(0)
        private set

    /** Each piece's three corners right now, after falling and turning. */
    var placed = FloatArray(0)
        private set

    /** How bright each piece is right now, 0 to 1 (pieces darken as they turn away). */
    var shade = FloatArray(0)
        private set

    /** True for pieces that are in view right now. */
    var shown = BooleanArray(0)
        private set

    /** How much of the crack lines show right now, 0 to 1, by distance from the impact. */
    var crackReach = 0f
        private set

    /** How bright the crack lines are right now, 0 to 1. */
    var crackGlow = 0f
        private set

    private var middleX = FloatArray(0)
    private var middleY = FloatArray(0)
    private var distance = FloatArray(0)
    private var spin = FloatArray(0)
    private var drift = FloatArray(0)
    private var builtPieces = -1
    private var builtX = 0f
    private var builtY = 0f
    private var builtAspect = 0f
    private var builtSeed = 0L

    /**
     * Cuts the picture for [pieces] pieces from the impact at [impactX], [impactY], for a
     * picture [aspect] wide for each 1 tall. [seed] keeps one firing's pieces the same.
     */
    fun build(pieces: Int, impactX: Float, impactY: Float, aspect: Float, seed: Long) {
        if (pieces == builtPieces && impactX == builtX && impactY == builtY &&
            aspect == builtAspect && seed == builtSeed
        ) {
            return
        }
        builtPieces = pieces
        builtX = impactX
        builtY = impactY
        builtAspect = aspect
        builtSeed = seed
        val cells = max(MIN_CELLS, ceil(pieces / 2f).toInt())
        val cols = max(2, sqrt(cells * aspect).roundToInt())
        val rows = max(2, ceil(cells / cols.toFloat()).toInt())
        val salt = (seed % 100_000L).toInt()
        // Corner points, jittered so no piece is a neat square. Edge points stay on the edge.
        val px = FloatArray((cols + 1) * (rows + 1))
        val py = FloatArray(px.size)
        for (r in 0..rows) {
            for (c in 0..cols) {
                val i = r * (cols + 1) + c
                var x = c / cols.toFloat()
                var y = r / rows.toFloat()
                if (c in 1 until cols) x += (EffectMath.hash(i, salt) - 0.5f) * JITTER / cols
                if (r in 1 until rows) y += (EffectMath.hash(i, salt + 7) - 0.5f) * JITTER / rows
                px[i] = x
                py[i] = y
            }
        }
        count = cols * rows * 2
        home = FloatArray(count * 6)
        placed = FloatArray(count * 6)
        shade = FloatArray(count)
        shown = BooleanArray(count)
        middleX = FloatArray(count)
        middleY = FloatArray(count)
        distance = FloatArray(count)
        spin = FloatArray(count)
        drift = FloatArray(count)
        var piece = 0
        var farthest = 0.01f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val a = r * (cols + 1) + c
                val b = a + 1
                val d = a + cols + 1
                val e = d + 1
                val split = EffectMath.hash(a, salt + 3) < 0.5f
                val first = if (split) intArrayOf(a, b, e) else intArrayOf(a, b, d)
                val second = if (split) intArrayOf(a, e, d) else intArrayOf(b, e, d)
                for (corners in arrayOf(first, second)) {
                    var sx = 0f
                    var sy = 0f
                    for (k in 0..2) {
                        home[piece * 6 + k * 2] = px[corners[k]]
                        home[piece * 6 + k * 2 + 1] = py[corners[k]]
                        sx += px[corners[k]]
                        sy += py[corners[k]]
                    }
                    middleX[piece] = sx / 3f
                    middleY[piece] = sy / 3f
                    val dist = hypot((middleX[piece] - impactX) * aspect, middleY[piece] - impactY)
                    distance[piece] = dist
                    farthest = max(farthest, dist)
                    spin[piece] = (EffectMath.hash(piece, salt + 11) - 0.5f) * 2f
                    val away = if (middleX[piece] >= impactX) 1f else -1f
                    drift[piece] = away * (0.05f + 0.25f * EffectMath.hash(piece, salt + 13))
                    piece++
                }
            }
        }
        for (i in 0 until count) distance[i] /= farthest
    }

    /** Moves every piece to where it is [since] seconds after the shatter fired. */
    fun pose(since: Float, times: EffectTimes.Shatter) {
        val fallStart = times.crack
        val backStart = times.crack + times.fall + times.broken
        crackReach = EffectMath.smooth(since / times.crack)
        crackGlow = when {
            since < backStart -> 1f
            else -> 1f - EffectMath.smooth((since - backStart) / times.back)
        }
        for (i in 0 until count) {
            // Each piece falls a little after the ones nearer the impact.
            val delay = distance[i] * FALL_DELAY_SHARE * times.fall
            val fallLength = times.fall * (1f - FALL_DELAY_SHARE)
            val fallen = EffectMath.clamp01((since - fallStart - delay) / fallLength)
            val travel = if (since < backStart) {
                fallen
            } else {
                // Coming back runs the fall backwards, the farthest pieces last.
                val backDelay = (1f - distance[i]) * BACK_DELAY_SHARE * times.back
                val back = EffectMath.easeOut((since - backStart - backDelay) / (times.back * (1f - BACK_DELAY_SHARE)))
                1f - back
            }
            place(i, travel)
        }
    }

    private fun place(i: Int, u: Float) {
        val base = i * 6
        if (u <= 0f) {
            for (k in 0..5) placed[base + k] = home[base + k]
            shade[i] = 1f
            shown[i] = true
            return
        }
        val fall = u * u * FALL_DISTANCE
        val dx = drift[i] * u
        val angle = spin[i] * u * MAX_TURN
        val turnCos = cos(angle)
        val turnSin = sin(angle)
        // A piece tipping over looks thinner. This fakes the tumble without real 3D.
        val tip = 1f - 0.45f * u * kotlin.math.abs(sin(angle * 1.7f))
        val shrink = 1f - SHRINK * u
        var lowest = Float.MAX_VALUE
        for (k in 0..2) {
            val ox = (home[base + k * 2] - middleX[i]) * tip * shrink
            val oy = (home[base + k * 2 + 1] - middleY[i]) * shrink
            val x = middleX[i] + dx + ox * turnCos - oy * turnSin
            val y = middleY[i] + fall + ox * turnSin + oy * turnCos
            placed[base + k * 2] = x
            placed[base + k * 2 + 1] = y
            lowest = minOf(lowest, y)
        }
        shade[i] = 1f - 0.35f * u * (0.5f + 0.5f * kotlin.math.abs(turnSin))
        shown[i] = lowest < 1.05f
    }

    /** How far each piece is from the impact, 0 nearest to 1 farthest. Crack lines use it. */
    fun distanceOf(i: Int): Float = distance[i]

    companion object {
        const val MIN_CELLS = 4
        const val JITTER = 0.7f
        const val FALL_DELAY_SHARE = 0.35f
        const val BACK_DELAY_SHARE = 0.3f
        const val FALL_DISTANCE = 1.6f
        const val MAX_TURN = 2.4f
        const val SHRINK = 0.18f
    }
}
