package com.offgrid.compositor

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraOnlyManifestTest {
    @Test
    fun appManifestRemovesMicrophonePermissions() {
        val text = File("src/main/AndroidManifest.xml").readText()
        assertTrue(text.contains("android.permission.CAMERA"))
        assertTrue(text.contains("android.permission.RECORD_AUDIO"))
        assertTrue(text.contains("android.permission.FOREGROUND_SERVICE_MICROPHONE"))
        assertTrue(text.contains("tools:node=\"remove\""))
        assertTrue(text.contains("Theme.Offgrid.Splash"))
    }

    @Test
    fun splashThemeUsesEmptyIconAndAppBlack() {
        val theme = File("src/main/res/values/themes.xml").readText()
        assertTrue(theme.contains("Theme.Offgrid.Splash"))
        assertTrue(theme.contains("Theme.SplashScreen"))
        assertTrue(theme.contains("@drawable/splash_empty"))
        assertTrue(theme.contains("postSplashScreenTheme"))
        val icon = File("src/main/res/drawable/splash_empty.xml").readText()
        assertTrue(icon.contains("#00000000"))
        assertFalse(icon.contains("#39FF6A"))
    }
}
