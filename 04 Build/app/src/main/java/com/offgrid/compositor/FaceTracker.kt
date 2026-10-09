package com.offgrid.compositor

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Preview
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.FaceSpot
import com.offgrid.compositor.effects.FxKeys

/**
 * Finds the face with the camera's own face finder (final build, M4), so the face diamond,
 * the radar, and pinned ink follow it. It only uses the camera, already allowed, and the
 * face data never leaves the phone or gets saved.
 *
 * The camera reports a face box on its sensor. This turns it upright and cuts it the same
 * way as the video, so the place matches the finished picture.
 */
internal class FaceTracker(context: Context, private val engine: EffectEngine) {
    private val faceMode: Int
    private val sensorOrientation: Int
    private val activeArray: Rect?

    /** The video's width divided by its height, for the same cut as the video. */
    @Volatile
    var outputAspect = 9f / 19.5f

    /** The camera stream's long side divided by its short side. */
    @Volatile
    var streamAspect = 16f / 9f

    private var smoothX = Float.NaN
    private var smoothY = Float.NaN
    private var smoothSize = Float.NaN

    val available: Boolean get() = faceMode != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF && activeArray != null

    init {
        var mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
        var orientation = DEFAULT_ORIENTATION
        var array: Rect? = null
        try {
            val manager = context.getSystemService(CameraManager::class.java)
            val front = manager?.cameraIdList?.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
            }
            if (manager != null && front != null) {
                val info = manager.getCameraCharacteristics(front)
                val modes = info.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES) ?: IntArray(0)
                mode = when {
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE in modes -> CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL in modes -> CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL
                    else -> CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF
                }
                orientation = info.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: DEFAULT_ORIENTATION
                array = info.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            }
        } catch (error: Exception) {
            Log.w(TAG, "The face finder is not available", error)
        }
        faceMode = mode
        sensorOrientation = orientation
        activeArray = array
    }

    /** Asks the camera to find faces while [builder]'s preview runs. Does nothing if the phone can't. */
    @OptIn(ExperimentalCamera2Interop::class)
    fun attach(builder: Preview.Builder) {
        if (!available) return
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceMode)
            .setSessionCaptureCallback(callback)
    }

    private val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            onResult(result)
        }
    }

    private fun onResult(result: CaptureResult) {
        val array = activeArray ?: return
        val faces = result.get(CaptureResult.STATISTICS_FACES) ?: return
        val face = faces.maxByOrNull { it.bounds.width() * it.bounds.height() } ?: return
        val crop = result.get(CaptureResult.SCALER_CROP_REGION) ?: array
        val spot = FaceMath.toVideo(
            face.bounds,
            crop,
            streamAspect,
            sensorOrientation,
            outputAspect,
            flipX = engine.settings.on(FxKeys.FACE_FLIP_X) != engine.settings.on(FxKeys.MIRROR_ON),
        ) ?: return
        // A light smoothing, so the diamond glides instead of shaking.
        val fresh = smoothX.isNaN() || engine.freshFace(engine.now()) == null
        smoothX = if (fresh) spot[0] else smoothX + (spot[0] - smoothX) * SMOOTH
        smoothY = if (fresh) spot[1] else smoothY + (spot[1] - smoothY) * SMOOTH
        smoothSize = if (fresh) spot[2] else smoothSize + (spot[2] - smoothSize) * SMOOTH
        engine.face = FaceSpot(smoothX, smoothY, smoothSize, engine.now())
    }

    private companion object {
        const val TAG = "OffgridFaces"
        const val DEFAULT_ORIENTATION = 270
        const val SMOOTH = 0.45f
    }
}

/** The math that turns the camera's face box into a place in the finished video. Tests check it. */
internal object FaceMath {
    /**
     * Returns the face's middle x, middle y, and width as shares of the video (0 to 1), or null
     * when the face is outside the video. [crop] is the part of the sensor the camera used,
     * [streamAspect] is the camera stream's long side over its short side, [orientation] is
     * how far the sensor is turned, and [outputAspect] is the video's width over its height.
     */
    fun toVideo(
        bounds: Rect,
        crop: Rect,
        streamAspect: Float,
        orientation: Int,
        outputAspect: Float,
        flipX: Boolean,
    ): FloatArray? {
        if (crop.width() <= 0 || crop.height() <= 0) return null
        // The stream is the middle of the crop, cut to the stream's shape.
        val cropAspect = crop.width().toFloat() / crop.height()
        var fieldW = crop.width().toFloat()
        var fieldH = crop.height().toFloat()
        if (cropAspect > streamAspect) fieldW = fieldH * streamAspect else fieldH = fieldW / streamAspect
        val left = crop.left + (crop.width() - fieldW) / 2f
        val top = crop.top + (crop.height() - fieldH) / 2f
        val sx = (bounds.exactCenterX() - left) / fieldW
        val sy = (bounds.exactCenterY() - top) / fieldH
        val sw = bounds.width() / fieldW
        val sh = bounds.height() / fieldH
        // Turn upright, the way the camera picture is turned for the screen.
        var ux: Float
        val uy: Float
        val faceWidth: Float
        when (((orientation % 360) + 360) % 360) {
            90 -> { ux = 1f - sy; uy = sx; faceWidth = sh }
            180 -> { ux = 1f - sx; uy = 1f - sy; faceWidth = sw }
            270 -> { ux = sy; uy = 1f - sx; faceWidth = sh }
            else -> { ux = sx; uy = sy; faceWidth = sw }
        }
        if (flipX) ux = 1f - ux
        // The upright stream is cut in the middle to the video's shape, like the screen.
        val turned = orientation % 180 != 0
        val uprightAspect = if (turned) 1f / streamAspect else streamAspect
        var x = ux
        var y = uy
        var width = faceWidth
        if (outputAspect < uprightAspect) {
            val share = outputAspect / uprightAspect
            x = (ux - (1f - share) / 2f) / share
            width = faceWidth / share
        } else if (outputAspect > uprightAspect) {
            val share = uprightAspect / outputAspect
            y = (uy - (1f - share) / 2f) / share
        }
        if (x < -0.2f || x > 1.2f || y < -0.2f || y > 1.2f) return null
        return floatArrayOf(x, y, width)
    }
}
