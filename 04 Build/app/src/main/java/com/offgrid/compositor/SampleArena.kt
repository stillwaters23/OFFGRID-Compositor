package com.offgrid.compositor

import java.nio.ByteBuffer

/**
 * One fixed block of memory that holds encoded video pieces, newest after oldest, like a ring.
 * Nothing is allocated per piece. When a new piece needs room, the oldest pieces are let go.
 * So the memory never grows past [capacity], and a busy video simply keeps fewer seconds.
 * All calls must hold the same lock.
 */
internal class SampleArena(val capacity: Int, direct: Boolean = true) {
    class Piece(val offset: Int, val size: Int, val timeUs: Long, val flags: Int)

    private val memory: ByteBuffer = if (direct) ByteBuffer.allocateDirect(capacity) else ByteBuffer.allocate(capacity)
    private val pieces = ArrayDeque<Piece>()
    private var writeAt = 0

    /** How many pieces were let go because the arena needed their room. Time-based drops do not count. */
    var overwritten = 0L
        private set

    val size: Int get() = pieces.size

    val newestTimeUs: Long? get() = pieces.lastOrNull()?.timeUs

    val oldestTimeUs: Long? get() = pieces.firstOrNull()?.timeUs

    /** Copies [size] bytes from [source] in. Returns false when one piece is bigger than the whole arena. */
    fun add(source: ByteBuffer, size: Int, timeUs: Long, flags: Int): Boolean {
        if (size > capacity) return false
        if (writeAt + size > capacity) writeAt = 0
        while (pieces.isNotEmpty() && pieces.first().let { it.offset < writeAt + size && it.offset + it.size > writeAt }) {
            pieces.removeFirst()
            overwritten++
        }
        val target = memory.duplicate()
        target.limit(writeAt + size)
        target.position(writeAt)
        val from = source.duplicate()
        from.limit(from.position() + size)
        target.put(from)
        pieces.addLast(Piece(writeAt, size, timeUs, flags))
        writeAt += size
        return true
    }

    /** Lets go of pieces older than [timeUs], but always keeps the newest one. */
    fun dropBefore(timeUs: Long) {
        while (pieces.size > 1 && pieces.first().timeUs < timeUs) pieces.removeFirst()
    }

    fun snapshot(): List<Piece> = pieces.toList()

    /** The bytes of [piece], ready to write. */
    fun read(piece: Piece): ByteBuffer {
        val view = memory.duplicate()
        view.limit(piece.offset + piece.size)
        view.position(piece.offset)
        return view.slice()
    }

    fun clear() {
        pieces.clear()
        writeAt = 0
    }
}
