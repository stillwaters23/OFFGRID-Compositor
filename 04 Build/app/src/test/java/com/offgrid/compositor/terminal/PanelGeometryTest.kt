package com.offgrid.compositor.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelGeometryTest {
    @Test
    fun fullBleedNeedsBothDimensionsUnlessFullScreenIsSelected() {
        assertEquals(true, PanelGeometry.isFullBleed(100, 100, fullScreen = false))
        assertEquals(false, PanelGeometry.isFullBleed(100, 99, fullScreen = false))
        assertEquals(false, PanelGeometry.isFullBleed(99, 100, fullScreen = false))
        assertEquals(true, PanelGeometry.isFullBleed(40, 60, fullScreen = true))
    }

    @Test
    fun panelSizeUsesFullAvailableAreaAtOneHundredPercent() {
        val width = PanelGeometry.sizeRange(1080, minimum = 240)
        val height = PanelGeometry.sizeRange(2400, minimum = 320)

        assertEquals(1080, PanelGeometry.sizeFor(100, width))
        assertEquals(2400, PanelGeometry.sizeFor(100, height))
        assertEquals(100, PanelGeometry.percentFor(1080f, width))
        assertEquals(100, PanelGeometry.percentFor(2400f, height))
    }

    @Test
    fun panelSizeClampsPercentAndHandlesScreensSmallerThanMinimum() {
        val range = PanelGeometry.sizeRange(800, minimum = 240)
        assertEquals(240, PanelGeometry.sizeFor(-1, range))
        assertEquals(520, PanelGeometry.sizeFor(50, range))
        assertEquals(800, PanelGeometry.sizeFor(101, range))
        assertEquals(0 to 0, PanelGeometry.sizeRange(0, minimum = 240))
        assertEquals(0, PanelGeometry.sizeFor(50, 0 to 0))
        assertEquals(100, PanelGeometry.percentFor(0f, 0 to 0))
    }

    @Test
    fun fullBleedBorderStaysInsideDisplayCorners() {
        assertEquals(2f, PanelGeometry.borderStrokeDp(0), 0f)
        assertEquals(3f, PanelGeometry.borderStrokeDp(50), 0f)
        assertEquals(6f, PanelGeometry.borderStrokeDp(100), 0f)
        assertEquals(2f, PanelGeometry.borderInsetDp(50), 0f)
        assertEquals(18f, PanelGeometry.borderCornerRadius(20f, 2f), 0f)
        assertEquals(0f, PanelGeometry.borderCornerRadius(1f, 2f), 0f)
    }

    @Test
    fun glowCurveIsContinuousAndGetsBrighterAboveTheNormalSetting() {
        assertEquals(0f, PanelGeometry.glowStrength(0), 0f)
        assertEquals(0.7f, PanelGeometry.glowStrength(50), 0f)
        assertEquals(1f, PanelGeometry.glowStrength(100), 0f)
        assertTrue(PanelGeometry.glowStrength(75) > PanelGeometry.glowStrength(50))
        assertTrue(PanelGeometry.glowBloom(100) > PanelGeometry.glowBloom(50))
    }

    @Test
    fun resizeZonesAvoidSystemGestureInsets() {
        assertEquals(
            PanelResizeZone.BOTTOM_LEFT,
            PanelGeometry.resizeZone(30f, 360f, 300f, 400f, 24f, 20f, 24f, 24f),
        )
        assertEquals(
            PanelResizeZone.BOTTOM_EDGE,
            PanelGeometry.resizeZone(150f, 360f, 300f, 400f, 24f, 20f, 24f, 24f),
        )
        assertEquals(
            PanelResizeZone.BOTTOM_RIGHT,
            PanelGeometry.resizeZone(265f, 360f, 300f, 400f, 24f, 20f, 24f, 24f),
        )
        assertEquals(
            PanelResizeZone.NONE,
            PanelGeometry.resizeZone(10f, 360f, 300f, 400f, 24f, 20f, 24f, 24f),
        )
        assertEquals(
            PanelResizeZone.NONE,
            PanelGeometry.resizeZone(150f, 390f, 300f, 400f, 24f, 20f, 24f, 24f),
        )
    }

    @Test
    fun bottomResizeDragClampsAndMapsHeightToPercent() {
        val minimum = 260f
        val maximum = 800f
        assertEquals(minimum, PanelGeometry.resizeHeight(300f, -100f, minimum, maximum), 0f)
        assertEquals(800f, PanelGeometry.resizeHeight(700f, 500f, minimum, maximum), 0f)
        val range = PanelGeometry.sizeRange(800, minimum = 260)
        for (percent in 0..100) {
            val height = PanelGeometry.sizeFor(percent, range)
            assertEquals(percent, PanelGeometry.percentFor(height.toFloat(), range))
        }
        assertEquals(0f, PanelGeometry.resizeWidthDelta(PanelResizeZone.BOTTOM_EDGE, 24f), 0f)
        assertEquals(24f, PanelGeometry.resizeWidthDelta(PanelResizeZone.BOTTOM_LEFT, 24f), 0f)
        assertEquals(-24f, PanelGeometry.resizeWidthDelta(PanelResizeZone.BOTTOM_RIGHT, -24f), 0f)
        assertEquals(320f, PanelGeometry.resizeWidth(300f, 20f, minimum, maximum), 0f)
    }

    @Test
    fun resizeBarGeometryAndGlowScaleWithTheSetting() {
        assertEquals(6f, PanelGeometry.RESIZE_BAR_HEIGHT_DP, 0f)
        assertEquals(16f, PanelGeometry.RESIZE_BAR_INSET_DP, 0f)
        assertEquals(3f, PanelGeometry.RESIZE_BAR_CAP_RADIUS_DP, 0f)
        assertEquals(24f, PanelGeometry.resizeBarGlowRadiusDp(50), 0f)
        assertEquals(32f, PanelGeometry.resizeBarGlowRadiusDp(100), 0f)
        assertEquals(0f, PanelGeometry.resizeBarLayerAlphaSum(0), 0f)
        assertTrue(
            PanelGeometry.resizeBarLayerAlphaSum(100) >=
                PanelGeometry.resizeBarLayerAlphaSum(50) * 2.5f,
        )
    }

    @Test
    fun breathingGlowVariesByFifteenPercent() {
        assertEquals(0.85f, PanelGeometry.pulseScale(0f), 0f)
        assertEquals(1f, PanelGeometry.pulseScale(0.5f), 0f)
        assertEquals(1.15f, PanelGeometry.pulseScale(1f), 0f)
    }

    @Test
    fun scrollThumbTracksTheScrollableRangeAndHasAMinimumHeight() {
        val smallThumb = PanelGeometry.scrollThumbHeight(100, 1000, 32)
        assertEquals(32f, smallThumb, 0f)
        assertEquals(0f, PanelGeometry.scrollThumbTop(100, 1000, 0, smallThumb), 0f)
        assertEquals(68f, PanelGeometry.scrollThumbTop(100, 1000, 900, smallThumb), 0f)
        val normalThumb = PanelGeometry.scrollThumbHeight(100, 200, 32)
        assertEquals(50f, normalThumb, 0f)
        assertEquals(68f, PanelGeometry.scrollThumbTop(100, 1000, 2000, smallThumb), 0f)
    }

    @Test
    fun folderGridUsesOnlyColumnsThatFitItsMinimumCellWidth() {
        assertEquals(1, folderColumnCount(innerWidth = 0, minimumCellWidth = 72f, requestedColumns = 3))
        assertEquals(2, folderColumnCount(innerWidth = 215, minimumCellWidth = 72f, requestedColumns = 3))
        assertEquals(2, folderColumnCount(innerWidth = 216, minimumCellWidth = 72.5f, requestedColumns = 3))
        assertEquals(3, folderColumnCount(innerWidth = 218, minimumCellWidth = 72.5f, requestedColumns = 3))
        assertEquals(2, folderColumnCount(innerWidth = 800, minimumCellWidth = 72f, requestedColumns = 2))
    }

    @Test
    fun folderSizeScaleClampsAndCentersOnNormalSize() {
        assertEquals(0.7f, FolderSettings.scaleOf(-1), 0f)
        assertEquals(1f, FolderSettings.scaleOf(50), 0f)
        assertEquals(1.3f, FolderSettings.scaleOf(100), 0f)
        assertEquals(1.3f, FolderSettings.scaleOf(101), 0f)
    }
}
