package com.offgrid.compositor.audio.voice

import java.util.concurrent.atomic.AtomicBoolean

enum class MicState { ON, OFF, BLOCKED }

fun interface MicSource {
    /** Starts capture and forwards mono float samples. False means capture did not start. */
    fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean

    fun stop() = Unit

    /**
     * Hard block. Implementations must release the recorder when [blocked] is true
     * and must refuse [start] until the block is cleared.
     */
    fun setHardBlock(blocked: Boolean) = Unit
}

fun interface VoiceAudioOutput {
    /** Consume the processed samples before returning. The array is reused by the controller. */
    fun write(samples: FloatArray, count: Int)
}

/**
 * Owns capture transitions. BLOCKED stays blocked until [setBlocked] is called with false.
 * Stopping the recorder happens outside the controller lock so a capture callback can finish.
 */
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

    private val lock = Any()
    private val hardBlocked = AtomicBoolean(false)
    private var processed = FloatArray(0)

    fun setEnabled(enabled: Boolean): Boolean {
        if (!enabled) {
            val blocked = synchronized(lock) {
                if (state == MicState.BLOCKED || hardBlocked.get()) {
                    true
                } else {
                    transition(MicState.OFF)
                    false
                }
            }
            if (blocked) return false
            stopSource()
            return true
        }
        val startNow = synchronized(lock) {
            if (state == MicState.BLOCKED || hardBlocked.get()) {
                false
            } else if (state == MicState.ON) {
                null
            } else {
                transition(MicState.ON)
                true
            }
        }
        if (startNow == null) return true
        if (!startNow) return false
        val started = try {
            source.start(::onSamples, ::failClosed)
        } catch (_: RuntimeException) {
            false
        }
        val stillOn = synchronized(lock) { state == MicState.ON && !hardBlocked.get() }
        if (!started || !stillOn) {
            failClosed()
            return false
        }
        return true
    }

    fun setBlocked(blocked: Boolean) {
        if (blocked) hardBlocked.set(true)
        val action = synchronized(lock) {
            if (blocked) {
                transition(MicState.BLOCKED)
                BlockAction.ENGAGE
            } else if (state == MicState.BLOCKED) {
                transition(MicState.OFF)
                hardBlocked.set(false)
                BlockAction.RELEASE
            } else {
                BlockAction.NONE
            }
        }
        when (action) {
            BlockAction.ENGAGE -> {
                engageSourceBlock()
                stopSource()
            }
            BlockAction.RELEASE -> {
                releaseSourceBlock()
                stopSource()
            }
            BlockAction.NONE -> Unit
        }
    }

    private fun onSamples(input: FloatArray, count: Int) {
        if (hardBlocked.get()) return
        val fail = synchronized(lock) {
            if (hardBlocked.get() || state != MicState.ON) return@synchronized false
            if (count <= 0 || count > input.size) return@synchronized true
            if (processed.size < count) processed = FloatArray(count)
            val started = clockNanos()
            val healthy = try {
                dsp.process(input, count, processed)
            } catch (_: RuntimeException) {
                false
            }
            val elapsed = clockNanos() - started
            val tooSlow = elapsed < 0L || elapsed > maxProcessNanos
            if (!healthy || tooSlow || hardBlocked.get()) {
                processed.fill(0f, 0, count)
                return@synchronized !hardBlocked.get() && (!healthy || tooSlow)
            }
            try {
                output.write(processed, count)
            } catch (_: RuntimeException) {
                processed.fill(0f, 0, count)
                return@synchronized true
            }
            processed.fill(0f, 0, count)
            false
        }
        if (fail) failClosed()
    }

    private fun failClosed() {
        synchronized(lock) {
            if (state == MicState.ON) transition(MicState.OFF)
            dsp.reset()
        }
        stopSource()
    }

    private fun engageSourceBlock() {
        try {
            source.setHardBlock(true)
        } catch (_: RuntimeException) {
            Unit
        }
    }

    private fun releaseSourceBlock() {
        try {
            source.setHardBlock(false)
        } catch (_: RuntimeException) {
            Unit
        }
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

    private enum class BlockAction { ENGAGE, RELEASE, NONE }

    companion object {
        const val MAX_PROCESS_NANOS = 80_000_000L
    }
}
