package com.offgrid.compositor.audio.voice

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lane integration point for processed PCM. Recording and Send-to-PC adapters attach here;
 * raw microphone frames are never offered to destinations.
 */
class VoiceAudioRouter : VoiceAudioOutput {
    private val outputs = CopyOnWriteArrayList<VoiceAudioOutput>()

    fun addOutput(output: VoiceAudioOutput): AutoCloseable {
        outputs += output
        return AutoCloseable { outputs -= output }
    }

    override fun write(samples: FloatArray, count: Int) {
        for (output in outputs) output.write(samples, count)
    }
}
