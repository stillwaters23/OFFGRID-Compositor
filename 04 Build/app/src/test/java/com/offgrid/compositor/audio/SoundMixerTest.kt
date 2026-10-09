package com.offgrid.compositor.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SoundMixerTest {
    @Test
    fun ignoresSoundsWhenNothingIsRecording() {
        val mixer = SoundMixer(48000)
        mixer.add(ShortArray(100) { 1000 }, 48000, 1f)
        val block = ShortArray(10)
        mixer.mixInto(block, 10)
        assertEquals(0.toShort(), block[0])
    }

    @Test
    fun playsALowerRateSoundLongerAtTheRecordingRate() {
        val mixer = SoundMixer(48000).apply { listening = true }
        mixer.add(ShortArray(2205) { 1000 }, 22050, 1f)
        var loud = 0
        val block = ShortArray(480)
        repeat(20) {
            block.fill(0)
            mixer.mixInto(block, 480)
            loud += block.count { it.toInt() == 1000 }
        }
        assertEquals(4800.0, loud.toDouble(), 4.0)
    }

    @Test
    fun addsToTheMicAndStopsAtFullScale() {
        val mixer = SoundMixer(48000).apply { listening = true }
        mixer.add(ShortArray(100) { 30000 }, 48000, 1f)
        val block = ShortArray(10) { 30000 }
        mixer.mixInto(block, 10)
        assertEquals(Short.MAX_VALUE, block[0])
    }
}
