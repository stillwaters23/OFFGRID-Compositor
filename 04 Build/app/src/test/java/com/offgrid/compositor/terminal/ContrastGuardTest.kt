package com.offgrid.compositor.terminal

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastGuardTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun locksWhiteSkinWhenTypewriterTextIsLight() {
        val reason = ContrastGuard.lockReason(ColorPart.SKIN, white, black, white, white)
        assertNotNull(reason)
        assertTrue(reason!!.startsWith("Locked:"))
        assertTrue(reason.contains("white"))
    }

    @Test
    fun locksBlackTypewriterOnBlackSkin() {
        val reason = ContrastGuard.lockReason(ColorPart.TEXT, black, black, black, white)
        assertNotNull(reason)
        assertTrue(reason!!.contains("black"))
        assertTrue(reason.contains("can't be read"))
    }

    @Test
    fun acceptsReadablePairs() {
        assertNull(ContrastGuard.lockReason(ColorPart.SKIN, black, black, white, white))
        assertNull(ContrastGuard.lockReason(ColorPart.TEXT, white, black, white, white))
        assertNull(ContrastGuard.lockReason(ColorPart.STATUS, black, white, black, black))
    }

    @Test
    fun everyNeonSwatchIsLockedOrClearsWcagAgainstBlackAndWhite() {
        for (swatch in NeonPalette.swatches) {
            val onBlack = ContrastGuard.ratio(swatch.color, black)
            val onWhite = ContrastGuard.ratio(swatch.color, white)
            val skinOnLightText = ContrastGuard.lockReason(ColorPart.SKIN, swatch.color, black, white, white)
            if (onBlack < ContrastGuard.MIN_RATIO && onWhite < ContrastGuard.MIN_RATIO) {
                assertNotNull("${swatch.name} must lock on one of the skins", skinOnLightText)
            }
            if (onBlack < ContrastGuard.MIN_RATIO) {
                assertNotNull(
                    "${swatch.name} text on black",
                    ContrastGuard.lockReason(ColorPart.TEXT, swatch.color, black, swatch.color, swatch.color),
                )
            }
            if (onWhite < ContrastGuard.MIN_RATIO) {
                assertNotNull(
                    "${swatch.name} text on white",
                    ContrastGuard.lockReason(ColorPart.TEXT, swatch.color, white, swatch.color, swatch.color),
                )
            }
        }
    }

    @Test
    fun wcagRatioMatchesTheNumberGuard() {
        assertTrue(ContrastGuard.ratio(white, black) >= 4.5)
        assertTrue(ContrastGuard.ratio(black, white) >= 4.5)
        assertTrue(ContrastGuard.ratio(black, black) < 4.5)
        assertTrue(ContrastGuard.ratio(white, white) < 4.5)
    }
}
