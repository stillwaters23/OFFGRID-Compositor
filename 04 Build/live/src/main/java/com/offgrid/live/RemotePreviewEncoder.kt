package com.offgrid.live

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RemotePreviewEncoder(
    private val onEncodedFrame: (ByteArray, Long, Int) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val codecWorker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "OffgridRemoteCodec").apply { isDaemon = true }
    }
    private var codec: MediaCodec? = null
    private var drainThread: Thread? = null
    private var inputSurface: Surface? = null
    private var frameSink: FrameSink? = null
    private var codecStarted = false

    @Synchronized
    fun start(host: OffgridHost, onStarted: (Boolean) -> Unit) {
        if (closed.get() || !running.compareAndSet(false, true)) {
            mainHandler.post { onStarted(false) }
            return
        }
        codecWorker.execute {
            val started = configure(host)
            mainHandler.post { onStarted(started) }
        }
    }

    private fun configure(host: OffgridHost): Boolean {
        if (!running.get()) return false
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
            codecStarted = true
            frameSink = host.frameSurfaceSink()
            inputSurface = input
            frameSink?.attach(OUTPUT_ID, input, WIDTH, HEIGHT)
            drainThread = Thread({ drain(encoder) }, "OffgridRemoteVideo").apply { start() }
            return true
        } catch (error: IllegalStateException) {
            running.set(false)
            releaseCodec()
            onFailure("Remote preview could not start.")
            return false
        } catch (error: SecurityException) {
            running.set(false)
            releaseCodec()
            onFailure("Remote preview permission is missing.")
            return false
        } catch (error: IOException) {
            running.set(false)
            releaseCodec()
            onFailure("Remote preview encoder is not available.")
            return false
        }
    }

    private fun drain(encoder: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (running.get()) {
                val index = encoder.dequeueOutputBuffer(info, 10_000)
                if (index >= 0) {
                    val buffer = encoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        val data = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.get(data)
                        try {
                            onEncodedFrame(data, info.presentationTimeUs, info.flags)
                        } catch (error: IllegalStateException) {
                            running.set(false)
                            onFailure("Remote preview send failed.")
                        } catch (error: java.io.IOException) {
                            running.set(false)
                            onFailure("Remote preview send failed.")
                        }
                    }
                    encoder.releaseOutputBuffer(index, false)
                }
            }
        } catch (error: IllegalStateException) {
            if (running.getAndSet(false)) onFailure("Remote preview could not encode.")
        }
    }

    @Synchronized
    override fun close() {
        running.set(false)
        if (!closed.compareAndSet(false, true)) return
        codecWorker.execute {
            releaseCodec()
            codecWorker.shutdown()
        }
    }

    private fun releaseCodec() {
        drainThread?.join(500)
        drainThread = null
        val surface = inputSurface
        if (surface != null) {
            frameSink?.detach(OUTPUT_ID)
            surface.release()
        }
        inputSurface = null
        frameSink = null
        codec?.let { encoder ->
            if (codecStarted) {
                try {
                    encoder.stop()
                } catch (_: IllegalStateException) {
                    Unit
                }
            }
            encoder.release()
        }
        codecStarted = false
        codec = null
    }

    private companion object {
        const val OUTPUT_ID = "remote"
        const val WIDTH = 960
        const val HEIGHT = 540
        const val BIT_RATE = 1_500_000
        const val FRAME_RATE = 24
    }
}
