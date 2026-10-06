package com.offgrid.compositor

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.camera.core.SurfaceRequest

/**
 * Live front-camera picture drawn with OpenGL ES.
 * Step 0 only. Effects are later steps.
 */
class FrontCameraGlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    val renderer = FrontCameraRenderer(this)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        keepScreenOn = true
    }

    fun submit(request: SurfaceRequest) {
        renderer.submit(request)
        requestRender()
    }

    fun clearPending() {
        renderer.clearPending()
    }
}
