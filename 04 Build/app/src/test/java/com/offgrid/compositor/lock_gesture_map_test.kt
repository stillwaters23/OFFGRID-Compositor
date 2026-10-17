package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V2 section 10: the lock gesture rejects any start outside the middle 60 percent; no edge gestures. */
class LockGestureMapTest {
    private val width = 1440
    private val height = 3120

    private fun twoFingers() = FingerHoldRecognizer(fingers = 2, slopPx = 24f, zone = LockGestureMap::inCenterZone)

    private fun feed(
        recognizer: FingerHoldRecognizer,
        kind: TouchKind,
        vararg points: Pair<Float, Float>,
    ): HoldEvent = recognizer.feed(
        kind,
        points.size,
        FloatArray(points.size) { points[it].first },
        FloatArray(points.size) { points[it].second },
        width,
        height,
    )

    @Test
    fun centerZoneIsTheMiddleSixtyPercentInBothAxes() {
        val zone = LockGestureMap.centerZone(width, height)
        assertEquals(288, zone[0])
        assertEquals(624, zone[1])
        assertEquals(1152, zone[2])
        assertEquals(2496, zone[3])
        assertTrue(LockGestureMap.inCenterZone(720f, 1560f, width, height))
        assertFalse("left edge", LockGestureMap.inCenterZone(10f, 1560f, width, height))
        assertFalse("top edge", LockGestureMap.inCenterZone(720f, 10f, width, height))
        assertFalse("right edge", LockGestureMap.inCenterZone(1430f, 1560f, width, height))
        assertFalse("bottom edge", LockGestureMap.inCenterZone(720f, 3110f, width, height))
        assertFalse("just outside", LockGestureMap.inCenterZone(287f, 1560f, width, height))
        assertFalse("no size yet", LockGestureMap.inCenterZone(1f, 1f, 0, 0))
    }

    @Test
    fun twoFingersInTheCenterStartAHold() {
        val hold = twoFingers()
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.DOWN, 700f to 1500f))
        assertEquals(HoldEvent.HOLD_STARTED, feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f))
        assertTrue(hold.holding)
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.MOVE, 705f to 1503f, 798f to 1604f))
        assertTrue(hold.holding)
    }

    @Test
    fun aStartOutsideTheCenterZoneNeverHolds() {
        val hold = twoFingers()
        feed(hold, TouchKind.DOWN, 700f to 1500f)
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 1400f to 1600f))
        assertFalse(hold.holding)
        val fromTop = twoFingers()
        feed(fromTop, TouchKind.DOWN, 700f to 40f)
        assertEquals(HoldEvent.NONE, feed(fromTop, TouchKind.POINTER_DOWN, 700f to 40f, 800f to 60f))
        assertFalse(fromTop.holding)
    }

    @Test
    fun oneFingerNeverLocks() {
        val hold = twoFingers()
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.DOWN, 700f to 1500f))
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.MOVE, 700f to 1500f))
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.UP, 700f to 1500f))
        assertFalse(hold.holding)
    }

    @Test
    fun slidingLiftingOrAThirdFingerCancels() {
        val slide = twoFingers()
        feed(slide, TouchKind.DOWN, 700f to 1500f)
        feed(slide, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f)
        assertEquals(HoldEvent.HOLD_CANCELLED, feed(slide, TouchKind.MOVE, 760f to 1500f, 800f to 1600f))
        assertFalse(slide.holding)

        val lift = twoFingers()
        feed(lift, TouchKind.DOWN, 700f to 1500f)
        feed(lift, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f)
        assertEquals(HoldEvent.HOLD_CANCELLED, feed(lift, TouchKind.POINTER_UP, 700f to 1500f, 800f to 1600f))

        val third = twoFingers()
        feed(third, TouchKind.DOWN, 700f to 1500f)
        feed(third, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f)
        assertEquals(
            HoldEvent.HOLD_CANCELLED,
            feed(third, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f, 900f to 1700f),
        )
        assertFalse(third.holding)
    }

    @Test
    fun aFiredHoldIgnoresTheRestOfTheTouchAndResetsWhenFingersLift() {
        val hold = twoFingers()
        feed(hold, TouchKind.DOWN, 700f to 1500f)
        feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f)
        hold.fired()
        assertTrue(hold.consumed)
        assertFalse(hold.holding)
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.POINTER_UP, 700f to 1500f, 800f to 1600f))
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f))
        assertFalse("no second hold inside the same touch", hold.holding)
        feed(hold, TouchKind.UP, 700f to 1500f)
        assertFalse(hold.consumed)
        feed(hold, TouchKind.DOWN, 700f to 1500f)
        assertEquals(HoldEvent.HOLD_STARTED, feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f))
    }

    @Test
    fun panelOpenZoneIsTheTopLeftCornerInsetFromTheEdges() {
        val inset = 150
        val zone = LockGestureMap.panelOpenZone(width, height, inset)
        assertEquals(inset, zone[0])
        assertEquals(inset, zone[1])
        assertEquals(432, zone[2])
        assertEquals(936, zone[3])
        assertTrue(LockGestureMap.inPanelOpenZone(300f, 500f, width, height, inset))
        assertFalse("on the left edge", LockGestureMap.inPanelOpenZone(20f, 500f, width, height, inset))
        assertFalse("on the top edge", LockGestureMap.inPanelOpenZone(300f, 20f, width, height, inset))
        assertFalse("middle of the stage", LockGestureMap.inPanelOpenZone(720f, 1560f, width, height, inset))
    }

    @Test
    fun oneFingerHoldInTheTopLeftZoneOpensThePanelAndASecondFingerCancelsIt() {
        val open = FingerHoldRecognizer(fingers = 1, slopPx = 24f) { x, y, w, h ->
            LockGestureMap.inPanelOpenZone(x, y, w, h, 150)
        }
        assertEquals(HoldEvent.HOLD_STARTED, feed(open, TouchKind.DOWN, 300f to 500f))
        assertEquals(HoldEvent.HOLD_CANCELLED, feed(open, TouchKind.POINTER_DOWN, 300f to 500f, 400f to 600f))
        feed(open, TouchKind.UP, 300f to 500f)
        assertEquals(HoldEvent.NONE, feed(open, TouchKind.DOWN, 720f to 1560f))
    }

    @Test
    fun aHoldMeansWhatItMeantWhenTheFingersLanded() {
        assertEquals(HoldOutcome.LOCK, LockGestureMap.holdOutcome(lockedAtStart = false, lockedNow = false))
        assertEquals(HoldOutcome.UNLOCK, LockGestureMap.holdOutcome(lockedAtStart = true, lockedNow = true))
        assertEquals(
            "Phone 2 unlocked under the fingers: the hold must not lock again",
            HoldOutcome.NONE,
            LockGestureMap.holdOutcome(lockedAtStart = true, lockedNow = false),
        )
        assertEquals(
            "the lock came on under the fingers: the hold must not unlock it",
            HoldOutcome.NONE,
            LockGestureMap.holdOutcome(lockedAtStart = false, lockedNow = true),
        )
    }

    @Test
    fun everyCornerAndEdgeOfTheStageRejectsALockStart() {
        val corners = listOf(0f to 0f, width.toFloat() to 0f, 0f to height.toFloat(), width.toFloat() to height.toFloat())
        for ((x, y) in corners) assertFalse("corner $x,$y", LockGestureMap.inCenterZone(x, y, width, height))
        val hold = twoFingers()
        feed(hold, TouchKind.DOWN, 700f to 1500f)
        assertEquals(
            "one finger in the middle, one on the right edge",
            HoldEvent.NONE,
            feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, (width - 1).toFloat() to 1500f),
        )
        assertFalse(hold.holding)
    }

    @Test
    fun anUnlockHoldStartsLikeALockHoldAndKeepsHoldingStill() {
        // Locked or not, the recognizer sees the same two-finger hold. The store decides lock versus unlock.
        val hold = twoFingers()
        feed(hold, TouchKind.DOWN, 700f to 1500f)
        assertEquals(HoldEvent.HOLD_STARTED, feed(hold, TouchKind.POINTER_DOWN, 700f to 1500f, 800f to 1600f))
        assertEquals(HoldEvent.NONE, feed(hold, TouchKind.MOVE, 710f to 1505f, 805f to 1596f))
        assertEquals(HoldEvent.HOLD_CANCELLED, feed(hold, TouchKind.CANCEL, 710f to 1505f, 805f to 1596f))
    }
}
