package com.offgrid.compositor.audio

import com.offgrid.compositor.effects.EffectSnapshot

/** The live mic: capture with Microphone Effects applied, and the beat that follows it. */
class MicPipeline {
    val engine = MicEngine()
    val beats = BeatDetector()

    init {
        engine.addSink(beats)
    }

    /** Follows the typed Microphone Effects values. Takes effect on the next block of sound. */
    fun apply(settings: EffectSnapshot) {
        engine.settings = VoiceSettings.from(settings)
    }

    /** Opens the mic. Returns false when the phone would not give it. */
    fun start(): Boolean {
        beats.reset()
        return engine.start()
    }

    /** Releases the mic, so the phone's orange dot goes away. */
    fun stop() {
        engine.stop()
        beats.reset()
    }
}
