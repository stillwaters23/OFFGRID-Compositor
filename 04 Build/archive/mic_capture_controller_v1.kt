package com.offgrid.compositor.audio.voice

enum class MicState { ON, OFF, BLOCKED }

fun interface MicSource {
    /** Starts capture and forwards mono float samples. False means capture did not start. */
    fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean

    fun stop() = Unit
}

fun interface VoiceAudioOutput {
    /** Consume the processed samples before returning. The array is reused by the controller. */
    fun write(samples: FloatArray, count: Int)
}

/** Owns capture transitions. BLOCKED is sticky until explicitly unblocked. */
class MicCaptureController(
    private val source: MicSource,
    private val dsp: VoiceSwapDsp,
    private val output: VoiceAudioOutput,
    private val maxProcessNanos: Long = MAX_PROCESS_NANOS,
    private val clockNanos: () -> Long = System::nanoTime,
    private val onState: (MicState) -> Unit = {},
) {
    @Volatile
    var state: MicState = MicState.OFF
        private set

    private var processed = FloatArray(0)

    @Synchronized
    fun setEnabled(enabled: Boolean): Boolean {
        if (state == MicState.BLOCKED) return false
        if (!enabled) {
            transition(MicState.OFF)
            stopSource()
            return true
        }
        if (state == MicState.ON) return true

        transition(MicState.ON)
        val started = try {
            source.start(::onSamples, ::failClosed)
        } catch (_: RuntimeException) {
            false
        }
        if (!started) failClosed()
        return started
    }

    @Synchronized
    fun setBlocked(blocked: Boolean) {
        if (blocked) {
            transition(MicState.BLOCKED)
            stopSource()
        } else if (state == MicState.BLOCKED) {
            transition(MicState.OFF)
            stopSource()
        }
    }

    @Synchronized
    private fun onSamples(input: FloatArray, count: Int) {
        if (state != MicState.ON) return
        if (count <= 0 || count > input.size) {
            failClosed()
            return
        }
        if (processed.size < count) processed = FloatArray(count)
        val started = clockNanos()
        val healthy = try {
            dsp.process(input, count, processed)
        } catch (_: RuntimeException) {
            false
        }
        val elapsed = clockNanos() - started
        if (!healthy || elapsed < 0L || elapsed > maxProcessNanos) {
            processed.fill(0f, 0, count)
            failClosed()
            return
        }
        try {
            output.write(processed, count)
        } catch (_: RuntimeException) {
            processed.fill(0f, 0, count)
            failClosed()
        }
        processed.fill(0f, 0, count)
    }

    @Synchronized
    private fun failClosed() {
        if (state == MicState.ON) transition(MicState.OFF)
        dsp.reset()
        stopSource()
    }

    private fun stopSource() {
        try {
            source.stop()
        } catch (_: RuntimeException) {
            Unit
        }
    }

    private fun transition(next: MicState) {
        if (state == next) return
        state = next
        if (next != MicState.ON) dsp.reset()
        try {
            onState(next)
        } catch (_: RuntimeException) {
            Unit
        }
    }

    companion object {
        const val MAX_PROCESS_NANOS = 80_000_000L
    }
}
