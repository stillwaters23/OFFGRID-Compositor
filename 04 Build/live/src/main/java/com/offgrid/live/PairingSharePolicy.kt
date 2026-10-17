package com.offgrid.live

/**
 * Whether the pairing screen must close for this recording state on this API level.
 *
 * Generating a key puts the secret 128-character pairing key and this phone's Wi-Fi
 * address on screen as selectable text. Android 15 (API 35, Vanilla Ice Cream) is the
 * first release whose [android.view.WindowManager.addScreenRecordingCallback] reports
 * [android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE]. Below that API the
 * official callback does not exist, so the guard does nothing. Any state other than
 * visible leaves the screen up.
 *
 * The numbers are the platform constants. [PairingSharePolicyTest] checks they still
 * match the SDK, so a stub jar that zeroes Android fields cannot hide a wrong value.
 */
internal object PairingSharePolicy {
    /** [android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM]. */
    const val CALLBACK_API = 35

    /** [android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE]. */
    const val VISIBLE = 1

    fun shouldClose(apiLevel: Int, recordingState: Int): Boolean =
        apiLevel >= CALLBACK_API && recordingState == VISIBLE
}
