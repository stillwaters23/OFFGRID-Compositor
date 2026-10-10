package com.offgrid.compositor.audio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSwapDspTest {
    @Test
    fun bypassPassesCleanDrySamples() {
        val dsp = VoiceSwapDsp()
        dsp.settings = VoiceSwapSettings(bypass = true, pitchSemitones = 7f, lowDb = 9f)
        val input = floatArrayOf(-0.7f, 0f, 0.42f)
        val output = FloatArray(input.size)

        assertTrue(dsp.process(input, input.size, output))
        assertEquals(input.toList(), output.toList())
    }

    @Test
    fun zeroWetIsDryAndRexPresetIsTunable() {
        val input = FloatArray(480) { if (it % 2 == 0) 0.4f else -0.4f }
        val output = FloatArray(input.size)
        val dsp = VoiceSwapDsp().apply {
            settings = VoiceSwapPresets.REX.settings.copy(wet = 0f)
        }

        assertTrue(dsp.process(input, input.size, output))
        assertEquals(input.toList(), output.toList())

        dsp.settings = VoiceSwapPresets.REX.settings.copy(pitchSemitones = 4f)
        assertEquals(4f, dsp.settings.pitchSemitones)
        assertEquals("Rex", VoiceSwapPresets.REX.name)
    }

    @Test
    fun invalidInputFailsWithoutWritingAnyRawSamples() {
        val dsp = VoiceSwapDsp()
        val output = FloatArray(3) { 1f }

        assertTrue(!dsp.process(floatArrayOf(0f, Float.NaN, 0f), 3, output))
        assertEquals(listOf(0f, 0f, 0f), output.toList())
    }
}
