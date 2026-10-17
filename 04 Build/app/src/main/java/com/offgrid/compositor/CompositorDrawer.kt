package com.offgrid.compositor

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Paints one frame: camera, then the effect list, then one bright shape.
 */
class CompositorDrawer {
    private val cameraProgram: Int
    private val shapeProgram: Int
    private val cameraPosition: Int
    private val cameraTexCoord: Int
    private val cameraMatrix: Int
    private val cameraSampler: Int
    private val cameraFlipX: Int
    private val shapePosition: Int
    private val shapeColor: Int

    private val quadPositions: FloatBuffer = floatBuffer(
        floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        ),
    )
    private val quadTexCoords: FloatBuffer = floatBuffer(
        floatArrayOf(
            // Identity UVs. CameraX / SurfaceTexture uTexMatrix orients the frame;
            // uFlipX then mirrors in display space for a stable upright selfie.
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        ),
    )
    private val outlineDiamond: FloatBuffer = diamond(0.40f, 0.70f, -0.04f)
    private val fillDiamond: FloatBuffer = diamond(0.32f, 0.60f, 0.04f)

    init {
        cameraProgram = buildProgram(CAMERA_VERTEX, CAMERA_FRAGMENT)
        shapeProgram = buildProgram(SHAPE_VERTEX, SHAPE_FRAGMENT)
        cameraPosition = GLES20.glGetAttribLocation(cameraProgram, "aPosition")
        cameraTexCoord = GLES20.glGetAttribLocation(cameraProgram, "aTexCoord")
        cameraMatrix = GLES20.glGetUniformLocation(cameraProgram, "uTexMatrix")
        cameraSampler = GLES20.glGetUniformLocation(cameraProgram, "sTexture")
        cameraFlipX = GLES20.glGetUniformLocation(cameraProgram, "uFlipX")
        shapePosition = GLES20.glGetAttribLocation(shapeProgram, "aPosition")
        shapeColor = GLES20.glGetUniformLocation(shapeProgram, "uColor")
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }

    /**
     * Draws the front-camera OES texture.
     * [flipX] 1 (default) mirrors left/right in display space after [texMatrix]
     * (upright selfie). Pass 0 for an unmirrored feed.
     */
    fun drawCamera(textureId: Int, texMatrix: FloatArray, flipX: Float = 1f) {
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(cameraProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(cameraSampler, 0)
        GLES20.glUniformMatrix4fv(cameraMatrix, 1, false, texMatrix, 0)
        GLES20.glUniform1f(cameraFlipX, flipX)
        drawStrip(cameraPosition, quadPositions, cameraTexCoord, quadTexCoords)
    }

    fun drawTopLayer() {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(shapeProgram)
        GLES20.glUniform4f(shapeColor, 1f, 0.08f, 0.55f, 1f)
        drawStrip(shapePosition, outlineDiamond, -1, null)
        GLES20.glUniform4f(shapeColor, 1f, 0.92f, 0.08f, 1f)
        drawStrip(shapePosition, fillDiamond, -1, null)
    }

    fun release() {
        GLES20.glDeleteProgram(cameraProgram)
        GLES20.glDeleteProgram(shapeProgram)
    }

    private fun drawStrip(
        positionHandle: Int,
        positions: FloatBuffer,
        texHandle: Int,
        texCoords: FloatBuffer?,
    ) {
        positions.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, positions)
        if (texHandle >= 0 && texCoords != null) {
            texCoords.position(0)
            GLES20.glEnableVertexAttribArray(texHandle)
            GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texCoords)
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        if (texHandle >= 0) {
            GLES20.glDisableVertexAttribArray(texHandle)
        }
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException(log)
        }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return program
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

    private fun diamond(halfWidth: Float, top: Float, bottom: Float): FloatBuffer {
        val mid = (top + bottom) / 2f
        return floatBuffer(
            floatArrayOf(
                0f, top,
                -halfWidth, mid,
                halfWidth, mid,
                0f, bottom,
            ),
        )
    }

    private companion object {
        const val CAMERA_VERTEX = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uTexMatrix;
            uniform float uFlipX;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
                vec2 tc = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
                vTexCoord = vec2(mix(tc.x, 1.0 - tc.x, uFlipX), tc.y);
            }
        """

        const val CAMERA_FRAGMENT = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTexCoord);
            }
        """

        const val SHAPE_VERTEX = """
            attribute vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        const val SHAPE_FRAGMENT = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
    }
}
