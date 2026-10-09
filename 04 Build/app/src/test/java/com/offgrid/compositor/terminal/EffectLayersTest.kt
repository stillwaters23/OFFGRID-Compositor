package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectSnapshot
import com.offgrid.compositor.effects.FaceSpot
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.Fired
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.InkStroke
import com.offgrid.compositor.effects.OverlayLayer
import com.offgrid.compositor.effects.layers.BeachLayer
import com.offgrid.compositor.effects.layers.CloudsLayer
import com.offgrid.compositor.effects.layers.EmojiRainLayer
import com.offgrid.compositor.effects.layers.FaceDiamondLayer
import com.offgrid.compositor.effects.layers.FireworksLayer
import com.offgrid.compositor.effects.layers.FlyingHeroesLayer
import com.offgrid.compositor.effects.layers.GlowBorderLayer
import com.offgrid.compositor.effects.layers.IllusionLayer
import com.offgrid.compositor.effects.layers.InkLayer
import com.offgrid.compositor.effects.layers.LaserWriteLayer
import com.offgrid.compositor.effects.layers.PortalLayer
import com.offgrid.compositor.effects.layers.QuietLightLayer
import com.offgrid.compositor.effects.layers.RadarLayer
import com.offgrid.compositor.effects.layers.RedSweepLayer
import com.offgrid.compositor.effects.layers.RollerCoasterLayer
import com.offgrid.compositor.effects.layers.StatusLabelLayer
import com.offgrid.compositor.effects.layers.TestCodeLayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks every drawn effect (final build, M4): it is off when its setting is off, it draws
 * without failing when on, and it never flashes more than 3 times a second. It runs on the
 * computer that builds the app, not on the phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class EffectLayersTest {
    private val w = 150
    private val h = 325

    private fun settings(toggles: Map<String, Boolean> = emptyMap(), choices: Map<String, String> = emptyMap(),
                         numbers: Map<String, Int> = emptyMap(), texts: Map<String, String> = emptyMap()): EffectSnapshot {
        val d = PanelTree.defaults
        return EffectSnapshot(d.numbers + numbers, d.toggles + toggles, d.choices + choices, d.texts + texts)
    }

    private val face = FaceSpot(0.5f, 0.36f, 0.32f, 0f)

    private fun frame(t: Float, s: EffectSnapshot, fired: List<Fired> = emptyList(), withFace: Boolean = true,
                      ink: List<InkStroke> = emptyList(), changedAt: Float = -100f, before: String = FxKeys.MODE_LIVE) =
        EffectFrame(w, h, t, s, fired, if (withFace) FaceSpot(face.x, face.y, face.size, t) else null, ink,
            0xFF2BF5E6.toInt(), false, changedAt, before)

    private fun camera(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF8A6A58.toInt(), 0xFF4B5A46.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
        p.color = 0xFFE0B49A.toInt()
        c.drawOval(w * 0.34f, h * 0.24f, w * 0.66f, h * 0.48f, p)
        p.color = 0xFF2C5A8C.toInt()
        c.drawRect(w * 0.18f, h * 0.55f, w * 0.82f, h.toFloat(), p)
    }

    private class Case(val name: String, val layer: OverlayLayer, val frames: List<EffectFrame>)

    private fun drawAll(case: Case) {
        val cell = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (f in case.frames) {
            val c = Canvas(cell)
            camera(c)
            assertTrue("${case.name} should be on", case.layer.isActive(f))
            val save = c.save()
            case.layer.draw(c, f)
            c.restoreToCount(save)
        }
    }

    @Test
    fun everyEffectDrawsAndNeverFlashesFast() {
        val ts = listOf(0.3f, 1.1f, 2.4f, 4.0f)
        fun on(key: String, extra: Map<String, String> = emptyMap()) = settings(toggles = mapOf(key to true), choices = extra)
        fun fired(kind: Fire, arg: String? = null) = listOf(Fired(kind, 0f, arg, 1L))
        val stroke = InkStroke(floatArrayOf(0.2f, 0.7f, 0.35f, 0.62f, 0.5f, 0.7f, 0.65f, 0.6f, 0.8f, 0.72f), 10, null, 0.3f, 0.6f, 0xFF2BF5E6.toInt())
        val pinned = InkStroke(floatArrayOf(0.42f, 0.3f, 0.5f, 0.27f, 0.58f, 0.3f), 6, face, 0.4f, 0.8f, 0xFFFF3D9A.toInt())
        val cases = listOf(
            Case("emoji", EmojiRainLayer(), ts.map { frame(it, on(FxKeys.EMOJI_ON)) }),
            Case("fireworks", FireworksLayer(), listOf(0.2f, 0.6f, 1.2f, 2.0f).map { frame(it, settings(), fired(Fire.FIREWORKS)) }),
            Case("border", GlowBorderLayer(), listOf(FxKeys.COLOR_PANEL, FxKeys.COLOR_WHITE, FxKeys.COLOR_RAINBOW, FxKeys.COLOR_HUE).mapIndexed { i, c ->
                frame(1f + i, settings(toggles = mapOf(FxKeys.BORDER_ON to true), choices = mapOf(FxKeys.BORDER_COLOR to c))) }),
            Case("sweep", RedSweepLayer(), listOf(0.4f, 1.5f, 6f, 14.0f).map { frame(it, settings(), fired(Fire.RED_SWEEP)) }),
            Case("write", LaserWriteLayer(), listOf(0.5f, 1.5f, 3.5f, 6.5f).map { frame(it, settings(), fired(Fire.LASER_WRITE, "Royalty is here")) }),
            Case("ink", InkLayer(), listOf(frame(1f, settings(), ink = listOf(stroke, pinned)), frame(1f, settings(), ink = listOf(stroke, pinned), withFace = false))),
            Case("diamond", FaceDiamondLayer(), listOf(FxKeys.DIAMOND_CLASSIC, FxKeys.DIAMOND_LASER, FxKeys.DIAMOND_BLACK).map {
                frame(1f, settings(choices = mapOf(FxKeys.DIAMOND_STYLE to it))) } + frame(1f, settings(), withFace = false)),
            Case("radar", RadarLayer(), listOf(frame(1f, on(FxKeys.RADAR_ON)), frame(2.5f, on(FxKeys.RADAR_ON)),
                frame(1f, on(FxKeys.RADAR_ON, mapOf(FxKeys.RADAR_MODE to FxKeys.RADAR_ROOM))), frame(1f, on(FxKeys.MIRROR_ON)))),
            Case("illusion", IllusionLayer(), listOf(FxKeys.ILLUSION_TUNNEL, FxKeys.ILLUSION_TRIANGLE, FxKeys.ILLUSION_STAIRS, FxKeys.ILLUSION_DRIFT).map {
                frame(2f, on(FxKeys.ILLUSION_ON, mapOf(FxKeys.ILLUSION_KIND to it))) }),
            Case("quiet", QuietLightLayer(), listOf(0.3f, 1.2f, 2.5f, 3.2f).map { frame(it, settings(), fired(Fire.QUIET)) }),
            Case("testcode", TestCodeLayer(), listOf(frame(2f, settings(), fired(Fire.TEST_CODE, "482913")))),
            Case("beach", BeachLayer(), ts.map { frame(it, on(FxKeys.BEACH_ON)) }),
            Case("clouds", CloudsLayer(), listOf(frame(1f, on(FxKeys.CLOUDS_ON)), frame(3f, on(FxKeys.CLOUDS_ON)),
                frame(1f, on(FxKeys.CLOUDS_ON, mapOf(FxKeys.CLOUDS_PLACE to FxKeys.CLOUDS_AROUND))), frame(3f, on(FxKeys.CLOUDS_ON, mapOf(FxKeys.CLOUDS_PLACE to FxKeys.CLOUDS_AROUND))))),
            Case("status", StatusLabelLayer(), listOf(0.2f, 0.6f, 2f, 5f).map {
                frame(it, settings(choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_STATUS)), changedAt = 0f) }),
            Case("heroes", FlyingHeroesLayer(), listOf(FxKeys.HERO_NOVA, FxKeys.HERO_VOLT, FxKeys.HERO_COMET, FxKeys.HERO_PIXEL).map {
                frame(2f, settings(toggles = mapOf(FxKeys.HEROES_ON to true), choices = mapOf(FxKeys.HEROES_HERO to it), numbers = mapOf(FxKeys.HEROES_COUNT to 60, FxKeys.HEROES_SIZE to 60))) }),
            Case("coaster", RollerCoasterLayer(), listOf(frame(1f, on(FxKeys.COASTER_ON)), frame(3f, on(FxKeys.COASTER_ON)),
                frame(1f, on(FxKeys.COASTER_ON, mapOf(FxKeys.COASTER_VIEW to FxKeys.COASTER_RIDE))), frame(3f, on(FxKeys.COASTER_ON, mapOf(FxKeys.COASTER_VIEW to FxKeys.COASTER_RIDE))))),
            Case("portal", PortalLayer(), listOf(FxKeys.PORTAL_SCENE, FxKeys.PORTAL_WORMHOLE, FxKeys.PORTAL_VOID).map {
                frame(2f, on(FxKeys.PORTAL_ON, mapOf(FxKeys.PORTAL_STYLE to it))) }),
        )
        for (case in cases) {
            drawAll(case)
            // Off means nothing draws.
            assertFalse("${case.name} off", case.layer.isActive(frame(1f, settings(choices = mapOf(FxKeys.CAMERA_MODE to FxKeys.MODE_LIVE)), withFace = true).let {
                EffectFrame(it.width, it.height, it.time, EffectSnapshot.EMPTY, emptyList(), it.face, emptyList(), it.panelColor, false)
            }))
        }
        // No effect flashes more than 3 times a second: whole-picture brightness jumps, at 30 pictures a second.
        val small = Bitmap.createBitmap(150, 325, Bitmap.Config.ARGB_8888)
        for (case in cases) {
            val base = case.frames.first()
            val starts = ArrayList<Float>()
            var last = -1.0
            var t = 0f
            while (t < 6f) {
                val f = EffectFrame(150, 325, t, base.settings, base.fired, base.face, base.ink, base.panelColor, false, base.modeChangedAt, base.previousMode)
                val c = Canvas(small)
                camera(c)
                if (case.layer.isActive(f)) {
                    c.save()
                    c.scale(1f, 1f)
                    case.layer.draw(c, f)
                    c.restore()
                }
                val px = IntArray(150 * 325)
                small.getPixels(px, 0, 150, 0, 0, 150, 325)
                var sum = 0.0
                for (p in px) sum += (Color.red(p) + Color.green(p) + Color.blue(p)) / 3.0
                val mean = sum / px.size
                if (last >= 0 && mean - last > FLASH_JUMP) starts += t
                last = mean
                t += 1f / 30f
            }
            var worst = 0
            for (s0 in starts) worst = maxOf(worst, starts.count { it >= s0 && it < s0 + 1f })
            assertTrue("${case.name} flashes $worst times in a second", worst <= 3)
        }
    }

    private companion object {
        /** A brightness jump of this much (0 to 255, whole picture) counts as a flash. */
        const val FLASH_JUMP = 12.0
    }
}
