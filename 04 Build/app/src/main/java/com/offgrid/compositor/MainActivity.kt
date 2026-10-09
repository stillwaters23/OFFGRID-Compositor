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
import android.view.RoundedCorner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalMirrorMode
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.offgrid.compositor.audio.CaptureAction
import com.offgrid.compositor.audio.CaptureSwitch
import com.offgrid.compositor.audio.MicKeys
import com.offgrid.compositor.audio.MicPipeline
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.EffectLayers
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.terminal.ScreenEdges
import com.offgrid.compositor.terminal.TerminalPanel
import java.io.File

/**
 * Step 1. Shows the painted camera picture, records, and plays it back.
 * Step 2. The floating terminal panel sits on top. It is not painted into the video.
 * Step 2.1. The yellow test diamond is off unless Panel settings turns it on.
 * Final build. The effects, the no-face camera modes, and the face finder are baked into the
 * video. The screen shows the same picture viewers get, not a mirror. Videos can be any
 * length and also go to the gallery.
 * Lane B. The camera and mic toggles in the panel start and release the camera and the mic.
 * Videos are recorded by [AvSession], with the mic and its effects. While the camera is off,
 * [DarkStage] shows black with the effects drawn over it.
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
    private lateinit var terminalPanel: TerminalPanel

    private var cameraProvider: ProcessCameraProvider? = null
    private var processor: CompositorProcessor? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var bindGeneration = 0
    private var permissionAsked = false
    private val capture = CaptureSwitch()
    private lateinit var mic: MicPipeline
    private lateinit var darkStage: DarkStage
    private var avSession: AvSession? = null
    private var resumed = false
    private var cameraDark = false
    private var micAsking = false

    /** The words for why the mic is off even though its toggle is on, or 0. */
    private var micProblem = 0
    private var recording = false
    private var stopRequested = false
    private var playRequested = false
    private var playbackSurface: Surface? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackFd: ParcelFileDescriptor? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var playbackRotation = 0
    private var shownMessage = 0
    private var diamondInVideo: Boolean? = null
    private lateinit var effects: EffectsController

    /** True when the effects could not start on this phone. The camera screen says why it is black. */
    private var effectsBroken = false
    private lateinit var faces: FaceTracker

    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRecordingTask: Runnable? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (effects.engine.settings.on(MicKeys.CAM_ON, true)) startCamera()
        } else {
            showPermissionHelp()
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micAsking = false
        if (!granted) {
            micProblem = R.string.mic_denied
            capture.micPermissionDenied()
        }
        applyCapture()
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
        terminalPanel = findViewById(R.id.terminal_panel)

        onBackPressedDispatcher.addCallback(this, terminalPanel.backCallback)
        terminalPanel.matchLook(
            bars = listOf(controlBar, playbackBar),
            texts = listOf(statusText, playbackHint),
            buttons = listOf(recordButton, playButton, actionButton, backButton),
        )
        terminalPanel.onTestDiamond = { on -> onTestDiamondChanged(on) }
        val engine = EffectEngine()
        mic = MicPipeline()
        engine.beatCount = { mic.beats.beats }
        faces = FaceTracker(this, engine)
        effects = EffectsController(this, terminalPanel, engine)
        effects.lastVideo = { recordingFile() }
        effects.onSettings = { onEffectSettings() }
        effects.start()
        effects.attachInk(previewView)
        terminalPanel.useGuides(findViewById(R.id.snap_guides))
        describeCamera(terminalPanel.testDiamondOn)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            terminalPanel.onKeyboardInset(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
            terminalPanel.onScreenEdges(screenEdges(insets))
            insets
        }
        darkStage = DarkStage(this, findViewById(R.id.root), previewView) { processor }
        recordButton.setOnClickListener { toggleRecording() }
        playButton.setOnClickListener { showPlayback() }
        backButton.setOnClickListener { hidePlayback() }
        playbackView.surfaceTextureListener = playbackListener
        showMessage(R.string.camera_starting, ButtonMode.NONE)
    }

    override fun onResume() {
        super.onResume()
        effects.resume()
        hideSystemBars()
        resumed = true
        applyCapture()
        if (hasCameraPermission()) {
            return
        }
        if (!permissionAsked) {
            permissionAsked = true
            showMessage(R.string.camera_needs_ok, ButtonMode.NONE)
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            showPermissionHelp()
        }
    }

    override fun onPause() {
        effects.pause()
        hidePlayback()
        stopRecording()
        resumed = false
        applyCapture()
        super.onPause()
    }

    override fun onDestroy() {
        mic.stop()
        darkStage.hide()
        effects.release()
        processor?.onFirstFrame = null
        processor?.onFailed = null
        processor?.onEffectsFailed = null
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

    /** The compositor draws the camera, or black while the camera is off. It is made once. */
    private fun ensureProcessor(): CompositorProcessor {
        processor?.let { return it }
        val created = CompositorProcessor(effects.engine, EffectLayers.all())
        created.onFirstFrame = {
            created.inputSize?.let { size ->
                faces.streamAspect = maxOf(size.width, size.height).toFloat() / minOf(size.width, size.height)
            }
            showLivePicture()
        }
        created.onEffectsFailed = {
            effectsBroken = true
            showLivePicture()
        }
        created.onFailed = {
            stopRecording()
            showMessage(R.string.camera_failed, ButtonMode.NONE)
        }
        created.testShapeOn = terminalPanel.testDiamondOn
        created.reducedMotion = reducedMotion()
        effects.processor = created
        processor = created
        return created
    }

    @OptIn(ExperimentalMirrorMode::class)
    private fun bindCamera(provider: ProcessCameraProvider, withFrameRate: Boolean) {
        val painter = ensureProcessor()
        faces.outputAspect = previewView.width.toFloat() / previewView.height
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
        // Portrait rotation plays it as 720 by 1280. This recorder is only the fallback when AvSession
        // cannot start, and it has no audio.
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                ),
            )
            .build()
        // The screen shows exactly what viewers get, not a mirror, so words read the right way
        // and the face diamond and drawings land where they look (final build).
        val previewBuilder = Preview.Builder()
            .setResolutionSelector(previewResolution())
            .setTargetRotation(rotation)
            .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
        faces.attach(previewBuilder)
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

    private fun toggleRecording() {
        if (recording) {
            // While recording, the same button is Stop.
            requestStop()
            return
        }
        val painter = processor ?: return
        val temp = File(filesDir, TEMP_NAME)
        if (temp.exists() && !temp.delete()) {
            showMessage(R.string.record_failed, ButtonMode.LIVE)
            return
        }
        stopRequested = false
        diamondInVideo = terminalPanel.testDiamondOn
        val session = avSession ?: AvSession(painter, mic.engine, effects.videoSounds).also { avSession = it }
        if (session.start(temp) { whole -> onRecordingFinished(temp, whole) }) {
            beginRecording()
            val seconds = recordSeconds()
            if (seconds > 0) {
                val stopTask = Runnable { requestStop() }
                stopRecordingTask = stopTask
                mainHandler.postDelayed(stopTask, seconds * 1000L)
            }
        } else {
            startFallbackRecording(temp)
        }
    }

    private fun beginRecording() {
        recording = true
        showMessage(R.string.recording_now, ButtonMode.RECORDING)
        recordSeconds().takeIf { it > 0 }?.let { statusText.text = getString(R.string.recording_timed, it) }
    }

    /** Records with the camera's own recorder when [AvSession] would not start. It has no sound. */
    private fun startFallbackRecording(temp: File) {
        val capture = videoCapture
        if (capture == null) {
            showMessage(R.string.record_failed, ButtonMode.LIVE)
            return
        }
        beginRecording()
        statusText.setText(R.string.record_no_sound)
        val options = FileOutputOptions.Builder(temp).build()
        val pending = capture.output.prepareRecording(this, options)
        val started = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            onRecordEvent(event, temp)
        }
        activeRecording = started
        val seconds = recordSeconds()
        if (seconds > 0) {
            // A safety stop in case the recorder never says it started.
            val stopTask = Runnable { requestStop() }
            stopRecordingTask = stopTask
            mainHandler.postDelayed(stopTask, seconds * 1000L + FALLBACK_EXTRA_MS)
        }
    }

    /** The typed record length in seconds. 0 means record until Stop. */
    private fun recordSeconds(): Int = effects.engine.settings.raw(FxKeys.RECORD_SECONDS)

    private fun requestStop() {
        if (stopRequested) {
            return
        }
        stopRequested = true
        if (avSession?.active == true) avSession?.stop() else activeRecording?.stop()
    }

    private fun onRecordEvent(event: VideoRecordEvent, temp: File) {
        when (event) {
            is VideoRecordEvent.Start -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                stopRecordingTask = null
                val seconds = recordSeconds()
                if (seconds > 0) {
                    val stopTask = Runnable { requestStop() }
                    stopRecordingTask = stopTask
                    mainHandler.postDelayed(stopTask, seconds * 1000L)
                }
            }
            is VideoRecordEvent.Finalize -> {
                stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
                stopRecordingTask = null
                activeRecording = null
                recording = false
                // Some errors still leave a whole, playable video, like a full phone or the camera
                // closing when the app goes to the background. Those videos are kept.
                val kept = !event.hasError() || (event.error in KEPT_ERRORS && temp.exists() && temp.length() > 0L)
                if (event.hasError()) Log.e(TAG, "Recording ended with code ${event.error}, kept: $kept")
                finishRecording(temp, kept)
            }
            else -> Unit
        }
    }

    private fun onRecordingFinished(temp: File, whole: Boolean) {
        stopRecordingTask?.let { mainHandler.removeCallbacks(it) }
        stopRecordingTask = null
        recording = false
        finishRecording(temp, whole && temp.exists() && temp.length() > 0L)
    }

    /** Moves a finished video into place and says so, or says it failed. */
    private fun finishRecording(temp: File, kept: Boolean) {
        if (!kept) {
            if (temp.exists()) {
                temp.delete()
            }
            showMessage(R.string.record_failed, ButtonMode.LIVE)
            return
        }
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
        if (effects.engine.settings.on(FxKeys.RECORD_GALLERY)) {
            GallerySaver.saveLater(this, saved) { name ->
                // Only change the words if they still say the video was saved.
                if (isDestroyed || shownMessage != R.string.record_saved) return@saveLater
                showMessage(
                    if (name != null) R.string.record_saved_gallery else R.string.record_gallery_failed,
                    ButtonMode.LIVE,
                )
            }
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
        terminalPanel.setHiddenForPlayback(true)
        playbackBackdrop.visibility = View.VISIBLE
        playbackView.visibility = View.VISIBLE
        playbackBar.visibility = View.VISIBLE
        val diamond = diamondInVideo ?: terminalPanel.testDiamondOn
        playbackHint.setText(if (diamond) R.string.playback_hint else R.string.playback_hint_plain)
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
        terminalPanel.setHiddenForPlayback(false)
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
            player.setVolume(1f, 1f)
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
        val settings = effects.engine.settings
        val hidingFace = settings.on(FxKeys.DIAMOND_ON) || !settings.chose(FxKeys.CAMERA_MODE, FxKeys.MODE_LIVE)
        val message = when {
            effectsBroken && hidingFace && !cameraDark -> R.string.effects_failed
            micProblem != 0 -> micProblem
            cameraDark -> R.string.camera_off
            settings.chose(FxKeys.CAMERA_MODE, FxKeys.MODE_GHOST) -> R.string.ghost_on
            terminalPanel.testDiamondOn -> R.string.shape_ready
            else -> R.string.camera_ready
        }
        showMessage(message, ButtonMode.LIVE)
    }

    /** The Test diamond toggle in Panel settings. Off paints nothing extra into the video. */
    private fun onTestDiamondChanged(on: Boolean) {
        processor?.testShapeOn = on
        describeCamera(on)
        if (shownMessage in LIVE_MESSAGES) {
            showLivePicture()
        }
    }

    /** Effect settings changed: the GHOST ON line and the Record button's length follow at once. */
    private fun onEffectSettings() {
        if (!::effects.isInitialized) return
        processor?.reducedMotion = reducedMotion()
        applyCapture()
        if (shownMessage in LIVE_MESSAGES) {
            showLivePicture()
        } else if (!recording) {
            setRecordLabel()
        }
    }

    private fun hasMicPermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Makes the camera and the mic match the two toggles in the panel, and the mic's Microphone Effects
     * match their typed values. The camera and the mic are both let go while the app is hidden.
     */
    private fun applyCapture() {
        val settings = effects.engine.settings
        val cameraWanted = settings.on(MicKeys.CAM_ON, true)
        val micToggle = settings.on(MicKeys.MIC_ON)
        if (!micToggle) micProblem = 0
        mic.apply(settings)
        if (resumed && micToggle && micProblem == 0 && !hasMicPermission() && !micAsking) askForMic()
        val micWanted = micToggle && micProblem == 0
        val actions = capture.update(cameraWanted, micWanted, hasMicPermission(), resumed)
        for (action in actions) {
            when (action) {
                CaptureAction.UNBIND_CAMERA -> unbindCamera()
                CaptureAction.BIND_CAMERA -> if (hasCameraPermission()) startCamera()
                CaptureAction.START_MIC -> if (!mic.start()) {
                    capture.micFailed()
                    micProblem = R.string.mic_failed
                }
                CaptureAction.RELEASE_MIC -> mic.stop()
            }
        }
        showDark(resumed && !cameraWanted)
        if (resumed && shownMessage in LIVE_MESSAGES) showLivePicture()
    }

    /** Says why first, then asks the phone for the mic. */
    private fun askForMic() {
        micAsking = true
        showMessage(R.string.mic_needs_ok, ButtonMode.LIVE)
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /** Black picture with the effects drawn over it, instead of the camera. */
    private fun showDark(on: Boolean) {
        if (on == cameraDark) return
        cameraDark = on
        if (on) {
            ensureProcessor().setCameraOff(true)
            darkStage.show()
            showLivePicture()
        } else {
            darkStage.hide()
            processor?.setCameraOff(false)
        }
    }

    /** True when the phone's Remove animations setting is on. Effects stay, but calmer. */
    private fun reducedMotion(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    private fun setRecordLabel() {
        val seconds = if (::effects.isInitialized) recordSeconds() else 0
        if (seconds > 0) {
            recordButton.text = getString(R.string.record_seconds, seconds)
        } else {
            recordButton.setText(R.string.record_open)
        }
    }

    private fun describeCamera(diamondOn: Boolean) {
        previewView.contentDescription = getString(
            if (diamondOn) R.string.camera_picture else R.string.camera_picture_plain,
        )
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
        shownMessage = textRes
        statusText.setText(textRes)
        val showRecord = buttonMode == ButtonMode.LIVE || buttonMode == ButtonMode.RECORDING
        recordButton.visibility = if (showRecord) View.VISIBLE else View.GONE
        playButton.visibility = if (showRecord) View.VISIBLE else View.GONE
        recordButton.isEnabled = buttonMode == ButtonMode.LIVE || buttonMode == ButtonMode.RECORDING
        if (buttonMode == ButtonMode.RECORDING) {
            recordButton.setText(R.string.record_stop)
        } else {
            setRecordLabel()
        }
        refreshPlayButton()
        // A button to tap must never hide behind the panel (final build, Rex's review).
        terminalPanel.setBarNeedsTap(buttonMode == ButtonMode.TRY_AGAIN || buttonMode == ButtonMode.SETTINGS)
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

    /**
     * The bars, camera hole, gesture areas, and round corners, for the panel (final build, M2).
     * Bars are measured as if they show, so pulling the status bar down never moves the panel.
     */
    private fun screenEdges(insets: WindowInsetsCompat): ScreenEdges {
        val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val gestures = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemGestures())
        val mandatory = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.mandatorySystemGestures())
        val platform = insets.toWindowInsets()
        val corner = if (platform != null) {
            listOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
                RoundedCorner.POSITION_BOTTOM_RIGHT,
            ).maxOf { platform.getRoundedCorner(it)?.radius ?: 0 }
        } else {
            0
        }
        return ScreenEdges(
            top = maxOf(bars.top, cutout.top),
            bottom = maxOf(bars.bottom, cutout.bottom),
            left = maxOf(bars.left, cutout.left),
            right = maxOf(bars.right, cutout.right),
            gestureLeft = gestures.left,
            gestureRight = gestures.right,
            gestureBottom = maxOf(gestures.bottom, mandatory.bottom),
            cornerRadius = corner,
        )
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
        const val FALLBACK_EXTRA_MS = 3_000L

        /** The words shown while the camera is live, which follow the settings. */
        val LIVE_MESSAGES = setOf(
            R.string.shape_ready,
            R.string.camera_ready,
            R.string.ghost_on,
            R.string.effects_failed,
            R.string.camera_off,
            R.string.mic_denied,
            R.string.mic_failed,
        )

        /** Recording errors that still leave a whole video behind. */
        val KEPT_ERRORS = setOf(
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE,
        )
    }
}
