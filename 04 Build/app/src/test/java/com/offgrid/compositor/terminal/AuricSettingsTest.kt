package com.offgrid.compositor.terminal

import com.offgrid.compositor.audio.clicker.ClickerSoundSynth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuricSettingsTest {
    @Test
    fun wheelAndClickerSettingsAreInTheirOwnPanelFolders() {
        val settings = PanelTree.build().folders.first { it.id == "panel_settings" }
        val auric = settings.folders.first { it.id == "auric_wheel" }
        val clicker = settings.folders.first { it.id == "clicker_sounds" }
        val defaults = PanelTree.defaults

        assertEquals(72, defaults.numbers[PanelTree.AURIC_LASER_DIMMER])
        assertEquals("2BF5E6", defaults.choices[PanelTree.AURIC_LASER_COLOR])
        assertEquals(7, clicker.items.filterIsInstance<ChoiceItem>().single().choices.size)
        assertEquals(77, ClickerSoundSynth.VARIANT_COUNT)
        assertTrue(auric.notes.any { it.contains("five-row") })
        assertTrue(defaults.toggles[PanelTree.CLICKER_ENABLED] == true)
    }

    @Test
    fun colorFoldersHaveSeparateGuardedDarkAndLightNumberColors() {
        val settings = PanelTree.build().folders.first { it.id == "panel_settings" }
        val colors = settings.folders.first { it.id == "colors" }
        assertTrue(colors.folders.any { it.id == "colors.number_dark" })
        assertTrue(colors.folders.any { it.id == "colors.number_light" })
    }
}
