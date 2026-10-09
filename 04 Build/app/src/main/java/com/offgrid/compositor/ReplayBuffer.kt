package com.offgrid.compositor

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

/**
 * The replay buffer (feature 7, Step 9; final build M4). It keeps the last moments of the
 * finished video, effects and all (never the panel), in the phone's memory, so Royalty can
 * save a clip without streaming again. It has no sound, because this app never uses the mic.
 *
 * The camera stage draws every finished picture into [surface] too. The phone's video chip
 * packs them, and the newest [keepSeconds] are kept. Saving writes them to a file.
 */
internal class ReplayBuffer(val width: Int, val height: Int, @Volatile var keepSeconds: Int) {
    private val thread = HandlerThread("OffgridReplay").apply { start() }
    private val handler = Handler(thread.looper)
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MIME)
    private val samples = ArrayDeque<Sample>()
    private val lock = Any()
    private var format: MediaFormat? = null

    /** The encoder's input. The camera stage draws into it. */
    val surface: Surface

    private class Sample(val data: ByteArray, val timeUs: Long, val flags: Int)

    init {
        val config = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEYFRAME_SECONDS)
        }
        codec.setCallback(
            object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    keep(codec, index, info)
                }

                override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) {
                    Log.e(TAG, "The replay encoder stopped", error)
                }

                override fun onOutputFormatChanged(codec: MediaCodec, next: MediaFormat) {
                    synchronized(lock) { format = next }
                }
            },
            handler,
        )
        codec.configure(config, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = codec.createInputSurface()
        codec.start()
    }

    private fun keep(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        try {
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                val data = ByteArray(info.size)
                buffer.position(info.offset)
                buffer.get(data, 0, info.size)
                synchronized(lock) {
                    samples.addLast(Sample(data, info.presentationTimeUs, info.flags))
                    trim()
                }
            }
            codec.releaseOutputBuffer(index, false)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "A replay picture was dropped", error)
        }
    }

    /** Lets go of pictures older than the kept time, always keeping a whole start picture. */
    private fun trim() {
        val newest = samples.lastOrNull()?.timeUs ?: return
        val oldest = newest - (keepSeconds + KEYFRAME_SECONDS) * MICROS
        while (samples.size > 1 && samples.first().timeUs < oldest) samples.removeFirst()
    }

    /** Writes the kept moments to [file]. Returns false if nothing is kept yet. Call off the main thread. */
    fun save(file: File): Boolean {
        val copy: List<Sample>
        val shape: MediaFormat
        synchronized(lock) {
            shape = format ?: return false
            val newest = samples.lastOrNull()?.timeUs ?: return false
            val from = newest - keepSeconds * MICROS
            // Start at the last whole picture at or before the start time.
            val all = samples.toList()
            val start = all.indexOfLast { it.timeUs <= from && it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
                .takeIf { it >= 0 } ?: all.indexOfFirst { it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
            if (start < 0) return false
            copy = all.subList(start, all.size)
        }
        if (copy.isEmpty()) return false
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        return try {
            val track = muxer.addTrack(shape)
            muxer.start()
            val info = MediaCodec.BufferInfo()
            val first = copy.first().timeUs
            for (sample in copy) {
                info.set(0, sample.data.size, sample.timeUs - first, sample.flags)
                muxer.writeSampleData(track, ByteBuffer.wrap(sample.data), info)
            }
            muxer.stop()
            true
        } catch (error: Exception) {
            Log.e(TAG, "Saving the replay failed", error)
            false
        } finally {
            try {
                muxer.release()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "The replay file was already closed", error)
            }
        }
    }

    fun release() {
        handler.post {
            try {
                codec.stop()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "The replay encoder was already stopped", error)
            }
            codec.release()
            surface.release()
            synchronized(lock) { samples.clear() }
            thread.quitSafely()
        }
    }

    private companion object {
        const val TAG = "OffgridReplay"
        const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        const val BIT_RATE = 4_000_000
        const val FRAME_RATE = 30
        const val KEYFRAME_SECONDS = 1
        const val MICROS = 1_000_000L
    }
}
