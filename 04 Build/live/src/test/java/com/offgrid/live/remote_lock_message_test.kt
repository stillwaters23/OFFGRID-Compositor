package com.offgrid.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 1.3: the Broadcast Lock words Phone 2 may send and the display keys it receives. */
class RemoteLockMessageTest {
    @Test
    fun lockCommandsAreOnTheFireAllowlistAndRoundTrip() {
        assertTrue(RemoteMessage.LOCK_ACTIONS.all { it in RemoteMessage.FIRE_ACTIONS })
        for (action in RemoteMessage.LOCK_ACTIONS) {
            val message = RemoteMessage("fx.lock", "fire", action)
            assertEquals(message, RemoteMessage.decode(message.encode()))
        }
    }

    @Test
    fun lockStateAndStatusLineTravelAsDisplaySettings() {
        val state = RemoteMessage(RemoteMessage.LOCK_STATE_KEY, "setting", "locked")
        val status = RemoteMessage(RemoteMessage.STATUS_LINE_KEY, "setting", "Unlock refused. A screen share is live on the S25.")
        assertEquals(state, RemoteMessage.decode(state.encode()))
        assertEquals(status, RemoteMessage.decode(status.encode()))
    }

    @Test
    fun phone2HasNoCommandThatReopensThePanel() {
        assertTrue(RemoteMessage.FIRE_ACTIONS.none { it.contains("panel") })
        assertTrue("open" !in RemoteMessage.FIRE_ACTIONS)
        assertTrue("reopen" !in RemoteMessage.FIRE_ACTIONS)
    }

    @Test
    fun lockCommandsStayOutOfTheGenericEffectButtons() {
        val effectButtons = RemoteMessage.FIRE_ACTIONS - RemoteMessage.LOCK_ACTIONS
        assertTrue("shatter" in effectButtons)
        assertTrue(RemoteMessage.LOCK_ACTIONS.none { it in effectButtons })
    }
}
