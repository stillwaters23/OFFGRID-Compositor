package com.offgrid.compositor.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Changes the pitch and keeps the length. Two read heads sweep a short delay line and fade
 * into each other, so the voice stays in time. The delay is about [windowSize] / 2 samples.
 */
class PitchShifter(private val windowSize: Int) {
    private val line = FloatArray(windowSize * 2 + 2)
    private val fade = FloatArray(windowSize) { sin(PI * it / windowSize).let { s -> (s * s).toFloat() } }
    private var write = 0
    private var phase = 0f

    /** 0.5 is an octave down and 2.0 is an octave up. */
    fun process(block: FloatArray, count: Int, ratio: Float) {
        val step = 1f - ratio
        val size = line.size
        for (i in 0 until count) {
            line[write] = block[i]
            val first = phase
            var second = phase + windowSize / 2f
            if (second >= windowSize) second -= windowSize
            block[i] = tap(first) * fade[first.toInt()] + tap(second) * fade[second.toInt()]
            write = (write + 1) % size
            phase += step
            if (phase >= windowSize) phase -= windowSize
            if (phase < 0f) phase += windowSize
        }
    }

    private fun tap(delay: Float): Float {
        val position = write - (delay + MIN_DELAY)
        val base = kotlin.math.floor(position).toInt()
        val mix = position - base
        val size = line.size
        val a = line[Math.floorMod(base, size)]
        val b = line[Math.floorMod(base + 1, size)]
        return a + (b - a) * mix
    }

    fun reset() {
        line.fill(0f)
        phase = 0f
    }

    private companion object {
        const val MIN_DELAY = 2f
    }
}
