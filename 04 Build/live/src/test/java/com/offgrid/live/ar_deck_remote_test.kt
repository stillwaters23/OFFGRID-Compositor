package com.offgrid.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phone 2 can play, pause, and skip the AR deck on the existing fire allowlist. */
class ArDeckRemoteTest {
    @Test
    fun arDeckCommandsStayOnTheFireAllowlist() {
        assertTrue(RemoteMessage.FIRE_ACTIONS.containsAll(listOf("ar_play", "ar_pause", "ar_next")))
        for (action in listOf("ar_play", "ar_pause", "ar_next")) {
            val message = RemoteMessage("", "fire", action)
            assertEquals(message, RemoteMessage.decode(message.encode()))
        }
        assertFalse(runCatching { RemoteMessage("", "fire", "read_media_video") }.isSuccess)
    }

    @Test
    fun arDeckSettingsFitTheExistingSettingMessage() {
        val opacity = RemoteMessage("fx.ar.opacity", "setting", "80")
        val clip = RemoteMessage("fx.ar.clip", "setting", "c1")
        val loop = RemoteMessage("fx.ar.loop", "setting", "true")
        assertEquals(opacity, RemoteMessage.decode(opacity.encode()))
        assertEquals("c1", RemoteMessage.decode(clip.encode()).value)
        assertEquals(loop, RemoteMessage.decode(loop.encode()))
    }
}
