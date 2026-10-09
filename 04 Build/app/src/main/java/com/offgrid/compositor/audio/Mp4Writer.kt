package com.offgrid.compositor.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/** Where finished video and audio pieces go. The phone's muxer is the real one. */
interface MuxerSink {
    fun addTrack(format: MediaFormat): Int

    fun start()

    fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo)

    fun stop()

    fun release()
}

/** The phone's MP4 muxer. */
class FileMuxerSink(path: String) : MuxerSink {
    private val muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    override fun addTrack(format: MediaFormat): Int = muxer.addTrack(format)

    override fun start() = muxer.start()

    override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) = muxer.writeSampleData(track, data, info)

    override fun stop() = muxer.stop()

    override fun release() = muxer.release()
}

/**
 * Writes the video track and the audio track into one MP4. The muxer only starts once both
 * tracks are known, so anything that arrives earlier waits here. Safe to call from two threads.
 */
class Mp4Writer(private val sink: MuxerSink) {
    private class Waiting(val track: Int, val data: ByteBuffer, val info: MediaCodec.BufferInfo)

    private val waiting = ArrayList<Waiting>()
    private var waitingBytes = 0
    private var tracks = 0
    private var started = false
    private var finished = false

    @Synchronized
    fun addTrack(format: MediaFormat): Int {
        val track = sink.addTrack(format)
        tracks++
        if (tracks == TRACKS) {
            sink.start()
            started = true
            for (piece in waiting) sink.write(piece.track, piece.data, piece.info)
            waiting.clear()
            waitingBytes = 0
        }
        return track
    }

    @Synchronized
    fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (finished || info.size <= 0) return
        if (started) {
            data.limit(info.offset + info.size)
            data.position(info.offset)
            sink.write(track, data, info)
            return
        }
        if (waitingBytes + info.size > MAX_WAITING_BYTES) {
            Log.w(TAG, "The other track is late. A piece was dropped.")
            return
        }
        val copy = ByteBuffer.allocate(info.size)
        data.limit(info.offset + info.size)
        data.position(info.offset)
        copy.put(data)
        copy.flip()
        val saved = MediaCodec.BufferInfo().apply { set(0, info.size, info.presentationTimeUs, info.flags) }
        waiting += Waiting(track, copy, saved)
        waitingBytes += info.size
    }

    /** Closes the file. Returns true when both tracks were written. */
    @Synchronized
    fun finish(): Boolean {
        if (finished) return false
        finished = true
        val whole = started
        try {
            if (whole) sink.stop()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "The video file could not be closed", error)
            return false
        } finally {
            sink.release()
        }
        return whole
    }

    private companion object {
        const val TAG = "OffgridAv"
        const val TRACKS = 2
        const val MAX_WAITING_BYTES = 16 * 1024 * 1024
    }
}
