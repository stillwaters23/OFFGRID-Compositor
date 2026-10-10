package com.offgrid.compositor.terminal

import android.content.Context
import android.view.View
import android.view.ViewGroup
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

    @Test
    fun topBarMicAndCameraBlocksDriveTheMicEngineKeys() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("terminal_panel", Context.MODE_PRIVATE).edit().clear().commit()
        val panel = TerminalPanel(context)
        val cameraBlock = findQuick(panel, "Camera")
        val micBlock = findQuick(panel, "Microphone")
        assertTrue(cameraBlock.isOn)
        assertFalse(micBlock.isOn)
        assertTrue(panel.effectSnapshot().on(MicKeys.CAM_ON))
        assertFalse(panel.effectSnapshot().on(MicKeys.MIC_ON))

        micBlock.performClick()
        assertTrue(micBlock.isOn)
        assertTrue(panel.effectSnapshot().on(MicKeys.MIC_ON))

        cameraBlock.performClick()
        assertFalse(cameraBlock.isOn)
        assertFalse(panel.effectSnapshot().on(MicKeys.CAM_ON))

        panel.setToggleSetting(MicKeys.MIC_ON, false)
        panel.setToggleSetting(MicKeys.CAM_ON, true)
        assertFalse(micBlock.isOn)
        assertTrue(cameraBlock.isOn)
        assertFalse(panel.effectSnapshot().on(MicKeys.MIC_ON))
        assertTrue(panel.effectSnapshot().on(MicKeys.CAM_ON))
    }

    private fun findQuick(root: View, description: String): QuickToggleView =
        findQuickOrNull(root, description) ?: throw AssertionError("Missing $description")

    private fun findQuickOrNull(root: View, description: String): QuickToggleView? {
        if (root is QuickToggleView && root.contentDescription?.startsWith(description) == true) return root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findQuickOrNull(root.getChildAt(index), description)?.let { return it }
            }
        }
        return null
    }
}
