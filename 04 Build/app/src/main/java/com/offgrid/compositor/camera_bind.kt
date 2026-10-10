package com.offgrid.compositor

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

private const val CAMERA_TAG = "OffgridCompositor"

internal fun MainActivity.startCamera() {
        if (previewView.width == 0 || previewView.height == 0) {
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
