package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Small textures made on the phone, for the premium look. No picture files.
 * Each is made once and shared.
 */
internal object PanelTextures {
    /** Faint white specks that frost glass. */
    val grain: Bitmap by lazy {
        val size = 96
        val random = Random(23)
        val pixels = IntArray(size * size) {
            val speck = random.nextInt(0, 22)
            (speck shl 24) or 0x00FFFFFF
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Brushed metal: fine light and dark streaks that run sideways. */
    val brushed: Bitmap by lazy {
        val width = 256
        val height = 64
        val random = Random(41)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val row = random.nextFloat() * 2f - 1f
            var run = 0f
            for (x in 0 until width) {
                run = run * 0.92f + (random.nextFloat() * 2f - 1f) * 0.08f
                val value = (row * 0.6f + run * 2.2f).coerceIn(-1f, 1f)
                val alpha = (abs(value) * 46f).toInt().coerceIn(0, 255)
                val rgb = if (value >= 0f) 0x00FFFFFF else 0x00000000
                pixels[y * width + x] = (alpha shl 24) or rgb
            }
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private val carbonTiles = HashMap<Float, Bitmap>()

    /**
     * Woven carbon fiber: a two by two weave. Each cell is a bundle of fibers lit from one side,
     * and the bundles cross at right angles, so the weave catches light like the real thing.
     */
    fun carbon(density: Float): Bitmap = carbonTiles.getOrPut(density) {
        val cell = (3f * density).toInt().coerceAtLeast(3)
        val size = cell * 2
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val across = (x / cell + y / cell) % 2 == 0
                val along = if (across) (x % cell) / cell.toFloat() else (y % cell) / cell.toFloat()
                val light = sin(along * PI).toFloat()
                val level = (18 + light * 34).toInt()
                val green = (level * 1.04f).toInt().coerceAtMost(255)
                val blue = (level * 1.12f).toInt().coerceAtMost(255)
                pixels[y * size + x] = (0xFF shl 24) or (level shl 16) or (green shl 8) or blue
            }
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
