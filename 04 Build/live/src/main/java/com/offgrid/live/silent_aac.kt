package com.offgrid.live

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class SilentAacEncoder(
    private val onSample: (PcSample) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var codec: MediaCodec? = null
    private var started = false
    private var thread: Thread? = null

    fun start() {
        if (closed.get() || !running.compareAndSet(false, true)) return
        try {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, FRAME_BYTES)
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec = encoder
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            started = true
            thread = Thread({ run(encoder) }, "OffgridPcAudio").apply { isDaemon = true; start() }
        } catch (error: IllegalStateException) {
            running.set(false)
            onFailure("Silent audio track could not start.")
        } catch (error: java.io.IOException) {
            running.set(false)
            onFailure("Silent audio track is not available.")
        }
    }

    private fun run(encoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var timeUs = 0L
        try {
            while (running.get()) {
                val input = encoder.dequeueInputBuffer(10_000)
                if (input >= 0) {
                    val buffer = encoder.getInputBuffer(input)
                    buffer?.clear()
                    buffer?.put(ByteArray(FRAME_BYTES))
                    encoder.queueInputBuffer(input, 0, FRAME_BYTES, timeUs, 0)
                    timeUs += FRAME_US
                }
                val output = encoder.dequeueOutputBuffer(info, 0)
                if (output >= 0) {
                    val data = read(encoder.getOutputBuffer(output), info)
                    if (data.isNotEmpty()) publish(data, info.presentationTimeUs, info.flags)
                    encoder.releaseOutputBuffer(output, false)
                }
            }
        } catch (error: IllegalStateException) {
            if (running.get()) onFailure("Silent audio track stopped.")
        }
    }

    private fun publish(data: ByteArray, timeUs: Long, flags: Int) {
        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            onSample(PcSample.AudioConfig(data))
        } else {
            onSample(PcSample.Audio(data, timeUs))
        }
    }

    private fun read(buffer: ByteBuffer?, info: MediaCodec.BufferInfo): ByteArray {
        if (buffer == null || info.size <= 0) return ByteArray(0)
        val data = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        buffer.get(data)
        return data
    }

    override fun close() {
        running.set(false)
        if (!closed.compareAndSet(false, true)) return
        thread?.join(300)
        codec?.let { encoder ->
            if (started) {
                try {
                    encoder.stop()
                } catch (_: IllegalStateException) {
                    Unit
                }
            }
            encoder.release()
        }
        codec = null
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        const val BITRATE = 128_000
        const val FRAME_BYTES = 2048
        const val FRAME_US = 1_024L * 1_000_000L / SAMPLE_RATE
    }
}
