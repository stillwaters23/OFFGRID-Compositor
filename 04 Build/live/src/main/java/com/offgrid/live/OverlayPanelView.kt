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
        fun bubbleDrag(showTarget: Boolean)
        fun closeTargetContains(x: Float, y: Float): Boolean
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
    var cameraEnabled = true
        set(value) {
            field = value
            cameraLabel = if (value) "CAM ON" else "CAM OFF"
        }
    var microphoneEnabled = false
        set(value) {
            field = value
            microphoneLabel = if (value) "MIC ON" else "MIC OFF"
        }
        var fullScreenEnabled = false
            set(value) {
                field = value
                fullScreenLabel = if (value) "FULL ON" else "FULL OFF"
            }
        var widthLabel = "W [40]"
            set(value) {
                field = value
            }
        var heightLabel = "H [35]"
        private var cameraLabel = "CAM ON"
        private var microphoneLabel = "MIC OFF"
        private var fullScreenLabel = "FULL OFF"

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
        paint.style = Paint.Style.STROKE
        paint.color = 0x559b5cff
        paint.strokeWidth = 5f
        canvas.drawRoundRect(bounds, 12f, 12f, paint)
        paint.color = UV
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(bounds, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        text(canvas, "OFFGRID / LIVE", 12f, 21f, 12f, WHITE, true)
        text(canvas, "−", width - 28f, 21f, 18f, UV)
        drawToggle(canvas, 12f, 35f, cameraLabel, cameraEnabled)
        drawToggle(canvas, 96f, 35f, microphoneLabel, microphoneEnabled)
        EFFECT_LABELS.forEachIndexed { index, label ->
            val row = index / 2
            val column = index % 2
            val x = 12f + column * max(105f, (width - 24f) / 2)
            val y = 84f + row * 24f
            text(canvas, "[$label]", x, y, 9f, WHITE)
        }
        text(canvas, "MODE  OPEN APP  CLOSE", 12f, height - 53f, 9f, UV, true)
        val columnWidth = (width - 24f) / 3
        drawSizeBox(canvas, 12f, height - 35f, columnWidth - 4f, widthLabel)
        drawSizeBox(canvas, 12f + columnWidth, height - 35f, columnWidth - 4f, heightLabel)
        drawSizeBox(canvas, 12f + columnWidth * 2, height - 35f, columnWidth - 4f, fullScreenLabel)
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
                    listener.bubbleDrag(true)
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
                    listener.bubbleDrag(false)
                    if (!dragging) listener.action("expand")
                    else if (listener.closeTargetContains(event.rawX, event.rawY)) {
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
                    y > h - 38f && x < 12f + (w - 24f) / 3 -> listener.openSizeEntry("W")
                    y > h - 38f && x < 12f + (w - 24f) * 2 / 3 -> listener.openSizeEntry("H")
                    y > h - 38f && x < w - 12f -> listener.action("full-screen")
                    y > h - 38f -> listener.action("close")
                    y > h - 66f && x < w / 3f -> listener.action("mode")
                    y > h - 66f && x < w * 2f / 3f -> listener.action("open")
                    y > h - 66f -> listener.action("close")
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

    private fun drawToggle(canvas: Canvas, x: Float, y: Float, label: String, enabled: Boolean) {
        val color = if (enabled) WHITE else 0xff777777.toInt()
        bounds.set(x, y, x + 76f, y + 25f)
        paint.color = BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(bounds, paint)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRect(bounds, paint)
        paint.style = Paint.Style.FILL
        text(canvas, label, x + 5f, y + 17f, 9f, color, true)
    }

    private fun drawSizeBox(canvas: Canvas, x: Float, y: Float, width: Float, label: String) {
        bounds.set(x, y, x + width, y + 22f)
        paint.color = 0xff101010.toInt()
        canvas.drawRect(bounds, paint)
        paint.color = UV
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(bounds, paint)
        paint.style = Paint.Style.FILL
        text(canvas, label, x + 4f, y + 15f, 8f, WHITE)
    }

    private companion object {
        const val BLACK = 0xff000000.toInt()
        const val WHITE = 0xfff4f4f4.toInt()
        const val UV = 0xff9b5cff.toInt()
        val EFFECTS = listOf("shatter", "freeze", "quiet", "fireworks", "red_sweep", "laser_writing")
        val EFFECT_LABELS = listOf("[SHATTER]", "[FREEZE]", "[QUIET]", "[FIREWORKS]", "[RED SWEEP]", "[LASER]")
        val BOLD_MONO = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
    }
}
