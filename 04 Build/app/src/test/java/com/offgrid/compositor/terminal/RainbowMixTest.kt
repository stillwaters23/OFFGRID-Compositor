package com.offgrid.compositor.terminal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Checks the six-color mixer in the theme office. It runs on the computer, not the phone. */
class RainbowMixTest {

    private fun only(index: Int) = IntArray(6).also { it[index] = 100 }

    @Test
    fun eachColorAtFullStrength() {
        val expected = listOf("#FF0000", "#FFFF00", "#00FF00", "#0000FF", "#5500FF", "#BF00FF")
        expected.forEachIndexed { index, hex ->
            assertEquals(RainbowMix.NAMES[index], hex, RainbowMix.hex(RainbowMix.mix(only(index))))
            assertEquals(RainbowMix.BASE_COLORS[index], RainbowMix.mix(only(index)))
        }
    }

    @Test
    fun allZeroIsBlack() {
        assertEquals("#000000", RainbowMix.hex(RainbowMix.mix(IntArray(6))))
    }

    @Test
    fun tooBrightMixesScaleDownInsteadOfClipping() {
        // Red 100 and Yellow 100 make orange, not plain yellow.
        assertEquals("#FF8000", RainbowMix.hex(RainbowMix.mix(intArrayOf(100, 100, 0, 0, 0, 0))))
        // Blue and Yellow make white.
        assertEquals("#FFFFFF", RainbowMix.hex(RainbowMix.mix(intArrayOf(0, 100, 0, 100, 0, 0))))
    }

    @Test
    fun typedHexGivesNumbersThatMixBackToTheSameColor() {
        val codes = listOf(
            "#00FF66", "#FF6600", "#123456", "#A8C7FA", "#FFFFFF", "#000000", "#5500FF", "#BF00FF",
            "#8000FF", "#FF00FF", "#7F7F7F", "#33FF00", "#46F0FF", "#FFC34D", "#C00040", "#0A0B0C",
        )
        for (code in codes) {
            val color = RainbowMix.parseHex(code) ?: error("$code did not read")
            val numbers = RainbowMix.split(color)
            assertTrue(code, numbers.all { it in 0..100 })
            val back = RainbowMix.mix(numbers)
            for (shift in intArrayOf(16, 8, 0)) {
                val wanted = color shr shift and 0xFF
                val got = back shr shift and 0xFF
                assertTrue("$code came back as ${RainbowMix.hex(back)}", abs(wanted - got) <= 4)
            }
        }
    }

    @Test
    fun wrongHexCodesAreRefused() {
        for (bad in listOf("abc", "#abc", "150", "#12345", "#1234567", "#GG0000", "", "#", "00FF66FF", "red")) {
            assertNull("'$bad' should be refused", RainbowMix.parseHex(bad))
        }
    }

    @Test
    fun goodHexCodesAreRead() {
        assertEquals(0xFF00FF66.toInt(), RainbowMix.parseHex("#00FF66"))
        assertEquals(0xFF00FF66.toInt(), RainbowMix.parseHex("00ff66"))
        assertEquals(0xFF00FF66.toInt(), RainbowMix.parseHex(" #00ff66 "))
        assertEquals("#00FF66", RainbowMix.hex(0xFF00FF66.toInt()))
    }

    @Test
    fun savedNumbersReadBack() {
        val numbers = intArrayOf(10, 0, 55, 100, 3, 0)
        assertArrayEquals(numbers, RainbowMix.decode(RainbowMix.encode(numbers)))
        assertNull(RainbowMix.decode(null))
        assertNull(RainbowMix.decode("1,2,3"))
        assertNull(RainbowMix.decode("a,b,c,d,e,f"))
        assertArrayEquals(intArrayOf(100, 0, 0, 0, 0, 0), RainbowMix.decode("150,0,0,0,0,0"))
    }
}
