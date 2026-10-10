package com.offgrid.compositor.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatDetectorTest {
    private val quiet = ShortArray(480) { 100 }
    private val loud = ShortArray(480) { if (it % 2 == 0) 16000 else -16000 }

    private fun feed(detector: BeatDetector, burstsPerSecond: Int, seconds: Int) {
        val blocksPerBurst = 100 / burstsPerSecond
        repeat(seconds * 100) { block ->
            detector.onPcm(if (block % blocksPerBurst == 0) loud else quiet, 480)
        }
    }

    @Test
    fun steadyQuietSoundHasNoBeats() {
        val detector = BeatDetector()
        repeat(300) { detector.onPcm(quiet, 480) }
        assertEquals(0L, detector.beats)
    }

    @Test
    fun findsEachBurstOfASteadyBeat() {
        val detector = BeatDetector()
        feed(detector, burstsPerSecond = 4, seconds = 4)
        assertTrue("beats ${detector.beats}", detector.beats in 14..16)
    }

    @Test
    fun neverReportsMoreThanEightBeatsASecond() {
        val detector = BeatDetector()
        feed(detector, burstsPerSecond = 20, seconds = 5)
        assertTrue("beats ${detector.beats}", detector.beats <= 5 * BeatDetector.MAX_BEATS_PER_SECOND)
    }

    @Test
    fun levelFollowsTheLoudness() {
        val detector = BeatDetector()
        detector.onPcm(loud, 480)
        assertEquals(1f, detector.level, 0f)
        detector.onPcm(ShortArray(480), 480)
        assertEquals(0f, detector.level, 0f)
    }
}
