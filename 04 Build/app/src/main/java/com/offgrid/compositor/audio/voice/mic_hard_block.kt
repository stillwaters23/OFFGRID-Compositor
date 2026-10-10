package com.offgrid.compositor.audio.voice

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Source-level gate for the microphone.
 * A hard block closes capture immediately and keeps [canOpen] false until the block is cleared.
 * Clearing the block does not open the recorder again.
 */
class MicHardBlock {
    private val blocked = AtomicBoolean(false)
    private val open = AtomicBoolean(false)

    fun isBlocked(): Boolean = blocked.get()

    fun isOpen(): Boolean = open.get() && !blocked.get()

    fun setBlocked(value: Boolean) {
        blocked.set(value)
        if (value) open.set(false)
    }

    /** Permission alone is not enough. A hard block refuses the open. */
    fun canOpen(permissionGranted: Boolean): Boolean = permissionGranted && !blocked.get()

    /**
     * Marks the recorder as running. Returns false when a block landed during setup,
     * in which case the caller must release the recorder without delivering samples.
     */
    fun markOpen(): Boolean {
        if (blocked.get()) {
            open.set(false)
            return false
        }
        open.set(true)
        return true
    }

    fun markClosed() {
        open.set(false)
    }

    fun mayDeliver(): Boolean = open.get() && !blocked.get()
}
