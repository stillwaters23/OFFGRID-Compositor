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
}
