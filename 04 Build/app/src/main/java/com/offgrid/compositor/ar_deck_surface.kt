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

    /**
     * The clip's real picture size in square pixels, from Media3's onVideoSizeChanged (width
     * times the pixel ratio, height). 0 until it is known. The stage draws nothing before then.
     */
    var videoWidth: Int = 0
        private set
    var videoHeight: Int = 0
        private set
    val hasSize: Boolean
        get() = videoWidth > 0 && videoHeight > 0

    /** GL thread. Media3 reported the clip's size. A rotated clip arrives already upright. */
    fun setVideoSize(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        val ratio = if (pixelWidthHeightRatio > 0f) pixelWidthHeightRatio else 1f
        videoWidth = Math.round(width * ratio)
        videoHeight = height
    }

    /** GL thread. A new clip is loading, so its size is unknown again. */
    fun forgetVideoSize() {
        videoWidth = 0
        videoHeight = 0
        hasFrame = false
    }

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
