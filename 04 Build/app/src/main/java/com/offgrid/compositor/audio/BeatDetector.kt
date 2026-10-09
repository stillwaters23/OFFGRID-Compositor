package com.offgrid.compositor.audio

import kotlin.math.sqrt

/**
 * Follows how loud the mic is and calls out beats. [level] is 0 to 1. A beat is a sudden rise
 * above the recent average, and never more than [MAX_BEATS_PER_SECOND] a second.
 */
class BeatDetector(private val sampleRate: Int = VoiceFx.SAMPLE_RATE) : PcmSink {
    @Volatile
    var level = 0f
        private set

    /** The time in nanoseconds of the latest beat, or 0 when none. */
    @Volatile
    var lastBeatNanos = 0L
        private set

    /** How many beats were heard since the start. */
    @Volatile
    var beats = 0L
        private set

    private var average = 0f
    private var framesSinceBeat = Int.MAX_VALUE / 2

    override fun onPcm(block: ShortArray, frames: Int) {
        if (frames <= 0) return
        var sum = 0.0
        for (i in 0 until frames) {
            val v = block[i] / SCALE
            sum += v * v
        }
        val rms = sqrt(sum / frames).toFloat()
        level = (rms * LEVEL_GAIN).coerceIn(0f, 1f)
        framesSinceBeat = minOf(framesSinceBeat + frames, Int.MAX_VALUE / 2)
        val spacing = sampleRate / MAX_BEATS_PER_SECOND
        if (rms > MIN_RMS && rms > average * RISE && framesSinceBeat >= spacing) {
            framesSinceBeat = 0
            lastBeatNanos = System.nanoTime()
            beats++
        }
        average += (rms - average) * (frames.toFloat() / sampleRate / AVERAGE_SECONDS).coerceAtMost(1f)
    }

    fun reset() {
        level = 0f
        lastBeatNanos = 0L
        beats = 0L
        average = 0f
        framesSinceBeat = Int.MAX_VALUE / 2
    }

    companion object {
        const val MAX_BEATS_PER_SECOND = 8
        private const val SCALE = 32768f
        private const val LEVEL_GAIN = 4f
        private const val MIN_RMS = 0.02f
        private const val RISE = 1.6f
        private const val AVERAGE_SECONDS = 0.5f
    }
}
