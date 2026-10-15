package com.offgrid.compositor

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * The second camera-style picture the AR clip is drawn into.
 *
 * The GL thread creates the external texture, then builds this. ExoPlayer draws into
 * [surface]. [updateTexImage] runs only on that same GL thread, when a new frame is ready.
 */
internal class ArDeckSurface(
    val textureId: Int,
    val surfaceTexture: SurfaceTexture,
) {
    val surface: Surface = Surface(surfaceTexture)
    val texMatrix = FloatArray(16)
    var hasFrame: Boolean = false
        private set

    /** GL thread only. Samples the newest clip frame into [textureId]. */
    fun updateTexImage() {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(texMatrix)
        hasFrame = true
    }

    fun releaseGl() {
        surfaceTexture.setOnFrameAvailableListener(null)
        surface.release()
        surfaceTexture.release()
    }
}
