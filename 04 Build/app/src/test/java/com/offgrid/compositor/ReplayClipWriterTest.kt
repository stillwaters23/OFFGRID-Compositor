package com.offgrid.compositor

import android.media.MediaCodec
import android.media.MediaFormat
import com.offgrid.compositor.audio.MuxerSink
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReplayClipWriterTest {
    @Test
    fun pictureOnlyReplayWritesOneTrack() {
        val sink = FakeSink()
        val video = listOf(sample(8, 0))
        assertTrue(ReplayClipWriter().write(sink, format("video/avc"), video, null, emptyList()))
        assertEquals(listOf("video/avc"), sink.formats)
        assertEquals(listOf("start", "write 0", "stop"), sink.events)
    }

    @Test
    fun micAudioAddsASecondTrack() {
        val sink = FakeSink()
        val video = listOf(sample(8, 0))
        val audio = listOf(sample(4, 1000))
        assertTrue(
            ReplayClipWriter().write(
                sink,
                format("video/avc"),
                video,
                format("audio/mp4a-latm"),
                audio,
            ),
        )
        assertEquals(listOf("video/avc", "audio/mp4a-latm"), sink.formats)
        assertEquals(listOf("start", "write 0", "write 1", "stop"), sink.events)
    }

    private fun format(mime: String) = MediaFormat().apply { setString(MediaFormat.KEY_MIME, mime) }

    private fun sample(size: Int, timeUs: Long) = ReplaySample(ByteBuffer.allocate(size), timeUs, 0, size)

    private class FakeSink : MuxerSink {
        val formats = ArrayList<String>()
        val events = ArrayList<String>()

        override fun addTrack(format: MediaFormat): Int {
            formats += format.getString(MediaFormat.KEY_MIME).orEmpty()
            return formats.size - 1
        }

        override fun start() {
            events += "start"
        }

        override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
            events += "write $track"
        }

        override fun stop() {
            events += "stop"
        }

        override fun release() = Unit
    }
}
