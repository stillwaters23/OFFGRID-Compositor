package com.offgrid.compositor

/** The radar sweep's ping, mixed into the video whenever the radar or the mirror scan is on. */
internal object RadarPing {
    const val PING = "ping"
    const val QUIET_GAIN = 0.6f

    fun intoVideo(radarOn: Boolean, mirrorOn: Boolean): Boolean = radarOn || mirrorOn

    /** The chosen sound, or the ping when that choice is missing or off. */
    fun soundName(choice: String): String = if (choice.isEmpty() || choice == "off") PING else choice
}
