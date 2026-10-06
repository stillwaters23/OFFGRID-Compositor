package com.offgrid.compositor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Step 0. Opens the front camera and shows it on the screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: FrontCameraGlView
    private lateinit var statusBox: View
    private lateinit var statusText: TextView
    private lateinit var actionButton: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var bindGeneration = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionHelp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        cameraView = findViewById(R.id.camera_view)
        statusBox = findViewById(R.id.status_box)
        statusText = findViewById(R.id.status_text)
        actionButton = findViewById(R.id.action_button)
        cameraView.renderer.onFirstFrame = { showCamera() }
        cameraView.renderer.onRendererFailed = { showMessage(R.string.camera_failed, ButtonMode.NONE) }
    }

    override fun onResume() {
        super.onResume()
        cameraView.onResume()
        hideSystemBars()
        if (hasCameraPermission()) {
            startCamera()
        } else if (!permissionAsked) {
            permissionAsked = true
            showMessage(R.string.camera_needs_ok, ButtonMode.NONE)
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            showPermissionHelp()
        }
    }

    override fun onPause() {
        stopCamera()
        cameraView.clearPending()
        cameraView.onPause()
        super.onPause()
    }

    private var permissionAsked = false

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val generation = ++bindGeneration
        showMessage(R.string.camera_starting, ButtonMode.NONE)
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
                        showMessage(R.string.camera_missing, ButtonMode.NONE)
                        return@addListener
                    }
                    bindCamera(provider, withFrameRate = true)
                } catch (error: Exception) {
                    Log.e(TAG, "Camera did not start", error)
                    showMessage(R.string.camera_failed, ButtonMode.NONE)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun bindCamera(provider: ProcessCameraProvider, withFrameRate: Boolean) {
        val previewBuilder = Preview.Builder()
            .setResolutionSelector(previewResolution())
            .setTargetRotation(displayRotation())
            .setPreviewStabilizationEnabled(false)
        if (withFrameRate) {
            previewBuilder.setTargetFrameRate(Range(30, 30))
        }
        val preview = previewBuilder.build()
        preview.setSurfaceProvider { request -> cameraView.submit(request) }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
        } catch (error: Exception) {
            if (withFrameRate) {
                Log.w(TAG, "Steady frame rate was not available. Trying again.")
                bindCamera(provider, withFrameRate = false)
            } else {
                Log.e(TAG, "Camera did not start", error)
                showMessage(R.string.camera_failed, ButtonMode.NONE)
            }
        }
    }

    private fun stopCamera() {
        bindGeneration++
        cameraProvider?.unbindAll()
    }

    private fun previewResolution(): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
    }

    private fun displayRotation(): Int {
        return cameraView.display?.rotation ?: Surface.ROTATION_0
    }

    private fun showCamera() {
        statusBox.visibility = View.GONE
    }

    private fun showPermissionHelp() {
        val canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        if (canAskAgain) {
            showMessage(R.string.camera_denied, ButtonMode.TRY_AGAIN)
        } else {
            showMessage(R.string.camera_blocked, ButtonMode.SETTINGS)
        }
    }

    private fun showMessage(textRes: Int, buttonMode: ButtonMode) {
        statusBox.visibility = View.VISIBLE
        statusText.setText(textRes)
        when (buttonMode) {
            ButtonMode.NONE -> actionButton.visibility = View.GONE
            ButtonMode.TRY_AGAIN -> {
                actionButton.visibility = View.VISIBLE
                actionButton.setText(R.string.try_again)
                actionButton.setOnClickListener {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            ButtonMode.SETTINGS -> {
                actionButton.visibility = View.VISIBLE
                actionButton.setText(R.string.open_settings)
                actionButton.setOnClickListener { openAppSettings() }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private enum class ButtonMode {
        NONE,
        TRY_AGAIN,
        SETTINGS,
    }

    private companion object {
        const val TAG = "OffgridPreview"
    }
}
