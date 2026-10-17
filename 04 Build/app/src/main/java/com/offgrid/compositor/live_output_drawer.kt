package com.offgrid.compositor

import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.view.Surface
import com.offgrid.compositor.effects.StageGeometry

internal class LiveOutputDrawer {
    private class Slot(
        val surface: Surface,
        val width: Int,
        val height: Int,
        val intervalNs: Long,
        var eglSurface: android.opengl.EGLSurface,
        var lastStamp: Long,
    )

    private val slots = LinkedHashMap<String, Slot>()

    fun set(processor: CompositorProcessor, id: String, surface: Surface?, width: Int, height: Int) {
        remove(processor, id)
        val config = processor.config ?: return
        if (surface == null || width <= 0 || height <= 0) return
        if (processor.display == EGL14.EGL_NO_DISPLAY) return
        val eglSurface = EGL14.eglCreateWindowSurface(
            processor.display,
            config,
            surface,
            intArrayOf(EGL14.EGL_NONE),
            0,
        ) ?: EGL14.EGL_NO_SURFACE
        if (eglSurface == EGL14.EGL_NO_SURFACE) return
        val interval = if (id == "pc") 1_000_000_000L / 30 else 1_000_000_000L / 24
        slots[id] = Slot(surface, width, height, interval, eglSurface, 0L)
    }

    fun clear(processor: CompositorProcessor, surface: Surface) {
        val id = slots.entries.firstOrNull { it.value.surface === surface }?.key ?: return
        remove(processor, id)
    }

    fun draw(
        processor: CompositorProcessor,
        stamp: Long,
        slotsOut: List<CompositorProcessor.OutputSlot>,
        painted: GlTarget?,
    ) {
        if (slots.isEmpty() || processor.display == EGL14.EGL_NO_DISPLAY) return
        val main = StageGeometry.pickMain(slotsOut, { it.isPreview }, { it.width }, { it.height })?.surfaceOutput
        for (slot in slots.values) {
            if (stamp - slot.lastStamp < slot.intervalNs) continue
            slot.lastStamp = stamp
            if (!EGL14.eglMakeCurrent(processor.display, slot.eglSurface, slot.eglSurface, processor.context)) {
                continue
            }
            val curtain = processor.idleCurtainColor()
            val cr = if (curtain != null) ((curtain shr 16) and 0xFF) / 255f else 0f
            val cg = if (curtain != null) ((curtain shr 8) and 0xFF) / 255f else 0f
            val cb = if (curtain != null) (curtain and 0xFF) / 255f else 0f
            GLES20.glViewport(0, 0, slot.width, slot.height)
            GLES20.glClearColor(cr, cg, cb, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            val stage = processor.stage
            if (curtain == null) {
                if (painted != null && stage != null) {
                    stage.present(painted, slot.width, slot.height)
                } else if (!processor.coverFace && main != null && processor.drawer != null) {
                    main.updateTransformMatrix(processor.updatedTransform, processor.textureTransform)
                    processor.drawer?.drawCamera(processor.inputTextureId, processor.updatedTransform)
                }
            }
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            processor.effectList.drawAll(slot.width, slot.height)
            if (processor.testShapeOn) processor.drawer?.drawTopLayer()
            EGLExt.eglPresentationTimeANDROID(processor.display, slot.eglSurface, stamp)
            EGL14.eglSwapBuffers(processor.display, slot.eglSurface)
        }
        if (processor.dummy != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(processor.display, processor.dummy, processor.dummy, processor.context)
        }
    }

    fun release(processor: CompositorProcessor) {
        slots.keys.toList().forEach { remove(processor, it) }
    }

    private fun remove(processor: CompositorProcessor, id: String) {
        val slot = slots.remove(id) ?: return
        if (slot.eglSurface != EGL14.EGL_NO_SURFACE && processor.display != EGL14.EGL_NO_DISPLAY) {
            if (processor.dummy != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(processor.display, processor.dummy, processor.dummy, processor.context)
            }
            EGL14.eglDestroySurface(processor.display, slot.eglSurface)
        }
    }
}
