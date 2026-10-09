package com.offgrid.compositor.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatPulseTest {
    @Test
    fun startsDark() {
        assertEquals(0f, BeatPulse().level(1f, 0), 1e-3f)
    }

    @Test
    fun aBeatFlashesThenFades() {
        val pulse = BeatPulse()
        assertEquals(1f, pulse.level(2f, 1), 1e-4f)
        assertTrue(pulse.level(2.1f, 1) < 0.5f)
        assertTrue(pulse.level(3f, 1) < 0.01f)
    }

    @Test
    fun neverStartsMoreThanThreePulsesASecond() {
        val pulse = BeatPulse()
        var starts = 0
        var beats = 0L
        var previous = 0f
        var time = 10f
        while (time < 12f) {
            beats++
            val level = pulse.level(time, beats)
            if (level > 0.99f && previous <= 0.99f) starts++
            previous = level
            time += 0.05f
        }
        assertTrue("starts $starts", starts <= 6)
    }
}
