package com.offgrid.compositor.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberColorGuardTest {
    @Test
    fun rejectsLowContrastNumbersInBothThemeBackgrounds() {
        assertFalse(NumberColorGuard.accepts(0xFF777777.toInt(), NumberColorGuard.LIGHT_BACKGROUND))
        assertFalse(NumberColorGuard.accepts(0xFF727272.toInt(), NumberColorGuard.DARK_BACKGROUND))
    }

    @Test
    fun acceptsTheDefaultNumberColorsForDarkAndLightThemes() {
        assertTrue(NumberColorGuard.accepts(0xFFFFFFFF.toInt(), NumberColorGuard.DARK_BACKGROUND))
        assertTrue(NumberColorGuard.accepts(0xFF000000.toInt(), NumberColorGuard.LIGHT_BACKGROUND))
    }

    @Test
    fun keepsVisibleNumbersReadableAfterAThemeChange() {
        val backgrounds = listOf(0xFF000000.toInt(), 0xFF777777.toInt(), 0xFFFFFFFF.toInt())
        backgrounds.forEach { background ->
            val guarded = NumberColorGuard.visibleColor(0xFF777777.toInt(), background)
            assertTrue(NumberColorGuard.accepts(guarded, background))
        }
    }
}
