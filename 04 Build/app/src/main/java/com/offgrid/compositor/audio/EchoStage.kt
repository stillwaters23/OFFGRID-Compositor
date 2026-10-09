package com.offgrid.compositor.audio

/** Repeats the voice after [delayMs] milliseconds. Each repeat is quieter by the feedback. */
class EchoStage(private val sampleRate: Int) {
    private val line = FloatArray((VoiceSettings.ECHO_MAX_MS / 1000f * sampleRate).toInt() + 2)
    private var write = 0

    fun process(block: FloatArray, count: Int, delayMs: Float, feedback: Float) {
        val delay = (delayMs / 1000f * sampleRate).toInt().coerceIn(1, line.size - 1)
        for (i in 0 until count) {
            val heard = line[Math.floorMod(write - delay, line.size)]
            line[write] = block[i] + heard * feedback
            block[i] += heard * WET
            write = (write + 1) % line.size
        }
    }

    fun reset() {
        line.fill(0f)
        write = 0
    }

    private companion object {
        const val WET = 0.6f
    }
}
