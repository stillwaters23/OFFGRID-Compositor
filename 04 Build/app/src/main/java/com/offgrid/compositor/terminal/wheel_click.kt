package com.offgrid.compositor.terminal

import com.offgrid.compositor.audio.clicker.ClickerSoundSynth

internal val CLICKER_STYLES = listOf("soft", "wood", "digital", "rubber", "glass", "metal", "muted")

/** Reads the saved clicker after a theme or settings change. */
internal fun TerminalPanel.syncClicker() {
    clickerEnabled = store.toggle(PanelTree.CLICKER_ENABLED)
    clickerStyle = CLICKER_STYLES.indexOf(store.choice(PanelTree.CLICKER_STYLE)).coerceAtLeast(0)
    clickerVariant = store.number(PanelTree.CLICKER_VARIANT)
    clickerVolume = store.number(PanelTree.CLICKER_VOLUME)
}

/** One notch on a width or height drum. Silent when the clicker is off. */
internal fun TerminalPanel.playWheelClick(value: Int) {
    if (!clickerEnabled) return
    clickerSounds.play(
        clickerStyle.coerceIn(0, ClickerSoundSynth.STYLE_COUNT - 1),
        ClickerSoundSynth.mixTone(clickerVariant, value),
        clickerVolume,
    )
}

/** The Auric laser color, or the panel edge when the saved hex is missing. */
internal fun TerminalPanel.auricLaserColor(): Int {
    val rgb = store.choice(PanelTree.AURIC_LASER_COLOR).toLongOrNull(16)?.toInt()
        ?: return look.edge
    return 0xFF000000.toInt() or rgb
}
