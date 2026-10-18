package com.offgrid.compositor.ui.wheel

import com.offgrid.compositor.terminal.NumberColorGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class ReelContractTest {
    @Test
    fun physicsRunsAtSixtyFramesAndKeepsAuricFriction() {
        assertEquals(60, WheelPhysics.FRAMES_PER_SECOND)
        assertEquals(1000.0 / 60.0, WheelPhysics.FRAME_MS, 1e-9)
        assertEquals(0.94, WheelPhysics.FRICTION, 0.0)
        assertEquals(1, WheelPhysics.frameCount(16))
        assertEquals(3, WheelPhysics.frameCount(50))
        assertEquals(Math.pow(0.94, 60.0), WheelPhysics.frictionAfter(60), 1e-9)
    }

    @Test
    fun oneFrameAppliesFrictionThenSnapSettlesOnAWholeNumber() {
        val coast = WheelPhysics.step(50.0, 1.0)
        assertEquals(50.94, coast.position, 1e-9)
        assertEquals(0.94, coast.velocity, 1e-9)
        assertTrue(!coast.settled)

        var sample = WheelPhysics.Sample(40.2, 0.0, false)
        var frames = 0
        while (!sample.settled && frames < WheelPhysics.FRAMES_PER_SECOND) {
            sample = WheelPhysics.step(sample.position, sample.velocity)
            frames++
        }
        assertTrue(sample.settled)
        assertEquals(40.0, sample.position, 1e-6)
        assertTrue(frames < WheelPhysics.FRAMES_PER_SECOND)
    }

    @Test
    fun flingStaysInsideZeroToOneHundredAndLandsOnAnInteger() {
        var sample = WheelPhysics.Sample(10.0, 6.0, false)
        var frames = 0
        while (!sample.settled && frames < 300) {
            sample = WheelPhysics.step(sample.position, sample.velocity)
            frames++
            assertTrue(sample.position in 0.0..100.0)
        }
        assertTrue(sample.settled)
        assertEquals(sample.position, WheelPhysics.snap(sample.position).toDouble(), 0.0)
    }

    @Test
    fun reducedMotionSnapsOnTheSameFrame() {
        val sample = WheelPhysics.step(18.6, 4.0, reducedMotion = true)
        assertTrue(sample.settled)
        assertEquals(19.0, sample.position, 0.0)
        assertEquals(0.0, sample.velocity, 0.0)
    }

    @Test
    fun dragUpLowersTheNumberAndKeysStayInRange() {
        val (delta, velocity) = WheelPhysics.dragDelta(dyPixels = -56.0, dtMs = 16.0)
        assertTrue(delta < 0.0)
        assertTrue(velocity < 0.0)
        val down = WheelPhysics.dragDelta(dyPixels = 28.0, dtMs = 16.0).first
        assertTrue(down > 0.0)
        assertEquals(0, WheelPhysics.keyTarget(4, WheelPhysics.Key.HOME))
        assertEquals(100, WheelPhysics.keyTarget(4, WheelPhysics.Key.END))
        assertEquals(14, WheelPhysics.keyTarget(4, WheelPhysics.Key.PAGE_UP))
        assertEquals(0, WheelPhysics.keyTarget(4, WheelPhysics.Key.PAGE_DOWN))
        assertEquals(100, WheelPhysics.keyTarget(99, WheelPhysics.Key.UP))
        val fling = WheelPhysics.flingNotchesPerFrame(-2800.0, 28.0)
        assertTrue(fling < 0.0)
    }

    @Test
    fun resizeKeepsTheValueAndFitsTheGlyphAtEveryScale() {
        assertEquals(77, ReelGeometry.valueAfterResize(77))
        assertEquals(0, ReelGeometry.valueAfterResize(-3))
        assertEquals(100, ReelGeometry.valueAfterResize(140))
        val scales = listOf(0.75f, 1f, 1.45f)
        val widths = listOf(72, 120, 180, 320, 480)
        for (scale in scales) {
            for (width in widths) {
                val fit = ReelGeometry.fit(width, 10_000, scale, density = 1f)
                assertTrue("glyph clipped at $width scale $scale", fit.glyphFits)
                assertTrue(fit.fontPx * 3f * 0.62f <= fit.innerWidthPx + 0.51f)
                assertTrue(fit.heightPx > 0)
                val center = fit.heightPx / 2f
                val selected = ReelGeometry.projectedY(center, 0f, fit.radiusPx)
                assertEquals(center, selected, 0.01f)
                val neighbor = ReelGeometry.projectedY(center, 1f, fit.radiusPx)
                assertTrue(abs(neighbor - center) > 1f)
                assertTrue(ReelGeometry.depth(0f) > ReelGeometry.depth(2f))
            }
        }
    }

    @Test
    fun narrowHeightShrinksTheBarrelInsteadOfRunningOff() {
        val wide = ReelGeometry.fit(400, 10_000, 1.45f, 2f)
        val short = ReelGeometry.fit(400, 90, 1.45f, 2f)
        assertTrue(short.heightPx <= 90)
        assertTrue(short.fontPx < wide.fontPx)
        assertTrue(short.glyphFits)
    }

    @Test
    fun laserCoreIsWhiteHotAndBloomIsTheLaserColor() {
        val core = LaserStrip.coreArgb(100)
        assertTrue(LaserStrip.channel(core, 16) >= LaserStrip.CORE_MIN_CHANNEL)
        assertTrue(LaserStrip.channel(core, 8) >= LaserStrip.CORE_MIN_CHANNEL)
        assertTrue(LaserStrip.channel(core, 0) >= LaserStrip.CORE_MIN_CHANNEL)
        assertEquals(0, LaserStrip.coreArgb(0))
        assertTrue(LaserStrip.strokes(LaserStrip.DEFAULT_COLOR, 0, 1f).isEmpty())

        val bloom = LaserStrip.bloomArgb(LaserStrip.DEFAULT_COLOR, 100, distancePx = 0f, spreadPx = 14f)
        assertEquals(0x2B, LaserStrip.channel(bloom, 16))
        assertEquals(0xF5, LaserStrip.channel(bloom, 8))
        assertEquals(0xE6, LaserStrip.channel(bloom, 0))
        assertTrue(LaserStrip.saturation(bloom) >= 0.6)
        assertTrue(LaserStrip.channel(bloom, 24) > 12)
        val faded = LaserStrip.bloomArgb(LaserStrip.DEFAULT_COLOR, 100, distancePx = 14f, spreadPx = 14f)
        assertTrue(LaserStrip.channel(faded, 24) < 13)

        val strokes = LaserStrip.strokes(LaserStrip.DEFAULT_COLOR, 100, 1f)
        val coreStroke = strokes.single { it.core }
        assertTrue(LaserStrip.channel(coreStroke.color, 16) >= LaserStrip.CORE_MIN_CHANNEL)
        assertTrue(strokes.first().widthPx > coreStroke.widthPx)
    }

    @Test
    fun oneStripCoversTheGapBetweenReels() {
        assertTrue(LaserStrip.coversGap(0f, 400f, 180f, 200f))
        assertTrue(!LaserStrip.coversGap(0f, 100f, 180f, 200f))
        assertTrue(!LaserStrip.coversGap(20f, 400f, 0f, 16f))
    }

    @Test
    fun selectedNumbersClearFourPointFiveOnTheBloom() {
        val lasers = listOf(LaserStrip.DEFAULT_COLOR, 0xFFFF00A6.toInt(), 0xFFFFFFFF.toInt())
        val backgrounds = listOf(0xFF000000.toInt(), NumberColorGuard.DARK_BACKGROUND, 0xFFF6F1E8.toInt(), 0xFFFFFFFF.toInt())
        val colors = listOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF777777.toInt(), 0xFF2BF5E6.toInt())
        for (laser in lasers) {
            for (background in backgrounds) {
                for (dimmer in listOf(0, 40, 100)) {
                    for (color in colors) {
                        val paint = NumberColorGuard.colorOnBeam(color, background, laser, dimmer)
                        val beam = LaserStrip.bloomBackground(background, laser, dimmer)
                        val ratio = NumberColorGuard.contrastRatio(paint, beam)
                        assertTrue("contrast $ratio for $color on $background dimmer $dimmer", ratio >= NumberColorGuard.MIN_CONTRAST)
                    }
                }
            }
        }
    }

    @Test
    fun androidGradlePluginStaysAtEightThirteenTwo() {
        val gradle = listOf(
            File("../build.gradle.kts"),
            File("build.gradle.kts"),
            File("04 Build/build.gradle.kts"),
        ).filter { it.exists() }.joinToString("\n") { it.readText() }
        assertTrue(gradle.contains("id(\"com.android.application\") version \"8.13.2\""))
    }
}
