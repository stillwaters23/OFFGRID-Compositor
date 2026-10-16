package com.offgrid.compositor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.OverlayLayer

/**
 * Draws the effects that sit on top of the camera (emoji rain, fireworks, the face diamond,
 * and the rest) with Android's Canvas, on the phone's graphics chip, on its own thread
 * (final build, M4). The camera stage picks up the newest drawing every frame and bakes it
 * into the picture, so viewers see it. The terminal panel is never drawn here.
 *
 * Made and used on the camera drawing thread, which owns the GPU.
 */
internal class OverlayRenderer(
    private val engine: EffectEngine,
    private val layers: List<OverlayLayer>,
    glHandler: Handler,
) : SurfaceTexture.OnFrameAvailableListener {
    private val thread = HandlerThread("OffgridEffects").apply { start() }
    private val handler = Handler(thread.looper)

    /** The GPU texture that holds the newest drawing. */
    val texture: Int
    private val surfaceTexture: SurfaceTexture
    private val surface: Surface

    /** How the newest drawing sits in [texture]. */
    val matrix = FloatArray(16)

    /** True when the newest drawing has something in it. */
    var hasPicture = false
        private set

    /** True when the phone asks for less motion. Set from the main thread. */
    @Volatile
    var reducedMotion = false

    @Volatile
    private var busy = false

    @Volatile
    private var released = false

    @Volatile
    private var postedEmpty = true

    @Volatile
    private var newDrawing = false

    @Volatile
    private var wantWidth = 0

    @Volatile
    private var wantHeight = 0
    private var sizeWidth = 0
    private var sizeHeight = 0

    /**
     * Phase 1.1: counts stage size changes. A drawing made for an older size is skipped at
     * [latch] instead of being stretched over the new stage for one frame.
     */
    @Volatile
    private var sizeGeneration = 0

    @Volatile
    private var postedGeneration = 0
    private var toldFailure = false
    private val active = BooleanArray(layers.size)

    private val drawTask = Runnable { drawOnce() }

    init {
        val names = IntArray(1)
        GLES20.glGenTextures(1, names, 0)
        texture = names[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        surfaceTexture = SurfaceTexture(texture)
        surfaceTexture.setOnFrameAvailableListener(this, glHandler)
        surface = Surface(surfaceTexture)
    }

    /** Asks for a fresh drawing at [width] by [height]. Skipped if the last one is still drawing. */
    fun request(width: Int, height: Int) {
        if (released || width <= 0 || height <= 0) return
        // The new size is published before the generation moves, so a drawing that reads the new
        // generation in drawLayers() always reads the new size too.
        wantWidth = width
        wantHeight = height
        if (width != sizeWidth || height != sizeHeight) {
            sizeWidth = width
            sizeHeight = height
            surfaceTexture.setDefaultBufferSize(width, height)
            sizeGeneration++
        }
        if (busy) return
        busy = true
        handler.post(drawTask)
    }

    override fun onFrameAvailable(texture: SurfaceTexture) {
        newDrawing = true
    }

    /** Takes the newest drawing into [texture]. Call on the drawing thread before using it. */
    fun latch() {
        if (released || !newDrawing) return
        newDrawing = false
        try {
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(matrix)
            // A drawing for an older stage size would be stretched to the new one. Skip it; the next one fits.
            hasPicture = !postedEmpty && postedGeneration == sizeGeneration
        } catch (error: RuntimeException) {
            Log.w(TAG, "Effects picture was not ready", error)
            hasPicture = false
        }
    }

    fun release() {
        released = true
        hasPicture = false
        surfaceTexture.setOnFrameAvailableListener(null)
        handler.post { surface.release() }
        thread.quitSafely()
        surfaceTexture.release()
        GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    private fun drawOnce() {
        try {
            if (!released) drawLayers()
        } finally {
            busy = false
        }
    }

    private fun drawLayers() {
        val generation = sizeGeneration
        val width = wantWidth
        val height = wantHeight
        val frame = engine.frame(width, height, reducedMotion)
        // A layer that covers everything hides the ones under it, so those are skipped.
        var first = 0
        var any = false
        for (i in layers.indices) {
            active[i] = safely(i) { layers[i].isActive(frame) }
            if (active[i]) {
                any = true
                if (safely(i) { layers[i].coversAll(frame) }) first = i
            }
        }
        if (!any && postedEmpty) return
        val canvas = lockCanvas() ?: return
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            for (i in first until layers.size) {
                if (!active[i]) continue
                val save = canvas.save()
                try {
                    layers[i].draw(canvas, frame)
                } catch (error: RuntimeException) {
                    tellFailure(i, error)
                } finally {
                    canvas.restoreToCount(save)
                }
            }
        } finally {
            postedEmpty = !any
            postedGeneration = generation
            try {
                surface.unlockCanvasAndPost(canvas)
            } catch (error: RuntimeException) {
                Log.w(TAG, "Effects picture was not sent", error)
            }
        }
    }

    private fun lockCanvas(): Canvas? = try {
        surface.lockHardwareCanvas()
    } catch (error: RuntimeException) {
        // Some phones can't draw this way. The slower way still works.
        try {
            surface.lockCanvas(null)
        } catch (second: RuntimeException) {
            Log.w(TAG, "Effects picture could not start", second)
            null
        }
    }

    private inline fun safely(index: Int, check: () -> Boolean): Boolean = try {
        check()
    } catch (error: RuntimeException) {
        tellFailure(index, error)
        false
    }

    private fun tellFailure(index: Int, error: RuntimeException) {
        if (toldFailure) return
        toldFailure = true
        Log.e(TAG, "Effect ${layers[index].javaClass.simpleName} failed", error)
    }

    private companion object {
        const val TAG = "OffgridEffects"
    }
}
