package com.offgrid.compositor

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * The screen while the camera is off: a view above the camera picture that shows the black stage
 * with the effects drawn over it, the same picture viewers get.
 */
internal class DarkStage(context: Context, parent: FrameLayout, below: View, private val processor: () -> CompositorProcessor?) {
    private val view = TextureView(context)
    private var texture: SurfaceTexture? = null
    private var width = 0
    private var height = 0
    private var surface: Surface? = null
    private var shown = false

    init {
        view.visibility = View.GONE
        view.contentDescription = context.getString(R.string.dark_picture)
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                texture = st
                width = w
                height = h
                connect()
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                width = w
                height = h
                disconnect()
                connect()
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                disconnect()
                texture = null
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        val index = parent.indexOfChild(below) + 1
        parent.addView(view, index, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun show() {
        shown = true
        view.visibility = View.VISIBLE
        connect()
    }

    fun hide() {
        shown = false
        view.visibility = View.GONE
        disconnect()
    }

    private fun connect() {
        val st = texture ?: return
        if (!shown || surface != null) return
        val created = Surface(st)
        surface = created
        processor()?.setDarkScreen(created, width, height)
    }

    private fun disconnect() {
        val old = surface ?: return
        surface = null
        processor()?.setDarkScreen(null, 0, 0)
        old.release()
    }
}
