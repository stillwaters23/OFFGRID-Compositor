package com.offgrid.live

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class PcVideoEncoder(
    private val config: PcOutputConfig,
    private val host: OffgridHost,
    private val onSample: (PcSample) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var codec: MediaCodec? = null
    private var surface: Surface? = null
    private var started = false
    private var drain: Thread? = null

    fun start() {
        if (closed.get() || !running.compareAndSet(false, true)) return
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, config.width, config.height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, PcOutputConfig.FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, PcOutputConfig.KEYFRAME_INTERVAL_SECONDS)
            format.setInteger(MediaFormat.KEY_PRIORITY, 0)
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec = encoder
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val input = encoder.createInputSurface()
            encoder.start()
            started = true
            surface = input
            host.frameSurfaceSink().attach(OUTPUT_ID, input, config.width, config.height)
            drain = Thread({ drain(encoder) }, "OffgridPcVideo").apply { isDaemon = true; start() }
        } catch (error: IllegalStateException) {
            fail("PC video encoder could not start.")
        } catch (error: IOException) {
            fail("PC video encoder is not available.")
        } catch (error: SecurityException) {
            fail("PC video permission is missing.")
        }
    }

    private fun drain(encoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (running.get()) {
                val index = encoder.dequeueOutputBuffer(info, 10_000)
                if (index < 0) continue
                val buffer = encoder.getOutputBuffer(index)
                if (buffer != null && info.size > 0) {
                    val data = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.get(data)
                    publish(data, info.presentationTimeUs, info.flags)
                }
                encoder.releaseOutputBuffer(index, false)
            }
        } catch (error: IllegalStateException) {
            if (running.get()) fail("PC video encoder stopped.")
        }
    }

    private fun publish(data: ByteArray, timeUs: Long, flags: Int) {
        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            val pair = H264Payload.codecConfig(data) ?: return
            onSample(PcSample.VideoConfig(pair.first, pair.second))
            return
        }
        val units = H264Payload.nalUnits(data)
        if (units.isEmpty()) return
        val keyframe = flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 ||
            units.any { H264Payload.nalType(it) == 5 }
        onSample(PcSample.Video(units, timeUs, keyframe))
    }

    private fun fail(message: String) {
        running.set(false)
        main.post { onFailure(message) }
        releaseCodec()
    }

    override fun close() {
        running.set(false)
        if (!closed.compareAndSet(false, true)) return
        releaseCodec()
    }

    private fun releaseCodec() {
        drain?.join(300)
        drain = null
        val input = surface
        if (input != null) {
            try {
                host.frameSurfaceSink().detach(OUTPUT_ID)
            } catch (_: RuntimeException) {
                Unit
            }
            input.release()
        }
        surface = null
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
        started = false
        codec = null
    }

    companion object {
        const val OUTPUT_ID = "pc"
    }
}
