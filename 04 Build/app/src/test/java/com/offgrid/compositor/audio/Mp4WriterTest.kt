package com.offgrid.compositor.audio

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Mp4WriterTest {
    private class FakeSink : MuxerSink {
        val formats = ArrayList<String>()
        val events = ArrayList<String>()
        var released = false

        override fun addTrack(format: MediaFormat): Int {
            formats += format.getString(MediaFormat.KEY_MIME).orEmpty()
            return formats.size - 1
        }

        override fun start() {
            events += "start"
        }

        override fun write(track: Int, data: ByteBuffer, info: MediaCodec.BufferInfo) {
            events += "write $track ${info.presentationTimeUs} ${data.remaining()}"
        }

        override fun stop() {
            events += "stop"
        }

        override fun release() {
            released = true
        }
    }

    private fun format(mime: String) = MediaFormat().apply { setString(MediaFormat.KEY_MIME, mime) }

    private fun info(size: Int, timeUs: Long) = MediaCodec.BufferInfo().apply { set(0, size, timeUs, 0) }

    @Test
    fun startsOnlyWhenBothTracksAreKnownAndKeepsEarlyPieces() {
        val sink = FakeSink()
        val writer = Mp4Writer(sink)
        val video = writer.addTrack(format("video/avc"))
        writer.write(video, ByteBuffer.allocate(10), info(10, 0))
        assertTrue(sink.events.isEmpty())
        val audio = writer.addTrack(format("audio/mp4a-latm"))
        writer.write(audio, ByteBuffer.allocate(4), info(4, 21000))
        assertEquals(listOf("video/avc", "audio/mp4a-latm"), sink.formats)
        assertEquals(listOf("start", "write 0 0 10", "write 1 21000 4"), sink.events)
    }

    @Test
    fun finishClosesTheFileWithOneVideoAndOneAudioTrack() {
        val sink = FakeSink()
        val writer = Mp4Writer(sink)
        writer.addTrack(format("video/avc"))
        writer.addTrack(format("audio/mp4a-latm"))
        assertTrue(writer.finish())
        assertEquals("stop", sink.events.last())
        assertTrue(sink.released)
    }

    @Test
    fun finishReportsAFileWithoutBothTracks() {
        val sink = FakeSink()
        val writer = Mp4Writer(sink)
        writer.addTrack(format("video/avc"))
        assertFalse(writer.finish())
        assertTrue(sink.released)
        assertFalse(sink.events.contains("stop"))
    }
}
