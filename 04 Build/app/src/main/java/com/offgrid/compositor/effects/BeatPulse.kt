package com.offgrid.compositor.effects

import kotlin.math.exp

/**
 * Turns mic beats into a short glow pulse, from 1 down to 0. Beats come as a running count.
 * Pulses start at most [MAX_PULSES_PER_SECOND] times a second, so the glow never flashes faster than 3 times a second.
 * Call from one thread.
 */
class BeatPulse {
    private var seen = 0L
    private var startedAt = -NEVER

    @Synchronized
    fun level(now: Float, beatCount: Long): Float {
        if (beatCount > seen) {
            seen = beatCount
            if (now - startedAt >= 1f / MAX_PULSES_PER_SECOND) startedAt = now
        }
        val age = now - startedAt
        return if (age < 0f) 0f else exp(-age / DECAY_SECONDS)
    }

    companion object {
        const val MAX_PULSES_PER_SECOND = 3
        private const val DECAY_SECONDS = 0.12f
        private const val NEVER = 1000f
    }
}
