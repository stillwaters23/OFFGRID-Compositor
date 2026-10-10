package com.offgrid.compositor.terminal

import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** The glass edges inside this view. They follow the size at once, before the next frame is drawn. */
internal fun TerminalPanel.glassArea(out: RectF) {
    out.set(margin.toFloat(), margin.toFloat(), (width - margin).toFloat(), (height - margin).toFloat())
}

internal fun TerminalPanel.bothOnGlass(event: MotionEvent): Boolean {
    glassArea(glassBox)
    return glassBox.contains(event.getX(0), event.getY(0)) && glassBox.contains(event.getX(1), event.getY(1))
}

/** Sideways pinch changes width, up and down changes height, and a slant changes both. */
internal fun TerminalPanel.handlePinch(event: MotionEvent) {
    val started = pinch
    when (event.actionMasked) {
        MotionEvent.ACTION_MOVE -> {
            if (started == null || event.pointerCount < 2) return
            val spanX = kotlin.math.abs(event.getX(1) - event.getX(0))
            val spanY = kotlin.math.abs(event.getY(1) - event.getY(0))
            val widths = widthRange(areaWidth())
            val heights = heightRange(areaHeight())
            val newWidth = (started.width + spanX - started.spanX).coerceIn(widths.first.toFloat(), widths.second.toFloat())
            val newHeight = (started.height + spanY - started.spanY).coerceIn(heights.first.toFloat(), heights.second.toFloat())
            liveSize = newWidth to newHeight
            showSizeNumbers(percentFor(newWidth, widths), percentFor(newHeight, heights))
            requestLayout()
        }
        MotionEvent.ACTION_POINTER_UP -> if (started != null) finishPinch()
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            if (started != null) finishPinch()
            pinchDone = false
        }
    }
}

internal fun TerminalPanel.finishPinch() {
    pinch = null
    pinchDone = true
    finishLiveSize()
}

internal fun TerminalPanel.isInside(view: View, event: MotionEvent): Boolean {
    val spot = IntArray(2)
    view.getLocationOnScreen(spot)
    val x = event.rawX
    val y = event.rawY
    return x >= spot[0] && x <= spot[0] + view.width && y >= spot[1] && y <= spot[1] + view.height
}
