package com.offgrid.compositor.ui.wheel

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The Auric drum's motion, stepped at 60 frames per second.
 * Each frame multiplies velocity by 0.94, then eases onto the nearest whole number.
 */
internal object WheelPhysics {
    const val FRAMES_PER_SECOND = 60
    const val FRAME_MS = 1000.0 / FRAMES_PER_SECOND
    const val FRICTION = 0.94
    const val STOP_SPEED = 0.004
    const val SNAP_EPSILON = 0.002
    const val SNAP_BLEND = 0.22
    const val PIXELS_PER_NOTCH = 28.0
    const val MIN_VALUE = 0
    const val MAX_VALUE = 100

    enum class Key { UP, DOWN, LEFT, RIGHT, PAGE_UP, PAGE_DOWN, HOME, END }

    data class Sample(
        val position: Double,
        val velocity: Double,
        val settled: Boolean,
    )

    fun snap(position: Double): Int =
        position.roundToInt().coerceIn(MIN_VALUE, MAX_VALUE)

    /** How many 60fps steps belong in this much time. Late frames catch up a little, then drop the rest. */
    fun frameCount(elapsedMs: Long, cap: Int = 4): Int =
        (elapsedMs / FRAME_MS).roundToInt().coerceIn(1, cap)

    fun frictionAfter(frames: Int): Double = FRICTION.pow(frames.coerceAtLeast(0))

    /**
     * One frame. The wheel follows the finger: a swipe up lowers the number.
     * [reducedMotion] lands on the nearest number in this same step.
     */
    fun step(position: Double, velocity: Double, reducedMotion: Boolean = false): Sample {
        if (reducedMotion) {
            return Sample(snap(position).toDouble(), 0.0, true)
        }
        var vel = velocity * FRICTION
        var pos = position + vel
        if (pos <= MIN_VALUE.toDouble() || pos >= MAX_VALUE.toDouble()) {
            pos = pos.coerceIn(MIN_VALUE.toDouble(), MAX_VALUE.toDouble())
            vel = 0.0
        }
        if (abs(vel) < STOP_SPEED) {
            val target = snap(pos).toDouble()
            val diff = target - pos
            if (abs(diff) < SNAP_EPSILON) {
                return Sample(target, 0.0, true)
            }
            pos = (pos + diff * SNAP_BLEND).coerceIn(MIN_VALUE.toDouble(), MAX_VALUE.toDouble())
            vel = 0.0
        }
        return Sample(pos, vel, false)
    }

    /** Drag distance for this event, and the velocity to keep when the finger lifts. */
    fun dragDelta(dyPixels: Double, dtMs: Double, pixelsPerNotch: Double = PIXELS_PER_NOTCH): Pair<Double, Double> {
        val notch = pixelsPerNotch.coerceAtLeast(1.0)
        val delta = dyPixels / notch
        val velocity = delta * (FRAME_MS / dtMs.coerceAtLeast(8.0))
        return delta to velocity
    }

    /** Pixel speed from a fling, turned into notches per 60fps frame. Swipe up lowers the number. */
    fun flingNotchesPerFrame(yVelocityPxPerSecond: Double, pixelsPerNotch: Double): Double {
        val notch = pixelsPerNotch.coerceAtLeast(1.0)
        return (yVelocityPxPerSecond / notch * (FRAME_MS / 1000.0)).coerceIn(-8.0, 8.0)
    }

    fun keyTarget(current: Int, key: Key): Int = when (key) {
        Key.UP, Key.RIGHT -> current + 1
        Key.DOWN, Key.LEFT -> current - 1
        Key.PAGE_UP -> current + 10
        Key.PAGE_DOWN -> current - 10
        Key.HOME -> MIN_VALUE
        Key.END -> MAX_VALUE
    }.coerceIn(MIN_VALUE, MAX_VALUE)
}
