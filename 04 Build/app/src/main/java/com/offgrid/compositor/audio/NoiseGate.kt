package com.offgrid.compositor.audio

import kotlin.math.abs

/** Silences the mic while it is quieter than [level], with a short fade so it never clicks. */
class NoiseGate(private val sampleRate: Int) {
    private var envelope = 0f
    private var open = 1f
    private var holdLeft = 0

    fun process(block: FloatArray, count: Int, level: Float) {
        if (level <= 0f) {
            open = 1f
            return
        }
        val release = coefficient(RELEASE_SECONDS)
        val fade = coefficient(FADE_SECONDS)
        val hold = (HOLD_SECONDS * sampleRate).toInt()
        for (i in 0 until count) {
            val magnitude = abs(block[i])
            envelope = if (magnitude > envelope) magnitude else envelope * release
            if (envelope >= level) holdLeft = hold else if (holdLeft > 0) holdLeft--
            val target = if (holdLeft > 0) 1f else 0f
            open += (1f - fade) * (target - open)
            block[i] *= open
        }
    }

    fun reset() {
        envelope = 0f
        open = 1f
        holdLeft = 0
    }

    private fun coefficient(seconds: Float): Float = kotlin.math.exp(-1f / (seconds * sampleRate))

    private companion object {
        const val RELEASE_SECONDS = 0.05f
        const val FADE_SECONDS = 0.004f
        const val HOLD_SECONDS = 0.12f
    }
}
