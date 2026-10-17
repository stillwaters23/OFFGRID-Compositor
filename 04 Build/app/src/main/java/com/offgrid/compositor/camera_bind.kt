package com.offgrid.compositor

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.offgrid.compositor.effects.StageGeometry

private const val CAMERA_TAG = "OffgridCompositor"

internal fun MainActivity.startCamera() {
        // Phase 1.1: bind only to a laid-out view, so the view port is built from the real shape.
        if (previewView.width == 0 || previewView.height == 0 || !previewView.isLaidOut) {
            previewView.post { startCamera() }
            return
        }
        val generation = ++bindGeneration
        showMessage(R.string.camera_starting, MainActivity.ButtonMode.NONE)
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                if (generation != bindGeneration || isFinishing) {
                    return@addListener
                }
                try {
                    val provider = future.get()
                    cameraProvider = provider
                    if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        showMessage(R.string.camera_missing, MainActivity.ButtonMode.NONE)
                        return@addListener
                    }
                    bindCamera(provider, withFrameRate = true)
                } catch (error: Exception) {
                    Log.e(CAMERA_TAG, "Camera did not start", error)
                    showMessage(R.string.camera_failed, MainActivity.ButtonMode.NONE)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

/**
 * Phase 1.1: the PreviewView was laid out at [width] by [height]. When its shape differs from the
 * shape the camera was bound at by more than 1 percent (fold, split screen, a late system bar),
 * one rebind is scheduled, debounced so a burst of layout passes ends in a single bind.
 */
internal fun MainActivity.onPreviewLayout(width: Int, height: Int) {
    if (!rebindGate.onLayout(width, height)) return
    mainHandler.removeCallbacks(rebindTask)
    mainHandler.postDelayed(rebindTask, REBIND_DEBOUNCE_MS)
}

internal fun MainActivity.rebindForNewShape() {
    if (isFinishing || isDestroyed || !resumed || !capture.cameraRunning || !hasCameraPermission()) return
    if (cameraProvider == null) return
    if (!rebindGate.boundWidth.let { it > 0 }) return
    if (!StageGeometry.aspectDiffers(rebindGate.boundWidth, rebindGate.boundHeight, previewView.width, previewView.height)) {
        // The burst ended back at the bound shape (A to B to A): no bind, and the gate may ask again.
        rebindGate.bound(rebindGate.boundWidth, rebindGate.boundHeight)
        return
    }
    Log.i(
        CAMERA_TAG,
        "View shape changed from ${rebindGate.boundWidth}x${rebindGate.boundHeight} to " +
            "${previewView.width}x${previewView.height}. Binding the camera again.",
    )
    startCamera()
}

internal const val REBIND_DEBOUNCE_MS = 150L
