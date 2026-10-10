package com.offgrid.compositor.audio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MicHardBlockTest {
    @Test
    fun hardBlockClosesCaptureAndRefusesPermissionUntilCleared() {
        val gate = MicHardBlock()
        assertTrue(gate.canOpen(permissionGranted = true))
        assertTrue(gate.markOpen())
        assertTrue(gate.isOpen())
        assertTrue(gate.mayDeliver())

        gate.setBlocked(true)

        assertTrue(gate.isBlocked())
        assertFalse(gate.isOpen())
        assertFalse(gate.mayDeliver())
        assertFalse(gate.canOpen(permissionGranted = true))
        assertFalse(gate.markOpen())

        gate.setBlocked(false)
        assertFalse(gate.isBlocked())
        assertFalse(gate.isOpen())
        assertTrue(gate.canOpen(permissionGranted = true))
        assertFalse(gate.canOpen(permissionGranted = false))
    }

    @Test
    fun controllerHardBlockStopsTheSourceAndDropsLaterSamples() {
        val source = LiveSource()
        val written = mutableListOf<Float>()
        val controller = MicCaptureController(
            source = source,
            dsp = VoiceSwapDsp(),
            output = VoiceAudioOutput { samples, count -> written += samples.take(count) },
        )

        assertTrue(controller.setEnabled(true))
        assertTrue(source.capturing)
        source.push(floatArrayOf(0.2f, -0.2f))
        assertEquals(2, written.size)

        controller.setBlocked(true)

        assertEquals(MicState.BLOCKED, controller.state)
        assertFalse(source.capturing)
        assertTrue(source.blocked)
        assertTrue(source.stops >= 1)
        source.push(floatArrayOf(0.9f, 0.9f))
        assertEquals(2, written.size)
        assertFalse(controller.setEnabled(true))
        assertEquals(1, source.starts)
        assertFalse(source.capturing)

        controller.setBlocked(false)
        assertEquals(MicState.OFF, controller.state)
        assertFalse(source.blocked)
        assertFalse(source.capturing)
        assertTrue(controller.setEnabled(true))
        assertTrue(source.capturing)
    }

    @Test(timeout = 3000)
    fun hardBlockReleasesTheControllerLockBeforeStoppingCapture() {
        val controllerRef = AtomicReference<MicCaptureController>()
        val source = object : MicSource {
            override fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean = true

            override fun setHardBlock(blocked: Boolean) {
                if (!blocked) return
                val entered = CountDownLatch(1)
                Thread {
                    controllerRef.get().setEnabled(true)
                    entered.countDown()
                }.start()
                if (!entered.await(2, TimeUnit.SECONDS)) error("hard block held the controller lock")
            }
        }
        val controller = MicCaptureController(source, VoiceSwapDsp(), VoiceAudioOutput { _, _ -> })
        controllerRef.set(controller)
        assertTrue(controller.setEnabled(true))

        controller.setBlocked(true)

        assertEquals(MicState.BLOCKED, controller.state)
        assertFalse(controller.setEnabled(true))
    }

    private class LiveSource : MicSource {
        var starts = 0
        var stops = 0
        var blocked = false
        var capturing = false
        private var onSamples: ((FloatArray, Int) -> Unit)? = null

        override fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean {
            if (blocked) return false
            starts++
            capturing = true
            this.onSamples = onSamples
            return true
        }

        override fun stop() {
            stops++
            capturing = false
        }

        override fun setHardBlock(blocked: Boolean) {
            this.blocked = blocked
            if (blocked) stop()
        }

        fun push(data: FloatArray) {
            onSamples?.invoke(data, data.size)
        }
    }
}
