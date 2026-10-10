package com.offgrid.compositor

/**
 * Gives every recorded picture a steady time, even when the pictures switch between the camera and
 * the black stage. The two sources count time from different starting points, so when the source
 * changes the clock carries on one picture later instead of jumping.
 */
internal class StampClock {
    enum class Source { CAMERA, BLACK }

    private var source: Source? = null
    private var offset = 0L
    private var last = Long.MIN_VALUE

    fun next(from: Source, rawNanos: Long): Long {
        if (source != null && from != source) offset = last + FRAME_NANOS - rawNanos
        source = from
        var stamp = rawNanos + offset
        if (last != Long.MIN_VALUE && stamp <= last) stamp = last + 1
        last = stamp
        return stamp
    }

    private companion object {
        const val FRAME_NANOS = 33_333_333L
    }
}
