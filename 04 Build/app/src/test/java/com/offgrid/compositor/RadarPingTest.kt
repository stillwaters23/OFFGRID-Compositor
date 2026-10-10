package com.offgrid.compositor

import com.offgrid.compositor.audio.SoundMixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarPingTest {
    @Test
    fun radarAndMirrorScanGoIntoTheVideoAndOffDoesNot() {
        assertTrue(RadarPing.intoVideo(radarOn = true, mirrorOn = false))
        assertTrue(RadarPing.intoVideo(radarOn = false, mirrorOn = true))
        assertFalse(RadarPing.intoVideo(radarOn = false, mirrorOn = false))
    }

    @Test
    fun aMissingOrOffChoiceStillUsesThePing() {
        assertEquals(RadarPing.PING, RadarPing.soundName(""))
        assertEquals(RadarPing.PING, RadarPing.soundName("off"))
        assertEquals("chime", RadarPing.soundName("chime"))
    }

    @Test
    fun thePingIsMixedIntoRecordedAudio() {
        val mixer = SoundMixer(SoundShapes.RATE).apply { listening = true }
        val ping = SoundShapes.make(RadarPing.PING)
        mixer.add(ping, SoundShapes.RATE, RadarPing.QUIET_GAIN)
        val block = ShortArray(ping.size.coerceAtMost(SoundShapes.RATE))
        mixer.mixInto(block, block.size)
        assertTrue(block.any { it != 0.toShort() })
    }
}
