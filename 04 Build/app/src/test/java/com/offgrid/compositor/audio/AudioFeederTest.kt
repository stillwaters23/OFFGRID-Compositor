package com.offgrid.compositor.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFeederTest {
    private val block = ShortArray(100)

    @Test
    fun usesTheMicWhenItIsReady() {
        val ring = ShortRing(1000)
        ring.write(ShortArray(100) { 5 }, 100)
        assertTrue(AudioFeeder(ring, null).fill(block, 100))
        assertTrue(block.all { it == 5.toShort() })
    }

    @Test
    fun waitsAMomentForALateMic() {
        val ring = ShortRing(1000)
        ring.write(ShortArray(40) { 5 }, 40)
        assertFalse(AudioFeeder(ring, null).fill(block, 100))
    }

    @Test
    fun givesSilenceWhenTheMicIsOff() {
        val ring = ShortRing(1000)
        block.fill(9)
        assertTrue(AudioFeeder(ring, null).fill(block, 300))
        assertArrayEquals(ShortArray(100), block)
    }

    @Test
    fun padsAShortBlockWithSilenceWhenLate() {
        val ring = ShortRing(1000)
        ring.write(ShortArray(40) { 5 }, 40)
        assertTrue(AudioFeeder(ring, null).fill(block, 300))
        assertEquals(5.toShort(), block[39])
        assertEquals(0.toShort(), block[40])
    }

    @Test
    fun neverLagsTheMicByMoreThan100Milliseconds() {
        val ring = ShortRing(48000)
        ring.write(ShortArray(20000) { it.toShort() }, 20000)
        AudioFeeder(ring, null).fill(block, 100)
        assertEquals((20000 - 4800).toShort(), block[0])
    }

    @Test
    fun mixesSoundsIntoSilence() {
        val mixer = SoundMixer(48000).apply { listening = true }
        mixer.add(ShortArray(1000) { 1000 }, 48000, 0.5f)
        AudioFeeder(ShortRing(1000), mixer).fill(block, 300)
        assertEquals(500.toShort(), block[10])
    }
}
