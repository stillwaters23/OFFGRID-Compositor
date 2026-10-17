package com.offgrid.compositor

import android.media.MediaCodec
import android.media.MediaFormat
import com.offgrid.compositor.audio.MuxerSink
import java.nio.ByteBuffer

/** One encoded piece ready to write into the replay file. */
internal class ReplaySample(val data: ByteBuffer, val timeUs: Long, val flags: Int, val size: Int)

/**
 * Writes the replay's picture, and its audio when the mic was on.
 * The audio track is left out when there is no audio, so a camera-only replay stays picture only.
 */
internal class ReplayClipWriter {
    fun write(
        sink: MuxerSink,
        videoFormat: MediaFormat,
        video: List<ReplaySample>,
        audioFormat: MediaFormat?,
        audio: List<ReplaySample>,
    ): Boolean {
        try {
            if (video.isEmpty()) return false
            val videoTrack = sink.addTrack(videoFormat)
            val audioTrack = if (audioFormat != null && audio.isNotEmpty()) sink.addTrack(audioFormat) else -1
            sink.start()
            val info = MediaCodec.BufferInfo()
            for (sample in video) {
                info.set(0, sample.size, sample.timeUs, sample.flags)
                sink.write(videoTrack, sample.data, info)
            }
            if (audioTrack >= 0) {
                for (sample in audio) {
                    info.set(0, sample.size, sample.timeUs, sample.flags)
                    sink.write(audioTrack, sample.data, info)
                }
            }
            sink.stop()
            return true
        } finally {
            try {
                sink.release()
            } catch (error: IllegalStateException) {
                // The file was already closed.
            }
        }
    }
}
