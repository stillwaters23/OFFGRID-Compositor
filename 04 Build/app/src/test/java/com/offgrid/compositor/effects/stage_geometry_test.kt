package com.offgrid.compositor.effects

import com.offgrid.compositor.effects.StageGeometry.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 1.1: the shared stage geometry. Pure math, every rule from the roadmap's acceptance list. */
class StageGeometryTest {
    private val preview = Slot(1080, 2340, preview = true)
    private val video = Slot(720, 1280, preview = false)

    @Test
    fun previewIsTheMasterInBothArrivalOrders() {
        for (order in listOf(listOf(preview, video), listOf(video, preview))) {
            val main = StageGeometry.pickMain(order)
            assertSame(preview, main)
            val stage = StageGeometry.stageSize(main!!.width, main.height, 1920f)
            assertEquals(1920, stage.height)
            assertEquals(StageGeometry.aspectOf(1080, 2340), stage.aspect, StageGeometry.aspectOf(1080, 2340) * 0.005f)
        }
    }

    @Test
    fun equalAreaTieGoesToPreviewNotArrivalOrder() {
        val screen = Slot(590, 1280, preview = true)
        val record = Slot(590, 1280, preview = false)
        assertSame(screen, StageGeometry.pickMain(listOf(record, screen)))
        assertSame(screen, StageGeometry.pickMain(listOf(screen, record)))
        // The generic form the compositor uses on its own slot class follows the same rule.
        val picked = StageGeometry.pickMain(listOf(record, screen), { it.preview }, { it.width }, { it.height })
        assertSame(screen, picked)
    }

    @Test
    fun withoutAPreviewOutputTheBiggestWins() {
        val small = Slot(720, 1280, preview = false)
        val big = Slot(1080, 1920, preview = false)
        assertSame(big, StageGeometry.pickMain(listOf(small, big)))
        assertNull(StageGeometry.pickMain(emptyList()))
    }

    @Test
    fun stageKeepsTheMasterShapeUnderTheCap() {
        assertEquals(StageGeometry.Size(590, 1280), StageGeometry.stageSize(590, 1280, 1920f))
        val capped = StageGeometry.stageSize(1440, 3120, 1920f)
        assertEquals(1920, capped.height)
        assertEquals(StageGeometry.aspectOf(1440, 3120), capped.aspect, 0.002f)
        assertEquals(StageGeometry.Size(2, 2), StageGeometry.stageSize(0, 0, 1920f))
    }

    @Test
    fun fillCropOfALandscapeClipOnAPortraitStage() {
        val fill = StageGeometry.fillCrop(1920, 1080, 1080, 2340)
        val expectedU = StageGeometry.aspectOf(1080, 2340) / StageGeometry.aspectOf(1920, 1080)
        assertEquals(expectedU, fill.rect.width, 0.001f)
        assertEquals(0.26f, fill.rect.width, 0.005f)
        assertEquals(1f, fill.rect.height, 0f)
        assertEquals((1f - expectedU) / 2f, fill.rect.x, 0.0005f)
        assertEquals(0f, fill.rect.y, 0f)
        assertEquals(2340f / 1080f, fill.scale, 0.0001f)
        assertUniform(1920, 1080, fill, 1080, 2340, fill = true)
    }

    @Test
    fun fitRectOfALandscapeClipOnAPortraitStage() {
        val fit = StageGeometry.fitRect(1920, 1080, 1080, 2340)
        assertEquals(1f, fit.rect.width, 0f)
        val heightPx = fit.rect.height * 2340f
        assertEquals(1080f * 1080f / 1920f, heightPx, 0.5f)
        assertEquals(608f, heightPx, 1f)
        assertEquals(0f, fit.rect.x, 0f)
        assertEquals((1f - fit.rect.height) / 2f, fit.rect.y, 0.0005f)
        assertEquals(1080f / 1920f, fit.scale, 0.0001f)
        assertUniform(1920, 1080, fit, 1080, 2340, fill = false)
    }

    @Test
    fun fitAndFillOfAPortraitClipOnAPortraitStage() {
        val fill = StageGeometry.fillCrop(1080, 1920, 590, 1280)
        assertEquals(1f, fill.rect.height, 0f)
        assertTrue(fill.rect.width < 1f)
        assertUniform(1080, 1920, fill, 590, 1280, fill = true)
        val fit = StageGeometry.fitRect(1080, 1920, 590, 1280)
        assertEquals(1f, fit.rect.width, 0f)
        assertTrue(fit.rect.height < 1f)
        assertUniform(1080, 1920, fit, 590, 1280, fill = false)
        val same = StageGeometry.fillCrop(590, 1280, 590, 1280)
        assertEquals(StageGeometry.WHOLE, same.rect)
    }

    @Test
    fun unknownSizesGiveTheWholePictureAndNoStretch() {
        assertEquals(StageGeometry.WHOLE, StageGeometry.fillCrop(0, 0, 590, 1280).rect)
        assertEquals(StageGeometry.WHOLE, StageGeometry.fitRect(1920, 1080, 0, 0).rect)
    }

    @Test
    fun cropMatrixMapsTheWholeTextureOntoTheCrop() {
        val crop = StageGeometry.fillCrop(1920, 1080, 1080, 2340).rect
        val m = StageGeometry.cropMatrix(crop)
        fun apply(u: Float, v: Float): Pair<Float, Float> =
            Pair(m[0] * u + m[4] * v + m[12], m[1] * u + m[5] * v + m[13])
        assertEquals(Pair(crop.x, crop.y), apply(0f, 0f))
        assertEquals(crop.right, apply(1f, 1f).first, 0.0001f)
        assertEquals(crop.top, apply(1f, 1f).second, 0.0001f)
        assertEquals(crop.x + crop.width / 2f, apply(0.5f, 0.5f).first, 0.0001f)
    }

    @Test
    fun aspectDiffersUsesAOnePercentBand() {
        assertFalse(StageGeometry.aspectDiffers(1440, 3120, 1440, 3120))
        assertFalse(StageGeometry.aspectDiffers(1440, 3120, 720, 1560))
        assertFalse(StageGeometry.aspectDiffers(1440, 3120, 1440, 3140))
        assertTrue(StageGeometry.aspectDiffers(1440, 2900, 1440, 3120))
        assertFalse(StageGeometry.aspectDiffers(0, 0, 1440, 3120))
    }

    @Test
    fun layoutChangeRebindsExactlyOnce() {
        val gate = StageGeometry.RebindGate()
        gate.bound(1440, 2900)
        assertFalse(gate.onLayout(1440, 2900))
        assertTrue(gate.onLayout(1440, 3120))
        assertFalse(gate.onLayout(1440, 3120))
        assertFalse(gate.onLayout(1440, 3120))
        gate.bound(1440, 3120)
        assertFalse(gate.onLayout(1440, 3120))
        assertFalse(gate.onLayout(0, 0))
        assertTrue(gate.onLayout(1440, 2900))
    }

    @Test
    fun gateIsQuietBeforeTheFirstBind() {
        val gate = StageGeometry.RebindGate()
        assertFalse(gate.onLayout(1440, 3120))
    }

    /** Checks the scale is the same on both axes: no non-uniform scale anywhere. */
    private fun assertUniform(srcW: Int, srcH: Int, placement: StageGeometry.Placement, dstW: Int, dstH: Int, fill: Boolean) {
        val r = placement.rect
        val sx: Float
        val sy: Float
        if (fill) {
            // Source pixels shown per target pixel, each axis.
            sx = dstW / (r.width * srcW)
            sy = dstH / (r.height * srcH)
        } else {
            sx = r.width * dstW / srcW
            sy = r.height * dstH / srcH
        }
        assertEquals("sx == sy", sx, sy, 0.0005f)
        assertEquals(placement.scale, sx, 0.0005f)
    }
}
