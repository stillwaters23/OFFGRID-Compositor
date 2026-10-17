package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayAudioTest {
    @Test
    fun micOffDropsBlocksAndMicOnKeepsThem() {
        val audio = ReplayAudio(FakeReplayEncoder())
        audio.offer(shortArrayOf(1), 1, 0)
        assertEquals(0, audio.keptCount())
        assertFalse(audio.micOn)
        assertTrue(audio.setMicOn(true))
        audio.offer(shortArrayOf(1, 2), 2, 1000)
        assertEquals(1, audio.keptCount())
        assertTrue(audio.micOn)
        audio.setMicOn(false)
        assertEquals(1, audio.keptCount())
        audio.offer(shortArrayOf(3), 1, 2000)
        assertEquals(1, audio.keptCount())
    }

    private class FakeReplayEncoder : ReplayPcmEncoder {
        override fun start() = true

        override fun accept(pcm: ShortArray, frames: Int, timeUs: Long): List<ReplayAudioPiece> =
            listOf(ReplayAudioPiece(ByteArray(frames), timeUs, 0))

        override fun format() = null

        override fun release() = Unit
    }
}
