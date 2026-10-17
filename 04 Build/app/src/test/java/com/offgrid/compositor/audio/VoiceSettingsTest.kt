package com.offgrid.compositor.audio

import com.offgrid.compositor.effects.EffectSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSettingsTest {
    private fun snapshot(numbers: Map<String, Int> = emptyMap(), toggles: Map<String, Boolean> = emptyMap()) =
        EffectSnapshot(numbers, toggles, emptyMap(), emptyMap())

    @Test
    fun pitchGoesFromAnOctaveDownToAnOctaveUp() {
        assertEquals(0.5f, VoiceSettings.from(snapshot(mapOf(MicKeys.PITCH to 0))).pitchRatio, 1e-4f)
        assertEquals(1f, VoiceSettings.from(snapshot(mapOf(MicKeys.PITCH to 50))).pitchRatio, 1e-4f)
        assertEquals(2f, VoiceSettings.from(snapshot(mapOf(MicKeys.PITCH to 100))).pitchRatio, 1e-4f)
    }

    @Test
    fun echoDelayGoesFrom50To800Milliseconds() {
        val on = mapOf(MicKeys.ECHO_ON to true)
        assertEquals(50f, VoiceSettings.from(snapshot(mapOf(MicKeys.ECHO_DELAY to 0), on)).echoDelayMs, 1e-3f)
        assertEquals(800f, VoiceSettings.from(snapshot(mapOf(MicKeys.ECHO_DELAY to 100), on)).echoDelayMs, 1e-3f)
        assertEquals(0.85f, VoiceSettings.from(snapshot(mapOf(MicKeys.ECHO_AMOUNT to 100), on)).echoFeedback, 1e-4f)
    }

    @Test
    fun aStageThatIsOffStaysOff() {
        val settings = VoiceSettings.from(snapshot(mapOf(MicKeys.ROBOT_AMOUNT to 80, MicKeys.CAVE_AMOUNT to 80)))
        assertEquals(0f, settings.robotAmount, 0f)
        assertEquals(0f, settings.echoDelayMs, 0f)
        assertEquals(0f, settings.caveAmount, 0f)
        assertFalse(settings.robotOn || settings.echoOn || settings.caveOn || settings.radioOn)
    }

    @Test
    fun theMasterTurnsEveryStageOff() {
        val toggles = mapOf(MicKeys.ROBOT_ON to true, MicKeys.ECHO_ON to true, MicKeys.RADIO_ON to true, MicKeys.CAVE_ON to true)
        val numbers = mapOf(MicKeys.PITCH to 80)
        val off = VoiceSettings.from(snapshot(numbers, toggles))
        assertFalse(off.pitchOn || off.robotOn || off.echoOn || off.radioOn || off.caveOn)
        val on = VoiceSettings.from(snapshot(numbers, toggles + (MicKeys.FX_ON to true)))
        assertTrue(on.pitchOn && on.robotOn && on.echoOn && on.radioOn && on.caveOn)
    }

    @Test
    fun gainIsSilentAtZeroAndUnchangedAtFifty() {
        assertEquals(0f, VoiceSettings.gainFor(0), 0f)
        assertEquals(1f, VoiceSettings.gainFor(50), 0f)
        assertEquals(4f, VoiceSettings.gainFor(100), 0f)
    }

    @Test
    fun gateZeroIsOffAndLouderSettingsOpenLater() {
        assertEquals(0f, VoiceSettings.from(snapshot(mapOf(MicKeys.GATE to 0))).gateLevel, 0f)
        val low = VoiceSettings.from(snapshot(mapOf(MicKeys.GATE to 10))).gateLevel
        val high = VoiceSettings.from(snapshot(mapOf(MicKeys.GATE to 90))).gateLevel
        assertTrue(low > 0f && high > low && high < 0.2f)
    }
}
