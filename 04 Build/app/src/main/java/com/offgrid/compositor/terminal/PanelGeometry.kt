package com.offgrid.compositor.terminal

import kotlin.math.roundToInt

internal object PanelGeometry {
    fun isFullBleed(width: Int, height: Int, fullScreen: Boolean): Boolean =
        fullScreen || (width == 100 && height == 100)

    fun sizeRange(available: Int, minimum: Int): Pair<Int, Int> {
        val maximum = available.coerceAtLeast(0)
        return minOf(minimum.coerceAtLeast(0), maximum) to maximum
    }

    fun sizeFor(percent: Int, range: Pair<Int, Int>): Int {
        val minimum = range.first.coerceAtLeast(0)
        val maximum = maxOf(minimum, range.second)
        return minimum + ((maximum - minimum) * percent.coerceIn(0, 100) / 100f).roundToInt()
    }

    fun percentFor(size: Float, range: Pair<Int, Int>): Int {
        if (range.second <= range.first) return 100
        return ((size - range.first) / (range.second - range.first) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
