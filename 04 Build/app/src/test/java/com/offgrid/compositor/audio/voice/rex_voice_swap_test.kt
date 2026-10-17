package com.offgrid.compositor.audio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class RexVoiceSwapTest {
    @Test
    fun rexPresetLowersPitchAndMatchesThePanelKnobs() {
        assertTrue(!RexVoiceProfile.REX_WAV_PRESENT)
        assertTrue(RexVoiceProfile.pitchSemitones < -4.0)
        assertTrue(RexVoiceProfile.formantSemitones < -2.0)
        assertTrue(RexVoiceProfile.lowDb > 0.0)
        assertTrue(RexVoiceProfile.midDb < 0.0)
        assertEquals(-6.0, RexVoiceProfile.HIGH_DB, 0.001)
        assertEquals("Rex", VoiceSwapPresets.REX.name)

        val settings = VoiceSwapPresets.REX.settings
        assertEquals(RexVoiceProfile.settings().pitchSemitones, settings.pitchSemitones, 0.001f)
        assertEquals(
            RexVoiceProfile.pitchSemitones.toFloat(),
            settings.pitchSemitones,
            0.3f,
        )
        assertEquals(
            RexVoiceProfile.formantSemitones.toFloat(),
            settings.formantSemitones,
            0.3f,
        )
        assertEquals(RexVoiceProfile.lowDb.toFloat(), settings.lowDb, 0.3f)
        assertEquals(RexVoiceProfile.midDb.toFloat(), settings.midDb, 0.3f)
        assertEquals(RexVoiceProfile.HIGH_DB.toFloat(), settings.highDb, 0.3f)
        assertTrue(settings.pitchSemitones < 0f)
        assertTrue(settings.highDb < 0f)
    }

    @Test
    fun pitchShiftMovesFrequencyInTheRequestedDirection() {
        val rate = VoiceSwapDsp.SAMPLE_RATE
        val input = sine(rate, 200.0, seconds = 0.5)
        val flat = render(input, VoiceSwapSettings(pitchSemitones = 0f, wet = 1f))
        val higher = render(input, VoiceSwapSettings(pitchSemitones = 7f, wet = 1f))
        val lower = render(input, VoiceSwapSettings(pitchSemitones = -5f, wet = 1f))
        val skip = rate * 12 / 100

        val flatHz = frequency(flat, skip, rate)
        val higherHz = frequency(higher, skip, rate)
        val lowerHz = frequency(lower, skip, rate)

        assertEquals(200.0, flatHz, 1.0)
        assertTrue("raised pitch was $higherHz Hz", higherHz > 280.0)
        assertTrue("lowered pitch was $lowerHz Hz", lowerHz < 180.0)
    }

    @Test
    fun timbreEqBoostsLowsAndCutsHighs() {
        val rate = VoiceSwapDsp.SAMPLE_RATE
        val lowIn = sine(rate, 100.0, seconds = 0.25)
        val lowOut = render(lowIn, VoiceSwapSettings(lowDb = 12f, wet = 1f))
        val lowRatio = rms(lowOut, rate / 10) / rms(lowIn, rate / 10)
        assertTrue("low boost ratio $lowRatio", lowRatio > 2.0)

        val highIn = sine(rate, 6000.0, seconds = 0.25)
        val highOut = render(highIn, VoiceSwapSettings(highDb = -12f, wet = 1f))
        val highRatio = rms(highOut, rate / 10) / rms(highIn, rate / 10)
        assertTrue("high cut ratio $highRatio", highRatio < 0.75)
    }

    @Test
    fun formantShiftChangesTheVoiceColorWithoutLeavingRange() {
        val rate = VoiceSwapDsp.SAMPLE_RATE
        val vowel = FloatArray(rate / 5) { index ->
            val t = index.toDouble() / rate
            (0.3 * sin(2.0 * PI * 220.0 * t) +
                0.25 * sin(2.0 * PI * 700.0 * t) +
                0.2 * sin(2.0 * PI * 1400.0 * t)).toFloat()
        }
        val plain = render(vowel, VoiceSwapSettings(wet = 1f))
        val moved = render(vowel, VoiceSwapSettings(formantSemitones = -3f, wet = 1f))
        var diff = 0.0
        for (index in vowel.indices) {
            assertTrue(moved[index] in -1f..1f)
            diff += abs(moved[index] - plain[index])
        }
        assertTrue("formant path did not change the buffer", diff > 1.0)
    }

    @Test
    fun flatSettingsPassAudioAndASecondOfAudioStaysRealTime() {
        val rate = VoiceSwapDsp.SAMPLE_RATE
        val input = sine(rate, 440.0, seconds = 0.05)
        val output = render(input, VoiceSwapSettings(wet = 1f))
        for (index in input.indices) {
            assertEquals(input[index], output[index], 0.0001f)
        }

        assertEquals(20, VoiceSwapDsp.ALGORITHMIC_LATENCY_MILLIS)
        assertEquals(5, VoiceSwapDsp.BLOCK_FRAMES * 1000 / rate)
        val block = VoiceSwapDsp.BLOCK_FRAMES
        val dsp = VoiceSwapDsp()
        dsp.settings = VoiceSwapPresets.REX.settings
        val chunk = sine(rate, 180.0, seconds = block.toDouble() / rate)
        val rendered = FloatArray(block)
        val started = System.nanoTime()
        var energy = 0.0
        val blocks = rate / block
        repeat(blocks) {
            assertTrue(dsp.process(chunk, block, rendered))
            energy += rendered[block / 2]
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
        assertTrue(energy.isFinite())
        assertTrue("1s of Rex DSP took ${elapsedMs}ms", elapsedMs < 1000.0)
    }

    private fun render(input: FloatArray, settings: VoiceSwapSettings): FloatArray {
        val dsp = VoiceSwapDsp()
        dsp.settings = settings
        val output = FloatArray(input.size)
        check(dsp.process(input, input.size, output))
        return output
    }

    private fun sine(rate: Int, hz: Double, seconds: Double): FloatArray {
        val count = (rate * seconds).toInt()
        return FloatArray(count) { index ->
            sin(2.0 * PI * hz * index / rate).toFloat() * 0.5f
        }
    }

    private fun frequency(samples: FloatArray, skip: Int, rate: Int): Double {
        var crossings = 0
        for (index in skip + 1 until samples.size) {
            if (samples[index - 1] <= 0f && samples[index] > 0f) crossings++
        }
        val seconds = (samples.size - skip).toDouble() / rate
        return crossings / seconds
    }

    private fun rms(samples: FloatArray, skip: Int): Double {
        var sum = 0.0
        var count = 0
        for (index in skip until samples.size) {
            val value = samples[index].toDouble()
            sum += value * value
            count++
        }
        return kotlin.math.sqrt(sum / count)
    }
}
