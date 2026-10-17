package com.offgrid.compositor.audio

/**
 * Fills one block of recorded audio, always at the pace of the clock. The block holds the mic
 * when it has been heard in time, and silence when the mic is off or late. Sounds from the
 * soundboard are mixed in on top. The mic is never allowed to run more than 100 ms behind.
 */
class AudioFeeder(private val mic: ShortRing, private val sounds: SoundMixer?) {
    /**
     * Fills [block]. [dueFrames] is how many frames the clock says are still owed.
     * Returns false when the mic should be given a little more time.
     */
    fun fill(block: ShortArray, dueFrames: Long): Boolean {
        mic.skipTo(MAX_BACKLOG_FRAMES)
        val have = mic.available
        val late = dueFrames >= block.size * LATE_BLOCKS
        if (have < block.size && !late) return false
        block.fill(0)
        if (have > 0) mic.read(block, minOf(have, block.size))
        sounds?.mixInto(block, block.size)
        return true
    }

    private companion object {
        const val MAX_BACKLOG_FRAMES = 4800
        const val LATE_BLOCKS = 3
    }
}
