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

/**
 * The replay buffer (feature 7, Step 9; final build M4). It keeps the last moments of the
 * finished video, effects and all (never the panel), in the phone's memory, so Royalty can
 * save a clip without streaming again. It keeps the picture only, with no sound.
 *
 * The camera stage draws every finished picture into [surface] too. The phone's video chip
 * packs them, and the newest [keepSeconds] are kept. They live in one fixed block of
 * [MEMORY_BYTES] (64 MB), so a busy video keeps fewer seconds ([heldSeconds]) instead of using more memory.
 * Saving writes them to a file.
 */
internal class ReplayBuffer(val width: Int, val height: Int, @Volatile var keepSeconds: Int) {
    private val thread = HandlerThread("OffgridReplay").apply { start() }
    private val handler = Handler(thread.looper)
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MIME)
    private val arena = SampleArena(MEMORY_BYTES)
    private val lock = Any()
    private var format: MediaFormat? = null

    /** The encoder's input. The camera stage draws into it. */
    val surface: Surface

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
                buffer.limit(info.offset + info.size)
                buffer.position(info.offset)
                synchronized(lock) {
                    arena.add(buffer, info.size, info.presentationTimeUs, info.flags)
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
        val newest = arena.newestTimeUs ?: return
        arena.dropBefore(newest - (keepSeconds + KEYFRAME_SECONDS) * MICROS)
    }

    /** How many seconds are kept right now. It is lower than [keepSeconds] when the 64 MB block is full. */
    val heldSeconds: Int
        get() = synchronized(lock) {
            val newest = arena.newestTimeUs
            val oldest = arena.oldestTimeUs
            if (newest == null || oldest == null) 0 else ((newest - oldest) / MICROS).toInt()
        }

    /** The seconds actually kept when the 64 MB block, not the typed time, is what limits the replay. Otherwise null. */
    val memoryLimitSeconds: Int?
        get() = synchronized(lock) {
            if (arena.overwritten == 0L) null else heldSeconds.takeIf { it < keepSeconds }
        }

    /** Writes the kept moments to [file]. Returns false if nothing is kept yet. Call off the main thread. */
    fun save(file: File): Boolean {
        val shape = synchronized(lock) { format } ?: return false
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        return try {
            val track = muxer.addTrack(shape)
            // The lock is held while writing, so the block is not rewritten under the save. New pictures
            // wait a moment, which only matters to the replay, never to the live video.
            synchronized(lock) {
                val newest = arena.newestTimeUs ?: return false
                val from = newest - keepSeconds * MICROS
                // Start at the last whole picture at or before the start time.
                val all = arena.snapshot()
                val start = all.indexOfLast { it.timeUs <= from && it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
                    .takeIf { it >= 0 } ?: all.indexOfFirst { it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
                if (start < 0) return false
                muxer.start()
                val info = MediaCodec.BufferInfo()
                val first = all[start].timeUs
                for (index in start until all.size) {
                    val piece = all[index]
                    info.set(0, piece.size, piece.timeUs - first, piece.flags)
                    muxer.writeSampleData(track, arena.read(piece), info)
                }
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
            synchronized(lock) { arena.clear() }
            thread.quitSafely()
        }
    }

    companion object {
        /** The most memory the replay buffer uses for kept pictures: 100 seconds at 4 Mbps with room to spare. */
        const val MEMORY_BYTES = 64 * 1024 * 1024
        private const val TAG = "OffgridReplay"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val BIT_RATE = 4_000_000
        private const val FRAME_RATE = 30
        private const val KEYFRAME_SECONDS = 1
        private const val MICROS = 1_000_000L
    }

}
