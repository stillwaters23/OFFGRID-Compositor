package com.offgrid.compositor

import com.offgrid.compositor.effects.FxKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fail-closed mask rules (Lane E). A missing or stale person mask must hide the
 * picture. These run on the computer that builds the app, not on the phone.
 */
class MaskGateTest {
    private fun frame(
        width: Int = 2,
        height: Int = 2,
        publishedAtNs: Long = 1_000L,
        bytes: ByteArray? = null,
    ): MaskFrame {
        val pixels = if (width > 0 && height > 0) width * height else 0
        return MaskFrame(bytes ?: ByteArray(pixels), width, height, publishedAtNs)
    }

    @Test
    fun aMissingMaskFailsClosed() {
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(null, nowNs = 1_000L))
    }

    @Test
    fun aBrokenMaskFailsClosed() {
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(frame(width = 0, height = 2, bytes = ByteArray(0)), 1_000L))
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(frame(width = 2, height = 0, bytes = ByteArray(0)), 1_000L))
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(frame(width = -1, height = 2, bytes = ByteArray(0)), 1_000L))
        val short = MaskFrame(ByteArray(3), width = 2, height = 2, publishedAtNs = 1_000L)
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(short, 1_000L))
    }

    @Test
    fun aClockThatRunsBackwardFailsClosed() {
        val future = frame(publishedAtNs = 5_000L)
        assertEquals(MaskVerdict.FAIL_CLOSED, MaskGate.verdict(future, nowNs = 4_000L))
    }

    @Test
    fun aFreshMaskIsAppliedAndAStaleMaskFailsClosed() {
        val published = 10_000L
        val fresh = frame(publishedAtNs = published)
        assertEquals(MaskVerdict.APPLY, MaskGate.verdict(fresh, published))
        assertEquals(MaskVerdict.APPLY, MaskGate.verdict(fresh, published + MaskGate.FRAME_NS))
        assertEquals(
            "a mask exactly two frames old is the last one still trusted",
            MaskVerdict.APPLY,
            MaskGate.verdict(fresh, published + MaskGate.STALE_AFTER_NS),
        )
        assertEquals(
            MaskVerdict.FAIL_CLOSED,
            MaskGate.verdict(fresh, published + MaskGate.STALE_AFTER_NS + 1),
        )
    }

    @Test
    fun theStaleLimitIsTwoFramesAtThirtyFps() {
        assertEquals(30, MaskGate.TARGET_FPS)
        assertEquals(1_000_000_000L / 30, MaskGate.FRAME_NS)
        assertEquals(MaskGate.FRAME_NS * 2, MaskGate.STALE_AFTER_NS)
        assertTrue(MaskGate.STALE_AFTER_NS < 1_000_000_000L / 10)
    }

    @Test
    fun onlyBodyModesHideTheFace() {
        assertFalse(MaskGate.hidesFace(FxKeys.BODY_OFF))
        assertFalse(MaskGate.hidesFace(""))
        assertFalse(MaskGate.hidesFace("live"))
        assertTrue(MaskGate.hidesFace(FxKeys.BODY_SEE))
        assertTrue(MaskGate.hidesFace(FxKeys.BODY_REPLACE))
        assertTrue(MaskGate.hidesFace(FxKeys.BODY_CLOAK))
        assertTrue(MaskGate.hidesFace(FxKeys.BODY_HOLO))
    }

    @Test
    fun thirtyFpsMeansTheMaskWorkFinishesInsideOneFrameAndOffTheUiThread() {
        assertTrue(MaskGate.holdsFrameBudget(0L))
        assertTrue(MaskGate.holdsFrameBudget(MaskGate.FRAME_NS))
        assertFalse(MaskGate.holdsFrameBudget(MaskGate.FRAME_NS + 1))
        assertFalse(MaskGate.holdsFrameBudget(-1L))
        assertTrue(MaskGate.workStaysOffUiThread(onUiThread = false))
        assertFalse(MaskGate.workStaysOffUiThread(onUiThread = true))
    }

    @Test
    fun packingFlipsTheTopRowToTheBottomAndDropsBadSamples() {
        val packed = MaskPack.pack(
            floatArrayOf(
                1f, 0f,
                0.5f, Float.NaN,
            ),
            width = 2,
            height = 2,
        )
        assertEquals(4, packed!!.size)
        assertEquals(128.toByte(), packed[0])
        assertEquals(0.toByte(), packed[1])
        assertEquals(255.toByte(), packed[2])
        assertEquals(0.toByte(), packed[3])
        assertNull(MaskPack.pack(floatArrayOf(1f), width = 2, height = 2))
        assertNull(MaskPack.pack(floatArrayOf(1f), width = 0, height = 1))
    }
}
