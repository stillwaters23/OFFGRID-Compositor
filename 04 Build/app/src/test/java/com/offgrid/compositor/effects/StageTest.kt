package com.offgrid.compositor.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Checks the camera stage's math (final build, M4): the no-face modes, time freeze, quiet
 * mode, depth shift, shatter, echo, and takeover. It runs on the computer that builds the
 * app, not on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StageTest {
    private fun settings(
        numbers: Map<String, Int> = emptyMap(),
        toggles: Map<String, Boolean> = emptyMap(),
        choices: Map<String, String> = emptyMap(),
    ) = EffectSnapshot(numbers, toggles, choices, emptyMap())

    private fun plan(
        time: Float,
        s: EffectSnapshot,
        fired: List<Fired> = emptyList(),
        face: FaceSpot? = null,
        changedAt: Float = -100f,
        before: String = FxKeys.MODE_LIVE,
    ) = StagePlan().update(time, s, fired, face, changedAt, before)

    @Test
    fun nothingOnLeavesTheCameraUntouched() {
        assertTrue(plan(5f, settings()).plain)
        assertTrue(plan(5f, EffectSnapshot.EMPTY).plain)
    }

    @Test
    fun aSavedNoFaceModeCoversTheFirstPictureWithoutFading() {
        val engine = EffectEngine()
        engine.update(settings(choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_CLOAK)))
        assertEquals(-100f, engine.modeChangedAt)
        val first = plan(engine.now(), engine.settings, changedAt = engine.modeChangedAt, before = engine.previousMode)
        assertEquals(FxKeys.MODE_CLOAK, first.mode)
        assertEquals(1f, first.modeMix)
        assertFalse(first.plain)
    }

    @Test
    fun cloakFadesInOverItsTypedTimeAndGhostIsInstant() {
        val cloak = settings(
            numbers = mapOf(FxKeys.CLOAK_FADE to 0),
            choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_CLOAK),
        )
        val half = plan(10.05f, cloak, changedAt = 10f)
        assertEquals(0.5f, half.modeMix, 0.01f)
        assertEquals(1f, plan(10.2f, cloak, changedAt = 10f).modeMix)
        val ghost = settings(choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_GHOST))
        assertEquals(1f, plan(10f, ghost, changedAt = 10f).modeMix)
        assertTrue("ghost keeps the last picture unless blank is picked", plan(10f, ghost).ghostFrozen)
        val blank = settings(choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_GHOST, FxKeys.GHOST_STYLE to FxKeys.GHOST_BLANK))
        assertFalse(plan(10f, blank).ghostFrozen)
    }

    @Test
    fun timeFreezeHoldsThenLetsGoAndNeverFlickersMoreThanThreeTimesASecond() {
        for (length in listOf(0, 3, 50, 100)) {
            val s = settings(numbers = mapOf(FxKeys.FREEZE_LENGTH to length, FxKeys.FREEZE_GLITCH to 100))
            val fired = listOf(Fired(Fire.TIME_FREEZE, 1f, null, 7L))
            val seconds = EffectTimes.freezeSeconds(s)
            assertEquals(7L, plan(1.01f, s, fired).holdId)
            assertEquals(7L, plan(1f + seconds - 0.01f, s, fired).holdId)
            assertEquals(0L, plan(1f + seconds + 0.01f, s, fired).holdId)
            assertTrue(plan(1f + seconds + 0.01f, s, fired).plain)
            assertAtMostThreeFlashesASecond(1f, 1f + seconds) { t -> plan(t, s, fired).glitch > 0.05f }
        }
    }

    @Test
    fun quietModeDarkensHoldsAndComesBack() {
        val s = settings()
        val quiet = EffectTimes.quiet(s)
        val fired = listOf(Fired(Fire.QUIET, 2f, null, 9L))
        assertEquals(1f, plan(2f, s, fired).brightness, 0.01f)
        val dark = plan(2f + quiet.fade + quiet.dark / 2f, s, fired)
        assertEquals(9L, dark.holdId)
        assertEquals(StagePlan.QUIET_DARK, dark.brightness, 0.001f)
        val back = plan(2f + quiet.total - 0.001f, s, fired)
        assertEquals(0L, back.holdId)
        assertTrue(back.brightness > 0.95f)
        assertTrue(plan(2f + quiet.total + 0.01f, s, fired).plain)
        assertTrue("the boot light flickers no faster than 3 a second", quiet.flicker / quiet.flickers >= 1f / 3f)
    }

    @Test
    fun depthShiftSettlesPushesHoldsAndSnapsBack() {
        val s = settings()
        val times = EffectTimes.depth(s)
        val fired = listOf(Fired(Fire.DEPTH_SHIFT, 0f, null, 3L))
        var most = 0f
        var t = 0f
        while (t < times.total) {
            val p = plan(t, s, fired)
            assertTrue(p.depth in 0f..1f && p.depthFrame in 0f..1f)
            most = maxOf(most, p.depth)
            t += 1f / 60f
        }
        assertEquals(1f, most, 0.001f)
        val after = plan(times.total + 0.01f, s, fired)
        assertEquals(0f, after.depth)
        assertTrue(after.plain)
    }

    @Test
    fun shatterPiecesFillThePictureFallAwayAndComeBack() {
        val s = settings(numbers = mapOf(FxKeys.SHATTER_PIECES to 50))
        val times = EffectTimes.shatter(s)
        val fired = listOf(Fired(Fire.SHATTER, 0f, "0.3,0.6", 11L))
        val p = plan(0.1f, s, fired)
        assertEquals(11L, p.shatterId)
        assertEquals(0.3f, p.impactX, 0.001f)
        assertEquals(0.6f, p.impactY, 0.001f)
        val mesh = ShatterMesh()
        mesh.build(p.shatterPieces, p.impactX, p.impactY, 0.46f, p.shatterId)
        assertTrue(mesh.count >= p.shatterPieces)
        // The pieces cover the whole picture with no gaps and no overlaps.
        var area = 0.0
        for (i in 0 until mesh.count) {
            val h = mesh.home
            val b = i * 6
            area += abs(((h[b + 2] - h[b]) * (h[b + 5] - h[b + 1]) - (h[b + 4] - h[b]) * (h[b + 3] - h[b + 1])) / 2.0)
        }
        assertEquals(1.0, area, 1e-4)
        mesh.pose(0.1f, times)
        assertTrue(mesh.placed.contentEquals(mesh.home))
        mesh.pose(times.crack + times.fall + times.broken / 2f, times)
        assertFalse("every piece has fallen out of view", mesh.shown.any { it })
        mesh.pose(times.total - 0.0001f, times)
        for (k in mesh.home.indices) assertEquals(mesh.home[k], mesh.placed[k], 0.01f)
        assertEquals(0L, plan(times.total + 0.01f, s, fired).shatterId)
    }

    @Test
    fun echoTakesItsTypedCopies() {
        assertEquals(2, EffectTimes.echoCopies(settings(numbers = mapOf(FxKeys.ECHO_COPIES to 0))))
        assertEquals(EffectTimes.MAX_ECHO_COPIES, EffectTimes.echoCopies(settings(numbers = mapOf(FxKeys.ECHO_COPIES to 100))))
        val on = plan(1f, settings(toggles = mapOf(FxKeys.ECHO_ON to true)))
        assertTrue(on.echoOn)
        assertFalse(on.plain)
    }

    @Test
    fun takeoverZoomKeepsTheFaceDiamondOnTheFace() {
        val s = settings(numbers = mapOf(FxKeys.TAKEOVER_ZOOM to 100), toggles = mapOf(FxKeys.TAKEOVER_ON to true))
        val face = FaceSpot(0.5f, 0.4f, 0.2f, 0f)
        val shown = StageMath.faceInVideo(face, s)
        assertNotNull(shown)
        assertEquals(0.5f, shown!!.x, 0.001f)
        assertEquals(0.5f, shown.y, 0.001f)
        assertEquals(0.5f, shown.size, 0.001f)
        // A face near the edge: the window stops at the picture's edge, and the face is still mapped right.
        val edge = FaceSpot(0.05f, 0.1f, 0.1f, 0f)
        val window = StageMath.zoomWindow(edge, 2.5f, StageMath.Window())
        val mapped = StageMath.faceInVideo(edge, s)!!
        assertEquals((edge.x - window.x) * 2.5f + 0.5f, mapped.x, 0.001f)
        assertTrue(window.x - 0.5f / 2.5f >= -0.0001f)
        // Flip-flop puts the zoom in the small box, so the big picture's face stays where it was.
        val flipped = settings(toggles = mapOf(FxKeys.TAKEOVER_ON to true, FxKeys.TAKEOVER_FLIP to true))
        assertEquals(face.x, StageMath.faceInVideo(face, flipped)!!.x)
    }

    /** Samples at 30 pictures a second and counts flicker starts in every 1 second span. */
    private fun assertAtMostThreeFlashesASecond(from: Float, to: Float, lit: (Float) -> Boolean) {
        val starts = mutableListOf<Float>()
        var was = false
        var t = from
        while (t <= to) {
            val now = lit(t)
            if (now && !was) starts += t
            was = now
            t += 1f / 30f
        }
        for (start in starts) {
            val inSecond = starts.count { it >= start && it < start + 1f }
            assertTrue("$inSecond flickers within a second from $start", inSecond <= 3)
        }
    }
}
