package com.offgrid.compositor.audio.voice

import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * On-device Rex voice target. There is no Rex.wav in this project.
 * The numbers come from the three reference takes that are present:
 * `06 Prompts and Skills/Rex Voice/Rex 01.wav`, `Rex 02.wav`, and `Rex 03.wav`
 * (48 kHz mono). This is a pitch, formant, and timbre EQ preset. It is not a model.
 *
 * Fundamental medians were 116.5 Hz, 123.4 Hz, and 119.1 Hz.
 * Power-share medians were about 0.6 / 0.2 / 0.0, 0.6 / 0.3 / 0.0, and 0.5 / 0.3 / 0.0
 * for low (80–300 Hz), mid (300–2500 Hz), and high (2500–8000 Hz).
 * The high-band median printed as 0.0, so the high cut is a fixed shelf instead of a ratio.
 * Formant estimates from those takes were too unstable to copy, so formant shift follows
 * a fraction of the pitch shift toward Rex's lower fundamental.
 */
object RexVoiceProfile {
    const val REX_WAV_PRESENT = false
    const val REFERENCE = "Rex 01, Rex 02, Rex 03"
    const val ANCHOR_F0_HZ = 165.0
    const val FORMANT_FOLLOW = 0.55
    const val NEUTRAL_LOW_SHARE = 0.40
    const val NEUTRAL_MID_SHARE = 0.45

    /** High-band median power share was 0.0 at one decimal on every take. */
    const val HIGH_DB = -6.0
    const val WET_PERCENT = 75

    val measuredF0Hz: DoubleArray = doubleArrayOf(116.5, 123.4, 119.1)
    val measuredLowShare: DoubleArray = doubleArrayOf(0.6, 0.6, 0.5)
    val measuredMidShare: DoubleArray = doubleArrayOf(0.2, 0.3, 0.3)

    val measuredF0HzMean: Double = measuredF0Hz.average()
    val pitchSemitones: Double = 12.0 * log2(measuredF0HzMean / ANCHOR_F0_HZ)
    val formantSemitones: Double = pitchSemitones * FORMANT_FOLLOW
    val lowDb: Double = 10.0 * log10(measuredLowShare.average() / NEUTRAL_LOW_SHARE)
    val midDb: Double = 10.0 * log10(measuredMidShare.average() / NEUTRAL_MID_SHARE)

    val pitchPercent: Int = knob(pitchSemitones)
    val formantPercent: Int = knob(formantSemitones)
    val lowPercent: Int = knob(lowDb)
    val midPercent: Int = knob(midDb)
    val highPercent: Int = knob(HIGH_DB)

    /** Knob values are what the panel stores. The DSP preset matches those knobs. */
    fun settings(): VoiceSwapSettings = VoiceSwapSettings.fromPercent(
        wet = WET_PERCENT,
        pitch = pitchPercent,
        formant = formantPercent,
        lowEq = lowPercent,
        midEq = midPercent,
        highEq = highPercent,
        enabled = true,
        bypass = false,
    )

    private fun knob(value: Double): Int =
        (50.0 + value / VoiceSwapSettings.PERCENT_STEP).roundToInt().coerceIn(0, 100)
}
