package com.offgrid.compositor.audio.clicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickerSoundSynthTest {
    @Test
    fun makesSeventySevenNonSilentVariantsForEveryStyle() {
        for (style in 0 until ClickerSoundSynth.STYLE_COUNT) {
            val variants = (0 until ClickerSoundSynth.VARIANT_COUNT)
                .map { ClickerSoundSynth.make(style, it) }
            assertEquals(ClickerSoundSynth.VARIANT_COUNT, variants.map { it.toList() }.toSet().size)
            assertTrue(variants.all { it.size == ClickerSoundSynth.SAMPLE_RATE * 8 / 1000 })
            assertTrue(variants.all { samples -> samples.any { it != 0.toShort() } })
        }
    }

    @Test
    fun mapsTheFullControlRangeToTheFullVariantRange() {
        assertEquals(0, ClickerSoundSynth.variantFor(-1))
        assertEquals(0, ClickerSoundSynth.variantFor(0))
        assertEquals(76, ClickerSoundSynth.variantFor(100))
        assertEquals(76, ClickerSoundSynth.variantFor(101))
        assertEquals(50, ClickerSoundSynth.mixTone(0, 100))
        assertEquals(0, ClickerSoundSynth.mixTone(-4, 0))
        assertEquals(100, ClickerSoundSynth.mixTone(100, 120))
    }
}
