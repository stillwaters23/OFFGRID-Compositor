package com.offgrid.compositor

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One encoded audio piece kept for the replay. */
internal class ReplayAudioPiece(val bytes: ByteArray, val timeUs: Long, val flags: Int)

/** Turns mic blocks into encoded audio. Tests supply their own. */
internal interface ReplayPcmEncoder {
    fun start(): Boolean
    fun accept(pcm: ShortArray, frames: Int, timeUs: Long): List<ReplayAudioPiece>
    fun format(): MediaFormat?
    fun release()
}

/**
 * Audio kept for the replay while the mic is on.
 * Blocks that arrive while the mic is off are dropped. Pieces from earlier stay until the replay trims them.
 */
internal class ReplayAudio(private val encoder: ReplayPcmEncoder) {
    private val lock = Any()
    private val kept = ArrayDeque<ReplayAudioPiece>()
    var micOn: Boolean = false
        private set

    fun setMicOn(on: Boolean): Boolean {
        synchronized(lock) {
            if (on == micOn) return micOn
            if (!on) {
                micOn = false
                encoder.release()
                return false
            }
            if (!encoder.start()) return false
            micOn = true
            return true
        }
    }

    fun offer(pcm: ShortArray, frames: Int, timeUs: Long) {
        synchronized(lock) {
            if (!micOn) return
            kept.addAll(encoder.accept(pcm, frames, timeUs))
        }
    }

    fun dropBefore(timeUs: Long) {
        synchronized(lock) {
            while (kept.size > 1 && kept.first().timeUs < timeUs) kept.removeFirst()
        }
    }

    fun keptCount(): Int = synchronized(lock) { kept.size }

    /** The encoded pieces and their format, or null when the mic has not produced audio. */
    fun slice(): Pair<MediaFormat, List<ReplayAudioPiece>>? = synchronized(lock) {
        val shape = encoder.format() ?: return null
        if (kept.isEmpty()) return null
        shape to kept.toList()
    }

    fun release() {
        synchronized(lock) {
            micOn = false
            encoder.release()
            kept.clear()
        }
    }
}

/** AAC-LC at 48 kHz, the same audio the recordings use. */
internal class AacReplayEncoder : ReplayPcmEncoder {
    private var codec: MediaCodec? = null
    private var shape: MediaFormat? = null

    override fun start(): Boolean {
        if (codec != null) return true
        return try {
            val created = MediaCodec.createEncoderByType(MIME)
            created.configure(audioFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            created.start()
            codec = created
            true
        } catch (error: RuntimeException) {
            Log.e(TAG, "Replay audio did not start", error)
            false
        }
    }

    override fun accept(pcm: ShortArray, frames: Int, timeUs: Long): List<ReplayAudioPiece> {
        val current = codec ?: return emptyList()
        val index = current.dequeueInputBuffer(TIMEOUT_US)
        if (index >= 0) {
            val input = current.getInputBuffer(index)
            if (input != null) {
                input.clear()
                val bytes = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN)
                bytes.asShortBuffer().put(pcm, 0, frames)
                input.put(bytes.array(), 0, frames * 2)
                current.queueInputBuffer(index, 0, frames * 2, timeUs, 0)
            }
        }
        return drain(current)
    }

    override fun format(): MediaFormat? = shape

    override fun release() {
        val current = codec ?: return
        codec = null
        shape = null
        try {
            current.stop()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Replay audio was already stopped", error)
        }
        current.release()
    }

    private fun drain(current: MediaCodec): List<ReplayAudioPiece> {
        val out = ArrayList<ReplayAudioPiece>()
        val info = MediaCodec.BufferInfo()
        while (true) {
            val index = current.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> shape = current.outputFormat
                index < 0 -> return out
                else -> {
                    val buffer = current.getOutputBuffer(index)
                    val config = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (buffer != null && !config && info.size > 0) {
                        val bytes = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.get(bytes)
                        out += ReplayAudioPiece(bytes, info.presentationTimeUs, info.flags)
                    }
                    current.releaseOutputBuffer(index, false)
                }
            }
        }
    }

    private fun audioFormat(): MediaFormat =
        MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT)
        }

    private companion object {
        const val TAG = "OffgridReplay"
        const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        const val SAMPLE_RATE = 48_000
        const val BIT_RATE = 128_000
        const val MAX_INPUT = 4096
        const val TIMEOUT_US = 10_000L
    }
}
