package com.offgrid.compositor.audio

import com.offgrid.compositor.audio.voice.MicKeys
import com.offgrid.compositor.audio.voice.VoiceAudioOutput
import com.offgrid.compositor.audio.voice.VoiceAudioRouter
import com.offgrid.compositor.audio.voice.VoiceSwapDsp
import com.offgrid.compositor.audio.voice.VoiceSwapSettings
import com.offgrid.compositor.effects.EffectSnapshot

/**
 * The live mic: one capture, Microphone Effects, the Rex voice swap, and the beat that follows it.
 * A hard block refuses to open the recorder. Processed samples also go out through [addVoiceAudioOutput].
 */
class MicPipeline(val swap: VoiceSwapDsp = VoiceSwapDsp()) {
    val engine = MicEngine()
    val beats = BeatDetector()
    var hardBlocked = false
    private val router = VoiceAudioRouter()
    private var swapOut = FloatArray(480)

    init {
        engine.addSink(beats)
        engine.beforeVoice = { block, count -> applySwap(block, count) }
        engine.afterVoice = { block, count -> router.write(block, count) }
    }

    /** Lane hook: recording and Send to PC can listen to the processed voice. Raw mic frames are not offered. */
    fun addVoiceAudioOutput(output: VoiceAudioOutput): AutoCloseable = router.addOutput(output)

    /** Follows the typed Microphone Effects and Rex voice values. Takes effect on the next block of sound. */
    fun apply(settings: EffectSnapshot) {
        engine.settings = VoiceSettings.from(settings)
        hardBlocked = settings.on(MicKeys.BLOCKED)
        swap.settings = VoiceSwapSettings.fromPercent(
            wet = settings.raw(MicKeys.WET),
            pitch = settings.raw(MicKeys.PITCH),
            formant = settings.raw(MicKeys.FORMANT),
            lowEq = settings.raw(MicKeys.LOW_EQ),
            midEq = settings.raw(MicKeys.MID_EQ),
            highEq = settings.raw(MicKeys.HIGH_EQ),
            enabled = settings.on(MicKeys.VOICE_SWAP),
            bypass = settings.on(MicKeys.BYPASS),
        )
    }

    /** Opens the mic. Returns false when the phone would not give it, or while the hard block is on. */
    fun start(): Boolean {
        if (hardBlocked) return false
        beats.reset()
        return engine.start()
    }

    /** Releases the mic, so the phone's orange dot goes away. */
    fun stop() {
        engine.stop()
        beats.reset()
    }

    private fun applySwap(block: FloatArray, count: Int) {
        val config = swap.settings
        if (!config.enabled || config.bypass || count <= 0) return
        if (swapOut.size < count) swapOut = FloatArray(count)
        if (swap.process(block, count, swapOut)) {
            swapOut.copyInto(block, 0, 0, count)
        }
    }
}
