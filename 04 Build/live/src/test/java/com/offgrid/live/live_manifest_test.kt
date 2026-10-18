package com.offgrid.live

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveManifestTest {
    @Test
    fun networkPermissionIsInternetOnly() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue(manifest.isFile)
        val text = manifest.readText()
        assertTrue(text.contains("android.permission.INTERNET"))
        assertFalse(text.contains("ACCESS_WIFI_STATE"))
        assertFalse(text.contains("CHANGE_WIFI_STATE"))
        assertFalse(text.contains("CHANGE_WIFI_MULTICAST_STATE"))
        assertFalse(text.contains("NEARBY_WIFI_DEVICES"))
        assertFalse(text.contains("specialUse|camera|microphone"))
        assertTrue(text.contains("tools:node=\"remove\""))
    }
}
