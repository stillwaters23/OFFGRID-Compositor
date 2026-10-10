package com.offgrid.compositor.audio.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android capture adapter for the lane-neutral [MicSource] contract.
 * Reads [VoiceSwapDsp.BLOCK_FRAMES] at a time (5 ms at 48 kHz) so the DSP sees a short block.
 */
class AndroidMicSource(private val context: Context) : MicSource {
    internal val hardBlock = MicHardBlock()

    @Volatile
    private var session: Session? = null

    override fun setHardBlock(blocked: Boolean) {
        hardBlock.setBlocked(blocked)
        if (blocked) stop()
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun start(onSamples: (FloatArray, Int) -> Unit, onFailure: () -> Unit): Boolean {
        if (!hardBlock.canOpen(hasRecordPermission())) return false
        if (session?.running == true) return true
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimum <= 0) return false
        val record = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimum, BLOCK_FRAMES * 2 * BUFFER_BLOCKS))
                .build()
        } catch (error: RuntimeException) {
            Log.e(TAG, "Mic capture could not be created", error)
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        try {
            record.startRecording()
        } catch (error: RuntimeException) {
            record.release()
            Log.e(TAG, "Mic capture could not start", error)
            return false
        }
        if (!hardBlock.markOpen()) {
            releaseQuietly(record)
            return false
        }
        val active = Session(record)
        session = active
        active.thread = Thread({
            val raw = ShortArray(BLOCK_FRAMES)
            val samples = FloatArray(BLOCK_FRAMES)
            try {
                while (active.running && hardBlock.mayDeliver()) {
                    val read = record.read(raw, 0, raw.size)
                    if (!active.running || !hardBlock.mayDeliver()) break
                    if (read <= 0) {
                        onFailure()
                        break
                    }
                    for (i in 0 until read) samples[i] = raw[i] / SCALE
                    if (!hardBlock.mayDeliver()) break
                    onSamples(samples, read)
                }
            } catch (error: RuntimeException) {
                if (active.running) {
                    Log.e(TAG, "Mic capture failed", error)
                    onFailure()
                }
            } finally {
                active.stop()
                if (session === active) session = null
            }
        }, "OffgridMic").also(Thread::start)
        return true
    }

    override fun stop() {
        hardBlock.markClosed()
        val active = session ?: return
        active.stop()
        if (Thread.currentThread() !== active.thread) {
            try {
                active.thread?.join(JOIN_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        active.release()
        if (session === active) session = null
    }

    private class Session(private val record: AudioRecord) {
        private val released = AtomicBoolean(false)

        @Volatile
        var running = true

        @Volatile
        var thread: Thread? = null

        fun stop() {
            running = false
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            } catch (_: IllegalStateException) {
                Unit
            }
            release()
        }

        fun release() {
            if (released.compareAndSet(false, true)) record.release()
        }
    }

    private companion object {
        const val TAG = "OffgridMic"
        const val SAMPLE_RATE = VoiceSwapDsp.SAMPLE_RATE
        const val BLOCK_FRAMES = VoiceSwapDsp.BLOCK_FRAMES
        const val BUFFER_BLOCKS = 2
        const val SCALE = 32768f
        const val JOIN_MS = 500L

        fun releaseQuietly(record: AudioRecord) {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            } catch (_: IllegalStateException) {
                Unit
            }
            record.release()
        }
    }
}
