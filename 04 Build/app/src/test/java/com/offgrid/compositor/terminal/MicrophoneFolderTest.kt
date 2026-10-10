package com.offgrid.compositor.terminal

import android.content.Context
import com.offgrid.compositor.audio.MicKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MicrophoneFolderTest {
    @Test
    fun homeHasTheMicrophoneEffectsFolderAndTheMicStartsOff() {
        val folder = PanelTree.build().folders.single { it.id == "microphone" }
        assertEquals("📁 Microphone Effects", folder.title)
        val mic = folder.items.filterIsInstance<ToggleItem>().single { it.key == MicKeys.MIC_ON }
        assertFalse(mic.default)
        val toggles = folder.items.filterIsInstance<ToggleItem>().map { it.key }
        assertTrue(MicKeys.FX_ON in toggles)
        assertTrue(MicKeys.BEAT_GLOW in toggles)
        assertEquals(50, folder.items.filterIsInstance<NumberItem>().single { it.key == MicKeys.PITCH }.default)
    }

    @Test
    fun setToggleSettingTurnsTheMicOffInTheSavedState() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
        val panel = TerminalPanel(context)
        panel.setToggleSetting(MicKeys.MIC_ON, true)
        assertTrue(panel.effectSnapshot().on(MicKeys.MIC_ON))
        panel.setToggleSetting(MicKeys.MIC_ON, false)
        assertFalse(panel.effectSnapshot().on(MicKeys.MIC_ON))
    }
}
