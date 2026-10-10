package com.offgrid.compositor.terminal

import com.offgrid.compositor.audio.MicKeys

/** 📁 Microphone Effects. The mic starts off. Voice effects run on the phone. */
internal object MicrophoneEffectsFolder {
    fun folder() = PanelFolder(
        id = "microphone",
        title = "📁 Microphone Effects",
        tag = null,
        kind = FolderKind.EFFECTS,
        items = listOf(
            ToggleItem(MicKeys.MIC_ON, "Mic", "Record microphone audio. It starts off.", false, isTest = false),
            ToggleItem(MicKeys.FX_ON, "Microphone Effects", "Turn the voice effects on or off.", false, isTest = false),
            NumberItem(MicKeys.PITCH, "Pitch", "50 keeps your natural pitch.", 50),
            ToggleItem(MicKeys.ROBOT_ON, "Robot", "Give your voice a robot sound.", false, isTest = false),
            NumberItem(MicKeys.ROBOT_AMOUNT, "Robot amount", "0 is natural. 100 is fully robotic.", 50),
            ToggleItem(MicKeys.ECHO_ON, "Echo", "Add an echo to your voice.", false, isTest = false),
            NumberItem(MicKeys.ECHO_DELAY, "Echo delay", "Set the time before the echo.", 50),
            NumberItem(MicKeys.ECHO_AMOUNT, "Echo amount", "Set how much the echo repeats.", 50),
            ToggleItem(MicKeys.RADIO_ON, "Radio", "Give your voice a radio sound.", false, isTest = false),
            ToggleItem(MicKeys.CAVE_ON, "Cave", "Add a cave echo to your voice.", false, isTest = false),
            NumberItem(MicKeys.CAVE_AMOUNT, "Cave amount", "0 is light. 100 is deep.", 50),
            NumberItem(MicKeys.GAIN, "Mic volume", "0 is silent. 50 is natural. 100 is loud.", 50),
            NumberItem(MicKeys.GATE, "Noise gate", "0 turns the gate off. Higher values cut quiet sounds.", 0),
            ToggleItem(MicKeys.SOUNDS_IN_VIDEO, "Sounds in video", "Include soundboard audio in video recordings.", true, isTest = false),
            ToggleItem(MicKeys.BEAT_GLOW, "Beat glow", "Let the audio pulse the glow border.", false, isTest = false),
        ),
        notes = listOf("Everything runs on this phone. Nothing goes to the cloud."),
    )
}
