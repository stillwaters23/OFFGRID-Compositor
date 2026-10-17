package com.offgrid.compositor.audio.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

data class VoiceSwapSettings(
    val enabled: Boolean = true,
    val bypass: Boolean = false,
    val wet: Float = 0.75f,
    val pitchSemitones: Float = 0f,
    val formantSemitones: Float = 0f,
    val lowDb: Float = 0f,
    val midDb: Float = 0f,
    val highDb: Float = 0f,
) {
    init {
        require(wet in 0f..1f)
        require(pitchSemitones in -12f..12f)
        require(formantSemitones in -12f..12f)
        require(lowDb in -12f..12f && midDb in -12f..12f && highDb in -12f..12f)
    }

    companion object {
        const val PERCENT_STEP = 0.24

        fun fromPercent(
            wet: Int,
            pitch: Int,
            formant: Int,
            lowEq: Int,
            midEq: Int,
            highEq: Int,
            enabled: Boolean = true,
            bypass: Boolean = false,
        ) = VoiceSwapSettings(
            enabled = enabled,
            bypass = bypass,
            wet = wet.coerceIn(0, 100) / 100f,
            pitchSemitones = units(pitch),
            formantSemitones = units(formant),
            lowDb = units(lowEq),
            midDb = units(midEq),
            highDb = units(highEq),
        )

        private fun units(percent: Int): Float =
            ((percent.coerceIn(0, 100) - 50) * PERCENT_STEP).toFloat().coerceIn(-12f, 12f)
    }
}

data class VoiceSwapPreset(val name: String, val settings: VoiceSwapSettings)

object VoiceSwapPresets {
    /** Pitch, formant, and timbre EQ aimed at the Rex reference takes. Tunable from the panel. */
    val REX = VoiceSwapPreset("Rex", RexVoiceProfile.settings())
}

/**
 * Real-time voice chain for one capture block: dual-head pitch shift, formant move,
 * then low / mid / high timbre EQ. Processing does not allocate. Algorithmic latency is
 * the 15 ms pitch window plus one 5 ms block (20 ms), before the device's own recorder delay.
 */
class VoiceSwapDsp(private val sampleRate: Int = SAMPLE_RATE) {
    init {
        require(sampleRate >= 8_000)
    }

    @Volatile
    var settings = VoiceSwapPresets.REX.settings

    private val delay = FloatArray(sampleRate / 8)
    private val pitchWindowSamples = sampleRate * PITCH_WINDOW_MILLIS / 1000
    private var writeAt = 0
    private var phase = 0.0
    private var lowState = 0f
    private var midState = 0f
    private val baseLow = Biquad()
    private val baseHigh = Biquad()
    private val movedLow = Biquad()
    private val movedHigh = Biquad()

    /** Writes silence on invalid input or non-finite intermediates. */
    fun process(input: FloatArray, count: Int, output: FloatArray): Boolean {
        if (count < 0 || count > input.size || count > output.size) {
            output.fill(0f)
            return false
        }
        val config = settings
        if (!config.enabled || config.bypass) {
            reset()
            input.copyInto(output, 0, 0, count)
            return true
        }

        val pitchRatio = 2.0.pow(config.pitchSemitones / 12.0)
        val formantRatio = 2.0.pow(config.formantSemitones / 12.0)
        val lowGain = 10f.pow(config.lowDb / 20f)
        val midGain = 10f.pow(config.midDb / 20f)
        val highGain = 10f.pow(config.highDb / 20f)
        val delaySpan = pitchWindowSamples.toFloat()
        val pitchStep = abs(pitchRatio - 1.0) / delaySpan
        val lowAlpha = onePoleAlpha(LOW_CUTOFF)
        val highAlpha = onePoleAlpha(HIGH_CUTOFF)
        val formantAmount = (abs(config.formantSemitones) / 12f).coerceIn(0f, 1f) * FORMANT_MIX
        baseLow.setBandpass(FORMANT_LOW, FORMANT_Q, sampleRate)
        baseHigh.setBandpass(FORMANT_HIGH, FORMANT_Q, sampleRate)
        movedLow.setBandpass(
            (FORMANT_LOW * formantRatio).coerceIn(180.0, sampleRate * 0.2),
            FORMANT_Q,
            sampleRate,
        )
        movedHigh.setBandpass(
            (FORMANT_HIGH * formantRatio).coerceIn(400.0, sampleRate * 0.35),
            FORMANT_Q,
            sampleRate,
        )
        val shiftPitch = abs(config.pitchSemitones) > 0.01f

        for (index in 0 until count) {
            val dry = input[index]
            if (!dry.isFinite()) {
                output.fill(0f, 0, count)
                return false
            }
            delay[writeAt] = dry
            val shifted = if (shiftPitch) {
                phase = (phase + pitchStep) % 1.0
                val phase2 = (phase + 0.5) % 1.0
                val mix = if (phase < 0.5) phase * 2.0 else (1.0 - phase) * 2.0
                readDelay(delaySamples(phase, pitchRatio, delaySpan)) * mix.toFloat() +
                    readDelay(delaySamples(phase2, pitchRatio, delaySpan)) * (1f - mix.toFloat())
            } else {
                dry
            }
            val low = onePole(shifted, lowAlpha, low = true)
            val rest = shifted - low
            val mid = onePole(rest, highAlpha, low = false)
            val high = rest - mid
            var colored = low * lowGain + mid * midGain + high * highGain
            if (formantAmount > 0f) {
                val atBase = baseLow.process(colored) + baseHigh.process(colored)
                val moved = movedLow.process(colored) + movedHigh.process(colored)
                colored += (moved - atBase) * formantAmount
            }
            if (!colored.isFinite()) {
                output.fill(0f, 0, count)
                return false
            }
            val wet = colored.coerceIn(-1f, 1f)
            val value = dry * (1f - config.wet) + wet * config.wet
            if (!value.isFinite()) {
                output.fill(0f, 0, count)
                return false
            }
            output[index] = value.coerceIn(-1f, 1f)
            writeAt = (writeAt + 1) % delay.size
        }
        return true
    }

    fun reset() {
        delay.fill(0f)
        writeAt = 0
        phase = 0.0
        lowState = 0f
        midState = 0f
        baseLow.clear()
        baseHigh.clear()
        movedLow.clear()
        movedHigh.clear()
    }

    private fun delaySamples(phase: Double, pitchRatio: Double, delaySpan: Float): Float {
        val shaped = if (pitchRatio >= 1.0) 1.0 - phase else phase
        return delaySpan * shaped.toFloat()
    }

    private fun readDelay(amount: Float): Float {
        val index = (writeAt - amount).let { raw ->
            ((raw % delay.size) + delay.size) % delay.size
        }
        val before = index.toInt()
        val after = (before + 1) % delay.size
        val fraction = index - before
        return delay[before] * (1f - fraction) + delay[after] * fraction
    }

    private fun onePoleAlpha(cutoff: Float): Float =
        (1.0 - exp(-2.0 * PI * cutoff / sampleRate)).toFloat()

    private fun onePole(sample: Float, alpha: Float, low: Boolean): Float {
        val previous = if (low) lowState else midState
        val result = previous + alpha * (sample - previous)
        if (low) lowState = result else midState = result
        return result
    }

    private class Biquad {
        private var b0 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun setBandpass(centerHz: Double, q: Float, sampleRate: Int) {
            val alpha = (sin(PI * centerHz / sampleRate) / (2.0 * q)).toFloat()
            val a0 = 1f + alpha
            b0 = alpha / a0
            a1 = -2f * cos(2.0 * PI * centerHz / sampleRate).toFloat() / a0
            a2 = (1f - alpha) / a0
        }

        fun process(sample: Float): Float {
            val output = b0 * (sample - x2) - a1 * y1 - a2 * y2
            x2 = x1
            x1 = sample
            y2 = y1
            y1 = output
            return output
        }

        fun clear() {
            x1 = 0f
            x2 = 0f
            y1 = 0f
            y2 = 0f
        }
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        const val BLOCK_MILLIS = 5
        const val PITCH_WINDOW_MILLIS = 15
        const val BLOCK_FRAMES = SAMPLE_RATE * BLOCK_MILLIS / 1000
        const val PITCH_DELAY_FRAMES = SAMPLE_RATE * PITCH_WINDOW_MILLIS / 1000
        const val ALGORITHMIC_LATENCY_MILLIS = BLOCK_MILLIS + PITCH_WINDOW_MILLIS
        private const val LOW_CUTOFF = 250f
        private const val HIGH_CUTOFF = 2_500f
        private const val FORMANT_LOW = 650.0
        private const val FORMANT_HIGH = 1_500.0
        private const val FORMANT_Q = 2.4f
        private const val FORMANT_MIX = 0.65f
    }
}
