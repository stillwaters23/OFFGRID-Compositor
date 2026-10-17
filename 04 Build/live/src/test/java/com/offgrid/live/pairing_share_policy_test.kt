package com.offgrid.live

import android.os.Build
import android.view.WindowManager
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * The pairing screen closes only when Android 15 reports the window is inside a recording.
 * Phone 2's preview is a different screen and is not part of this guard.
 *
 * Conscrypt stays off. Robolectric would otherwise install it for the whole test JVM and
 * break the plain JUnit X25519 handshake tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@ConscryptMode(ConscryptMode.Mode.OFF)
class PairingSharePolicyTest {
    @Test
    fun thePolicyNumbersMatchThePlatformConstants() {
        assertEquals(PairingSharePolicy.CALLBACK_API, Build.VERSION_CODES.VANILLA_ICE_CREAM)
        assertEquals(PairingSharePolicy.VISIBLE, WindowManager.SCREEN_RECORDING_STATE_VISIBLE)
        assertEquals(0, WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE)
    }

    @Test
    fun aVisibleShareOnAndroid15ClosesPairing() {
        assertTrue(PairingSharePolicy.shouldClose(35, PairingSharePolicy.VISIBLE))
        assertTrue(PairingSharePolicy.shouldClose(36, WindowManager.SCREEN_RECORDING_STATE_VISIBLE))
    }

    @Test
    fun notVisibleLeavesPairingOpen() {
        assertFalse(PairingSharePolicy.shouldClose(35, WindowManager.SCREEN_RECORDING_STATE_NOT_VISIBLE))
        assertFalse(PairingSharePolicy.shouldClose(35, 0))
        assertFalse("an unknown state is not a share", PairingSharePolicy.shouldClose(36, 2))
    }

    @Test
    fun belowAndroid15DoesNothingEvenWhenTheStateSaysVisible() {
        assertFalse(PairingSharePolicy.shouldClose(34, PairingSharePolicy.VISIBLE))
        assertFalse(PairingSharePolicy.shouldClose(Build.VERSION_CODES.TIRAMISU, WindowManager.SCREEN_RECORDING_STATE_VISIBLE))
        assertFalse(PairingSharePolicy.shouldClose(33, 1))
    }

    @Test
    fun theGuardIsRegisteredOnlyByPairing() {
        val pairing = File("src/main/java/com/offgrid/live/PairingActivity.kt").readText()
        val preview = File("src/main/java/com/offgrid/live/RemotePreviewActivity.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(pairing.contains(".addScreenRecordingCallback("))
        assertTrue(pairing.contains(".removeScreenRecordingCallback("))
        assertTrue(pairing.contains("PairingSharePolicy.shouldClose"))
        assertFalse(preview.contains("addScreenRecordingCallback"))
        assertFalse(preview.contains("removeScreenRecordingCallback"))
        assertFalse(pairing.contains("FLAG_SECURE"))
        assertFalse(preview.contains("FLAG_SECURE"))
        assertFalse(manifest.contains("RECORD_AUDIO"))
        assertFalse(manifest.contains("DETECT_SCREEN_RECORDING"))
    }
}
