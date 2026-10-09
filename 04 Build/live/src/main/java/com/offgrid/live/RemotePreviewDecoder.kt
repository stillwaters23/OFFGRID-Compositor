package com.offgrid.live

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemotePreviewDecoder(
    private val surface: Surface,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val frames = ArrayBlockingQueue<RemotePacket.Video>(QUEUE_SIZE)
    private var thread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        thread = Thread(::decode, "OffgridRemoteDecode").apply { start() }
    }

    fun queue(frame: RemotePacket.Video) {
        if (running.get() && !frames.offer(frame)) {
            onFailure("Remote preview fell behind.")
        }
    }

    private fun decode() {
        var decoder: MediaCodec? = null
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT)
            decoder.configure(format, surface, null, 0)
            decoder.start()
            val info = MediaCodec.BufferInfo()
            while (running.get()) {
                val frame = frames.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS) ?: continue
                val inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                if (inputIndex < 0) {
                    if (!frames.offer(frame)) onFailure("Remote preview fell behind.")
                    continue
                }
                val input = decoder.getInputBuffer(inputIndex)
                if (input == null) {
                    decoder.queueInputBuffer(inputIndex, 0, 0, frame.presentationTimeUs, 0)
                    continue
                }
                input.clear()
                if (frame.sample.size > input.remaining()) {
                    onFailure("Remote video frame is too large.")
                    decoder.queueInputBuffer(inputIndex, 0, 0, frame.presentationTimeUs, 0)
                    continue
                }
                input.put(frame.sample)
                decoder.queueInputBuffer(
                    inputIndex,
                    0,
                    frame.sample.size,
                    frame.presentationTimeUs,
                    frame.flags,
                )
                var outputIndex = decoder.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
                while (outputIndex >= 0) {
                    decoder.releaseOutputBuffer(outputIndex, true)
                    outputIndex = decoder.dequeueOutputBuffer(info, 0)
                }
            }
        } catch (error: IllegalStateException) {
            if (running.getAndSet(false)) onFailure("Remote preview could not play.")
        } catch (error: java.io.IOException) {
            if (running.getAndSet(false)) onFailure("Remote preview could not start.")
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            decoder?.let {
                try {
                    it.stop()
                } catch (_: IllegalStateException) {
                    Unit
                }
                it.release()
            }
        }
    }

    override fun close() {
        running.set(false)
        thread?.interrupt()
        thread = null
        frames.clear()
    }

    private companion object {
        const val WIDTH = 960
        const val HEIGHT = 540
        const val QUEUE_SIZE = 4
        const val POLL_TIMEOUT_MS = 40L
        const val CODEC_TIMEOUT_US = 10_000L
    }
}
