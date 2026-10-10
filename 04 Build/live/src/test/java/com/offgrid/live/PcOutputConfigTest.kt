package com.offgrid.live

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcOutputConfigTest {
    @Test
    fun rejectsPublicAddressesAndInvalidPorts() {
        val publicAddress = InetAddress.getByName("8.8.8.8")
        val privateAddress = InetAddress.getByName("192.168.1.20")
        assertFalse(
            runCatching {
                PcOutputConfig.create("srt", "public.example", "9000", "720p", "6000000") {
                    arrayOf(publicAddress)
                }
            }.isSuccess,
        )
        assertFalse(
            runCatching {
                PcOutputConfig.create("srt", "192.168.1.20", "65536", "720p", "6000000") {
                    arrayOf(privateAddress)
                }
            }.isSuccess,
        )
    }

    @Test
    fun createsLocalSrtAndRtmpUrlsAndEncoderSettings() {
        val local = InetAddress.getByName("192.168.1.20")
        val srt = PcOutputConfig.create("srt", "pc.local", "9000", "720p", "6000000") {
            arrayOf(local)
        }
        assertEquals("srt://192.168.1.20:9000?mode=caller&streamid=live/offgrid", srt.url)
        assertEquals(1280, srt.width)
        assertEquals(720, srt.height)
        assertEquals(6_000_000, srt.bitrate)
        assertEquals(30, PcOutputConfig.FRAME_RATE)
        assertEquals(1, PcOutputConfig.KEYFRAME_INTERVAL_SECONDS)

        val rtmp = PcOutputConfig.create("rtmp", "192.168.1.20", "1935", "1080p", "6000000") {
            arrayOf(local)
        }
        assertEquals("rtmp://192.168.1.20:1935/live/offgrid", rtmp.url)
        assertEquals(1920, rtmp.width)
        assertEquals(1080, rtmp.height)
    }

    @Test
    fun reconnectBackoffDoublesCapsAndResets() {
        val backoff = ReconnectBackoff()
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 15_000L), List(6) { backoff.nextDelayMillis() })
        backoff.reset()
        assertEquals(1_000L, backoff.nextDelayMillis())
        assertTrue(PcOutputConfig.DEFAULT_BITRATE > 0)
    }
}
