package com.offgrid.compositor

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalMirrorMode
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.offgrid.compositor.audio.CaptureSwitch
import com.offgrid.compositor.audio.MicKeys
import com.offgrid.compositor.audio.MicPipeline
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.EffectLayers
import com.offgrid.compositor.audio.voice.VoiceAudioOutput
import com.offgrid.compositor.terminal.TerminalPanel

/**
 * Step 1. Shows the painted camera picture, records, and plays it back.
 * The mic stays off until Microphone Effects turns it on. A hard block keeps it closed.
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

    internal lateinit var previewView: PreviewView
    internal lateinit var playbackView: TextureView
    internal lateinit var playbackBackdrop: View
    internal lateinit var controlBar: View
    internal lateinit var statusText: TextView
    internal lateinit var recordButton: Button
    internal lateinit var playButton: Button
    internal lateinit var actionButton: Button
    internal lateinit var playbackBar: View
    internal lateinit var playbackHint: TextView
    private lateinit var backButton: Button
    internal lateinit var terminalPanel: TerminalPanel

    internal var cameraProvider: ProcessCameraProvider? = null
    internal var processor: CompositorProcessor? = null
    internal var videoCapture: VideoCapture<Recorder>? = null
    internal var activeRecording: Recording? = null
    internal var bindGeneration = 0
    private var permissionAsked = false
    internal val capture = CaptureSwitch()
    internal lateinit var mic: MicPipeline
    internal lateinit var darkStage: DarkStage
    internal var avSession: AvSession? = null
    internal var resumed = false
    internal var cameraDark = false
    internal var micAsking = false

    /** The words for why the mic is off even though its toggle is on, or 0. */
    internal var micProblem = 0
    internal var recording = false
    internal var stopRequested = false
    internal var playRequested = false
    internal var playbackSurface: Surface? = null
    internal var mediaPlayer: MediaPlayer? = null
    internal var playbackFd: ParcelFileDescriptor? = null
    internal var videoWidth = 0
    internal var videoHeight = 0
    internal var playbackRotation = 0
    internal var shownMessage = 0
    internal var diamondInVideo: Boolean? = null
    internal lateinit var effects: EffectsController

    internal fun effectsReady(): Boolean = ::effects.isInitialized

    /** Processed voice for another lane. The recording itself still comes from the same mic engine. */
    fun addVoiceAudioOutput(output: VoiceAudioOutput): AutoCloseable = mic.addVoiceAudioOutput(output)

    /** True when the effects could not start on this phone. The camera screen says why it is black. */
    internal var effectsBroken = false
    private lateinit var faces: FaceTracker

    /** The person mask, shared by the segmenter thread and the drawing thread. */
    private val masks = MaskBus()
    private lateinit var segmenter: PersonSegmenter

    internal val mainHandler = Handler(Looper.getMainLooper())
    internal var stopRecordingTask: Runnable? = null

    internal val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (effects.engine.settings.on(MicKeys.CAM_ON, true)) startCamera()
        } else {
            showPermissionHelp()
        }
    }

    internal val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onMicPermission(granted) }

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
        segmenter = PersonSegmenter(this, engine, masks)
        effects = EffectsController(this, terminalPanel, engine)
        LiveIntegration.install(this, effects, { processor }, controlBar, terminalPanel) // Lane C hook
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
        playbackView.surfaceTextureListener = playbackSurfaceListener()
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
        if (::segmenter.isInitialized) segmenter.release()
        super.onDestroy()
    }

    internal fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun ensureProcessor(): CompositorProcessor {
        processor?.let { return it }
        val created = CompositorProcessor(
            effects.engine,
            EffectLayers.all(),
            applicationContext,
            effects.deckCatalog,
        )
        created.useMasks(masks)
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
        created.probe.previewSampler = { describePreviewView() }
        effects.processor = created
        processor = created
        return created
    }

    @OptIn(ExperimentalMirrorMode::class)
    internal fun bindCamera(
        provider: ProcessCameraProvider,
        withFrameRate: Boolean,
        withSegment: Boolean = true,
    ) {
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
        painter.probe.onBind(
            bindGeneration,
            previewView.width,
            previewView.height,
            "${viewPort.aspectRatio.numerator}:${viewPort.aspectRatio.denominator}",
            rotation,
        )
        val groupBuilder = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(effect)
        if (withSegment) {
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(segmentResolution())
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
            segmenter.attach(analysis)
            groupBuilder.addUseCase(analysis)
        }
        val group = groupBuilder.build()
        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, group)
            videoCapture = capture
        } catch (error: Exception) {
            when {
                withFrameRate -> {
                    Log.w(TAG, "Steady frame rate was not available. Trying again.")
                    bindCamera(provider, withFrameRate = false, withSegment = withSegment)
                }
                withSegment -> {
                    Log.w(TAG, "Person segmentation stream was not available. Body modes stay black.")
                    bindCamera(provider, withFrameRate = false, withSegment = false)
                }
                else -> {
                    Log.e(TAG, "Camera did not start", error)
                    showMessage(R.string.camera_failed, ButtonMode.NONE)
                }
            }
        }
    }

    /** A small 16:9 frame for the person mask, the same crop as the preview. */
    private fun segmentResolution(): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(360, 640),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
    }

    internal fun unbindCamera() {
        bindGeneration++
        cameraProvider?.unbindAll()
        videoCapture = null
    }

    /**
     * Phase 0.5 probe sample, main thread: the PreviewView's size, its stream state, and the
     * TextureView child's size and transform. A non-uniform scale here is the stretch.
     */
    internal fun describePreviewView(): String {
        val child = (0 until previewView.childCount).map { previewView.getChildAt(it) }
            .firstOrNull { it is TextureView } as? TextureView
        val values = FloatArray(9)
        child?.getTransform(null)?.getValues(values)
        val transform = if (child == null) "none" else
            "scaleX=${values[0]} skewX=${values[1]} transX=${values[2]} skewY=${values[3]} scaleY=${values[4]} transY=${values[5]}"
        return "previewView=${previewView.width}x${previewView.height} laidOut=${previewView.isLaidOut} " +
            "stream=${previewView.previewStreamState.value} textureView=${child?.width ?: 0}x${child?.height ?: 0} $transform"
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

    internal enum class ButtonMode {
        NONE,
        TRY_AGAIN,
        SETTINGS,
        LIVE,
        RECORDING,
    }

    private companion object {
        const val TAG = "OffgridCompositor"
    }
}
