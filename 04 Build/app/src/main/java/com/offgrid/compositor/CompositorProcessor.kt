package com.offgrid.compositor

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
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
        drawReplay(snapshot, stamp, painted)
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

    /** Draws the finished picture into the replay buffer too, the same one the video gets. */
    private fun drawReplay(slots: List<OutputSlot>, stamp: Long, painted: GlTarget?) {
        if (!replayWanted || released || slots.isEmpty()) return
        val main = slots.maxBy { it.width * it.height }
        if (replay == null) startReplay(main.width, main.height)
        val buffer = replay ?: return
        if (replaySurface == EGL14.EGL_NO_SURFACE) return
        if (!EGL14.eglMakeCurrent(display, replaySurface, replaySurface, context)) return
        GLES20.glViewport(0, 0, buffer.width, buffer.height)
        val staged = stage
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (painted != null && staged != null) {
            staged.present(painted, buffer.width, buffer.height)
        } else if (!coverFace) {
            val painter = drawer ?: return
            main.surfaceOutput.updateTransformMatrix(updatedTransform, textureTransform)
            painter.drawCamera(inputTextureId, updatedTransform)
        }
        EGLExt.eglPresentationTimeANDROID(display, replaySurface, stamp)
        EGL14.eglSwapBuffers(display, replaySurface)
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
        if (!EGL14.eglMakeCurrent(display, slot.eglSurface, slot.eglSurface, context)) {
            fail("Could not draw a frame")
            return
        }
        GLES20.glViewport(0, 0, slot.width, slot.height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        slot.surfaceOutput.updateTransformMatrix(updatedTransform, textureTransform)
        val textureId = inputTextureId
        val painter = drawer
        if (textureId == 0 || painter == null) {
            return
        }
        val staged = stage
        if (painted != null && staged != null) {
            staged.present(painted, slot.width, slot.height)
        } else if (!coverFace) {
            painter.drawCamera(textureId, updatedTransform)
        }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        effectList.drawAll(slot.width, slot.height)
        if (testShapeOn) {
            painter.drawTopLayer()
        }
        EGLExt.eglPresentationTimeANDROID(display, slot.eglSurface, stamp)
        EGL14.eglSwapBuffers(display, slot.eglSurface)
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

        /** The stage picture's longest side, in pixels. The video is 1280 tall, so this keeps it sharp. */
        const val MAX_STAGE_SIDE = 1920f
    }
}
