package com.offgrid.compositor.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSwitchTest {
    private val switch = CaptureSwitch()

    @Test
    fun startsWithTheCameraOnAndNoChanges() {
        assertTrue(switch.update(cameraWanted = true, micWanted = false, micAllowed = false).isEmpty())
    }

    @Test
    fun cameraOffUnbindsAndCameraOnBindsAgain() {
        assertEquals(listOf(CaptureAction.UNBIND_CAMERA), switch.update(false, false, false))
        assertTrue(switch.update(false, false, false).isEmpty())
        assertEquals(listOf(CaptureAction.BIND_CAMERA), switch.update(true, false, false))
    }

    @Test
    fun micStartsAndIsReleasedOnEachToggle() {
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
        assertEquals(listOf(CaptureAction.RELEASE_MIC), switch.update(true, false, true))
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }

    @Test
    fun micStaysOffWithoutPermission() {
        assertTrue(switch.update(true, true, false).isEmpty())
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }

    @Test
    fun micIsReleasedWhileTheAppIsHiddenAndComesBack() {
        switch.update(true, true, true)
        assertEquals(listOf(CaptureAction.RELEASE_MIC), switch.update(true, true, true, foreground = false))
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true, foreground = true))
    }

    @Test
    fun bothTogglesCanChangeTogether() {
        val actions = switch.update(false, true, true)
        assertEquals(listOf(CaptureAction.UNBIND_CAMERA, CaptureAction.START_MIC), actions)
    }

    @Test
    fun aMicThatFailedToStartCanTryAgain() {
        switch.update(true, true, true)
        switch.micFailed()
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }
}
