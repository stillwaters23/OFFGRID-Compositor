package com.offgrid.compositor.audio.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidMicSourceBlockTest {
    @Test
    fun hardBlockRefusesCaptureEvenWhenPermissionWouldAllowIt() {
        val source = AndroidMicSource(RuntimeEnvironment.getApplication())
        assertTrue(source.hardBlock.canOpen(permissionGranted = true))
        assertFalse(source.hardBlock.isOpen())

        source.setHardBlock(true)

        assertTrue(source.hardBlock.isBlocked())
        assertFalse(source.hardBlock.isOpen())
        assertFalse(source.hardBlock.canOpen(permissionGranted = true))
        var delivered = false
        assertFalse(source.start({ _, _ -> delivered = true }, { delivered = true }))
        assertFalse(delivered)
        assertFalse(source.hardBlock.isOpen())
    }
}
