package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StampClockTest {
    private val ms = 1_000_000L

    @Test
    fun cameraStampsPassThrough() {
        val clock = StampClock()
        assertEquals(5_000 * ms, clock.next(StampClock.Source.CAMERA, 5_000 * ms))
        assertEquals(5_033 * ms, clock.next(StampClock.Source.CAMERA, 5_033 * ms))
    }

    @Test
    fun switchingSourceCarriesOnOneFrameLater() {
        val clock = StampClock()
        clock.next(StampClock.Source.CAMERA, 5_000 * ms)
        val black = clock.next(StampClock.Source.BLACK, 900_000 * ms)
        assertEquals(5_000 * ms + 33_333_333L, black)
        val back = clock.next(StampClock.Source.CAMERA, 7_000 * ms)
        assertEquals(black + 33_333_333L, back)
    }

    @Test
    fun timeNeverGoesBackwards() {
        val clock = StampClock()
        val first = clock.next(StampClock.Source.CAMERA, 100 * ms)
        assertTrue(clock.next(StampClock.Source.CAMERA, 90 * ms) > first)
    }
}
