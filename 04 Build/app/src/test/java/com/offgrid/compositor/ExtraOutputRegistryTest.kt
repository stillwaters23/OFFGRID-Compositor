package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtraOutputRegistryTest {
    @Test
    fun addReplaceAndRemoveKeepTheRemainingOutputs() {
        val registry = ExtraOutputRegistry<String>()
        assertNull(registry.set("preview", "one"))
        assertEquals("one", registry.set("preview", "two"))
        assertNull(registry.set("stream", "pc"))
        assertEquals(listOf("two", "pc"), registry.values())
        assertEquals("two", registry.set("preview", null))
        assertEquals(listOf("pc"), registry.values())
        assertEquals(listOf("pc"), registry.clear())
        assertEquals(emptyList<String>(), registry.values())
    }
}
