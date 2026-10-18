package com.offgrid.compositor.terminal

import android.os.SystemClock
import android.util.Log
import com.offgrid.compositor.audio.clicker.ClickerSoundEngine
import com.offgrid.compositor.audio.clicker.ClickerSoundSynth

internal val CLICKER_STYLES = listOf("soft", "wood", "digital", "rubber", "glass", "metal", "muted")

/** Reads the saved clicker after a theme or settings change. */
internal fun TerminalPanel.syncClicker() {
    clickerEnabled = store.toggle(PanelTree.CLICKER_ENABLED)
    clickerStyle = CLICKER_STYLES.indexOf(store.choice(PanelTree.CLICKER_STYLE)).coerceAtLeast(0)
    clickerVariant = store.number(PanelTree.CLICKER_VARIANT)
    clickerVolume = store.number(PanelTree.CLICKER_VOLUME)
}

/** One notch or settle on a width or height drum. Silent when the clicker is off. */
internal fun TerminalPanel.playWheelClick(value: Int) {
    clickerEnabled = store.toggle(PanelTree.CLICKER_ENABLED)
    if (!clickerEnabled) {
        Log.i(ClickerSoundEngine.TAG, "SoundPool play skipped clicker off")
        return
    }
    val now = SystemClock.uptimeMillis()
    if (value == lastWheelClickValue && now - lastWheelClickAt < CLICK_GAP_MS) return
    lastWheelClickValue = value
    lastWheelClickAt = now
    clickerSounds.play(
        clickerStyle.coerceIn(0, ClickerSoundSynth.STYLE_COUNT - 1),
        ClickerSoundSynth.mixTone(clickerVariant, value),
        clickerVolume,
    )
}

private const val CLICK_GAP_MS = 35L

/** The Auric laser color, or the panel edge when the saved hex is missing. */
internal fun TerminalPanel.auricLaserColor(): Int {
    val rgb = store.choice(PanelTree.AURIC_LASER_COLOR).toLongOrNull(16)?.toInt()
        ?: return look.edge
    return 0xFF000000.toInt() or rgb
}
