package com.offgrid.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

internal class OverlayPanelView(
    context: Context,
    private val listener: Listener,
    var bubble: Boolean = false,
) : View(context) {
    interface Listener {
        fun moveBy(dx: Int, dy: Int)
        fun resizeBy(dx: Int, dy: Int)
        fun action(name: String)
        fun openSizeEntry(axis: String)
    }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var resizing = false
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(density, density)
        val width = width / density
        val height = height / density
        if (bubble) {
            paint.color = BLACK
            canvas.drawCircle(width / 2, height / 2, 27f, paint)
            paint.color = UV
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(width / 2, height / 2, 25f, paint)
            paint.style = Paint.Style.FILL
            text(canvas, "O", width / 2, height / 2 + 5f, 16f, WHITE, true)
            canvas.restore()
            return
        }

        bounds.set(1f, 1f, max(2f, width - 1f), max(2f, height - 1f))
        paint.color = BLACK
        canvas.drawRoundRect(bounds, 12f, 12f, paint)
        paint.color = UV
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(bounds, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        text(canvas, "OFFGRID / LIVE", 12f, 21f, 12f, WHITE, true)
        text(canvas, "−", width - 28f, 21f, 18f, UV)
        text(canvas, "CAM  MIC", 12f, 45f, 11f, WHITE, true)
        text(canvas, "CAMERA   MIC", 12f, 62f, 10f, UV)
        val effects = listOf("SHATTER", "FREEZE", "QUIET", "FIREWORKS", "RED SWEEP", "LASER")
        effects.forEachIndexed { index, label ->
            val row = index / 2
            val column = index % 2
            val x = 12f + column * max(105f, (width - 24f) / 2)
            val y = 84f + row * 24f
            text(canvas, "[$label]", x, y, 9f, WHITE)
        }
        text(canvas, "MODE  OPEN APP  CLOSE", 12f, height - 41f, 9f, UV, true)
        text(canvas, "W [${LiveService.currentWidth}]   H [${LiveService.currentHeight}]   FULL SCREEN", 12f, height - 21f, 8f, WHITE)
        text(canvas, "DRAG EDGE TO RESIZE", 12f, height - 7f, 7f, UV)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bubble) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = downX
                    lastY = downY
                    dragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    if (kotlin.math.abs(event.rawX - downX) > 8 || kotlin.math.abs(event.rawY - downY) > 8) {
                        dragging = true
                    }
                    if (dragging) listener.moveBy(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) listener.action("expand")
                    else if (event.rawY > resources.displayMetrics.heightPixels - 96 * density) {
                        listener.action("close")
                    } else listener.action("snap")
                    return true
                }
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
                resizing = event.y / density > height / density - 24f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                if (resizing) listener.resizeBy(dx, dy)
                else if (event.y / density < 32f) listener.moveBy(dx, dy)
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                val x = event.x / density
                val y = event.y / density
                val w = width / density
                val h = height / density
                when {
                    y < 32f && x > w - 48f -> listener.action("bubble")
                    y in 36f..70f && x < w * 0.5f -> listener.action("toggle-camera")
                    y in 36f..70f -> listener.action("toggle-mic")
                    y in 70f..158f -> {
                        val row = ((y - 72f) / 24f).toInt()
                        val column = if (x > w / 2) 1 else 0
                        val index = row * 2 + column
                        EFFECTS.getOrNull(index)?.let(listener::action)
                    }
                    y > h - 48f && x < w * 0.30f -> listener.openSizeEntry("W")
                    y > h - 48f && x < w * 0.58f -> listener.openSizeEntry("H")
                    y > h - 48f && x < w * 0.75f -> listener.action("open")
                    y > h - 48f && x < w * 0.91f -> listener.action("full-screen")
                    y > h - 48f -> listener.action("close")
                    else -> listener.action("mode")
                }
                return true
            }
        }
        return true
    }

    private fun text(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) BOLD_MONO else android.graphics.Typeface.MONOSPACE
        canvas.drawText(value, x, y, paint)
    }

    private companion object {
        const val BLACK = 0xff000000.toInt()
        const val WHITE = 0xfff4f4f4.toInt()
        const val UV = 0xff9b5cff.toInt()
        val EFFECTS = listOf("shatter", "freeze", "quiet", "fireworks", "red_sweep", "laser_writing")
        val BOLD_MONO = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
    }
}
