package com.offgrid.compositor.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Listens to the mic: 48 kHz mono 16-bit, with Microphone Effects applied, and hands each block to the sinks.
 *
 * The source is VOICE_RECOGNITION. Of the sources every phone offers, it asks for the least processing:
 * no automatic gain and no noise cleanup, so the effects start from the plain voice.
 * Nothing is played back through the speaker. Stopping releases the mic, so the phone's mic dot goes away.
 */
class MicEngine(private val voice: VoiceFx = VoiceFx()) {
    private val sinks = CopyOnWriteArrayList<PcmSink>()
    private var recordRelease: MicRecordRelease? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    var settings: VoiceSettings
        get() = voice.settings
        set(value) {
            voice.settings = value
        }

    val isRunning: Boolean get() = running

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
        val minimum = AudioRecord.getMinBufferSize(VoiceFx.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) return false
        val created = try {
            AudioRecord.Builder()
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
        } catch (error: SecurityException) {
            Log.w(TAG, "The mic was not allowed", error)
            return false
        } catch (error: UnsupportedOperationException) {
            Log.e(TAG, "The mic could not be set up", error)
            return false
        }
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            created.release()
            return false
        }
        try {
            created.startRecording()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "The mic would not start", error)
            created.release()
            return false
        }
        val release = MicRecordRelease { created.release() }
        recordRelease = release
        running = true
        thread = Thread({ listen(created, release) }, "OffgridMic").also { it.start() }
        Log.i(TAG, "Mic started")
        return true
    }

    /** Stops listening and releases the mic. */
    fun stop() {
        running = false
        recordRelease?.stop()
        recordRelease = null
        thread?.join(JOIN_MS)
        thread = null
        Log.i(TAG, "Mic released")
    }

    private fun listen(source: AudioRecord, release: MicRecordRelease) {
        val raw = ShortArray(BLOCK_FRAMES)
        val work = FloatArray(BLOCK_FRAMES)
        while (running) {
            val read = source.read(raw, 0, BLOCK_FRAMES)
            if (read <= 0) {
                Log.w(TAG, "The mic stopped giving sound ($read)")
                running = false
                release.readFailed()
                return
            }
            for (i in 0 until read) work[i] = raw[i] / SCALE
            voice.process(work, read)
            for (i in 0 until read) raw[i] = (work[i] * SCALE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            for (sink in sinks) sink.onPcm(raw, read)
        }
    }

    private companion object {
        const val TAG = "OffgridMic"
        const val BLOCK_FRAMES = 480
        const val BUFFER_BLOCKS = 8
        const val SCALE = 32768f
        const val JOIN_MS = 500L
    }
}

internal class MicRecordRelease(private val releaseRecord: () -> Unit) {
    private var released = false

    @Synchronized
    fun readFailed() = releaseOnce()

    @Synchronized
    fun stop() = releaseOnce()

    private fun releaseOnce() {
        if (released) return
        released = true
        releaseRecord()
    }
}
