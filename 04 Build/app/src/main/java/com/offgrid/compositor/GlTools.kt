package com.offgrid.compositor

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * One GPU program, with its named inputs looked up once (final build, M4).
 * Throws [IllegalStateException] with the GPU's own words if the program does not build.
 */
internal class GlProgram(vertexSource: String, fragmentSource: String) {
    val id: Int
    private val names = HashMap<String, Int>()

    init {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException(log)
        }
        id = program
    }

    fun use() {
        GLES20.glUseProgram(id)
    }

    /** A per-corner input, or -1 if the program does not use it. */
    fun attrib(name: String): Int = names.getOrPut("a:$name") { GLES20.glGetAttribLocation(id, name) }

    /** A whole-picture input, or -1 if the program does not use it. */
    fun uniform(name: String): Int = names.getOrPut("u:$name") { GLES20.glGetUniformLocation(id, name) }

    fun set1f(name: String, value: Float) {
        val at = uniform(name)
        if (at >= 0) GLES20.glUniform1f(at, value)
    }

    fun set1i(name: String, value: Int) {
        val at = uniform(name)
        if (at >= 0) GLES20.glUniform1i(at, value)
    }

    fun set4f(name: String, a: Float, b: Float, c: Float, d: Float) {
        val at = uniform(name)
        if (at >= 0) GLES20.glUniform4f(at, a, b, c, d)
    }

    fun setMatrix(name: String, matrix: FloatArray) {
        val at = uniform(name)
        if (at >= 0) GLES20.glUniformMatrix4fv(at, 1, false, matrix, 0)
    }

    fun release() {
        GLES20.glDeleteProgram(id)
    }

    private fun compile(type: Int, source: String): Int {
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
}

/**
 * A picture kept on the GPU that the stage can draw into and read back from.
 * It is sized in pixels and can be resized; resizing clears it.
 */
internal class GlTarget {
    var texture = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    private var framebuffer = 0

    /** True after a picture was drawn into it since it was made or resized. */
    var filled = false

    val ready: Boolean get() = texture != 0

    fun ensure(w: Int, h: Int) {
        if (w == width && h == height && texture != 0) return
        release()
        val names = IntArray(1)
        GLES20.glGenTextures(1, names, 0)
        texture = names[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null,
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glGenFramebuffers(1, names, 0)
        framebuffer = names[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0,
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            release()
            throw IllegalStateException("Picture store failed: ${Integer.toHexString(status)}")
        }
        width = w
        height = h
        filled = false
    }

    /** Draws into this picture from now on. */
    fun bind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
    }

    fun release() {
        if (framebuffer != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        framebuffer = 0
        texture = 0
        width = 0
        height = 0
        filled = false
    }
}

/** Shared corner lists for drawing. */
internal object GlShapes {
    /** The whole picture, as two triangles in a strip: corners in GPU space. */
    val FULL_POSITIONS: FloatBuffer = floats(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)

    /** The matching picture corners, 0 to 1. */
    val FULL_TEX: FloatBuffer = floats(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)

    fun floats(vararg values: Float): FloatBuffer = buffer(values.size).apply {
        put(values)
        position(0)
    }

    fun buffer(count: Int): FloatBuffer = ByteBuffer.allocateDirect(count * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    /** Draws [count] corners from [positions] (with [size] numbers each) and [tex] as [mode]. */
    fun draw(
        program: GlProgram,
        positions: FloatBuffer,
        size: Int,
        tex: FloatBuffer?,
        mode: Int,
        count: Int,
        first: Int = 0,
    ) {
        val position = program.attrib("aPosition")
        positions.position(0)
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, size, GLES20.GL_FLOAT, false, 0, positions)
        val texAt = if (tex != null) program.attrib("aTexCoord") else -1
        if (texAt >= 0 && tex != null) {
            tex.position(0)
            GLES20.glEnableVertexAttribArray(texAt)
            GLES20.glVertexAttribPointer(texAt, 2, GLES20.GL_FLOAT, false, 0, tex)
        }
        GLES20.glDrawArrays(mode, first, count)
        GLES20.glDisableVertexAttribArray(position)
        if (texAt >= 0) GLES20.glDisableVertexAttribArray(texAt)
    }

    fun drawFull(program: GlProgram) {
        draw(program, FULL_POSITIONS, 2, FULL_TEX, GLES20.GL_TRIANGLE_STRIP, 4)
    }
}
