package com.offgrid.compositor

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.ArrayDeque
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

/**
 * Draws the front camera into a full-screen picture.
 * The picture is mirrored, the way a front camera usually looks.
 */
class FrontCameraRenderer(
    private val view: FrontCameraGlView,
) : GLSurfaceView.Renderer {

    var onFirstFrame: (() -> Unit)? = null
    var onRendererFailed: (() -> Unit)? = null

    private val pending = ArrayDeque<SurfaceRequest>()
    private val texMatrix = FloatArray(16)
    private val vertexBuffer = floatBuffer(
        floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        ),
    )
    private val texBuffer = floatBuffer(
        floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        ),
    )

    private var program = 0
    private var positionHandle = -1
    private var texCoordHandle = -1
    private var texMatrixHandle = -1
    private var scaleHandle = -1
    private var samplerHandle = -1

    private var activeTexture: SurfaceTexture? = null
    private var bufferWidth = 0
    private var bufferHeight = 0
    private var viewWidth = 0
    private var viewHeight = 0
    private var scaleX = 1f
    private var scaleY = 1f

    @Volatile
    private var frameAvailable = false

    // Camera mirror mode is left off, so the front camera picture is flipped here.
    // That makes it look like a normal front camera.
    private val mirror = true

    private var hasFrame = false
    private var toldActivity = false

    fun submit(request: SurfaceRequest) {
        synchronized(pending) {
            pending.add(request)
        }
    }

    fun clearPending() {
        val leftover = synchronized(pending) {
            val copy = pending.toList()
            pending.clear()
            copy
        }
        leftover.forEach { request ->
            request.willNotProvideSurface()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        activeTexture = null
        hasFrame = false
        toldActivity = false
        frameAvailable = false
        try {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Picture setup failed", error)
            program = 0
            view.post { onRendererFailed?.invoke() }
            return
        }
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        texMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
        scaleHandle = GLES20.glGetUniformLocation(program, "uScale")
        samplerHandle = GLES20.glGetUniformLocation(program, "sTexture")
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.setIdentityM(texMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        updateScale()
    }

    override fun onDrawFrame(gl: GL10?) {
        fulfillPending()
        val texture = activeTexture
        if (texture != null && frameAvailable) {
            frameAvailable = false
            try {
                texture.updateTexImage()
                texture.getTransformMatrix(texMatrix)
                hasFrame = true
                updateScale()
                if (!toldActivity) {
                    toldActivity = true
                    view.post { onFirstFrame?.invoke() }
                }
            } catch (error: RuntimeException) {
                Log.e(TAG, "Picture frame failed", error)
                hasFrame = false
            }
        }

        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (texture == null || !hasFrame || program == 0) {
            return
        }

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId(texture))
        GLES20.glUniform1i(samplerHandle, 0)
        GLES20.glUniformMatrix4fv(texMatrixHandle, 1, false, texMatrix, 0)
        GLES20.glUniform2f(scaleHandle, scaleX, scaleY)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        texBuffer.position(0)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun fulfillPending() {
        val batch = synchronized(pending) {
            val copy = pending.toList()
            pending.clear()
            copy
        }
        batch.forEach { request -> fulfill(request) }
    }

    private fun fulfill(request: SurfaceRequest) {
        if (program == 0) {
            request.willNotProvideSurface()
            return
        }
        val textureName = createExternalTexture()
        val surfaceTexture = SurfaceTexture(textureName)
        surfaceTexture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        bufferWidth = request.resolution.width
        bufferHeight = request.resolution.height
        hasFrame = false
        toldActivity = false
        frameAvailable = false
        activeTexture = surfaceTexture
        rememberTexture(surfaceTexture, textureName)
        surfaceTexture.setOnFrameAvailableListener {
            frameAvailable = true
            view.requestRender()
        }
        val surface = Surface(surfaceTexture)
        val mainExecutor = ContextCompat.getMainExecutor(view.context)
        request.provideSurface(surface, mainExecutor) {
            val release = Runnable {
                if (activeTexture === surfaceTexture) {
                    activeTexture = null
                    hasFrame = false
                }
                surfaceTexture.setOnFrameAvailableListener(null)
                surfaceTexture.release()
                surface.release()
                forgetTexture(surfaceTexture)
            }
            try {
                view.queueEvent(release)
            } catch (error: RuntimeException) {
                Log.w(TAG, "Picture cleanup ran off the drawing thread", error)
                release.run()
            }
        }
        updateScale()
    }

    private fun updateScale() {
        if (viewWidth == 0 || viewHeight == 0 || bufferWidth == 0 || bufferHeight == 0) {
            scaleX = if (mirror) -1f else 1f
            scaleY = 1f
            return
        }
        val swapsAxes = abs(texMatrix[1]) > abs(texMatrix[0])
        val contentWidth = if (swapsAxes) bufferHeight else bufferWidth
        val contentHeight = if (swapsAxes) bufferWidth else bufferHeight
        val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
        val contentAspect = contentWidth.toFloat() / contentHeight.toFloat()
        var nextScaleX = 1f
        var nextScaleY = 1f
        if (contentAspect > viewAspect) {
            nextScaleX = contentAspect / viewAspect
        } else {
            nextScaleY = viewAspect / contentAspect
        }
        if (mirror) {
            nextScaleX = -nextScaleX
        }
        scaleX = nextScaleX
        scaleY = nextScaleY
    }

    private val textureNames = HashMap<SurfaceTexture, Int>()

    private fun rememberTexture(surfaceTexture: SurfaceTexture, name: Int) {
        textureNames[surfaceTexture] = name
    }

    private fun forgetTexture(surfaceTexture: SurfaceTexture) {
        textureNames.remove(surfaceTexture)
    }

    private fun textureId(surfaceTexture: SurfaceTexture): Int {
        return textureNames[surfaceTexture] ?: 0
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

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertex)
        GLES20.glAttachShader(shaderProgram, fragment)
        GLES20.glLinkProgram(shaderProgram)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(shaderProgram)
            GLES20.glDeleteProgram(shaderProgram)
            throw IllegalStateException(log)
        }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return shaderProgram
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException(log)
        }
        return shader
    }

    private fun floatBuffer(values: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    private companion object {
        const val TAG = "OffgridPreview"

        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            uniform vec2 uScale;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPosition.x * uScale.x, aPosition.y * uScale.y, 0.0, 1.0);
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        """
    }
}
