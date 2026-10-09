package com.offgrid.live

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class RemotePreviewEncoder(
    private val onEncodedFrame: (ByteArray, Long, Int) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private var codec: MediaCodec? = null
    private var drainThread: Thread? = null

    fun start(host: OffgridHost): Boolean {
        if (!running.compareAndSet(false, true)) return false
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_PRIORITY, 0)
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            }
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec = encoder
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val input = encoder.createInputSurface()
            encoder.start()
            host.frameSurfaceSink().attach(input, WIDTH, HEIGHT)
            drainThread = Thread({ drain(encoder) }, "OffgridRemoteVideo").apply { start() }
            return true
        } catch (error: IllegalStateException) {
            close()
            onFailure("Remote preview could not start.")
            return false
        } catch (error: SecurityException) {
            close()
            onFailure("Remote preview permission is missing.")
            return false
        }
    }

    private fun drain(encoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running.get()) {
            val index = encoder.dequeueOutputBuffer(info, 10_000)
            if (index >= 0) {
                val buffer = encoder.getOutputBuffer(index)
                if (buffer != null && info.size > 0) {
                    val data = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    buffer.get(data)
                    onEncodedFrame(data, info.presentationTimeUs, info.flags)
                }
                encoder.releaseOutputBuffer(index, false)
            }
        }
    }

    override fun close() {
        if (!running.getAndSet(false)) return
        drainThread?.join(500)
        drainThread = null
        codec?.let { encoder ->
            encoder.stop()
            encoder.release()
        }
        codec = null
    }

    private companion object {
        const val WIDTH = 960
        const val HEIGHT = 540
        const val BIT_RATE = 1_500_000
        const val FRAME_RATE = 24
    }
}
