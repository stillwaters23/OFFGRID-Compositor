package com.offgrid.compositor.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class MicRecordReleaseTest {
    @Test
    fun readFailureAndLaterStopReleaseTheRecorderExactlyOnce() {
        var releases = 0
        val recorder = MicRecordRelease { releases++ }

        recorder.readFailed()
        recorder.stop()

        assertEquals(1, releases)
    }
}
