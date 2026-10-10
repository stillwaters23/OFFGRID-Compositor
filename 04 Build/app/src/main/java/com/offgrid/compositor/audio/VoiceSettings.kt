package com.offgrid.compositor.audio

import com.offgrid.compositor.effects.EffectSnapshot
import kotlin.math.pow

/**
 * The Microphone Effects settings turned into the numbers the voice stages use.
 * Mic volume and the noise gate always apply. The other stages only run while the master is on.
 */
data class VoiceSettings(
    val effectsOn: Boolean = false,
    /** 0.5 to 2.0. 1.0 is the natural voice. */
    val pitchRatio: Float = 1f,
    val robotAmount: Float = 0f,
    val echoDelayMs: Float = 0f,
    val echoFeedback: Float = 0f,
    val radio: Boolean = false,
    val caveAmount: Float = 0f,
    val gain: Float = 1f,
    /** The gate opens above this level, as a share of full scale. 0 means the gate is off. */
    val gateLevel: Float = 0f,
) {
    val pitchOn: Boolean get() = effectsOn && pitchRatio != 1f
    val robotOn: Boolean get() = effectsOn && robotAmount > 0f
    val echoOn: Boolean get() = effectsOn && echoDelayMs > 0f
    val caveOn: Boolean get() = effectsOn && caveAmount > 0f
    val radioOn: Boolean get() = effectsOn && radio

    companion object {
        const val ECHO_MIN_MS = 50f
        const val ECHO_MAX_MS = 800f
        const val ECHO_MAX_FEEDBACK = 0.85f
        private const val GATE_QUIET_DB = -60f
        private const val GATE_LOUD_DB = -20f
        private const val GAIN_MAX = 4f

        fun from(settings: EffectSnapshot): VoiceSettings {
            val gate = settings.raw(MicKeys.GATE)
            return VoiceSettings(
                effectsOn = settings.on(MicKeys.FX_ON),
                pitchRatio = 2f.pow((settings.raw(MicKeys.PITCH) - 50) / 50f),
                robotAmount = if (settings.on(MicKeys.ROBOT_ON)) settings.value(MicKeys.ROBOT_AMOUNT) else 0f,
                echoDelayMs = if (settings.on(MicKeys.ECHO_ON)) {
                    ECHO_MIN_MS + settings.value(MicKeys.ECHO_DELAY) * (ECHO_MAX_MS - ECHO_MIN_MS)
                } else {
                    0f
                },
                echoFeedback = settings.value(MicKeys.ECHO_AMOUNT) * ECHO_MAX_FEEDBACK,
                radio = settings.on(MicKeys.RADIO_ON),
                caveAmount = if (settings.on(MicKeys.CAVE_ON)) settings.value(MicKeys.CAVE_AMOUNT) else 0f,
                gain = gainFor(settings.raw(MicKeys.GAIN)),
                gateLevel = if (gate == 0) 0f else 10f.pow((GATE_QUIET_DB + (GATE_LOUD_DB - GATE_QUIET_DB) * gate / 100f) / 20f),
            )
        }

        /** 0 is silent, 50 is unchanged, 100 is four times louder. */
        fun gainFor(value: Int): Float = if (value <= 50) value / 50f else 1f + (value - 50) / 50f * (GAIN_MAX - 1f)
    }
}
