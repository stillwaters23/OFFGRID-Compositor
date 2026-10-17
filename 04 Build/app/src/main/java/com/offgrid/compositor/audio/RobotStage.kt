package com.offgrid.compositor.audio

import kotlin.math.PI
import kotlin.math.round
import kotlin.math.sin

/** The robot voice: a 30 to 120 Hz ring modulator and a light bit crush. */
class RobotStage(private val sampleRate: Int) {
    private var angle = 0.0

    fun process(block: FloatArray, count: Int, amount: Float) {
        val frequency = MIN_HZ + amount * (MAX_HZ - MIN_HZ)
        val step = 2.0 * PI * frequency / sampleRate
        val levels = (1 shl (CRUSH_BITS_START - (amount * CRUSH_BITS_DROP).toInt() - 1)).toFloat()
        for (i in 0 until count) {
            val carrier = sin(angle).toFloat()
            val ringed = block[i] * ((1f - amount) + amount * carrier)
            block[i] = round(ringed * levels) / levels
            angle += step
            if (angle > 2.0 * PI) angle -= 2.0 * PI
        }
    }

    fun reset() {
        angle = 0.0
    }

    private companion object {
        const val MIN_HZ = 30f
        const val MAX_HZ = 120f
        const val CRUSH_BITS_START = 12
        const val CRUSH_BITS_DROP = 4
    }
}
