package com.offgrid.compositor.terminal

import org.junit.Assert.assertEquals
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
