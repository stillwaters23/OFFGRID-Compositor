package com.offgrid.compositor.audio.voice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class VoiceSwapSettings(
    val enabled: Boolean = true,
    val bypass: Boolean = false,
    val wet: Float = 0.68f,
    val pitchSemitones: Float = 2.0f,
    val formantSemitones: Float = -1.5f,
    val lowDb: Float = 1.5f,
    val midDb: Float = 2.0f,
    val highDb: Float = -1.0f,
) {
    init {
        require(wet in 0f..1f)
        require(pitchSemitones in -12f..12f)
        require(formantSemitones in -12f..12f)
        require(lowDb in -12f..12f && midDb in -12f..12f && highDb in -12f..12f)
    }

    companion object {
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
            pitchSemitones = (pitch.coerceIn(0, 100) - 50) * 0.24f,
            formantSemitones = (formant.coerceIn(0, 100) - 50) * 0.24f,
            lowDb = (lowEq.coerceIn(0, 100) - 50) * 0.24f,
            midDb = (midEq.coerceIn(0, 100) - 50) * 0.24f,
            highDb = (highEq.coerceIn(0, 100) - 50) * 0.24f,
        )
    }
}

data class VoiceSwapPreset(val name: String, val settings: VoiceSwapSettings)

object VoiceSwapPresets {
    /** Placeholder values; Royalty will tune these against the private Rex reference. */
    val REX = VoiceSwapPreset("Rex", VoiceSwapSettings())
}

/**
 * Small real-time DSP chain: dual-head delay pitch shift, shifted resonant formant emphasis,
 * three-band tone shaping, then wet/dry mixing. It allocates no memory while processing.
 */
class VoiceSwapDsp(private val sampleRate: Int = SAMPLE_RATE) {
    init {
        require(sampleRate >= 8_000)
    }

    @Volatile
    var settings = VoiceSwapPresets.REX.settings

    private val delay = FloatArray(sampleRate / 8)
    private var writeAt = 0
    private var phase = 0.0
    private var low250 = 0f
    private var low2500 = 0f
    private val formant1 = BiquadState()
    private val formant2 = BiquadState()

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
        val formantGain = 10f.pow(config.formantSemitones.coerceIn(-6f, 6f) / 20f)
        val delaySpan = sampleRate * 0.035f
        val pitchStep = kotlin.math.abs(pitchRatio - 1.0) / delaySpan
        val lowAlpha = onePoleAlpha(LOW_CUTOFF)
        val highAlpha = onePoleAlpha(HIGH_CUTOFF)
        val formant1Coefficients = BiquadCoefficients.at(
            (700.0 * formantRatio).coerceIn(250.0, sampleRate * 0.18),
            FORMANT_Q,
            sampleRate,
        )
        val formant2Coefficients = BiquadCoefficients.at(
            (1_400.0 * formantRatio).coerceIn(500.0, sampleRate * 0.3),
            FORMANT_Q,
            sampleRate,
        )

        for (index in 0 until count) {
            val dry = input[index]
            if (!dry.isFinite()) {
                output.fill(0f, 0, count)
                return false
            }
            delay[writeAt] = dry
            var shifted = dry
            if (kotlin.math.abs(config.pitchSemitones) > 0.01f) {
                phase = (phase + pitchStep) % 1.0
                val phase2 = (phase + 0.5) % 1.0
                val delay1 = delaySpan * (if (pitchRatio >= 1.0) phase else 1.0 - phase)
                val delay2 = delaySpan * (if (pitchRatio >= 1.0) phase2 else 1.0 - phase2)
                val mix = if (phase < 0.5) phase * 2.0 else (1.0 - phase) * 2.0
                shifted = readDelay(delay1.toFloat()) * mix.toFloat() +
                    readDelay(delay2.toFloat()) * (1f - mix.toFloat())
            }

            val low = onePole(shifted, lowAlpha, first = true)
            val lowMid = onePole(shifted, highAlpha, first = false)
            val high = shifted - lowMid
            val mid = lowMid - low
            val emphasized1 = formant1.process(shifted, formant1Coefficients)
            val emphasized2 = formant2.process(shifted, formant2Coefficients)
            val colored = low * lowGain + mid * midGain + high * highGain +
                (emphasized1 + emphasized2) * (formantGain - 1f) * 0.35f
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
        low250 = 0f
        low2500 = 0f
        formant1.clear()
        formant2.clear()
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
        (1.0 - cos(2.0 * PI * cutoff / sampleRate)).toFloat()

    private fun onePole(sample: Float, alpha: Float, first: Boolean): Float {
        val previous = if (first) low250 else low2500
        val result = previous + alpha * (sample - previous)
        if (first) low250 = result else low2500 = result
        return result
    }

    private class BiquadState {
        var x1 = 0f
        var x2 = 0f
        var y1 = 0f
        var y2 = 0f

        fun process(sample: Float, coefficients: BiquadCoefficients): Float {
            val output = coefficients.b0 * (sample - x2) - coefficients.a1 * y1 - coefficients.a2 * y2
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

    private data class BiquadCoefficients(val b0: Float, val a1: Float, val a2: Float) {
        companion object {
            fun at(center: Double, q: Float, sampleRate: Int): BiquadCoefficients {
                val alpha = (sin(PI * center / sampleRate) / (2.0 * q)).toFloat()
                val a0 = 1f + alpha
                return BiquadCoefficients(
                    b0 = alpha / a0,
                    a1 = -2f * cos(2.0 * PI * center / sampleRate).toFloat() / a0,
                    a2 = (1f - alpha) / a0,
                )
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        private const val LOW_CUTOFF = 250f
        private const val HIGH_CUTOFF = 2_500f
        private const val FORMANT_Q = 1.1f
    }
}
