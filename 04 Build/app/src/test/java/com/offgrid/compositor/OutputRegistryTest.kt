package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutputRegistryTest {
    @Test
    fun addingReplacingAndRemovingOutputsKeepsOnlyTheCurrentEntry() {
        val outputs = OutputRegistry<String>()

        assertNull(outputs.put("preview", "first"))
        assertEquals("first", outputs.put("preview", "replacement"))
        assertEquals(listOf("replacement"), outputs.values())
        assertEquals("replacement", outputs.remove("preview"))
        assertNull(outputs.remove("preview"))
        assertEquals(emptyList<String>(), outputs.values())
    }
}
