package com.offgrid.compositor.terminal

internal data class GlowLayers(val core: Float, val mid: Float, val wide: Float) {
    val totalAlpha: Float get() = core + mid + wide
}

internal object PanelGeometry {
    fun glowLayers(value: Int): GlowLayers {
        val level = value.coerceIn(0, 100) / 100f
        return GlowLayers(
            core = level,
            mid = level * level,
            wide = level * level * level,
        )
    }

    fun borderThickness(value: Int): Float {
        val thickness = value.coerceIn(0, 100)
        return if (thickness <= 50) 2f + thickness / 50f else 3f + 3f * (thickness - 50) / 50f
    }

    fun panelDimension(available: Int, percent: Int, maximum: Int): Int {
        if (percent >= 100) return available.coerceAtLeast(0)
        return (available.coerceAtLeast(0) * percent.coerceIn(0, 100) / 100f)
            .toInt()
            .coerceAtMost(maximum.coerceAtLeast(0))
    }
}
