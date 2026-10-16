package com.offgrid.compositor

/**
 * Fixed-size ring of recent probe lines. Used only while [RenderProbe] is enabled.
 */
internal class ProbeLineBuffer(private val capacity: Int) {
    private val lines = ArrayDeque<String>(capacity.coerceAtLeast(1))
    private val lock = Any()

    fun add(line: String) {
        synchronized(lock) {
            if (lines.size >= capacity) lines.removeFirst()
            lines.addLast(line)
        }
    }

    fun snapshot(): String = synchronized(lock) {
        if (lines.isEmpty()) return ""
        lines.joinToString("\n")
    }

    fun size(): Int = synchronized(lock) { lines.size }
}
