package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun coldStartWhileTheShareIsVisibleAsksOnceAfterTheLockLifts() {
        val log = CameraAskLog()
        log.lockNotify(granted = false, locked = true, resumed = false)
        log.resume(granted = false, locked = true)
        assertEquals(0, log.asks)
        assertEquals(0, log.helps)
        log.lockNotify(granted = false, locked = false, resumed = true)
        assertEquals(1, log.asks)
        repeat(5) { log.lockNotify(granted = false, locked = false, resumed = true) }
        assertEquals("store notifies must not ask again", 1, log.asks)
        assertEquals(0, log.helps)
    }

    @Test
    fun resumeAfterBackgroundingWhileLockedDoesNotAskOrShowHelp() {
        val waiting = CameraAskLog()
        waiting.lockNotify(granted = false, locked = true, resumed = false)
        waiting.resume(granted = false, locked = true)
        repeat(4) { waiting.lockNotify(granted = false, locked = true, resumed = true) }
        assertEquals(0, waiting.asks)
        assertEquals(0, waiting.helps)
        waiting.lockNotify(granted = false, locked = false, resumed = true)
        assertEquals(1, waiting.asks)
        assertEquals(0, waiting.helps)

        val alreadyAsked = CameraAskLog()
        alreadyAsked.resume(granted = false, locked = false)
        assertEquals(1, alreadyAsked.asks)
        alreadyAsked.lockNotify(granted = false, locked = true, resumed = false)
        alreadyAsked.resume(granted = false, locked = true)
        repeat(6) { alreadyAsked.lockNotify(granted = false, locked = true, resumed = true) }
        alreadyAsked.lockNotify(granted = false, locked = false, resumed = true)
        assertEquals("unlock notifies do not show help", 0, alreadyAsked.helps)
        assertEquals(1, alreadyAsked.asks)
        alreadyAsked.resume(granted = false, locked = false)
        assertEquals("help is one resume while unlocked, not a notify stream", 1, alreadyAsked.helps)
        repeat(3) { alreadyAsked.lockNotify(granted = false, locked = false, resumed = true) }
        assertEquals(1, alreadyAsked.helps)
    }

    @Test
    fun aFreshInstallWhileUnlockedAsksOnce() {
        val log = CameraAskLog()
        log.lockNotify(granted = false, locked = false, resumed = false)
        log.resume(granted = false, locked = false)
        assertEquals(1, log.asks)
        repeat(8) { log.lockNotify(granted = false, locked = false, resumed = true) }
        assertEquals(1, log.asks)
        assertEquals(0, log.helps)
    }

    @Test
    fun cameraPermissionAlreadyGrantedNeverAsks() {
        val log = CameraAskLog()
        log.resume(granted = true, locked = false)
        log.lockNotify(granted = true, locked = false, resumed = true)
        log.lockNotify(granted = true, locked = true, resumed = true)
        log.resume(granted = true, locked = true)
        assertEquals(0, log.asks)
        assertEquals(0, log.helps)
    }

    @Test
    fun everyLockSourceHoldsTheCameraPrompt() {
        for (state in sources) {
            assertNull(
                "$state",
                LockGuards.cameraPrompt(
                    granted = false,
                    alreadyAsked = false,
                    locked = state.locked,
                    resumed = true,
                    fromLockNotify = false,
                ),
            )
        }
    }

    /**
     * The same decisions [com.offgrid.compositor.MainActivity] makes: one ask, no help from a
     * lock notification, and nothing while locked or before resume.
     */
    private class CameraAskLog {
        var asked = false
        var asks = 0
        var helps = 0

        fun resume(granted: Boolean, locked: Boolean) = act(granted, locked, resumed = true, fromLockNotify = false)

        fun lockNotify(granted: Boolean, locked: Boolean, resumed: Boolean) =
            act(granted, locked, resumed, fromLockNotify = true)

        private fun act(granted: Boolean, locked: Boolean, resumed: Boolean, fromLockNotify: Boolean) {
            when (LockGuards.cameraPrompt(granted, asked, locked, resumed, fromLockNotify)) {
                CameraPrompt.ASK_ONCE -> {
                    asked = true
                    asks += 1
                }
                CameraPrompt.SHOW_HELP -> helps += 1
                null -> Unit
            }
        }
    }
}
