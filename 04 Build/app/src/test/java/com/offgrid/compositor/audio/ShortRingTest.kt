package com.offgrid.compositor.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortRingTest {
    @Test
    fun readsWhatWasWrittenAcrossTheEnd() {
        val ring = ShortRing(8)
        val out = ShortArray(6)
        repeat(3) { round ->
            assertEquals(6, ring.write(ShortArray(6) { (round * 10 + it).toShort() }, 6))
            assertEquals(6, ring.read(out, 6))
            assertArrayEquals(ShortArray(6) { (round * 10 + it).toShort() }, out)
        }
    }

    @Test
    fun dropsTheExtraWhenFull() {
        val ring = ShortRing(4)
        assertEquals(4, ring.write(shortArrayOf(1, 2, 3, 4, 5, 6), 6))
        assertEquals(0, ring.write(shortArrayOf(7), 1))
        val out = ShortArray(8)
        assertEquals(4, ring.read(out, 8))
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), out.copyOf(4))
    }

    @Test
    fun skipKeepsTheNewestSamples() {
        val ring = ShortRing(16)
        ring.write(ShortArray(10) { it.toShort() }, 10)
        ring.skipTo(3)
        val out = ShortArray(3)
        assertEquals(3, ring.read(out, 3))
        assertArrayEquals(shortArrayOf(7, 8, 9), out)
    }
}
