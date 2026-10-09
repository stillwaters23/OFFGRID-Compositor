package com.offgrid.compositor.audio

import java.util.concurrent.atomic.AtomicLong

/**
 * A lock-free ring of audio samples for one writer thread and one reader thread.
 * The writer never waits. When the ring is full the extra samples are dropped.
 */
class ShortRing(capacity: Int) {
    private val data = ShortArray(capacity)
    private val head = AtomicLong()
    private val tail = AtomicLong()

    val available: Int get() = (tail.get() - head.get()).toInt()

    /** Writer thread only. Returns how many samples fit. */
    fun write(source: ShortArray, count: Int): Int {
        val used = (tail.get() - head.get()).toInt()
        val room = minOf(count, data.size - used)
        val start = tail.get()
        for (i in 0 until room) data[((start + i) % data.size).toInt()] = source[i]
        tail.set(start + room)
        return room
    }

    /** Reader thread only. Returns how many samples were read. */
    fun read(target: ShortArray, count: Int): Int {
        val ready = (tail.get() - head.get()).toInt()
        val take = minOf(count, ready)
        val start = head.get()
        for (i in 0 until take) target[i] = data[((start + i) % data.size).toInt()]
        head.set(start + take)
        return take
    }

    /** Reader thread only. Throws away the oldest samples, keeping the newest [keep]. */
    fun skipTo(keep: Int) {
        val extra = (tail.get() - head.get()).toInt() - keep
        if (extra > 0) head.set(head.get() + extra)
    }
}
