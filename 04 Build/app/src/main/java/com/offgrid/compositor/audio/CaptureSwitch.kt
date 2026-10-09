package com.offgrid.compositor.audio

/** What the camera screen must do to match the Camera and Mic toggles. */
enum class CaptureAction {
    /** Let go of the camera. The screen and videos show black with the drawn effects. */
    UNBIND_CAMERA,

    /** Take the camera again. */
    BIND_CAMERA,

    /** Start listening to the mic. */
    START_MIC,

    /** Stop listening and let go of the mic, so the phone's mic dot goes away. */
    RELEASE_MIC,
}

/**
 * Keeps track of what is running and says what to change when a toggle flips.
 * The camera starts bound and the mic starts released. Each call returns only what changed.
 */
class CaptureSwitch {
    var cameraRunning = true
        private set
    var micRunning = false
        private set

    /**
     * [micAllowed] is whether the phone gave the mic permission. [foreground] is false while the app is hidden.
     * The mic is only used when it is wanted, allowed, and the app is showing.
     */
    fun update(cameraWanted: Boolean, micWanted: Boolean, micAllowed: Boolean, foreground: Boolean = true): List<CaptureAction> {
        val actions = ArrayList<CaptureAction>(2)
        if (cameraWanted != cameraRunning) {
            cameraRunning = cameraWanted
            actions += if (cameraWanted) CaptureAction.BIND_CAMERA else CaptureAction.UNBIND_CAMERA
        }
        val micNeeded = micWanted && micAllowed && foreground
        if (micNeeded != micRunning) {
            micRunning = micNeeded
            actions += if (micNeeded) CaptureAction.START_MIC else CaptureAction.RELEASE_MIC
        }
        return actions
    }

    /** The mic could not start, so it counts as released and the next update may try again. */
    fun micFailed() {
        micRunning = false
    }
}
