package com.offgrid.compositor.ui.wheel

import org.junit.Assert.assertEquals
import org.junit.Test

class WheelSnapMathTest {
    @Test
    fun snapsToTheNearestWholeNotch() {
        assertEquals(14, WheelSnapMath.snap(14.49f))
        assertEquals(15, WheelSnapMath.snap(14.5f))
        assertEquals(87, WheelSnapMath.snap(87.1f))
    }

    @Test
    fun clampsSnappedValuesToTheReelRange() {
        assertEquals(0, WheelSnapMath.snap(-12.2f))
        assertEquals(100, WheelSnapMath.snap(105.6f))
        assertEquals(0, WheelSnapMath.clamp(-1))
        assertEquals(100, WheelSnapMath.clamp(101))
    }
}
