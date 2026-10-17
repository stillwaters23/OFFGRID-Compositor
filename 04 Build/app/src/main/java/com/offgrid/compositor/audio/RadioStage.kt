package com.offgrid.compositor.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** The radio voice: only 300 Hz to 3.4 kHz gets through, then a soft clip. */
class RadioStage(sampleRate: Int) {
    private val highPass = Biquad.highPass(sampleRate, LOW_HZ)
    private val lowPass = Biquad.lowPass(sampleRate, HIGH_HZ)

    fun process(block: FloatArray, count: Int) {
        for (i in 0 until count) {
            val filtered = lowPass.next(highPass.next(block[i]))
            block[i] = tanh(filtered * DRIVE) / tanh(DRIVE)
        }
    }

    fun reset() {
        highPass.reset()
        lowPass.reset()
    }

    private companion object {
        const val LOW_HZ = 300f
        const val HIGH_HZ = 3400f
        const val DRIVE = 2f
    }
}

/** One second-order filter section (Butterworth shape). */
class Biquad private constructor(
    private val b0: Float,
    private val b1: Float,
    private val b2: Float,
    private val a1: Float,
    private val a2: Float,
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun next(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f
        x2 = 0f
        y1 = 0f
        y2 = 0f
    }

    companion object {
        private const val Q = 0.7071f

        fun lowPass(sampleRate: Int, hz: Float): Biquad = build(sampleRate, hz) { c, alpha ->
            val half = (1f - c) / 2f
            floatArrayOf(half, 1f - c, half, 1f + alpha, -2f * c, 1f - alpha)
        }

        fun highPass(sampleRate: Int, hz: Float): Biquad = build(sampleRate, hz) { c, alpha ->
            val half = (1f + c) / 2f
            floatArrayOf(half, -(1f + c), half, 1f + alpha, -2f * c, 1f - alpha)
        }

        private inline fun build(sampleRate: Int, hz: Float, shape: (Float, Float) -> FloatArray): Biquad {
            val w = 2.0 * PI * hz / sampleRate
            val c = cos(w).toFloat()
            val alpha = (sin(w) / (2.0 * Q)).toFloat()
            val k = shape(c, alpha)
            return Biquad(k[0] / k[3], k[1] / k[3], k[2] / k[3], k[4] / k[3], k[5] / k[3])
        }
    }
}
