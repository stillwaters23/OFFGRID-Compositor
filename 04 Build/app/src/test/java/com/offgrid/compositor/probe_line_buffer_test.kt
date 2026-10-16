package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeLineBufferTest {
    @Test
    fun keepsOnlyTheLastNLines() {
        val buffer = ProbeLineBuffer(3)
        buffer.add("a")
        buffer.add("b")
        buffer.add("c")
        buffer.add("d")
        assertEquals(3, buffer.size())
        assertEquals("b\nc\nd", buffer.snapshot())
    }

    @Test
    fun emptySnapshotIsBlank() {
        val buffer = ProbeLineBuffer(200)
        assertEquals("", buffer.snapshot())
        assertEquals(0, buffer.size())
    }
}
