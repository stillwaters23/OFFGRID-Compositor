package com.offgrid.compositor.terminal

/**
 * 📁 Microphone Effects. The mic starts off. Voice effects run on the phone.
 * The panel shows this folder once. The rows live in [EffectFolders.microphone]
 * so the top bar and the folder share one set of keys.
 */
internal object MicrophoneEffectsFolder {
    fun folder(): PanelFolder = EffectFolders.microphone()
}
