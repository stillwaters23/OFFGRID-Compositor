package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtraOutputRegistryTest {
    @Test
    fun settingReplacingAndRemovingOutputsTracksCurrentTargets() {
        val outputs = ExtraOutputRegistry<String>()
        outputs.set("preview", "first")
        outputs.set("stream", "remote")
        outputs.set("preview", "replacement")

        assertEquals(listOf("replacement", "remote"), outputs.values())
        assertEquals("remote", outputs.remove("stream"))
        assertNull(outputs.remove("missing"))
        assertEquals(listOf("replacement"), outputs.values())
    }

    @Test
    fun settingNullRemovesAnOutput() {
        val outputs = ExtraOutputRegistry<String>()
        outputs.set("preview", "surface")

        outputs.set("preview", null)

        assertEquals(emptyList<String>(), outputs.values())
    }
}
