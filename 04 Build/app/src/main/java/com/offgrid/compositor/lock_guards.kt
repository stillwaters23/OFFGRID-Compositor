package com.offgrid.compositor

/**
 * Phase 1.3: the small rules that keep a stray key or a system dialog off a locked stage.
 * Pure, so the JVM tests cover them.
 */
internal object LockGuards {
    /** Back is swallowed while locked: leaving OFFGRID would put the home screen or another app on air. */
    fun swallowsBack(state: LockState): Boolean = state.locked

    /** A system permission dialog is chrome the lock cannot hide, so it never opens while locked. */
    fun mayShowPermissionDialog(state: LockState): Boolean = !state.locked

    /**
     * The camera permission dialog opens at most once, and only after the lock is off.
     *
     * [fromLockNotify] is a store notification. Those may launch the one deferred ask
     * when the lock lifts, but they never show the help line. A burst of notifications
     * therefore cannot stack help messages, and a resume while still locked shows neither.
     */
    fun cameraPrompt(
        granted: Boolean,
        alreadyAsked: Boolean,
        locked: Boolean,
        resumed: Boolean,
        fromLockNotify: Boolean,
    ): CameraPrompt? {
        if (granted || !resumed || locked) return null
        if (!alreadyAsked) return CameraPrompt.ASK_ONCE
        if (fromLockNotify) return null
        return CameraPrompt.SHOW_HELP
    }
}

/** What the activity may do about the camera permission. Null means do nothing. */
internal enum class CameraPrompt { ASK_ONCE, SHOW_HELP }
