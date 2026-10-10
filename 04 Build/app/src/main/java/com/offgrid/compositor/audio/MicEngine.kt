package com.offgrid.compositor.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/** The mic capture, so a test can stand in for the phone's recorder. */
internal interface MicCapture {
    val initialized: Boolean
    fun start()
    fun read(buffer: ShortArray, offset: Int, size: Int): Int
    fun release()
}

/**
 * Listens to the mic: 48 kHz mono 16-bit, with Microphone Effects applied, and hands each block to the sinks.
 *
 * The source is VOICE_RECOGNITION. Of the sources every phone offers, it asks for the least processing:
 * no automatic gain and no noise cleanup, so the effects start from the plain voice.
 * Nothing is played back through the speaker. Stopping releases the mic, so the phone's mic dot goes away.
 * A read error releases the mic too, and a later stop does not release it a second time.
 */
class MicEngine private constructor(
    private val voice: VoiceFx,
    private val captureFactory: () -> MicCapture,
) {
    constructor(voice: VoiceFx = VoiceFx()) : this(voice, { createPlatformCapture() })
    internal constructor(captureFactory: () -> MicCapture) : this(VoiceFx(), captureFactory)

    private val sinks = CopyOnWriteArrayList<PcmSink>()
    private var record: MicCapture? = null
    private var thread: Thread? = null
    private val captureLock = Any()

    @Volatile
    private var running = false

    var settings: VoiceSettings
        get() = voice.settings
        set(value) {
            voice.settings = value
        }

    val isRunning: Boolean get() = running

    /** Runs on the mic thread after the raw samples become floats, before Microphone Effects. */
    @Volatile
    var beforeVoice: ((FloatArray, Int) -> Unit)? = null

    /** Runs on the mic thread after Microphone Effects, with the same float block the recording uses. */
    @Volatile
    var afterVoice: ((FloatArray, Int) -> Unit)? = null

    fun addSink(sink: PcmSink) {
        sinks += sink
    }

    fun removeSink(sink: PcmSink) {
        sinks -= sink
    }

    /** Starts listening. Returns false when the phone would not give the mic. The app must already hold the mic permission. */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val created = try {
            captureFactory()
        } catch (error: SecurityException) {
            Log.w(TAG, "The mic was not allowed", error)
            return false
        } catch (error: UnsupportedOperationException) {
            Log.e(TAG, "The mic could not be set up", error)
            return false
        }
        if (!created.initialized) {
            created.release()
            return false
        }
        try {
            created.start()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "The mic would not start", error)
            created.release()
            return false
        }
        synchronized(captureLock) { record = created }
        running = true
        thread = Thread({ listen(created) }, "OffgridMic").also { it.start() }
        Log.i(TAG, "Mic started")
        return true
    }

    /** Stops listening and releases the mic. Safe to call more than once, and after a read error. */
    fun stop() {
        running = false
        val worker = thread
        if (worker !== Thread.currentThread()) worker?.join(JOIN_MS)
        thread = null
        val owned = synchronized(captureLock) { record }
        if (owned != null) release(owned)
    }

    private fun listen(source: MicCapture) {
        val raw = ShortArray(BLOCK_FRAMES)
        val work = FloatArray(BLOCK_FRAMES)
        try {
            while (running) {
                val read = source.read(raw, 0, BLOCK_FRAMES)
                if (read <= 0) {
                    Log.w(TAG, "The mic stopped giving sound ($read)")
                    running = false
                    return
                }
                for (i in 0 until read) work[i] = raw[i] / SCALE
                beforeVoice?.invoke(work, read)
                voice.process(work, read)
                afterVoice?.invoke(work, read)
                for (i in 0 until read) {
                    raw[i] = (work[i] * SCALE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                for (sink in sinks) sink.onPcm(raw, read)
            }
        } finally {
            running = false
            release(source)
        }
    }

    private fun release(source: MicCapture) {
        val owned = synchronized(captureLock) {
            if (record !== source) {
                false
            } else {
                record = null
                true
            }
        }
        if (!owned) return
        try {
            source.release()
        } catch (error: RuntimeException) {
            Log.w(TAG, "The mic was already released", error)
        }
        Log.i(TAG, "Mic released")
    }

    private companion object {
        @SuppressLint("MissingPermission")
        fun createPlatformCapture(): MicCapture {
            val minimum = AudioRecord.getMinBufferSize(VoiceFx.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) return UnavailableMicCapture
            val audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(VoiceFx.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimum, BLOCK_FRAMES * 2 * BUFFER_BLOCKS))
                .build()
            return PlatformMicCapture(audioRecord)
        }

        const val TAG = "OffgridMic"
        const val BLOCK_FRAMES = 480
        const val BUFFER_BLOCKS = 8
        const val SCALE = 32768f
        const val JOIN_MS = 500L
    }
}

private class PlatformMicCapture(private val audioRecord: AudioRecord) : MicCapture {
    override val initialized: Boolean get() = audioRecord.state == AudioRecord.STATE_INITIALIZED
    override fun start() = audioRecord.startRecording()
    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = audioRecord.read(buffer, offset, size)
    override fun release() = audioRecord.release()
}

private object UnavailableMicCapture : MicCapture {
    override val initialized = false
    override fun start() = Unit
    override fun read(buffer: ShortArray, offset: Int, size: Int) = 0
    override fun release() = Unit
}
