package com.offgrid.compositor

import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.Surface

/**
 * A camera picture that is plain black, for when the camera is off. The effects stage paints over it
 * the same way it paints over the real camera. Create and use it on the drawing thread.
 */
internal class BlackCamera(val textureId: Int) {
    private val texture = SurfaceTexture(textureId)
    private val surface = Surface(texture)

    @Volatile
    private var arrived = false
    private var latched = false

    init {
        texture.setDefaultBufferSize(SIZE, SIZE)
        texture.setOnFrameAvailableListener { arrived = true }
        val canvas = surface.lockCanvas(null)
        canvas.drawColor(Color.BLACK)
        surface.unlockCanvasAndPost(canvas)
    }

    /** True once the black picture is in the texture. Until then the stage is not painted. */
    fun update(): Boolean {
        if (latched) return true
        if (!arrived) return false
        texture.updateTexImage()
        latched = true
        return true
    }

    fun release() {
        texture.setOnFrameAvailableListener(null)
        texture.release()
        surface.release()
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    private companion object {
        const val SIZE = 16
    }
}
