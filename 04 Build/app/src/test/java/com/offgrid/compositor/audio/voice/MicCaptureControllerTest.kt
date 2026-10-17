package com.offgrid.compositor.audio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicCaptureControllerTest {
    @Test
    fun offAndBlockedReleaseCaptureAndBlockedCannotReopenUntilUnblocked() {
        val source = TestMicSource()
        val controller = MicCaptureController(source, VoiceSwapDsp(), VoiceAudioOutput { _, _ -> })

        assertTrue(controller.setEnabled(true))
        assertEquals(MicState.ON, controller.state)
        controller.setBlocked(true)
        assertEquals(MicState.BLOCKED, controller.state)
        assertEquals(1, source.stops)
        assertFalse(controller.setEnabled(true))
        assertEquals(1, source.starts)

        controller.setBlocked(false)
        assertEquals(MicState.OFF, controller.state)
        assertTrue(controller.setEnabled(true))
        assertEquals(2, source.starts)
        controller.setEnabled(false)
        assertEquals(MicState.OFF, controller.state)
        assertEquals(3, source.stops)
    }

    @Test
    fun processingStallMutesAndReleasesInsteadOfPublishingRawSamples() {
        val source = TestMicSource()
        val written = mutableListOf<Float>()
        var clockCalls = 0
        val controller = MicCaptureController(
            source = source,
            dsp = VoiceSwapDsp(),
            output = VoiceAudioOutput { samples, count -> written += samples.take(count) },
            clockNanos = { if (clockCalls++ == 0) 0L else 81_000_001L },
        )
        assertTrue(controller.setEnabled(true))

        source.send(floatArrayOf(0.25f, -0.25f), 2)

        assertTrue(written.isEmpty())
        assertEquals(MicState.OFF, controller.state)
        assertEquals(1, source.stops)
    }

    @Test
    fun sourceFailureMovesToOffAndReleases() {
        val source = TestMicSource()
        val controller = MicCaptureController(source, VoiceSwapDsp(), VoiceAudioOutput { _, _ -> })
        assertTrue(controller.setEnabled(true))

        source.fail()

        assertEquals(MicState.OFF, controller.state)
        assertEquals(1, source.stops)
    }

    @Test
    fun invalidFrameAndUiCallbackFailureStillReleaseCapture() {
        val source = TestMicSource()
        val controller = MicCaptureController(
            source = source,
            dsp = VoiceSwapDsp(),
            output = VoiceAudioOutput { _, _ -> },
            onState = { if (it == MicState.BLOCKED) error("view unavailable") },
        )
        assertTrue(controller.setEnabled(true))

        source.send(floatArrayOf(0.1f), 2)
        assertEquals(MicState.OFF, controller.state)
        assertEquals(1, source.stops)

        assertTrue(controller.setEnabled(true))
        controller.setBlocked(true)
        assertEquals(MicState.BLOCKED, controller.state)
        assertEquals(2, source.stops)
    }

    private class TestMicSource : MicSource {
        var starts = 0
        var stops = 0
        private var samples: ((FloatArray, Int) -> Unit)? = null
        private var failure: (() -> Unit)? = null

        override fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean {
            starts++
            samples = onSamples
            failure = onFailure
            return true
        }

        override fun stop() {
            stops++
        }

        fun send(data: FloatArray, count: Int) = samples?.invoke(data, count)
        fun fail() = failure?.invoke()
    }
}
