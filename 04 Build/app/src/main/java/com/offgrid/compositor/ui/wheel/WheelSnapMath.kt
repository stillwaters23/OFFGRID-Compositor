package com.offgrid.compositor.ui.wheel

import kotlin.math.roundToInt

internal object WheelSnapMath {
    fun clamp(value: Int, minimum: Int = 0, maximum: Int = 100): Int =
        value.coerceIn(minimum, maximum)

    fun snap(position: Float, minimum: Int = 0, maximum: Int = 100): Int =
        position.roundToInt().coerceIn(minimum, maximum)
}
