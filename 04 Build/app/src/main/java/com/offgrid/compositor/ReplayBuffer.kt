package com.offgrid.compositor

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.offgrid.compositor.audio.FileMuxerSink
import com.offgrid.compositor.audio.MicEngine
import com.offgrid.compositor.audio.PcmSink
import java.io.File
import java.nio.ByteBuffer

/**
 * The replay buffer (feature 7, Step 9; final build M4). It keeps the last moments of the
 * finished video, effects and all (never the panel), in the phone's memory, so Royalty can
 * save a clip without streaming again. While the mic is on, the same clip keeps an audio track.
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
    private val replayAudio = ReplayAudio(AacReplayEncoder())
    private val lock = Any()
    private var format: MediaFormat? = null
    private var micFeed: MicEngine? = null
    private var audioOriginUs: Long? = null
    private var audioFrames = 0L
    private val micSink = PcmSink { block, frames ->
        val copy = ShortArray(frames)
        block.copyInto(copy, endIndex = frames)
        handler.post { offerMic(copy, frames) }
    }

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
        arena.oldestTimeUs?.let { replayAudio.dropBefore(it) }
    }

    /** Listens to [engine] while the mic is on, and stops when it is null. The replay thread owns the encoder. */
    fun setMicFeed(engine: MicEngine?) {
        handler.post { attachMic(engine) }
    }

    private fun attachMic(engine: MicEngine?) {
        if (micFeed === engine) return
        micFeed?.removeSink(micSink)
        micFeed = engine
        if (engine == null) {
            replayAudio.setMicOn(false)
            return
        }
        if (replayAudio.setMicOn(true)) engine.addSink(micSink) else micFeed = null
    }

    private fun offerMic(block: ShortArray, frames: Int) {
        if (!replayAudio.micOn) return
        val timeUs = synchronized(lock) {
            val origin = audioOriginUs ?: arena.newestTimeUs ?: 0L
            if (audioOriginUs == null) audioOriginUs = origin
            val time = origin + audioFrames * MICROS / AUDIO_RATE
            audioFrames += frames
            time
        }
        replayAudio.offer(block, frames, timeUs)
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
        val video = ArrayList<ReplaySample>()
        val audio = ArrayList<ReplaySample>()
        var audioFormat: MediaFormat? = null
        val ready = synchronized(lock) {
            val newest = arena.newestTimeUs ?: return@synchronized false
            val from = newest - keepSeconds * MICROS
            val all = arena.snapshot()
            val start = all.indexOfLast { it.timeUs <= from && it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
                .takeIf { it >= 0 } ?: all.indexOfFirst { it.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 }
            if (start < 0) return@synchronized false
            val first = all[start].timeUs
            for (index in start until all.size) {
                val piece = all[index]
                val bytes = ByteArray(piece.size)
                arena.read(piece).get(bytes)
                video += ReplaySample(ByteBuffer.wrap(bytes), piece.timeUs - first, piece.flags, piece.size)
            }
            val slice = replayAudio.slice()
            if (slice != null) {
                audioFormat = slice.first
                for (piece in slice.second) {
                    val time = (piece.timeUs - first).coerceAtLeast(0L)
                    audio += ReplaySample(ByteBuffer.wrap(piece.bytes), time, piece.flags, piece.bytes.size)
                }
            }
            true
        }
        if (!ready) return false
        return try {
            ReplayClipWriter().write(FileMuxerSink(file.absolutePath), shape, video, audioFormat, audio)
        } catch (error: Exception) {
            Log.e(TAG, "Saving the replay failed", error)
            false
        }
    }

    fun release() {
        handler.post {
            attachMic(null)
            replayAudio.release()
            try {
                codec.stop()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "The replay encoder was already stopped", error)
            }
            codec.release()
            surface.release()
            synchronized(lock) {
                arena.clear()
                audioOriginUs = null
                audioFrames = 0L
            }
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
        private const val AUDIO_RATE = 48_000L
    }

}
