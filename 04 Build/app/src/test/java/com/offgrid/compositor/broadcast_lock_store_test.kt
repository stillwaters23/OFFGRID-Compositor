package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** V2 section 10, Broadcast Lock: every source combination, A4, lock_off scope, the resume gate. */
class BroadcastLockStoreTest {

    private fun store(persistence: MemoryLockPersistence = MemoryLockPersistence()) = BroadcastLockStore(persistence)

    @Test
    fun everyCombinationOfSourcesLocksWhenAnyIsSet() {
        for (mask in 0 until 16) {
            val auto = mask and 1 != 0
            val manual = mask and 2 != 0
            val remote = mask and 4 != 0
            val armed = mask and 8 != 0
            val state = LockState(recordingVisible = auto, manualLocked = manual, remoteLocked = remote, armed = armed)
            assertEquals("mask $mask", auto || manual || remote || armed, state.locked)
            val sources = state.sources
            assertEquals(auto, LockSource.AUTO in sources)
            assertEquals(manual, LockSource.MANUAL in sources)
            assertEquals(remote, LockSource.REMOTE in sources)
            assertEquals(armed, LockSource.ARMED in sources)
        }
    }

    @Test
    fun aFreshStoreIsUnlocked() {
        val lock = store()
        assertFalse(lock.locked)
        assertEquals("unlocked", lock.state.word)
    }

    @Test
    fun eachSourceLocksOnItsOwn() {
        for (source in LockSource.entries) {
            val lock = store()
            lock.lock(source)
            assertTrue("$source", lock.locked)
            assertEquals(setOf(source), lock.state.sources)
        }
    }

    @Test
    fun unlockIsRefusedWhileTheShareIsVisible() {
        val lock = store()
        lock.lock(LockSource.MANUAL)
        lock.lock(LockSource.REMOTE)
        lock.arm()
        lock.setRecordingVisible(true)
        assertEquals(UnlockResult.REFUSED_SHARE_VISIBLE, lock.unlock())
        assertTrue(lock.state.manualLocked)
        assertTrue(lock.state.remoteLocked)
        assertTrue(lock.state.armed)
        assertTrue(lock.locked)
        assertEquals("live", lock.state.word)
    }

    @Test
    fun autoLockHoldsWhileVisibleAndClearsItselfWhenNotVisible() {
        val lock = store()
        lock.setRecordingVisible(true)
        assertTrue(lock.locked)
        assertEquals(UnlockResult.REFUSED_SHARE_VISIBLE, lock.unlock())
        lock.setRecordingVisible(false)
        assertFalse(lock.locked)
        assertEquals(UnlockResult.NOTHING_TO_UNLOCK, lock.unlock())
    }

    @Test
    fun lockOffClearsManualRemoteAndArmedOnly() {
        val lock = store()
        lock.lock(LockSource.MANUAL)
        lock.lock(LockSource.REMOTE)
        lock.arm()
        assertEquals(UnlockResult.UNLOCKED, lock.unlock())
        assertFalse(lock.locked)
        assertEquals(emptySet<LockSource>(), lock.state.sources)

        lock.lock(LockSource.MANUAL)
        lock.setRecordingVisible(true)
        lock.setRecordingVisible(false)
        // AUTO came and went; the manual lock is still there until lock_off.
        assertTrue(lock.state.manualLocked)
        assertEquals(UnlockResult.UNLOCKED, lock.unlock())
        assertFalse(lock.locked)
    }

    @Test
    fun theShareEndingReturnsToArmedNotToUnlocked() {
        val lock = store()
        lock.arm()
        lock.setRecordingVisible(true)
        assertEquals("live", lock.state.word)
        lock.setRecordingVisible(false)
        assertTrue(lock.locked)
        assertEquals("armed", lock.state.word)
        assertEquals(setOf(LockSource.ARMED), lock.state.sources)
    }

    @Test
    fun manualRemoteAndArmedArePersistedAndComeBackOnAColdStart() {
        val persistence = MemoryLockPersistence()
        val first = store(persistence)
        first.lock(LockSource.MANUAL)
        first.arm()
        val second = store(persistence)
        assertTrue(second.state.manualLocked)
        assertTrue(second.state.armed)
        assertFalse(second.state.remoteLocked)
        assertFalse(second.state.recordingVisible)
        assertTrue(second.locked)
    }

    @Test
    fun autoIsNeverPersisted() {
        val persistence = MemoryLockPersistence()
        val first = store(persistence)
        first.setRecordingVisible(true)
        assertNull(persistence.values.keys.firstOrNull { it.contains("visible") })
        assertTrue(persistence.values.values.none { it })
        val second = store(persistence)
        assertFalse(second.state.recordingVisible)
        assertFalse(second.locked)
    }

    @Test
    fun resumeGateHoldsALockThatWasHeldWhenTheAppLeft() {
        val persistence = MemoryLockPersistence()
        val lock = store(persistence)
        lock.setRecordingVisible(true)
        lock.onStop()
        assertTrue(persistence.values[BroadcastLockStore.KEY_WAS_LOCKED] == true)

        // Back in front: the callback says NOT_VISIBLE for now because nothing has drawn yet.
        lock.setRecordingVisible(false)
        lock.onStart(initialVisible = false)
        assertTrue("the hold keeps the chrome away until the window passes", lock.locked)
        assertTrue(lock.state.resumeHold)
        assertEquals("locked", lock.state.word)

        // The callback flips to VISIBLE after the first draw: still locked, now by AUTO.
        lock.setRecordingVisible(true)
        lock.releaseResumeHold()
        assertTrue(lock.locked)
        assertEquals(UnlockResult.REFUSED_SHARE_VISIBLE, lock.unlock())
    }

    @Test
    fun resumeGateLetsGoWhenNothingWasLockedAndNothingIsVisible() {
        val persistence = MemoryLockPersistence()
        val lock = store(persistence)
        lock.onStop()
        lock.onStart(initialVisible = false)
        assertFalse(lock.locked)
        lock.releaseResumeHold()
        assertFalse(lock.locked)
    }

    @Test
    fun resumeGateReleasesTheHoldWhenTheWindowPassesQuietly() {
        val persistence = MemoryLockPersistence()
        val lock = store(persistence)
        lock.lock(LockSource.MANUAL)
        lock.onStop()
        lock.unlock()
        lock.onStart(initialVisible = false)
        assertTrue(lock.state.resumeHold)
        lock.releaseResumeHold()
        assertFalse(lock.locked)
    }

    @Test
    fun aColdStartWhileRecordedIsLockedBeforeAnythingDraws() {
        val lock = store()
        lock.onStart(initialVisible = true)
        assertTrue(lock.locked)
        assertTrue(lock.state.recordingVisible)
        assertEquals(UnlockResult.REFUSED_SHARE_VISIBLE, lock.unlock())
    }

    @Test
    fun aColdStartWhileArmedIsLockedWithoutAnyCallback() {
        val persistence = MemoryLockPersistence()
        store(persistence).arm()
        val lock = store(persistence)
        lock.onStart(initialVisible = null)
        assertTrue(lock.locked)
        assertEquals("armed", lock.state.word)
    }

    @Test
    fun manualRemoteAndArmedWorkWithoutTheCallbackOnOlderPhones() {
        val lock = store()
        lock.onStart(initialVisible = null)
        lock.lock(LockSource.REMOTE)
        assertTrue(lock.locked)
        assertEquals(UnlockResult.UNLOCKED, lock.unlock())
        lock.arm()
        assertTrue(lock.locked)
        assertEquals(UnlockResult.UNLOCKED, lock.unlock())
        assertFalse(lock.locked)
    }

    /**
     * The order Android 15 really uses when the S25 goes from OFFGRID to Bigo mid share:
     * NOT_VISIBLE comes as OFFGRID leaves the screen, around onPause and before onStop.
     */
    @Test
    fun leavingTheScreenDuringAShareKeepsTheAutoLockThroughOnStop() {
        val persistence = MemoryLockPersistence()
        val lock = store(persistence)
        lock.onStart(initialVisible = true)
        lock.onResume()
        lock.onPause()
        lock.reportRecording(false)
        assertTrue("leaving is not the share ending", lock.locked)
        assertEquals("live", lock.state.word)
        lock.onStop()
        assertTrue(persistence.values[BroadcastLockStore.KEY_WAS_LOCKED] == true)

        // Back in front with the share still running.
        lock.onStart(initialVisible = true)
        lock.onResume()
        assertTrue(lock.locked)
        assertEquals(UnlockResult.REFUSED_SHARE_VISIBLE, lock.unlock())
    }

    @Test
    fun theShareEndingWhileOffgridIsInFrontClearsTheAutoLock() {
        val lock = store()
        lock.onStart(initialVisible = true)
        lock.onResume()
        lock.reportRecording(false)
        assertFalse(lock.locked)
    }

    @Test
    fun aNotVisibleHeldWhilePausedIsAppliedOnResume() {
        val lock = store()
        lock.onStart(initialVisible = true)
        lock.onResume()
        lock.onPause()
        lock.reportRecording(false)
        assertTrue(lock.locked)
        lock.onResume()
        assertFalse(lock.locked)
    }

    @Test
    fun aVisibleReportDropsAHeldNotVisible() {
        val lock = store()
        lock.onStart(initialVisible = true)
        lock.onPause()
        lock.reportRecording(false)
        lock.reportRecording(true)
        lock.onResume()
        assertTrue(lock.locked)
        assertEquals("live", lock.state.word)
    }

    @Test
    fun onStartReplacesAHeldNotVisibleWithTheFreshFirstValue() {
        val lock = store()
        lock.onStart(initialVisible = true)
        lock.onResume()
        lock.onPause()
        lock.reportRecording(false)
        lock.onStop()
        lock.onStart(initialVisible = true)
        lock.onResume()
        assertTrue(lock.state.recordingVisible)
    }

    @Test
    fun listenersHearEveryChangeAndNoRepeats() {
        val lock = store()
        val heard = ArrayList<LockState>()
        lock.onChanged = { heard += it }
        lock.lock(LockSource.MANUAL)
        lock.lock(LockSource.MANUAL)
        lock.setRecordingVisible(true)
        lock.setRecordingVisible(true)
        lock.unlock()
        lock.setRecordingVisible(false)
        lock.unlock()
        // The refused unlock during the share is not a change. The share ending is one, and the
        // manual lock still holds through it until the second unlock clears it.
        assertEquals(4, heard.size)
        assertTrue(heard[0].manualLocked)
        assertTrue(heard[1].recordingVisible)
        assertFalse(heard[2].recordingVisible)
        assertTrue(heard[2].manualLocked)
        assertFalse(heard[3].locked)
    }
}
