package com.offgrid.compositor

/**
 * The middle sheet of the picture.
 * Step 1 leaves this empty. Later effects add a sheet here.
 * They do not add a second camera effect.
 */
class EffectList {
    private val layers = mutableListOf<EffectLayer>()

    fun add(layer: EffectLayer) {
        layers.add(layer)
    }

    fun drawAll(width: Int, height: Int) {
        for (layer in layers) {
            layer.draw(width, height)
        }
    }
}

fun interface EffectLayer {
    fun draw(width: Int, height: Int)
}
