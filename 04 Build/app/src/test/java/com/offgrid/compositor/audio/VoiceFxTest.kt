package com.offgrid.compositor.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceFxTest {
    private val rate = VoiceFx.SAMPLE_RATE

    private fun sine(hz: Double, seconds: Double, level: Float = 0.5f): FloatArray =
        FloatArray((seconds * rate).toInt()) { (level * sin(2 * PI * hz * it / rate)).toFloat() }

    private fun run(fx: VoiceFx, input: FloatArray): FloatArray {
        val out = input.copyOf()
        var at = 0
        while (at < out.size) {
            val n = minOf(BLOCK, out.size - at)
            val block = out.copyOfRange(at, at + n)
            fx.process(block, n)
            block.copyInto(out, at)
            at += n
        }
        return out
    }

    private fun fxWith(settings: VoiceSettings) = VoiceFx().apply { this.settings = settings }

    private fun rms(data: FloatArray, from: Int = 0, to: Int = data.size): Float {
        var sum = 0.0
        for (i in from until to) sum += data[i] * data[i]
        return sqrt(sum / (to - from)).toFloat()
    }

    private fun hertz(data: FloatArray, from: Int, to: Int): Double {
        var crossings = 0
        for (i in from + 1 until to) if (data[i - 1] < 0f && data[i] >= 0f) crossings++
        return crossings / ((to - from).toDouble() / rate)
    }

    @Test
    fun naturalPitchChangesNothing() {
        val input = sine(440.0, 0.5)
        val out = run(fxWith(VoiceSettings(effectsOn = true, pitchRatio = 1f)), input)
        for (i in input.indices) assertEquals(input[i], out[i], 1e-6f)
    }

    @Test
    fun octaveDownHalvesTheFrequency() {
        val out = run(fxWith(VoiceSettings(effectsOn = true, pitchRatio = 0.5f)), sine(440.0, 1.0))
        assertEquals(220.0, hertz(out, rate / 4, rate), 15.0)
    }

    @Test
    fun octaveUpDoublesTheFrequency() {
        val out = run(fxWith(VoiceSettings(effectsOn = true, pitchRatio = 2f)), sine(220.0, 1.0))
        assertEquals(440.0, hertz(out, rate / 4, rate), 30.0)
    }

    @Test
    fun pitchKeepsTheLengthAndLevel() {
        val out = run(fxWith(VoiceSettings(effectsOn = true, pitchRatio = 0.5f)), sine(440.0, 1.0))
        assertEquals(rate, out.size)
        assertEquals(0.35f, rms(out, rate / 2, rate), 0.1f)
    }

    @Test
    fun echoComesBackAtTheTypedDelay() {
        val input = FloatArray(rate)
        input[0] = 0.5f
        val settings = VoiceSettings(effectsOn = true, echoDelayMs = 200f, echoFeedback = 0.5f)
        val out = run(fxWith(settings), input)
        var peak = 1
        for (i in 1 until out.size) if (abs(out[i]) > abs(out[peak])) peak = i
        val delayMs = peak * 1000.0 / rate
        assertEquals(200.0, delayMs, 5.0)
        val second = (out.copyOfRange(peak + 100, out.size)).withIndex().maxBy { abs(it.value) }.index + peak + 100
        assertEquals(400.0, second * 1000.0 / rate, 5.0)
    }

    @Test
    fun echoOffAddsNothing() {
        val input = FloatArray(rate / 2)
        input[0] = 0.5f
        val out = run(fxWith(VoiceSettings(effectsOn = true, echoDelayMs = 0f, echoFeedback = 0.8f)), input)
        for (i in 1 until out.size) assertEquals(0f, out[i], 0f)
    }

    @Test
    fun masterOffSkipsEveryStage() {
        val input = sine(440.0, 0.3)
        val settings = VoiceSettings(effectsOn = false, pitchRatio = 0.5f, robotAmount = 1f, echoDelayMs = 100f, caveAmount = 1f, radio = true)
        val out = run(fxWith(settings), input)
        for (i in input.indices) assertEquals(input[i], out[i], 1e-6f)
    }

    @Test
    fun gateSilencesQuietNoiseAndKeepsVoice() {
        val noise = FloatArray(rate) { if (it % 2 == 0) 0.001f else -0.001f }
        val gated = run(fxWith(VoiceSettings(gateLevel = 0.01f)), noise)
        assertTrue(rms(gated, rate / 2, rate) < 1e-5f)
        val voice = run(fxWith(VoiceSettings(gateLevel = 0.01f)), sine(300.0, 1.0))
        assertEquals(0.35f, rms(voice, rate / 2, rate), 0.03f)
    }

    @Test
    fun gainScalesTheVoice() {
        val out = run(fxWith(VoiceSettings(gain = 0.5f)), sine(300.0, 0.3, 0.4f))
        assertEquals(0.2f, out.maxOf { it }, 0.01f)
    }

    @Test
    fun robotChangesTheVoiceAndStaysInRange() {
        val input = sine(300.0, 0.5)
        val out = run(fxWith(VoiceSettings(effectsOn = true, robotAmount = 0.5f)), input)
        assertTrue(input.indices.any { abs(out[it] - input[it]) > 0.05f })
        assertTrue(out.all { abs(it) <= 1f })
    }

    @Test
    fun radioKeepsTheMiddleAndCutsTheEnds() {
        val settings = VoiceSettings(effectsOn = true, radio = true)
        val low = rms(run(fxWith(settings), sine(60.0, 1.0)), rate / 2, rate)
        val middle = rms(run(fxWith(settings), sine(1000.0, 1.0)), rate / 2, rate)
        val high = rms(run(fxWith(settings), sine(10000.0, 1.0)), rate / 2, rate)
        assertTrue(low < middle * 0.2f)
        assertTrue(high < middle * 0.2f)
    }

    @Test
    fun caveLeavesATailAfterTheVoiceStops() {
        val input = FloatArray(rate)
        for (i in 0 until 2400) input[i] = 0.5f * sin(2 * PI * 300 * i / rate).toFloat()
        val dry = run(fxWith(VoiceSettings()), input)
        val wet = run(fxWith(VoiceSettings(effectsOn = true, caveAmount = 0.8f)), input)
        assertEquals(0f, rms(dry, 24000, 48000), 0f)
        assertTrue(rms(wet, 24000, 48000) > 0.001f)
        assertTrue(wet.all { !it.isNaN() })
    }

    @Test
    fun everyStageTogetherNeverClipsOrBreaks() {
        val settings = VoiceSettings(
            effectsOn = true, pitchRatio = 0.7f, robotAmount = 1f, echoDelayMs = 50f, echoFeedback = 0.85f,
            radio = true, caveAmount = 1f, gain = 4f, gateLevel = 0.001f,
        )
        val loud = FloatArray(rate * 2) { if ((it / 40) % 2 == 0) 1f else -1f }
        val out = run(fxWith(settings), loud)
        assertTrue(out.all { !it.isNaN() && abs(it) <= 1f })
    }

    @Test
    fun turnedOffEchoDoesNotComeBack() {
        val fx = fxWith(VoiceSettings(effectsOn = true, echoDelayMs = 100f, echoFeedback = 0.8f))
        run(fx, sine(300.0, 0.5))
        fx.settings = VoiceSettings()
        run(fx, FloatArray(rate / 2))
        fx.settings = VoiceSettings(effectsOn = true, echoDelayMs = 100f, echoFeedback = 0.8f)
        val out = run(fx, FloatArray(rate / 2))
        assertEquals(0f, rms(out), 0f)
    }

    private companion object {
        const val BLOCK = 480
    }
}
