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
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.offgrid.compositor.audio.CaptureSwitch
import com.offgrid.compositor.audio.MicKeys
import com.offgrid.compositor.audio.MicPipeline
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.EffectLayers
import com.offgrid.compositor.effects.StageGeometry
import com.offgrid.compositor.audio.voice.VoiceAudioOutput
import com.offgrid.compositor.terminal.PanelAction
import com.offgrid.compositor.terminal.TerminalPanel
import com.offgrid.compositor.terminal.showMinimized

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
 * Phase 1.2. The stage is clean: no record bar, no HUD words, no floating pill. Every message
 * goes to the panel's status line and to Phone 2. Record and Play live in the Record folder.
 * Phase 1.3. The Broadcast Lock ([BroadcastLockChrome], [LockTouch]) hides the panel and the
 * overlay whenever Android 15 reports a screen share, or Royalty locks by hand, by Phone 2, or
 * by arming. Nothing unlocks while a share is live.
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
    internal lateinit var unlockStrip: View
    internal lateinit var unlockButton: View

    internal var cameraProvider: ProcessCameraProvider? = null
    internal var processor: CompositorProcessor? = null
    internal var videoCapture: VideoCapture<Recorder>? = null
    internal var activeRecording: Recording? = null
    internal var bindGeneration = 0

    /** Phase 1.1: the view size the camera was bound at, and the one-rebind rule for a changed shape. */
    internal val rebindGate = StageGeometry.RebindGate()
    internal val rebindTask = Runnable { rebindForNewShape() }
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

    /** Phase 1.3: the Broadcast Lock, its chrome gate, and the stage touch sink and holds. */
    internal lateinit var lockStore: BroadcastLockStore
    internal lateinit var lockChrome: BroadcastLockChrome
    private lateinit var lockTouch: LockTouch
    internal lateinit var coachMark: CoachMark

    /** Lane C, Phase 1.2: the link to Phone 2, which now also shows the status line and the lock word. */
    internal lateinit var live: LiveIntegration

    internal fun effectsReady(): Boolean = ::effects.isInitialized
    internal fun micReady(): Boolean = ::mic.isInitialized
    internal fun darkStageReady(): Boolean = ::darkStage.isInitialized

    /** Processed voice for another lane. The recording itself still comes from the same mic engine. */
    fun addVoiceAudioOutput(output: VoiceAudioOutput): AutoCloseable = mic.addVoiceAudioOutput(output)

    /** True when the effects could not start on this phone. The camera screen says why it is black. */
    internal var effectsBroken = false

    /** False until the first frame has been presented. Camera, MediaPipe, and live stay off until then. */
    internal var afterFirstFrame = false
    private var pendingResume = false
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

    internal val playbackBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hidePlayback()
        }
    }

    internal val unlockBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            lockChrome.unlock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemBars()

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
        unlockStrip = findViewById(R.id.unlock_strip)
        unlockButton = findViewById(R.id.unlock_button)
        unlockButton.setOnClickListener { lockChrome.unlock() }

        onBackPressedDispatcher.addCallback(this, terminalPanel.backCallback)
        onBackPressedDispatcher.addCallback(this, unlockBackCallback)
        // Phase 1.2: with no Back to camera button on the stage, the phone's Back key ends playback.
        onBackPressedDispatcher.addCallback(this, playbackBackCallback)
        terminalPanel.matchLook(
            bars = listOf(controlBar, playbackBar),
            texts = listOf(statusText, playbackHint),
            buttons = listOf(recordButton, playButton, actionButton, backButton),
        )
        terminalPanel.onTestDiamond = { on -> onTestDiamondChanged(on) }
        // Phase 1.3: the Broadcast Lock exists before anything that can show chrome.
        lockStore = BroadcastLockStore(PrefsLockPersistence(this))
        lockChrome = BroadcastLockChrome(this, lockStore)
        lockChrome.onCreate()
        applySystemBars()
        terminalPanel.renderProbeMayOpen = { !recording && !lockStore.locked }
        // Tap is the escapable edit lock on the panel. Hold still arms the Broadcast Lock.
        terminalPanel.onLockTapped = null
        terminalPanel.onLockHeld = { lockChrome.toggleArmed() }
        val engine = EffectEngine()
        mic = MicPipeline()
        engine.beatCount = { mic.beats.beats }
        effects = EffectsController(this, terminalPanel, engine)
        effects.lastVideo = { recordingFile() }
        effects.onSettings = { onEffectSettings() }
        effects.onScreenAction = { action -> onScreenAction(action) }
        describeCamera(terminalPanel.testDiamondOn)
        val root = findViewById<FrameLayout>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            terminalPanel.onKeyboardInset(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
            terminalPanel.onScreenEdges(screenEdges(insets))
            insets
        }
        previewView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            if (afterFirstFrame) onPreviewLayout(right - left, bottom - top)
        }
        val panelOpenInset = (PANEL_OPEN_EDGE_INSET_DP * resources.displayMetrics.density).toInt()
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            lockChrome.applyGestureExclusion(root, panelOpenInset)
        }
        lockTouch = LockTouch(
            chrome = lockChrome,
            slopPx = LockTouch.slopFor(ViewConfiguration.get(this)),
            panelOpenInsetPx = panelOpenInset,
            panelHidden = { terminalPanel.hiddenFromStage },
            playbackShowing = { playRequested },
            cancelViews = { cancelViewTouches() },
            openPanel = {
                coachMark.hide()
                terminalPanel.showMinimized(false, animate = true)
            },
            closePlayback = { hidePlayback() },
            tick = { terminalPanel.look.tick },
            haptic = { kind -> window.decorView.performHapticFeedback(kind) },
            unlockHit = { x, y ->
                if (!::unlockStrip.isInitialized || unlockStrip.visibility != View.VISIBLE) {
                    false
                } else {
                    val loc = IntArray(2)
                    unlockStrip.getLocationInWindow(loc)
                    x >= loc[0] && x < loc[0] + unlockStrip.width &&
                        y >= loc[1] && y < loc[1] + unlockStrip.height
                }
            },
        )
        coachMark = CoachMark(root, CoachMark.prefs(this))
        terminalPanel.onShrunkByTap = {
            val state = lockStore.state
            coachMark.showOnce(
                LockGestureMap.panelOpenZone(root.width, root.height, panelOpenInset),
                allowed = !recording && !state.locked && !state.armed,
            )
        }
        playbackView.surfaceTextureListener = playbackSurfaceListener()
        showMessage(R.string.camera_starting, ButtonMode.NONE)
        android.util.Log.i(STARTUP_TAG, "onCreate_done")
        window.decorView.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                window.decorView.post { startAfterFirstFrame() }
                return true
            }
        })
    }

    /**
     * First present, then the heavy work. Camera bind, MediaPipe, and Live NSD on the first
     * frame were the cold-start FocusEvent ANR (5s wait) on a software GPU.
     */
    private fun startAfterFirstFrame() {
        if (afterFirstFrame || isFinishing || isDestroyed) return
        afterFirstFrame = true
        android.util.Log.i(STARTUP_TAG, "first_frame")
        val engine = effects.engine
        faces = FaceTracker(this, engine)
        segmenter = PersonSegmenter(this, engine, masks)
        live = LiveIntegration.install(this, effects, { processor }, lockStore, terminalPanel)
        effects.start()
        effects.attachInk(previewView)
        terminalPanel.useGuides(findViewById(R.id.snap_guides))
        val root = findViewById<FrameLayout>(R.id.root)
        darkStage = DarkStage(this, root, previewView) { processor }
        android.util.Log.i(STARTUP_TAG, "heavy_ready")
        if (pendingResume) {
            pendingResume = false
            effects.resume()
            applyCapture()
        }
    }

    override fun onStart() {
        super.onStart()
        lockChrome.onStart()
    }

    override fun onStop() {
        lockChrome.onStop()
        // A coach mark still up when the app leaves would be the first thing drawn on resume.
        if (::coachMark.isInitialized) coachMark.hide()
        super.onStop()
    }

    /**
     * Broadcast Lock hides the bars (immersive). If the Android 15 fullscreen hint
     * did not eat this Back, unlock here so Royalty is never stuck on a black stage.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK &&
            event.action == android.view.KeyEvent.ACTION_UP &&
            ::lockStore.isInitialized &&
            lockStore.locked &&
            !lockStore.state.recordingVisible
        ) {
            lockChrome.unlock()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** Phase 1.3: the touch sink and the stage holds see every touch before any view does. */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::lockTouch.isInitialized) {
            val root = findViewById<View>(R.id.root)
            if (lockTouch.dispatch(event, root.width, root.height)) return true
        }
        return super.dispatchTouchEvent(event)
    }

    /** A hold fired: the views under the fingers get a cancel so a pinch or a stroke ends cleanly. */
    private fun cancelViewTouches() {
        val now = android.os.SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        window.superDispatchTouchEvent(cancel)
        cancel.recycle()
    }

    /** Phase 1.2: Record / Stop and Play last video are rows in the Record folder now. */
    private fun onScreenAction(action: PanelAction) {
        when (action) {
            PanelAction.RECORD_TOGGLE -> toggleRecording()
            PanelAction.PLAY_LAST -> showPlayback()
            else -> Unit
        }
    }

    /** The lock changed. Locked stops a playing video: a looping saved clip is not the stage. */
    internal fun onLockApplied(state: LockState) {
        if (state.locked && playRequested) hidePlayback()
        if ((state.locked || state.armed) && ::coachMark.isInitialized) coachMark.hide()
        applySystemBars()
    }

    /** Bars stay up while the panel is visible. Immersive only while Broadcast Lock hid chrome. */
    internal fun applySystemBars() {
        val broadcastLocked = ::lockStore.isInitialized && lockStore.locked
        if (shouldHideSystemBars(broadcastLocked)) hideSystemBars() else showSystemBars()
    }

    /** Visible Unlock control whenever the Broadcast Lock hid the panel and a share is not live. */
    internal fun showUnlockStrip(show: Boolean) {
        if (!::unlockStrip.isInitialized) return
        unlockStrip.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            unlockStrip.bringToFront()
            unlockStrip.elevation = 24f
        }
        unlockBackCallback.isEnabled = show
    }

    /** One line for the panel's status line and for Phone 2. Nothing is drawn on the stage. */
    internal fun sayStatus(message: String) {
        terminalPanel.announce(message)
        if (::live.isInitialized) live.tellPhone2(com.offgrid.live.RemoteMessage.STATUS_LINE_KEY, message)
    }

    override fun onResume() {
        super.onResume()
        lockChrome.onResume()
        applySystemBars()
        resumed = true
        if (!afterFirstFrame) {
            pendingResume = true
        } else {
            effects.resume()
            applyCapture()
        }
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
        lockChrome.onPause()
        if (::effects.isInitialized) effects.pause()
        hidePlayback()
        stopRecording()
        resumed = false
        if (afterFirstFrame) applyCapture()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(rebindTask)
        if (::lockTouch.isInitialized) lockTouch.release()
        if (::lockChrome.isInitialized) lockChrome.onDestroy()
        if (::coachMark.isInitialized) coachMark.hide()
        if (::mic.isInitialized) mic.stop()
        if (::darkStage.isInitialized) darkStage.hide()
        if (::effects.isInitialized) effects.release()
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
            lockChrome.onFirstStageFrame()
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
        if (!::faces.isInitialized) return
        val painter = ensureProcessor()
        faces.outputAspect = previewView.width.toFloat() / previewView.height
        rebindGate.bound(previewView.width, previewView.height)
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
        if (withSegment && ::segmenter.isInitialized) {
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
     *
     * PreviewView (COMPATIBLE mode) sizes the TextureView to the stream resolution and fits it
     * with View scaleX/scaleY/translation; setTransform() only carries the rotation correction.
     * Both are logged, and shown= is the on-screen box those View properties produce.
     */
    internal fun describePreviewView(): String {
        val child = (0 until previewView.childCount).map { previewView.getChildAt(it) }
            .firstOrNull { it is TextureView } as? TextureView
        if (child == null) {
            return "previewView=${previewView.width}x${previewView.height} laidOut=${previewView.isLaidOut} " +
                "stream=${previewView.previewStreamState.value} textureView=none"
        }
        val values = FloatArray(9)
        child.getTransform(null).getValues(values)
        val matrix = "matrix(scaleX=${values[0]} skewX=${values[1]} transX=${values[2]} " +
            "skewY=${values[3]} scaleY=${values[4]} transY=${values[5]})"
        val view = "view(scaleX=${child.scaleX} scaleY=${child.scaleY} " +
            "translationX=${child.translationX} translationY=${child.translationY} " +
            "pivot=${child.pivotX},${child.pivotY} rotation=${child.rotation})"
        val shown = "shown=${child.width * child.scaleX}x${child.height * child.scaleY}"
        return "previewView=${previewView.width}x${previewView.height} laidOut=${previewView.isLaidOut} " +
            "stream=${previewView.previewStreamState.value} textureView=${child.width}x${child.height} " +
            "$view $shown $matrix"
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
        const val STARTUP_TAG = "OffgridStartup"

        /** The panel-open hold zone starts this far in from the screen edges: no edge gestures (V2 7.3). */
        const val PANEL_OPEN_EDGE_INSET_DP = 48f
    }
}
