package com.offgrid.compositor.terminal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Checks the nine-color mixer in the theme office. It runs on the computer, not the phone. */
class RainbowMixTest {

    private fun only(index: Int, amount: Int = 100) = IntArray(9).also { it[index] = amount }

    private fun mixHex(vararg weights: Int) = RainbowMix.hex(RainbowMix.mix(weights))

    @Test
    fun nineColorsInRainbowOrderThenWhiteAndBlack() {
        assertEquals(listOf("Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet", "White", "Black"), RainbowMix.NAMES)
        assertEquals(RainbowMix.NAMES.size, RainbowMix.BASE_COLORS.size)
    }

    @Test
    fun eachColorAtFullStrength() {
        val expected = listOf("#FF0000", "#FF8000", "#FFFF00", "#00FF00", "#0000FF", "#5500FF", "#BF00FF", "#FFFFFF", "#000000")
        expected.forEachIndexed { index, hex ->
            assertEquals(RainbowMix.NAMES[index], hex, RainbowMix.hex(RainbowMix.mix(only(index))))
            assertEquals(RainbowMix.BASE_COLORS[index], RainbowMix.mix(only(index)))
        }
    }

    @Test
    fun allZeroIsBlack() {
        assertEquals("#000000", RainbowMix.hex(RainbowMix.mix(IntArray(9))))
    }

    @Test
    fun tooBrightMixesScaleDownInsteadOfClipping() {
        // Red 100 and Yellow 100 make orange, not plain yellow.
        assertEquals("#FF8000", mixHex(100, 0, 100, 0, 0, 0, 0, 0, 0))
        // Blue and Yellow make white.
        assertEquals("#FFFFFF", mixHex(0, 0, 100, 0, 100, 0, 0, 0, 0))
    }

    @Test
    fun whiteMakesColorsPaler() {
        // Red with as much White is pink.
        assertEquals("#FF8080", mixHex(100, 0, 0, 0, 0, 0, 0, 100, 0))
        // White alone at half is gray.
        assertEquals("#808080", mixHex(0, 0, 0, 0, 0, 0, 0, 50, 0))
    }

    @Test
    fun blackDimsLikeADimmer() {
        assertEquals("#800000", mixHex(100, 0, 0, 0, 0, 0, 0, 0, 50))
        assertEquals("#000000", mixHex(100, 100, 100, 100, 100, 100, 100, 100, 100))
        assertEquals("#000000", mixHex(0, 0, 0, 0, 0, 0, 0, 100, 100))
    }

    @Test
    fun typedHexGivesNumbersThatMixBackToTheSameColor() {
        val codes = listOf(
            "#00FF66", "#FF6600", "#123456", "#A8C7FA", "#FFFFFF", "#000000", "#5500FF", "#BF00FF",
            "#8000FF", "#FF00FF", "#7F7F7F", "#33FF00", "#46F0FF", "#FFC34D", "#C00040", "#0A0B0C",
            "#FF8000", "#FFB000", "#FF4000", "#2BF5E6", "#B0C6FF", "#FF2D95", "#010101", "#FEFEFE",
        )
        for (code in codes) {
            val color = RainbowMix.parseHex(code) ?: error("$code did not read")
            val numbers = RainbowMix.split(color)
            assertEquals(9, numbers.size)
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
    fun hexCodesSplitIntoTheColorsTheyLookLike() {
        // Pure orange is Orange 100, not Red plus Yellow.
        assertArrayEquals(only(1), RainbowMix.split(0xFFFF8000.toInt()))
        // White is White 100. Black is Black 100.
        assertArrayEquals(only(7), RainbowMix.split(0xFFFFFFFF.toInt()))
        assertArrayEquals(only(8), RainbowMix.split(0xFF000000.toInt()))
        // Dark red is Red with Black.
        assertArrayEquals(intArrayOf(100, 0, 0, 0, 0, 0, 0, 0, 50), RainbowMix.split(0xFF800000.toInt()))
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
        val numbers = intArrayOf(10, 20, 0, 55, 100, 3, 0, 40, 5)
        assertArrayEquals(numbers, RainbowMix.decode(RainbowMix.encode(numbers)))
        assertNull(RainbowMix.decode(null))
        assertNull(RainbowMix.decode("1,2,3"))
        assertNull(RainbowMix.decode("a,b,c,d,e,f,g,h,i"))
        assertArrayEquals(only(0), RainbowMix.decode("150,0,0,0,0,0,0,0,0"))
    }

    @Test
    fun sixNumbersSavedByStep22StillLoadAsTheSameColor() {
        // Red, Yellow, Green, Blue, Indigo, Violet from before Orange, White, and Black existed.
        val old = intArrayOf(40, 10, 0, 70, 20, 5)
        val nine = RainbowMix.decode(RainbowMix.encode(old)) ?: error("old numbers did not load")
        assertArrayEquals(intArrayOf(40, 0, 10, 0, 70, 20, 5, 0, 0), nine)
        // The same color as the Step 2.2 mixer made from those six numbers.
        assertEquals("#9A1AF2", RainbowMix.hex(RainbowMix.mix(nine)))
    }
}
