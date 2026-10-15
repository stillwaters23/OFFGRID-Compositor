package com.offgrid.compositor

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.ShatterMesh
import com.offgrid.compositor.effects.StagePlan
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * The camera stage (final build, M4). It changes the camera picture itself, on the GPU,
 * before it goes to the screen and into the video: mirror, time freeze, quiet mode, echo
 * trail, takeover, the no-face camera modes, the drawn effects on top, depth shift, and
 * shatter. It follows a [StagePlan] and keeps the pictures those effects need, like the
 * held frame and the echo copies.
 *
 * It runs on the drawing thread only, with the GPU ready.
 */
internal class StagePainter {
    private val camera = GlProgram(StageShaders.CAMERA_VERTEX, StageShaders.CAMERA_FRAGMENT)
    private val overlay = GlProgram(StageShaders.CAMERA_VERTEX, StageShaders.OVERLAY_FRAGMENT)
    private val picture = GlProgram(StageShaders.PICTURE_VERTEX, StageShaders.PICTURE_FRAGMENT)
    private val tilt = GlProgram(StageShaders.TILT_VERTEX, StageShaders.PICTURE_FRAGMENT)
    private val shard = GlProgram(StageShaders.SHARD_VERTEX, StageShaders.SHARD_FRAGMENT)
    private val void = GlProgram(StageShaders.PICTURE_VERTEX, StageShaders.VOID_FRAGMENT)
    private val beach = GlProgram(StageShaders.PICTURE_VERTEX, StageShaders.BEACH_FRAGMENT)
    private val body = GlProgram(StageShaders.PICTURE_VERTEX, StageShaders.BODY_FRAGMENT)
    private val shape = GlProgram(StageShaders.SHAPE_VERTEX, StageShaders.SHAPE_FRAGMENT)

    /** The person mask, copied onto the GPU on this drawing thread. Never on the UI thread. */
    private var maskTexture = 0
    private var maskWidth = 0
    private var maskHeight = 0
    private var maskUpload: ByteBuffer? = null

    /** The camera picture this frame. Drawn fresh every frame. */
    private val live = GlTarget()

    /** Two work pictures the stage passes back and forth. */
    private val workA = GlTarget()
    private val workB = GlTarget()

    /** The still picture for time freeze and quiet mode. */
    private val held = GlTarget()
    private var heldId = 0L

    /** The still picture for ghost mode. */
    private val ghost = GlTarget()
    private var ghostFor = Float.NaN

    private val echoes = Array(EffectTimes.MAX_ECHO_COPIES) { GlTarget() }
    private val echoAt = FloatArray(EffectTimes.MAX_ECHO_COPIES)
    private var echoNext = 0
    private var echoCount = 0
    private var echoLastAt = -100f
    private var echoKept = 0

    private val shatter = ShatterMesh()
    private var shardPositions: FloatBuffer = GlShapes.buffer(0)
    private var shardTex: FloatBuffer = GlShapes.buffer(0)
    private var shardShade: FloatBuffer = GlShapes.buffer(0)
    private var crackLines: FloatBuffer = GlShapes.buffer(0)

    private val quad = GlShapes.buffer(QUAD_FLOATS)
    private val tiltQuad = GlShapes.buffer(TILT_FLOATS)
    private val quadTex = GlShapes.floats(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    private val outline = GlShapes.buffer(OUTLINE_FLOATS)
    private val insetTex = FloatArray(4)

    /** The stage picture's size in pixels, the same for the screen and the video. */
    var width = 0
        private set
    var height = 0
        private set

    /**
     * Paints one picture into a stage picture of [w] by [h] and returns it.
     * [cameraTexture] is the camera with [cameraMatrix]. [overlayTexture] is the drawn effects
     * (0 when none) with [overlayMatrix]. [mask] is the person cutout, or null when that look is off.
     */
    fun paint(
        cameraTexture: Int,
        cameraMatrix: FloatArray,
        w: Int,
        h: Int,
        plan: StagePlan,
        time: Float,
        overlayTexture: Int,
        overlayMatrix: FloatArray,
        mask: MaskFrame? = null,
    ): GlTarget {
        width = w
        height = h
        live.ensure(w, h)
        workA.ensure(w, h)
        workB.ensure(w, h)
        GLES20.glDisable(GLES20.GL_BLEND)

        // A body look with no fresh mask never samples the camera. The box stays black.
        if (plan.bodyOn && (mask == null || MaskGate.verdict(mask, System.nanoTime()) != MaskVerdict.APPLY)) {
            live.bind()
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            live.filled = true
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            return live
        }

        // 1. The camera, mirrored for mirror plus scan.
        live.bind()
        drawCamera(cameraTexture, cameraMatrix, if (plan.mirror) 0f else 1f)
        live.filled = true
        var source = live

        // 1b. The person mask: see-through, replace, body cloak, or hologram.
        if (plan.bodyOn && mask != null) {
            val into = other(source)
            drawBody(source, into, mask, plan, time)
            source = into
        }

        // 2. Time freeze and quiet mode hold one still picture.
        if (plan.holdId != 0L) {
            if (plan.holdId != heldId || !held.filled) {
                held.ensure(w, h)
                copy(live, held)
                heldId = plan.holdId
            }
            source = held
        } else {
            heldId = 0L
        }

        // 3. Echo trail: fading copies of the last moments.
        if (plan.echoOn) {
            keepEcho(source, plan, time)
            if (echoCount > 0) {
                val into = other(source)
                drawEcho(source, into, plan, time)
                source = into
            }
        } else {
            echoCount = 0
            echoKept = 0
        }

        // 4. Takeover: the big box and the small box.
        if (plan.takeoverOn) {
            val into = other(source)
            drawTakeover(source, into, plan)
            source = into
        }

        // 5. The camera mode: live, blackout, cloak, status, or ghost.
        source = drawMode(source, plan, time)

        // 6. Quiet mode dims, and time freeze glitches, the picture under the drawn effects.
        if (plan.brightness < 1f || plan.glitch > 0f) {
            val into = other(source)
            into.bind()
            drawPicture(source, brightness = plan.brightness, glitch = plan.glitch, seed = plan.glitchSeed)
            source = into
        }

        // 7. The drawn effects go on top, so they shatter and push out with the picture.
        if (overlayTexture != 0) {
            if (source !== workA && source !== workB && source !== live) {
                val into = other(source)
                copy(source, into)
                source = into
            }
            source.bind()
            drawOverlay(overlayTexture, overlayMatrix)
        }

        // 8. Depth shift, then shatter, move the whole finished picture.
        if (plan.depthFrame > 0f || plan.depth > 0f) {
            val into = other(source)
            drawDepth(source, into, plan)
            source = into
        }
        if (plan.shatterId != 0L) {
            val into = other(source)
            drawShatter(source, into, plan, time)
            source = into
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return source
    }

    /** Draws [target] filling the current output, cut to fit its shape without stretching. */
    fun present(target: GlTarget, outputWidth: Int, outputHeight: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glDisable(GLES20.GL_BLEND)
        val pictureAspect = target.width.toFloat() / target.height
        val outputAspect = outputWidth.toFloat() / outputHeight
        var sx = 1f
        var sy = 1f
        if (pictureAspect > outputAspect) sx = outputAspect / pictureAspect else sy = pictureAspect / outputAspect
        drawPicture(target, rectX = (1f - sx) / 2f, rectY = (1f - sy) / 2f, rectW = sx, rectH = sy)
    }

    /** Draws the camera straight to the current output, for when no effect changes it. */
    fun drawCameraOnly(cameraTexture: Int, cameraMatrix: FloatArray) {
        GLES20.glDisable(GLES20.GL_BLEND)
        drawCamera(cameraTexture, cameraMatrix, 0f)
    }

    /** Drops the still pictures and copies, so nothing old shows after the camera restarts. */
    fun forgetPictures() {
        heldId = 0L
        held.filled = false
        ghost.filled = false
        ghostFor = Float.NaN
        echoCount = 0
        echoKept = 0
    }

    fun release() {
        for (program in listOf(camera, overlay, picture, tilt, shard, void, beach, body, shape)) program.release()
        if (maskTexture != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(maskTexture), 0)
            maskTexture = 0
            maskWidth = 0
            maskHeight = 0
        }
        for (target in listOf(live, workA, workB, held, ghost)) target.release()
        for (target in echoes) target.release()
    }

    // ---- The steps ----

    private fun keepEcho(source: GlTarget, plan: StagePlan, time: Float) {
        val copies = plan.echoCopies
        if (copies != echoKept) {
            echoKept = copies
            echoCount = 0
            echoNext = 0
            echoLastAt = -100f
        }
        val every = plan.echoSeconds / copies
        if (time - echoLastAt < every && time >= echoLastAt) return
        val target = echoes[echoNext]
        target.ensure(maxOf(1, width / ECHO_SHRINK), maxOf(1, height / ECHO_SHRINK))
        copy(source, target)
        echoAt[echoNext] = time
        echoLastAt = time
        echoNext = (echoNext + 1) % copies
        echoCount = minOf(echoCount + 1, copies)
    }

    /**
     * Blends the echo copies and the picture as a weighted mix: the picture counts the most,
     * older copies count less. Things that stand still look the same, moving things trail.
     */
    private fun drawEcho(source: GlTarget, into: GlTarget, plan: StagePlan, time: Float) {
        into.bind()
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        var total = 0f
        // Oldest first. The ring's oldest copy is at echoNext once it is full.
        for (step in 0 until echoCount) {
            val slot = if (echoCount < plan.echoCopies) step else (echoNext + step) % plan.echoCopies
            val age = time - echoAt[slot]
            val weight = plan.echoStrength * ECHO_WEIGHT * (1f - age / plan.echoSeconds)
            if (weight <= 0f) continue
            total += weight
            drawPicture(echoes[slot], alpha = if (total <= weight) 1f else weight / total)
        }
        total += 1f
        drawPicture(source, alpha = if (total <= 1f) 1f else 1f / total)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawTakeover(source: GlTarget, into: GlTarget, plan: StagePlan) {
        into.bind()
        val window = plan.window
        val span = 1f / window.zoom
        // Picture shares count from the top. The stored picture counts from the bottom.
        val zoomX = window.x - span / 2f
        val zoomY = 1f - window.y - span / 2f
        if (plan.takeoverFlip) {
            drawPicture(source)
        } else {
            drawPicture(source, rectX = zoomX, rectY = zoomY, rectW = span, rectH = span)
        }
        if (!plan.insetOn) return
        val share = plan.insetShare
        val unit = minOf(width, height).toFloat()
        val marginX = INSET_MARGIN * unit / width
        val left = 1f - marginX - share
        val bottom = INSET_LIFT
        // A soft shadow, a thin bright edge, then the small picture.
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        val edgeX = INSET_EDGE * unit / width
        val edgeY = INSET_EDGE * unit / height
        val shadowX = INSET_SHADOW * unit / width
        val shadowY = INSET_SHADOW * unit / height
        fillRect(left - edgeX + shadowX, bottom - edgeY - shadowY, share + edgeX * 2, share + edgeY * 2, 0f, 0f, 0f, 0.45f)
        fillRect(left - edgeX, bottom - edgeY, share + edgeX * 2, share + edgeY * 2, 1f, 1f, 1f, 0.9f)
        GLES20.glDisable(GLES20.GL_BLEND)
        setQuad(left, bottom, share, share)
        val texture = if (plan.takeoverFlip) {
            insetTex[0] = zoomX
            insetTex[1] = zoomY
            insetTex[2] = span
            insetTex[3] = span
            insetTex
        } else {
            WHOLE
        }
        drawPictureAt(source, texture)
    }

    private fun drawMode(source: GlTarget, plan: StagePlan, time: Float): GlTarget {
        val mode = plan.mode
        val ghostReady = keepGhost(source, plan, time)
        if (mode == FxKeys.MODE_LIVE && plan.modeMix >= 1f) return source
        val into = other(source)
        into.bind()
        val previous = plan.previousMode
        val mix = plan.modeMix
        when {
            mix < 1f && previous == FxKeys.MODE_LIVE && mode == FxKeys.MODE_CLOAK ->
                drawVoid(source, mix, plan, time, 1f)
            mix < 1f && previous == FxKeys.MODE_CLOAK && mode == FxKeys.MODE_LIVE ->
                drawVoid(source, 1f - mix, plan, time, 1f)
            mix < 1f -> {
                drawModeLook(previous, source, plan, time, 1f, ghostReady)
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                drawModeLook(mode, source, plan, time, mix, ghostReady)
                GLES20.glDisable(GLES20.GL_BLEND)
            }
            else -> drawModeLook(mode, source, plan, time, 1f, ghostReady)
        }
        return into
    }

    /**
     * Ghost mode keeps the last picture viewers saw, made the moment ghost turned on.
     * If ghost was already on when the app opened, there is no such picture, so it stays blank:
     * a frozen picture is never made from the live face.
     */
    private fun keepGhost(source: GlTarget, plan: StagePlan, time: Float): Boolean {
        if (plan.mode != FxKeys.MODE_GHOST || !plan.ghostFrozen) return false
        if (plan.ghostSince == ghostFor) return ghost.filled
        ghostFor = plan.ghostSince
        ghost.filled = false
        if (time - plan.ghostSince > GHOST_CATCH_SECONDS) return false
        ghost.ensure(width, height)
        ghost.bind()
        drawModeLook(plan.previousMode, source, plan, time, 1f, false)
        ghost.filled = true
        return true
    }

    private fun drawModeLook(mode: String, source: GlTarget, plan: StagePlan, time: Float, alpha: Float, ghostReady: Boolean) {
        when (mode) {
            FxKeys.MODE_CLOAK -> drawVoid(source, 1f, plan, time, alpha)
            FxKeys.MODE_BLACKOUT, FxKeys.MODE_STATUS -> fillRect(0f, 0f, 1f, 1f, 0f, 0f, 0f, alpha)
            FxKeys.MODE_GHOST -> {
                if (ghostReady && ghost.filled) {
                    drawPicture(ghost, alpha = alpha)
                } else {
                    fillRect(0f, 0f, 1f, 1f, 0f, 0f, 0f, alpha)
                }
            }
            else -> drawPicture(source, alpha = alpha)
        }
    }

    private fun drawVoid(source: GlTarget, mix: Float, plan: StagePlan, time: Float, alpha: Float) {
        void.use()
        bindPicture(void, source)
        void.set4f("uTexRect", 0f, 0f, 1f, 1f)
        void.set1f("uTime", time)
        void.set1f("uMix", mix)
        void.set1f("uShimmer", plan.cloakShimmer)
        void.set1f("uStrength", plan.cloakStrength)
        void.set1f("uAspect", width.toFloat() / height)
        void.set1f("uAlpha", alpha)
        GlShapes.drawFull(void)
    }

    private fun drawDepth(source: GlTarget, into: GlTarget, plan: StagePlan) {
        into.bind()
        val settle = plan.depthFrame
        val push = plan.depth
        // The room behind, dimmed, so the box never goes empty.
        drawPicture(source, brightness = 1f - DEPTH_DIM * settle)
        val inset = DEPTH_FRAME * settle
        val frameW = 1f - inset * 2f
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // The frame the picture breaks out of.
        val edge = 2f / minOf(width, height)
        outlineRect(inset - edge, inset - edge, frameW + edge * 2, frameW + edge * 2, 1f, 1f, 1f, 0.85f * settle)
        // The card: the picture, pushed toward the viewer with a slight turn and a shadow.
        val scale = frameW * (1f + plan.depthDistance * DEPTH_GROW * push)
        val turn = DEPTH_TURN * push
        val shadow = DEPTH_SHADOW * push
        fillRect(
            0.5f - scale / 2f + shadow, 0.5f - scale / 2f - shadow, scale, scale,
            0f, 0f, 0f, 0.55f * push,
        )
        GLES20.glDisable(GLES20.GL_BLEND)
        drawTiltedCard(source, scale, turn)
    }

    private fun drawShatter(source: GlTarget, into: GlTarget, plan: StagePlan, time: Float) {
        into.bind()
        when (plan.shatterBehind) {
            FxKeys.BEHIND_VOID -> drawVoid(source, 1f, plan, time, 1f)
            FxKeys.BEHIND_BEACH -> {
                beach.use()
                beach.set4f("uTexRect", 0f, 0f, 1f, 1f)
                beach.set1f("uTime", time)
                GlShapes.drawFull(beach)
            }
            else -> {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            }
        }
        val times = plan.shatterTimes ?: return
        shatter.build(plan.shatterPieces, plan.impactX, plan.impactY, width.toFloat() / height, plan.shatterId)
        shatter.pose(plan.shatterTime, times)
        val count = shatter.count
        if (shardPositions.capacity() < count * 6) {
            shardPositions = GlShapes.buffer(count * 6)
            shardTex = GlShapes.buffer(count * 6)
            shardShade = GlShapes.buffer(count * 3)
            crackLines = GlShapes.buffer(count * 12)
        }
        shardPositions.clear()
        shardTex.clear()
        shardShade.clear()
        crackLines.clear()
        var drawn = 0
        var lines = 0
        val placed = shatter.placed
        val home = shatter.home
        for (i in 0 until count) {
            if (!shatter.shown[i]) continue
            for (k in 0..2) {
                shardPositions.put(placed[i * 6 + k * 2] * 2f - 1f)
                shardPositions.put(1f - placed[i * 6 + k * 2 + 1] * 2f)
                shardTex.put(home[i * 6 + k * 2])
                shardTex.put(1f - home[i * 6 + k * 2 + 1])
                shardShade.put(shatter.shade[i])
            }
            drawn++
            if (shatter.distanceOf(i) <= shatter.crackReach) {
                for (k in 0..2) {
                    val n = (k + 1) % 3
                    crackLines.put(placed[i * 6 + k * 2] * 2f - 1f)
                    crackLines.put(1f - placed[i * 6 + k * 2 + 1] * 2f)
                    crackLines.put(placed[i * 6 + n * 2] * 2f - 1f)
                    crackLines.put(1f - placed[i * 6 + n * 2 + 1] * 2f)
                }
                lines += 6
            }
        }
        if (drawn > 0) {
            shard.use()
            bindPicture(shard, source)
            val shadeAt = shard.attrib("aShade")
            shardShade.position(0)
            GLES20.glEnableVertexAttribArray(shadeAt)
            GLES20.glVertexAttribPointer(shadeAt, 1, GLES20.GL_FLOAT, false, 0, shardShade)
            GlShapes.draw(shard, shardPositions, 2, shardTex, GLES20.GL_TRIANGLES, drawn * 3)
            GLES20.glDisableVertexAttribArray(shadeAt)
        }
        if (lines > 0 && shatter.crackGlow > 0f) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glLineWidth(maxOf(1f, minOf(width, height) / CRACK_LINE_SHARE))
            shape.use()
            shape.set4f("uColor", 0.92f, 0.97f, 1f, CRACK_ALPHA * shatter.crackGlow)
            GlShapes.draw(shape, crackLines, 2, null, GLES20.GL_LINES, lines)
            GLES20.glLineWidth(1f)
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    // ---- Drawing helpers ----

    private fun other(source: GlTarget): GlTarget = if (source === workA) workB else workA

    private fun copy(from: GlTarget, to: GlTarget) {
        to.bind()
        GLES20.glDisable(GLES20.GL_BLEND)
        drawPicture(from)
        to.filled = true
    }

    private fun drawBody(source: GlTarget, into: GlTarget, frame: MaskFrame, plan: StagePlan, time: Float) {
        uploadMask(frame)
        into.bind()
        body.use()
        bindPicture(body, source)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture)
        body.set1i("sMask", 1)
        body.set1f("uKind", bodyKind(plan.bodyMode))
        body.set1f("uPlate", if (plan.bodyPlate == FxKeys.BODY_PLATE_VOID) 1f else 0f)
        body.set1f("uCut", plan.bodyCut)
        body.set1f("uTime", time)
        body.set1f("uAspect", width.toFloat() / height.coerceAtLeast(1))
        GlShapes.drawFull(body)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        into.filled = true
    }

    /** Puts the person mask on the GPU. Called only from the drawing thread. */
    private fun uploadMask(frame: MaskFrame) {
        if (maskTexture == 0) {
            val names = IntArray(1)
            GLES20.glGenTextures(1, names, 0)
            maskTexture = names[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTexture)
        }
        val needed = frame.bytes.size
        var buffer = maskUpload
        if (buffer == null || buffer.capacity() < needed) {
            buffer = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder())
            maskUpload = buffer
        }
        buffer.clear()
        buffer.put(frame.bytes, 0, needed)
        buffer.position(0)
        if (frame.width != maskWidth || frame.height != maskHeight) {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA,
                frame.width, frame.height, 0,
                GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, buffer,
            )
            maskWidth = frame.width
            maskHeight = frame.height
        } else {
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D, 0, 0, 0,
                frame.width, frame.height,
                GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, buffer,
            )
        }
    }

    private fun bodyKind(mode: String): Float = when (mode) {
        FxKeys.BODY_REPLACE -> 2f
        FxKeys.BODY_CLOAK -> 3f
        FxKeys.BODY_HOLO -> 4f
        else -> 1f
    }

    private fun drawCamera(texture: Int, matrix: FloatArray, flipX: Float) {
        camera.use()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        camera.set1i("sTexture", 0)
        camera.setMatrix("uTexMatrix", matrix)
        camera.set1f("uFlipX", flipX)
        camera.set1f("uAlpha", 1f)
        GlShapes.drawFull(camera)
    }

    private fun drawOverlay(texture: Int, matrix: FloatArray) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        overlay.use()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        overlay.set1i("sTexture", 0)
        overlay.setMatrix("uTexMatrix", matrix)
        overlay.set1f("uFlipX", 0f)
        overlay.set1f("uAlpha", 1f)
        GlShapes.drawFull(overlay)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun bindPicture(program: GlProgram, target: GlTarget) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, target.texture)
        program.set1i("sPicture", 0)
    }

    private fun drawPicture(
        target: GlTarget,
        alpha: Float = 1f,
        brightness: Float = 1f,
        glitch: Float = 0f,
        seed: Float = 0f,
        rectX: Float = 0f,
        rectY: Float = 0f,
        rectW: Float = 1f,
        rectH: Float = 1f,
    ) {
        picture.use()
        bindPicture(picture, target)
        picture.set4f("uTexRect", rectX, rectY, rectW, rectH)
        picture.set1f("uAlpha", alpha)
        picture.set1f("uBrightness", brightness)
        picture.set1f("uGlitch", glitch)
        picture.set1f("uSeed", seed)
        GlShapes.drawFull(picture)
    }

    /** Draws [target] (the part named by [texture]) into the quad last set by [setQuad]. */
    private fun drawPictureAt(target: GlTarget, texture: FloatArray) {
        picture.use()
        bindPicture(picture, target)
        picture.set4f("uTexRect", texture[0], texture[1], texture[2], texture[3])
        picture.set1f("uAlpha", 1f)
        picture.set1f("uBrightness", 1f)
        picture.set1f("uGlitch", 0f)
        picture.set1f("uSeed", 0f)
        GlShapes.draw(picture, quad, 2, quadTex, GLES20.GL_TRIANGLE_STRIP, 4)
    }

    /** A picture card [size] wide (as a share of the box) in the middle, turned by [turn]. */
    private fun drawTiltedCard(target: GlTarget, size: Float, turn: Float) {
        tiltQuad.clear()
        // Corners: bottom left, bottom right, top left, top right. The right side sits farther away.
        for (corner in 0..3) {
            val x = CARD_CORNERS[corner * 2]
            val y = CARD_CORNERS[corner * 2 + 1]
            tiltQuad.put(x * size).put(y * size).put(0f).put(1f + turn * x)
        }
        tiltQuad.position(0)
        tilt.use()
        bindPicture(tilt, target)
        tilt.set4f("uTexRect", 0f, 0f, 1f, 1f)
        tilt.set1f("uAlpha", 1f)
        tilt.set1f("uBrightness", 1f)
        tilt.set1f("uGlitch", 0f)
        tilt.set1f("uSeed", 0f)
        GlShapes.draw(tilt, tiltQuad, 4, quadTex, GLES20.GL_TRIANGLE_STRIP, 4)
    }

    /** Sets the shared quad to a box [w] by [h] from [x], [y], in shares counted from the bottom left. */
    private fun setQuad(x: Float, y: Float, w: Float, h: Float) {
        val left = x * 2f - 1f
        val right = (x + w) * 2f - 1f
        val bottom = y * 2f - 1f
        val top = (y + h) * 2f - 1f
        quad.clear()
        quad.put(left).put(bottom).put(right).put(bottom).put(left).put(top).put(right).put(top)
        quad.position(0)
    }

    private fun fillRect(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float) {
        setQuad(x, y, w, h)
        shape.use()
        shape.set4f("uColor", r, g, b, a)
        GlShapes.draw(shape, quad, 2, null, GLES20.GL_TRIANGLE_STRIP, 4)
    }

    private fun outlineRect(x: Float, y: Float, w: Float, h: Float, r: Float, g: Float, b: Float, a: Float) {
        val left = x * 2f - 1f
        val right = (x + w) * 2f - 1f
        val bottom = y * 2f - 1f
        val top = (y + h) * 2f - 1f
        outline.clear()
        outline.put(left).put(bottom).put(right).put(bottom).put(right).put(top).put(left).put(top)
        outline.position(0)
        GLES20.glLineWidth(maxOf(1f, minOf(width, height) / FRAME_LINE_SHARE))
        shape.use()
        shape.set4f("uColor", r, g, b, a)
        GlShapes.draw(shape, outline, 2, null, GLES20.GL_LINE_LOOP, 4)
        GLES20.glLineWidth(1f)
    }

    private companion object {
        const val QUAD_FLOATS = 8
        const val TILT_FLOATS = 16
        const val OUTLINE_FLOATS = 8

        /** Echo copies are kept at half size each way. They are soft see-through copies anyway. */
        const val ECHO_SHRINK = 2

        /** How much each echo copy counts next to the picture itself (which counts 1). */
        const val ECHO_WEIGHT = 0.8f

        /** Takeover's small box: its gap from the edges, its lift off the bottom, its edge and shadow, as shares. */
        const val INSET_MARGIN = 0.04f
        const val INSET_LIFT = 0.07f
        const val INSET_EDGE = 0.006f
        const val INSET_SHADOW = 0.012f

        /** Ghost keeps a picture only if it turned on this recently, so the picture is the one before. */
        const val GHOST_CATCH_SECONDS = 1f

        /** Depth shift: how much the room dims, the frame's gap, how far the card grows, turns, and casts a shadow. */
        const val DEPTH_DIM = 0.72f
        const val DEPTH_FRAME = 0.08f
        const val DEPTH_GROW = 1.4f
        const val DEPTH_TURN = 0.22f
        const val DEPTH_SHADOW = 0.025f

        /** Line thickness: one pixel for every this many pixels of the short side. */
        const val CRACK_LINE_SHARE = 420f
        const val FRAME_LINE_SHARE = 300f
        const val CRACK_ALPHA = 0.8f

        val WHOLE = floatArrayOf(0f, 0f, 1f, 1f)

        /** The card's corners from its middle, for a triangle strip. */
        val CARD_CORNERS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    }
}
