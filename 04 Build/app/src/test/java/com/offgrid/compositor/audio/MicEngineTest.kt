package com.offgrid.compositor.audio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MicEngineTest {
    @Test
    fun readErrorReleasesCaptureOnceEvenWhenStopFollows() {
        val readStarted = CountDownLatch(1)
        val released = CountDownLatch(1)
        val releaseCount = AtomicInteger()
        val capture = object : MicCapture {
            override val initialized = true
            override fun start() = Unit
            override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
                readStarted.countDown()
                return -1
            }

            override fun release() {
                releaseCount.incrementAndGet()
                released.countDown()
            }
        }
        val engine = MicEngine { capture }

        assertTrue(engine.start())
        assertTrue(readStarted.await(1, TimeUnit.SECONDS))
        assertTrue(released.await(1, TimeUnit.SECONDS))
        engine.stop()

        assertFalse(engine.isRunning)
        assertEquals(1, releaseCount.get())
    }

    @Test
    fun stopWithoutAStartDoesNotRelease() {
        val releaseCount = AtomicInteger()
        val engine = MicEngine {
            object : MicCapture {
                override val initialized = true
                override fun start() = Unit
                override fun read(buffer: ShortArray, offset: Int, size: Int) = 0
                override fun release() {
                    releaseCount.incrementAndGet()
                }
            }
        }
        engine.stop()
        assertEquals(0, releaseCount.get())
    }
}
