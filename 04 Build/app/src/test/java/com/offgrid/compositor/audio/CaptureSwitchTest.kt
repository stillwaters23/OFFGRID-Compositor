package com.offgrid.compositor.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSwitchTest {
    private val switch = CaptureSwitch()

    @Test
    fun startsWithNothingRunningAndBindsTheCameraFirst() {
        assertEquals(listOf(CaptureAction.BIND_CAMERA), switch.update(cameraWanted = true, micWanted = false, micAllowed = false))
        assertTrue(switch.update(cameraWanted = true, micWanted = false, micAllowed = false).isEmpty())
    }

    @Test
    fun cameraOffUnbindsAndCameraOnBindsAgain() {
        switch.update(true, false, false)
        assertEquals(listOf(CaptureAction.UNBIND_CAMERA), switch.update(false, false, false))
        assertTrue(switch.update(false, false, false).isEmpty())
        assertEquals(listOf(CaptureAction.BIND_CAMERA), switch.update(true, false, false))
    }

    @Test
    fun micStartsAndIsReleasedOnEachToggle() {
        switch.update(true, false, true)
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
        assertEquals(listOf(CaptureAction.RELEASE_MIC), switch.update(true, false, true))
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }

    @Test
    fun micStaysOffWithoutPermission() {
        assertEquals(listOf(CaptureAction.BIND_CAMERA), switch.update(true, true, false))
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }

    @Test
    fun cameraAndMicAreReleasedWhileTheAppIsHiddenAndComeBack() {
        switch.update(true, true, true)
        assertEquals(
            listOf(CaptureAction.UNBIND_CAMERA, CaptureAction.RELEASE_MIC),
            switch.update(true, true, true, foreground = false),
        )
        assertEquals(
            listOf(CaptureAction.BIND_CAMERA, CaptureAction.START_MIC),
            switch.update(true, true, true, foreground = true),
        )
    }

    @Test
    fun bothTogglesCanChangeTogether() {
        switch.update(true, false, true)
        val actions = switch.update(false, true, true)
        assertEquals(listOf(CaptureAction.UNBIND_CAMERA, CaptureAction.START_MIC), actions)
    }

    @Test
    fun aMicThatFailedToStartCanTryAgain() {
        switch.update(true, true, true)
        switch.micFailed()
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }

    @Test
    fun deniedMicStaysOffUntilTheToggleIsTurnedOffAndOnAgain() {
        switch.update(true, true, true)
        switch.micPermissionDenied()
        assertEquals(listOf(CaptureAction.RELEASE_MIC), switch.update(true, true, true))
        assertTrue(switch.update(true, true, true).isEmpty())
        assertTrue(switch.update(true, false, true).isEmpty())
        assertEquals(listOf(CaptureAction.START_MIC), switch.update(true, true, true))
    }
}
