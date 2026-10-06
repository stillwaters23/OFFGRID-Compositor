package com.offgrid.compositor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

/**
 * Step 1. Shows the painted camera picture, records a few seconds, and plays it back.
 * The mic stays off.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var playbackView: TextureView
    private lateinit var playbackBackdrop: View
    private lateinit var controlBar: View
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var actionButton: Button
    private lateinit var playbackBar: View
    private lateinit var playbackHint: TextView
    private lateinit var backButton: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var processor: CompositorProcessor? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var bindGeneration = 0
    private var permissionAsked = false
    private var recording = false
    private var stopRequested = false
    private var playRequested = false
    private var playbackSurface: Surface? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackFd: ParcelFileDescriptor? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var playbackRotation = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRecordingTask: Runnable? = null

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

        previewView = findViewById(R.id.camera_view)
        playbackView = findViewById(R.id.playback_view)
        playbackBackdrop = findViewById(R.id.playback_backdrop)
        controlBar = findViewById(R.id.control_bar)
        statusText = findViewById(R.id.status_text)
        recordButton = findViewById(R.id.record_button)
        playButton = findViewById(R.id.play_button)
        actionButton = findViewById(R.id.action_button)
        playbackBar = findViewById(R.id.playback_bar)
        playbackHint = findViewById(R.id.playback_hint)
        backButton = findViewById(R.id.back_button)

        recordButton.setOnClickListener { startFiveSecondRecording() }
        playButton.setOnClickListener { showPlayback() }
        backButton.setOnClickListener { hidePlayback() }
        playbackView.surfaceTextureListener = playbackListener
        showMessage(R.string.camera_starting, ButtonMode.NONE)
    }

    override fun onResume() {
        super.onResume()
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
        hidePlayback()
        stopRecording()
        unbindCamera()
        super.onPause()
    }

    override fun onDestroy() {
        processor?.onFirstFrame = null
        processor?.onFailed = null
        processor?.release()
        processor = null
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        if (previewView.width == 0 || previewView.height == 0) {
            previewView.post { startCamera() }
            return
        }
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
        val painter = processor ?: CompositorProcessor().also { created ->
            created.onFirstFrame = { showLivePicture() }
            created.onFailed = {
                stopRecording()
                showMessage(R.string.camera_failed, ButtonMode.NONE)
            }
            processor = created
        }
        val effect = CompositorEffect(
            painter.executor,
            painter,
            Consumer { error ->
                Log.e(TAG, "Picture effect failed", error)
                painter.onFailed?.let { failure ->
                    runOnUiThread(failure)
                }
            },
        )
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        // Quality.HD is a 720p picture. The phone video chip saves it as H.264.
        // Portrait rotation plays it as 720 by 1280. Audio stays off.
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                ),
            )
            .build()
        val previewBuilder = Preview.Builder()
            .setResolutionSelector(previewResolution())
            .setTargetRotation(rotation)
        val videoBuilder = VideoCapture.Builder(recorder)
            .setTargetRotation(rotation)
            .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
        if (withFrameRate) {
            val thirty = Range(30, 30)
            previewBuilder.setTargetFrameRate(thirty)
            videoBuilder.setTargetFrameRate(thirty)
        }
        val preview = previewBuilder.build()
        val capture = videoBuilder.build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val viewPort = ViewPort.Builder(Rational(previewView.width, previewView.height), rotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()
        val group = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(effect)
            .build()
        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, group)
            videoCapture = capture
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

    private fun unbindCamera() {
        bindGeneration++
        cameraProvider?.unbindAll()
        videoCapture = null
    }

    private fun startFiveSecondRecording() {
        val capture = videoCapture ?: return
        if (recording) {
            return
        }
        val temp = File(filesDir, TEMP_NAME)
        if (temp.exists() && !temp.delete()) {
            showMessage(R.string.record_failed, ButtonMode.LIVE)
            return
        }
        recording = true
        stopRequested = false
        showMessage(R.string.recording_now, ButtonMode.RECORDING)
        val options = FileOutputOptions.Builder(temp).build()
        val pending = capture.output.prepareRecording(this, options)
        val started = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            onRecordEvent(event, temp)
        }
        activeRecording = started
        val stopTask = Runnable { requestStop() }
        stopRecordingTask = stopTask
        mainHandler.postDelayed(stopTask, FALLBACK_STOP_MS)
    }

    private fun requestStop() {
        if (stopRequested) {
            return
        }
        stopRequested = true
        activeRecording?.stop()
    }

    private fun onRecordEvent(event: VideoRecordEvent, temp: File) {
        when (event) {
            is VideoRecordEvent.Start -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                val stopTask = Runnable { requestStop() }
                stopRecordingTask = stopTask
                mainHandler.postDelayed(stopTask, RECORD_MS)
            }
            is VideoRecordEvent.Finalize -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                stopRecordingTask = null
                activeRecording = null
                recording = false
                if (event.hasError()) {
                    Log.e(TAG, "Recording failed with code ${event.error}")
                    if (temp.exists()) {
                        temp.delete()
                    }
                    showMessage(R.string.record_failed, ButtonMode.LIVE)
                } else {
                    val saved = recordingFile()
                    if (saved.exists()) {
                        saved.delete()
                    }
                    val moved = temp.renameTo(saved)
                    if (!moved) {
                        temp.copyTo(saved, overwrite = true)
                        temp.delete()
                    }
                    showMessage(R.string.record_saved, ButtonMode.LIVE)
                }
            }
            else -> Unit
        }
    }

    private fun stopRecording() {
        stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
        stopRecordingTask = null
        requestStop()
    }

    private fun showPlayback() {
        val file = recordingFile()
        if (!file.exists() || file.length() == 0L) {
            showMessage(R.string.record_failed, ButtonMode.LIVE)
            return
        }
        playRequested = true
        controlBar.visibility = View.GONE
        playbackBackdrop.visibility = View.VISIBLE
        playbackView.visibility = View.VISIBLE
        playbackBar.visibility = View.VISIBLE
        playbackHint.setText(R.string.playback_hint)
        playbackRotation = readVideoRotation(file)
        if (playbackSurface != null) {
            startPlayer(file)
        }
    }

    private fun hidePlayback() {
        playRequested = false
        releasePlayer()
        playbackRotation = 0
        playbackView.rotation = 0f
        playbackView.translationX = 0f
        playbackView.translationY = 0f
        playbackView.setTransform(Matrix())
        val params = playbackView.layoutParams
        params.width = FrameLayout.LayoutParams.MATCH_PARENT
        params.height = FrameLayout.LayoutParams.MATCH_PARENT
        playbackView.layoutParams = params
        playbackBackdrop.visibility = View.GONE
        playbackView.visibility = View.GONE
        playbackBar.visibility = View.GONE
        if (controlBar.visibility != View.VISIBLE && hasCameraPermission()) {
            controlBar.visibility = View.VISIBLE
            refreshPlayButton()
        }
    }

    private fun startPlayer(file: File) {
        val surface = playbackSurface ?: return
        releasePlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            playbackFd = fd
            player.setDataSource(fd.fileDescriptor, 0, file.length())
            player.setSurface(surface)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { mp, width, height ->
                if (mediaPlayer !== mp) {
                    return@setOnVideoSizeChangedListener
                }
                videoWidth = width
                videoHeight = height
                fitPlayback()
            }
            player.setOnPreparedListener { mp ->
                if (mediaPlayer !== mp) {
                    return@setOnPreparedListener
                }
                videoWidth = mp.videoWidth
                videoHeight = mp.videoHeight
                fitPlayback()
                mp.start()
            }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "Playback failed $what $extra")
                if (mediaPlayer === mp) {
                    runOnUiThread {
                        hidePlayback()
                        showMessage(R.string.record_failed, ButtonMode.LIVE)
                    }
                }
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            Log.e(TAG, "Playback failed", error)
            hidePlayback()
            showMessage(R.string.record_failed, ButtonMode.LIVE)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.let { player ->
            player.setOnPreparedListener(null)
            player.setOnErrorListener(null)
            player.setOnVideoSizeChangedListener(null)
            try {
                player.stop()
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Playback was already stopped")
            }
            player.release()
        }
        mediaPlayer = null
        playbackFd?.close()
        playbackFd = null
    }

    private fun fitPlayback() {
        val parent = playbackView.parent as? View ?: return
        val parentW = parent.width
        val parentH = parent.height
        if (parentW == 0 || parentH == 0 || videoWidth == 0 || videoHeight == 0) {
            return
        }
        val swap = playbackRotation == 90 || playbackRotation == 270
        val layoutW = if (swap) parentH else parentW
        val layoutH = if (swap) parentW else parentH
        val params = playbackView.layoutParams as FrameLayout.LayoutParams
        if (params.width != layoutW || params.height != layoutH) {
            params.width = layoutW
            params.height = layoutH
            params.gravity = Gravity.START or Gravity.TOP
            playbackView.layoutParams = params
        }
        playbackView.pivotX = layoutW / 2f
        playbackView.pivotY = layoutH / 2f
        playbackView.rotation = playbackRotation.toFloat()
        playbackView.translationX = (parentW - layoutW) / 2f
        playbackView.translationY = (parentH - layoutH) / 2f
        val viewAspect = layoutW.toFloat() / layoutH.toFloat()
        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
        val scaleX: Float
        val scaleY: Float
        if (videoAspect > viewAspect) {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        } else {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        }
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, layoutW / 2f, layoutH / 2f)
        playbackView.setTransform(matrix)
    }

    private fun readVideoRotation(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not read which way the video is turned", error)
            0
        } finally {
            retriever.release()
        }
    }

    private val playbackListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            playbackSurface = Surface(st)
            if (playRequested) {
                startPlayer(recordingFile())
            }
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
            fitPlayback()
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            releasePlayer()
            playbackSurface?.release()
            playbackSurface = null
            return true
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
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

    private fun showLivePicture() {
        if (recording || playRequested) {
            return
        }
        showMessage(R.string.shape_ready, ButtonMode.LIVE)
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
        if (playRequested && buttonMode != ButtonMode.LIVE && buttonMode != ButtonMode.RECORDING) {
            hidePlayback()
        }
        controlBar.visibility = View.VISIBLE
        statusText.setText(textRes)
        val showRecord = buttonMode == ButtonMode.LIVE || buttonMode == ButtonMode.RECORDING
        recordButton.visibility = if (showRecord) View.VISIBLE else View.GONE
        playButton.visibility = if (showRecord) View.VISIBLE else View.GONE
        recordButton.isEnabled = buttonMode == ButtonMode.LIVE
        if (buttonMode == ButtonMode.RECORDING) {
            recordButton.setText(R.string.recording_now)
        } else {
            recordButton.setText(R.string.record_five)
        }
        refreshPlayButton()
        when (buttonMode) {
            ButtonMode.NONE, ButtonMode.LIVE, ButtonMode.RECORDING -> {
                actionButton.visibility = View.GONE
            }
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

    private fun refreshPlayButton() {
        val file = recordingFile()
        playButton.isEnabled = !recording && file.exists() && file.length() > 0L
    }

    private fun recordingFile(): File = File(filesDir, SAVED_NAME)

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
        LIVE,
        RECORDING,
    }

    private companion object {
        const val TAG = "OffgridCompositor"
        const val SAVED_NAME = "last-recording.mp4"
        const val TEMP_NAME = "recording-now.mp4"
        const val RECORD_MS = 5_000L
        const val FALLBACK_STOP_MS = 8_000L
    }
}
