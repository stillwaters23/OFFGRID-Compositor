package com.offgrid.compositor

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Checks the camera screen's helpers (final build, M4): the face finder's math, the built-in
 * sounds, the handshake codes, and gallery names. It runs on the computer that builds the app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CameraToolsTest {
    private val sensor = Rect(0, 0, 4000, 3000)

    @Test
    fun aFaceInTheMiddleOfTheSensorIsInTheMiddleOfTheVideo() {
        val face = Rect(1800, 1300, 2200, 1700)
        val spot = FaceMath.toVideo(face, sensor, 16f / 9f, 270, 9f / 19.5f, flipX = false)
        assertNotNull(spot)
        assertEquals(0.5f, spot!![0], 0.01f)
        assertEquals(0.5f, spot[1], 0.01f)
        assertTrue(spot[2] > 0f)
    }

    @Test
    fun theFrontCameraTurnIsUndone() {
        // Near the sensor's top edge, middle across: with a 270 turn that is the video's left side.
        val face = Rect(1900, 600, 2100, 800)
        val spot = FaceMath.toVideo(face, sensor, 16f / 9f, 270, 9f / 16f, flipX = false)!!
        assertTrue("x was ${spot[0]}", spot[0] < 0.3f)
        assertEquals(0.5f, spot[1], 0.02f)
        val flipped = FaceMath.toVideo(face, sensor, 16f / 9f, 270, 9f / 16f, flipX = true)!!
        assertEquals(1f - spot[0], flipped[0], 0.001f)
        // A face cut off by the video's sides is not shown.
        assertNull(FaceMath.toVideo(Rect(0, 0, 10, 10), sensor, 16f / 9f, 270, 9f / 30f, flipX = false))
    }

    @Test
    fun everySoundIsShortAndNeverClips() {
        for (name in listOf("chime", "pop", "whoosh", "glass", "power", "ping", "sparkle", "boom")) {
            val samples = SoundShapes.make(name)
            assertTrue(name, samples.isNotEmpty())
            assertTrue("$name is short", samples.size <= SoundShapes.RATE * 2)
            assertTrue("$name is not silent", samples.any { abs(it.toInt()) > 1000 })
            assertTrue("$name starts and ends softly", abs(samples.first().toInt()) < 200 && abs(samples.last().toInt()) < 200)
        }
        assertTrue(SoundShapes.make("off").isEmpty())
    }

    @Test
    fun handshakeCodesAreLongRandomAndOnlyFingerprinted() {
        val one = Handshake.makeCode()
        val two = Handshake.makeCode()
        assertEquals(Handshake.CODE_LENGTH, one.length)
        assertNotEquals(one, two)
        assertTrue(Handshake.looksLikeCode(one))
        assertFalse(Handshake.looksLikeCode(one.dropLast(1)))
        assertFalse(Handshake.looksLikeCode(one.dropLast(1) + "0"))
        val print = Handshake.fingerprint(one)
        assertEquals(64, print.length)
        assertFalse("a fingerprint never holds the code", one.contains(print) || print.contains(one.take(20)))
    }

    @Test
    fun galleryNamesAreTitleCaseWithSpaces() {
        val at = Calendar.getInstance(Locale.US).apply { set(2026, Calendar.OCTOBER, 9, 14, 3, 22) }.time
        assertEquals("OFFGRID Video 2026-10-09 14.03.22.mp4", GallerySaver.fileName("Video", at))
    }
}
