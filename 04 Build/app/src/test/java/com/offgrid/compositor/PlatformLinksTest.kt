package com.offgrid.compositor

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlatformLinksTest {
    @Test
    fun kickHasAPackageAndNeverOpensTheStore() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertEquals("com.kick.mobile", PlatformLinks.PACKAGES["kick"])
        assertEquals(PlatformLinks.Opened.MISSING, PlatformLinks.open(context, "kick", "Kick"))
        assertFalse(PlatformLinks.Opened.entries.any { it.name == "STORE" })
    }

    @Test
    fun sourcesDoNotContainPlayStoreIntents() {
        val roots = listOf(
            File("src/main/java"),
            File("src/main/res"),
            File("../live/src/main/java"),
        )
        val hits = ArrayList<String>()
        for (root in roots) {
            if (!root.exists()) continue
            root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.forEach { file ->
                val text = file.readText()
                if (text.contains("market://") || text.contains("play.google.com/store")) {
                    hits += file.path
                }
            }
        }
        assertTrue("Play Store intents found in:\n${hits.joinToString("\n")}", hits.isEmpty())
    }
}
