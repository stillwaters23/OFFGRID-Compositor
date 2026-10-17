package com.offgrid.compositor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HR gate: Back and the system permission dialog may not reach a locked stage. */
class LockGuardsTest {
    private val sources = listOf(
        LockState(recordingVisible = true),
        LockState(manualLocked = true),
        LockState(remoteLocked = true),
        LockState(armed = true),
        LockState(resumeHold = true),
    )

    @Test
    fun backIsSwallowedForEveryLockSource() {
        for (state in sources) assertTrue("$state", LockGuards.swallowsBack(state))
    }

    @Test
    fun backIsFreeWhenUnlocked() {
        assertFalse(LockGuards.swallowsBack(LockState()))
    }

    @Test
    fun noPermissionDialogWhileLockedForAnySource() {
        for (state in sources) assertFalse("$state", LockGuards.mayShowPermissionDialog(state))
        assertTrue(LockGuards.mayShowPermissionDialog(LockState()))
    }

    @Test
    fun theShareEndingDoesNotUnlockWhileArmed() {
        val armedAfterShare = LockState(armed = true, recordingVisible = false)
        assertTrue(LockGuards.swallowsBack(armedAfterShare))
        assertFalse(LockGuards.mayShowPermissionDialog(armedAfterShare))
    }
}
