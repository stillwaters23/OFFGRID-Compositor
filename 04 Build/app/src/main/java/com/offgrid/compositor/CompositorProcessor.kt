package com.offgrid.compositor

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.offgrid.compositor.audio.AvRecorder
import com.offgrid.compositor.effects.EffectEngine
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import com.offgrid.compositor.effects.StagePlan
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Takes the front camera, paints the layer stack, and hands the
 * finished picture to the preview and to the video recorder.
 *
 * Final build, M4: the camera stage changes the picture for the camera effects, and the
 * drawn effects from [layers] go on top. Both follow [engine]. The same finished picture
 * goes to the screen and the video, so what Royalty sees is what viewers get.
 */
class CompositorProcessor(
    private val engine: EffectEngine = EffectEngine(),
    private val layers: List<OverlayLayer> = emptyList(),
) : SurfaceProcessor, SurfaceTexture.OnFrameAvailableListener {
    val effectList = EffectList()

    @Volatile
    var onFirstFrame: (() -> Unit)? = null

    @Volatile
    var onFailed: (() -> Unit)? = null

    /** Called once if the effects can't run on this phone. The picture then stays black when the face must be hidden. */
    @Volatile
    var onEffectsFailed: (() -> Unit)? = null
    private var toldEffectsFailed = false

    /**
     * True when this picture must not show the plain camera: a no-face mode or the face diamond
     * is on, but the effects that hide the face are not drawn (yet). The picture stays black.
     */
    private var coverFace = false

    /** The Step 1 yellow test diamond. Off by default, so nothing extra is painted. */
    @Volatile
    var testShapeOn = false

    /** True when the phone asks for less motion. Effects stay, but calmer. */
    @Volatile
    var reducedMotion = false
        set(value) {
            field = value
            effects?.reducedMotion = value
        }

    private val thread = HandlerThread("OffgridCompositor")
    private val handler: Handler
    private val mainHandler = Handler(Looper.getMainLooper())
    val executor: Executor

    private var display = EGL14.EGL_NO_DISPLAY
    private var context = EGL14.EGL_NO_CONTEXT
    private var dummy = EGL14.EGL_NO_SURFACE
    private var config: android.opengl.EGLConfig? = null
    private var drawer: CompositorDrawer? = null
    private var stage: StagePainter? = null
    private var effects: OverlayRenderer? = null
    private val plan = StagePlan()

    /** The replay buffer and its drawing target, when it is on. */
    @Volatile
    private var replay: ReplayBuffer? = null
    private var replaySurface = EGL14.EGL_NO_SURFACE
    private var replayWanted = false
    private var replaySeconds = 30

    /** The video recorder and its drawing target, while a recording runs. */
    @Volatile
    private var recorder: AvRecorder? = null
    private var recorderSurface = EGL14.EGL_NO_SURFACE
    private val stampClock = StampClock()

    /** True while the camera is off. A timer draws the stage from a black picture instead. */
    private var cameraOff = false
    private var blackCamera: BlackCamera? = null
    private var darkScreen = EGL14.EGL_NO_SURFACE
    private var darkScreenWidth = 0
    private var darkScreenHeight = 0
    private val darkTick = object : Runnable {
        override fun run() {
            if (!cameraOff || released) return
            val started = SystemClock.uptimeMillis()
            drawDark()
            handler.postDelayed(this, maxOf(1L, DARK_FRAME_MS - (SystemClock.uptimeMillis() - started)))
        }
    }

    /**
     * The size of the finished picture, from the biggest camera output. Before the camera starts it is a
     * portrait 720p picture. A recording is made at [recordingSize].
     */
    @Volatile
    private var pictureSize = android.util.Size(DEFAULT_WIDTH, DEFAULT_HEIGHT)

    /** The size to make a recorder at: the picture, no longer than 1280 on its long side, in steps of 16. */
    val recordingSize: android.util.Size
        get() {
            val size = pictureSize
            val scale = minOf(1f, RECORD_LONG_SIDE / maxOf(size.width, size.height).toFloat())
            return android.util.Size(
                maxOf(REPLAY_STEP, (size.width * scale).toInt() / REPLAY_STEP * REPLAY_STEP),
                maxOf(REPLAY_STEP, (size.height * scale).toInt() / REPLAY_STEP * REPLAY_STEP),
            )
        }

    /** The camera stream's size, once the camera starts. The face finder uses its shape. */
    @Volatile
    var inputSize: android.util.Size? = null
        private set
    private var ready = false
    private var released = false
    private val failed = AtomicBoolean(false)
    private var toldFirstFrame = false

    private var inputTexture: SurfaceTexture? = null
    private var inputTextureId = 0
    private val textureTransform = FloatArray(16)
    private val updatedTransform = FloatArray(16)
    private val outputs = mutableListOf<OutputSlot>()
    private val retiredTextures = HashSet<SurfaceTexture>()

    init {
        thread.start()
        handler = Handler(thread.looper)
        executor = Executor { command ->
            if (!handler.post(command)) {
                Log.w(TAG, "Drawing thread is stopped")
            }
        }
        executor.execute { initGl() }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        if (released || !ready) {
            request.willNotProvideSurface()
            if (!released) {
                fail("Picture setup was not ready")
            }
            return
        }
        val textureId = createExternalTexture()
        inputSize = request.resolution
        val surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        val surface = Surface(surfaceTexture)
        inputTexture = surfaceTexture
        inputTextureId = textureId
        toldFirstFrame = false
        surfaceTexture.setOnFrameAvailableListener(this, handler)
        request.provideSurface(surface, executor) {
            executor.execute {
                retiredTextures.add(surfaceTexture)
                if (inputTexture === surfaceTexture) {
                    inputTexture = null
                    inputTextureId = 0
                }
                surfaceTexture.setOnFrameAvailableListener(null)
                surfaceTexture.release()
                surface.release()
                deleteTexture(textureId)
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        if (released || !ready) {
            surfaceOutput.close()
            if (!released) {
                fail("Picture setup was not ready")
            }
            return
        }
        val size = surfaceOutput.size
        val surface = surfaceOutput.getSurface(executor) {
            executor.execute { detach(surfaceOutput) }
        }
        val eglSurface = EGL14.eglCreateWindowSurface(
            display,
            config,
            surface,
            intArrayOf(EGL14.EGL_NONE),
            0,
        )
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "Output surface failed, size ${size.width}x${size.height}")
            surfaceOutput.close()
            fail("Output surface failed")
            return
        }
        outputs.add(OutputSlot(surfaceOutput, eglSurface, size.width, size.height))
        pictureSize = outputs.maxBy { it.width * it.height }.let { android.util.Size(it.width, it.height) }
        Log.i(
            TAG,
            "Output ${size.width}x${size.height} targets=${surfaceOutput.targets}",
        )
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        if (released || surfaceTexture !== inputTexture || retiredTextures.contains(surfaceTexture)) {
            return
        }
        try {
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(textureTransform)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Picture frame failed", error)
            return
        }
        val stamp = surfaceTexture.timestamp
        val snapshot = outputs.toList()
        val painted = paintStage(snapshot)
        for (slot in snapshot) {
            if (!outputs.contains(slot)) {
                continue
            }
            drawSlot(slot, stamp, painted)
        }
        if (snapshot.isNotEmpty()) {
            val main = snapshot.maxBy { it.width * it.height }
            drawRecorders(main.width, main.height, stampClock.next(StampClock.Source.CAMERA, stamp), painted, main)
        }
    }

    /** Draws the finished picture into the replay buffer and the video recorder too, the same one the screen gets. */
    private fun drawRecorders(width: Int, height: Int, stamp: Long, painted: GlTarget?, camera: OutputSlot?) {
        if (released) return
        if (replayWanted) {
            if (replay == null) startReplay(width, height)
            val buffer = replay
            if (buffer != null && replaySurface != EGL14.EGL_NO_SURFACE) {
                drawFinal(replaySurface, buffer.width, buffer.height, stamp, painted, camera)
            }
        }
        val recorder = recorder
        if (recorder != null && recorderSurface != EGL14.EGL_NO_SURFACE) {
            recorder.frameDrawn(stamp)
            drawFinal(recorderSurface, recorder.width, recorder.height, stamp, painted, camera)
        }
    }

    /**
     * Draws one finished picture into [target]: the stage picture, or the plain camera when no
     * effect changes it, then the old effect list and the test diamond. Returns false if the target could not be used.
     */
    private fun drawFinal(target: android.opengl.EGLSurface, w: Int, h: Int, stamp: Long, painted: GlTarget?, camera: OutputSlot?): Boolean {
        if (!EGL14.eglMakeCurrent(display, target, target, context)) {
            return false
        }
        GLES20.glViewport(0, 0, w, h)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val painter = drawer ?: return true
        val staged = stage
        if (painted != null && staged != null) {
            staged.present(painted, w, h)
        } else if (camera != null && inputTextureId != 0 && !coverFace) {
            camera.surfaceOutput.updateTransformMatrix(updatedTransform, textureTransform)
            painter.drawCamera(inputTextureId, updatedTransform)
        }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        effectList.drawAll(w, h)
        if (testShapeOn) {
            painter.drawTopLayer()
        }
        EGLExt.eglPresentationTimeANDROID(display, target, stamp)
        EGL14.eglSwapBuffers(display, target)
        return true
    }

    /** Turns the replay buffer on or off, keeping the last [seconds] seconds. */
    fun setReplay(on: Boolean, seconds: Int) {
        executor.execute {
            replayWanted = on
            replaySeconds = seconds
            replay?.keepSeconds = seconds
            if (!on) stopReplay()
        }
    }

    /** Saves the replay buffer's kept moments to [file]. Returns false if there is nothing yet. Any thread. */
    fun saveReplay(file: java.io.File): Boolean = replay?.save(file) ?: false

    /** The seconds the replay buffer really keeps when its 64 MB is full and lower than the typed seconds. Otherwise null. Any thread. */
    fun replayMemoryLimitSeconds(): Int? = replay?.memoryLimitSeconds

    /** Starts drawing every finished picture into [next]. [next] must be built with [outputSize]. */
    fun startRecorder(next: AvRecorder) {
        executor.execute {
            val eglSurface = if (released) EGL14.EGL_NO_SURFACE else {
                EGL14.eglCreateWindowSurface(display, config, next.surface, intArrayOf(EGL14.EGL_NONE), 0)
            }
            if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                Log.e(TAG, "The video recorder could not draw")
                next.stop()
                return@execute
            }
            recorder = next
            recorderSurface = eglSurface
        }
    }

    /** Stops drawing into the recorder, then lets it finish its file. */
    fun stopRecorder() {
        executor.execute { endRecorder() }
    }

    private fun endRecorder() {
        val finished = recorder ?: return
        if (recorderSurface != EGL14.EGL_NO_SURFACE) {
            if (dummy != EGL14.EGL_NO_SURFACE) EGL14.eglMakeCurrent(display, dummy, dummy, context)
            EGL14.eglDestroySurface(display, recorderSurface)
            recorderSurface = EGL14.EGL_NO_SURFACE
        }
        recorder = null
        finished.stop()
    }

    private fun startReplay(width: Int, height: Int) {
        // Video chips want sizes in steps of 16.
        val w = maxOf(REPLAY_STEP, width / REPLAY_STEP * REPLAY_STEP)
        val h = maxOf(REPLAY_STEP, height / REPLAY_STEP * REPLAY_STEP)
        try {
            val buffer = ReplayBuffer(w, h, replaySeconds)
            val eglSurface = EGL14.eglCreateWindowSurface(display, config, buffer.surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                buffer.release()
                replayWanted = false
                Log.e(TAG, "The replay buffer could not start")
                return
            }
            replay = buffer
            replaySurface = eglSurface
        } catch (error: Exception) {
            Log.e(TAG, "The replay buffer could not start", error)
            replayWanted = false
        }
    }

    private fun stopReplay() {
        if (replaySurface != EGL14.EGL_NO_SURFACE) {
            if (dummy != EGL14.EGL_NO_SURFACE) EGL14.eglMakeCurrent(display, dummy, dummy, context)
            EGL14.eglDestroySurface(display, replaySurface)
            replaySurface = EGL14.EGL_NO_SURFACE
        }
        replay?.release()
        replay = null
    }

    /**
     * Paints the camera stage once for every output, when any effect is on.
     * Returns the stage picture, or null when the camera goes out untouched.
     */
    private fun paintStage(slots: List<OutputSlot>): GlTarget? {
        val settings = engine.settings
        val diamondOn = settings.on(FxKeys.DIAMOND_ON)
        val hideFace = diamondOn || settings.choice(FxKeys.CAMERA_MODE).ifEmpty { FxKeys.MODE_LIVE } != FxKeys.MODE_LIVE
        coverFace = hideFace
        val painter = stage ?: return null
        val textureId = inputTextureId
        if (textureId == 0 || slots.isEmpty()) return null
        val overlay = effects
        val time = engine.now()
        plan.update(
            time = time,
            settings = engine.settings,
            fired = engine.firedNow(),
            face = engine.freshFace(time),
            modeChangedAt = engine.modeChangedAt,
            previousMode = engine.previousMode,
        )
        // The biggest output sets the stage size. The others get the same picture, scaled.
        val main = slots.maxBy { it.width * it.height }
        val scale = minOf(1f, MAX_STAGE_SIDE / maxOf(main.width, main.height).toFloat())
        val stageWidth = maxOf(2, (main.width * scale).toInt())
        val stageHeight = maxOf(2, (main.height * scale).toInt())
        overlay?.latch()
        overlay?.request(stageWidth, stageHeight)
        val drawn = overlay != null && overlay.hasPicture
        // The face diamond is drawn with the other effects. Until that drawing arrives, the
        // picture stays black, so the face never shows for even a moment.
        coverFace = diamondOn && !drawn
        if (coverFace || (plan.plain && !drawn)) return null
        if (!EGL14.eglMakeCurrent(display, main.eglSurface, main.eglSurface, context)) {
            fail("Could not draw a frame")
            coverFace = hideFace
            return null
        }
        main.surfaceOutput.updateTransformMatrix(updatedTransform, textureTransform)
        return try {
            painter.paint(
                cameraTexture = textureId,
                cameraMatrix = updatedTransform,
                w = stageWidth,
                h = stageHeight,
                plan = plan,
                time = time,
                overlayTexture = if (drawn) overlay?.texture ?: 0 else 0,
                overlayMatrix = overlay?.matrix ?: updatedTransform,
            )
        } catch (error: RuntimeException) {
            Log.e(TAG, "Effects stage failed", error)
            stage?.release()
            stage = null
            effectsFailed()
            coverFace = hideFace
            null
        }
    }

    /**
     * Turns the black stage on or off. While the camera is off no pictures come in, so a timer draws the
     * effects over black for the screen, the video recorder, and the replay buffer. Any thread.
     */
    fun setCameraOff(off: Boolean) {
        executor.execute {
            if (off == cameraOff || released) return@execute
            cameraOff = off
            stage?.forgetPictures()
            if (off) handler.post(darkTick) else handler.removeCallbacks(darkTick)
        }
    }

    /** Shows the black stage on [surface] while the camera is off. Pass null to stop. Any thread. */
    fun setDarkScreen(surface: Surface?, width: Int, height: Int) {
        executor.execute {
            if (darkScreen != EGL14.EGL_NO_SURFACE) {
                if (dummy != EGL14.EGL_NO_SURFACE) EGL14.eglMakeCurrent(display, dummy, dummy, context)
                EGL14.eglDestroySurface(display, darkScreen)
                darkScreen = EGL14.EGL_NO_SURFACE
            }
            if (surface == null || released) return@execute
            val created = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (created == null || created == EGL14.EGL_NO_SURFACE) {
                Log.e(TAG, "The black screen could not draw")
                return@execute
            }
            darkScreen = created
            darkScreenWidth = width
            darkScreenHeight = height
        }
    }

    private fun drawDark() {
        if (display == EGL14.EGL_NO_DISPLAY || dummy == EGL14.EGL_NO_SURFACE || !ready) return
        if (!EGL14.eglMakeCurrent(display, dummy, dummy, context)) return
        val black = blackCamera ?: BlackCamera(createExternalTexture()).also { blackCamera = it }
        val size = recordingSize
        val painted = if (black.update()) paintDark(size.width, size.height, black.textureId) else null
        val stamp = stampClock.next(StampClock.Source.BLACK, SystemClock.elapsedRealtimeNanos())
        if (darkScreen != EGL14.EGL_NO_SURFACE) {
            drawFinal(darkScreen, darkScreenWidth, darkScreenHeight, stamp, painted, null)
        }
        drawRecorders(size.width, size.height, stamp, painted, null)
    }

    /** Paints the effects stage over the black picture. */
    private fun paintDark(width: Int, height: Int, blackTexture: Int): GlTarget? {
        val painter = stage ?: return null
        val overlay = effects
        val time = engine.now()
        plan.update(
            time = time,
            settings = engine.settings,
            fired = engine.firedNow(),
            face = engine.freshFace(time),
            modeChangedAt = engine.modeChangedAt,
            previousMode = engine.previousMode,
        )
        overlay?.latch()
        overlay?.request(width, height)
        val drawn = overlay != null && overlay.hasPicture
        coverFace = false
        return try {
            painter.paint(
                cameraTexture = blackTexture,
                cameraMatrix = IDENTITY,
                w = width,
                h = height,
                plan = plan,
                time = time,
                overlayTexture = if (drawn) overlay?.texture ?: 0 else 0,
                overlayMatrix = overlay?.matrix ?: IDENTITY,
            )
        } catch (error: RuntimeException) {
            Log.e(TAG, "Effects stage failed", error)
            stage?.release()
            stage = null
            effectsFailed()
            null
        }
    }

    private fun effectsFailed() {
        if (toldEffectsFailed) return
        toldEffectsFailed = true
        mainHandler.post { onEffectsFailed?.invoke() }
    }

    fun release() {
        executor.execute { releaseOnThread() }
    }

    private fun drawSlot(slot: OutputSlot, stamp: Long, painted: GlTarget?) {
        if (slot.width == 0 || slot.height == 0) {
            return
        }
        if (!drawFinal(slot.eglSurface, slot.width, slot.height, stamp, painted, slot)) {
            fail("Could not draw a frame")
            return
        }
        if (!toldFirstFrame && slot.surfaceOutput.targets and CameraEffect.PREVIEW != 0) {
            toldFirstFrame = true
            mainHandler.post { onFirstFrame?.invoke() }
        }
    }

    private fun detach(surfaceOutput: SurfaceOutput) {
        val slot = outputs.firstOrNull { it.surfaceOutput === surfaceOutput } ?: return
        outputs.remove(slot)
        if (dummy != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, dummy, dummy, context)
        }
        EGL14.eglDestroySurface(display, slot.eglSurface)
        surfaceOutput.close()
    }

    private fun initGl() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) {
            fail("No drawing display")
            return
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            fail("Drawing setup failed")
            return
        }
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val count = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) || count[0] < 1) {
            fail("No drawing setup")
            return
        }
        config = configs[0]
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE,
        )
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            fail("Drawing setup failed")
            return
        }
        val pbufferAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE,
        )
        dummy = EGL14.eglCreatePbufferSurface(display, config, pbufferAttribs, 0)
        if (dummy == EGL14.EGL_NO_SURFACE ||
            !EGL14.eglMakeCurrent(display, dummy, dummy, context)
        ) {
            fail("Drawing setup failed")
            return
        }
        EGL14.eglSwapInterval(display, 0)
        try {
            drawer = CompositorDrawer()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Picture setup failed", error)
            fail("Picture setup failed")
            return
        }
        // The effects are extra. If the phone can't build them, the plain camera still works.
        try {
            stage = StagePainter()
            effects = OverlayRenderer(engine, layers, handler).also { it.reducedMotion = reducedMotion }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Effects setup failed. The camera works without them.", error)
            stage?.release()
            stage = null
            effectsFailed()
        }
        ready = true
    }

    private fun releaseOnThread() {
        if (released) {
            return
        }
        released = true
        ready = false
        val leftover = outputs.toList()
        outputs.clear()
        if (display != EGL14.EGL_NO_DISPLAY && context != EGL14.EGL_NO_CONTEXT) {
            if (dummy != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, dummy, dummy, context)
            }
            for (slot in leftover) {
                EGL14.eglDestroySurface(display, slot.eglSurface)
                slot.surfaceOutput.close()
            }
            drawer?.release()
            drawer = null
            stage?.release()
            stage = null
            effects?.release()
            effects = null
            cameraOff = false
            handler.removeCallbacks(darkTick)
            endRecorder()
            blackCamera?.release()
            blackCamera = null
            if (darkScreen != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, darkScreen)
                darkScreen = EGL14.EGL_NO_SURFACE
            }
            stopReplay()
            if (dummy != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(display, dummy)
                dummy = EGL14.EGL_NO_SURFACE
            }
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
        inputTexture?.setOnFrameAvailableListener(null)
        inputTexture = null
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
        thread.quitSafely()
    }

    private fun createExternalTexture(): Int {
        val names = IntArray(1)
        GLES20.glGenTextures(1, names, 0)
        val name = names[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, name)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        return name
    }

    private fun deleteTexture(name: Int) {
        GLES20.glDeleteTextures(1, intArrayOf(name), 0)
    }

    private fun fail(message: String) {
        Log.e(TAG, "$message (gl ${Integer.toHexString(EGL14.eglGetError())})")
        if (failed.compareAndSet(false, true)) {
            mainHandler.post { onFailed?.invoke() }
        }
    }

    private class OutputSlot(
        val surfaceOutput: SurfaceOutput,
        val eglSurface: android.opengl.EGLSurface,
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val TAG = "OffgridCompositor"

        const val REPLAY_STEP = 16
        const val DEFAULT_WIDTH = 720
        const val DEFAULT_HEIGHT = 1280
        const val RECORD_LONG_SIDE = 1280f
        const val DARK_FRAME_MS = 33L
        val IDENTITY = FloatArray(16).also { android.opengl.Matrix.setIdentityM(it, 0) }

        /** The stage picture's longest side, in pixels. The video is 1280 tall, so this keeps it sharp. */
        const val MAX_STAGE_SIDE = 1920f
    }
}
