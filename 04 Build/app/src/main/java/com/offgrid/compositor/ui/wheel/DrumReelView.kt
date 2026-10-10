package com.offgrid.compositor.ui.wheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.animation.ValueAnimator
import com.offgrid.compositor.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Native 0–100 drum. Five rows sit on a cylinder, the finger flings at 60fps,
 * and the wheel settles on a whole number. The laser is optional so a bank can
 * draw one strip across several reels.
 */
internal class DrumReelView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var velocityTracker: VelocityTracker? = null
    private var position = 50f
    private var lastNotch = 50
    private var lastY = 0f
    private var downY = 0f
    private var lastMoveAt = 0L
    private var dragging = false
    private var velocity = 0.0
    private var lastFrame = 0L
    private var animationPosted = false
    private var dimmer = 72
    private var hapticsOn = true
    private var fontScale = 1f
    private var laserColor = LaserStrip.DEFAULT_COLOR
    private var numberColor = 0xFFFFFFFF.toInt()
    private var selectedNumberColor = 0xFFFFFFFF.toInt()
    private var reelLabel = "Value"
    private var fit = ReelGeometry.fit(1, 100_000, 1f, density)

    var drawsOwnLaser: Boolean = true
    var isInteracting: Boolean = false
        private set

    var onValueChanged: ((Int) -> Unit)? = null
    var onValueSettled: ((Int) -> Unit)? = null
    var onNotch: ((Int) -> Unit)? = null

    val value: Int get() = WheelPhysics.snap(position.toDouble())

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = describe(50)
    }

    fun setValue(value: Int) {
        removeCallbacks(frame)
        animationPosted = false
        velocity = 0.0
        isInteracting = false
        val safe = WheelSnapMath.clamp(value)
        position = safe.toFloat()
        lastNotch = safe
        contentDescription = describe(safe)
        invalidate()
    }

    fun setLaserColor(color: Int) {
        laserColor = color or 0xFF000000.toInt()
        invalidate()
    }

    fun setNumberColor(color: Int) {
        numberColor = color or 0xFF000000.toInt()
        invalidate()
    }

    fun setSelectedNumberColor(color: Int) {
        selectedNumberColor = color or 0xFF000000.toInt()
        invalidate()
    }

    fun setDimmer(value: Int) {
        dimmer = WheelSnapMath.clamp(value)
        invalidate()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsOn = enabled
    }

    fun setFontScale(scale: Float) {
        if (fontScale == scale) return
        fontScale = scale
        requestLayout()
    }

    fun setReelLabel(label: String) {
        reelLabel = label
        contentDescription = describe(value)
    }

    fun currentLaserColor(): Int = laserColor

    fun currentDimmer(): Int = dimmer

    fun glyphFits(): Boolean = fit.glyphFits

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize((160f * density).roundToInt(), widthMeasureSpec)
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val offered = if (mode == MeasureSpec.UNSPECIFIED) 100_000 else MeasureSpec.getSize(heightMeasureSpec)
        var next = ReelGeometry.fit(width, offered, fontScale, density)
        val height = resolveSize(next.heightPx, heightMeasureSpec)
        if (height in 1 until next.heightPx) {
            next = ReelGeometry.fit(width, height, fontScale, density)
        }
        fit = next
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // S-1: a new width or height must not drop the number the wheel is on.
        if (!isInteracting) {
            val kept = ReelGeometry.valueAfterResize(lastNotch)
            position = kept.toFloat()
            lastNotch = kept
            contentDescription = describe(kept)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (height <= 0 || width <= 0) return
        val centerY = height / 2f
        if (drawsOwnLaser) drawLaser(canvas, 0f, width.toFloat(), centerY)
        val base = floor(position).toInt()
        for (row in base - 2..base + 3) {
            if (row !in WheelPhysics.MIN_VALUE..WheelPhysics.MAX_VALUE) continue
            val offset = row - position
            if (abs(offset) > 2.5f) continue
            val depth = ReelGeometry.depth(offset)
            val y = ReelGeometry.projectedY(centerY, offset, fit.radiusPx)
            val selected = row == value
            textPaint.color = if (selected) selectedNumberColor else numberColor.withAlpha(depth * 0.56f)
            textPaint.textSize = fit.fontPx * (0.72f + 0.28f * depth)
            canvas.drawText(
                row.toString(),
                width / 2f,
                y - (textPaint.ascent() + textPaint.descent()) / 2f,
                textPaint,
            )
        }
    }

    fun drawLaser(canvas: Canvas, left: Float, right: Float, y: Float) {
        if (right <= left) return
        for (stroke in LaserStrip.strokes(laserColor, dimmer, density)) {
            beamPaint.color = stroke.color
            beamPaint.strokeWidth = stroke.widthPx
            canvas.drawLine(left, y, right, y, beamPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                stopFrame()
                velocity = 0.0
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                downY = event.y
                lastY = event.y
                lastMoveAt = SystemClock.uptimeMillis()
                dragging = false
                isInteracting = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val now = SystemClock.uptimeMillis()
                if (!dragging && abs(event.y - downY) > touchSlop) dragging = true
                if (dragging) {
                    val dy = event.y - lastY
                    val dt = (now - lastMoveAt).coerceAtLeast(8L)
                    val (delta, nextVelocity) = WheelPhysics.dragDelta(dy.toDouble(), dt.toDouble(), 28.0 * density)
                    velocity = nextVelocity
                    updatePosition((position + delta).toFloat())
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                lastY = event.y
                lastMoveAt = now
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                if (!dragging) {
                    performClick()
                    if (abs(position - value) > WheelPhysics.SNAP_EPSILON) startSnap() else isInteracting = false
                } else {
                    velocityTracker?.computeCurrentVelocity(1000)
                    velocity = WheelPhysics.flingNotchesPerFrame(
                        velocityTracker?.yVelocity?.toDouble() ?: 0.0,
                        28.0 * density,
                    )
                    lastFrame = SystemClock.uptimeMillis()
                    postFrame()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                if (dragging || abs(position - value) > WheelPhysics.SNAP_EPSILON) startSnap() else isInteracting = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val key = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> WheelPhysics.Key.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> WheelPhysics.Key.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> WheelPhysics.Key.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> WheelPhysics.Key.RIGHT
            KeyEvent.KEYCODE_PAGE_UP -> WheelPhysics.Key.PAGE_UP
            KeyEvent.KEYCODE_PAGE_DOWN -> WheelPhysics.Key.PAGE_DOWN
            KeyEvent.KEYCODE_MOVE_HOME -> WheelPhysics.Key.HOME
            KeyEvent.KEYCODE_MOVE_END -> WheelPhysics.Key.END
            else -> return super.onKeyDown(keyCode, event)
        }
        jumpTo(WheelPhysics.keyTarget(value, key))
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.NumberPicker"
        info.text = value.toString()
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo(
            AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,
            WheelPhysics.MIN_VALUE.toFloat(),
            WheelPhysics.MAX_VALUE.toFloat(),
            value.toFloat(),
        )
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        val next = when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> value - 1
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> value + 1
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id ->
                arguments?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)?.roundToInt()
                    ?: return false
            else -> return super.performAccessibilityAction(action, arguments)
        }
        jumpTo(next)
        return true
    }

    private fun jumpTo(next: Int) {
        stopFrame()
        velocity = 0.0
        isInteracting = true
        updatePosition(WheelSnapMath.clamp(next).toFloat())
        position = value.toFloat()
        isInteracting = false
        onValueSettled?.invoke(value)
        invalidate()
    }

    private fun updatePosition(next: Float) {
        position = next.coerceIn(WheelPhysics.MIN_VALUE.toFloat(), WheelPhysics.MAX_VALUE.toFloat())
        val snapped = WheelPhysics.snap(position.toDouble())
        if (snapped != lastNotch) {
            lastNotch = snapped
            onNotch?.invoke(snapped)
            if (hapticsOn && isHapticFeedbackEnabled) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onValueChanged?.invoke(snapped)
            contentDescription = describe(snapped)
        }
        invalidate()
    }

    private fun startSnap() {
        velocity = 0.0
        lastFrame = SystemClock.uptimeMillis()
        postFrame()
    }

    private fun stopFrame() {
        removeCallbacks(frame)
        animationPosted = false
    }

    private fun postFrame() {
        if (!animationPosted) {
            animationPosted = true
            postOnAnimation(frame)
        }
    }

    private val frame = Runnable {
        animationPosted = false
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastFrame
        lastFrame = now
        val reduced = !ValueAnimator.areAnimatorsEnabled()
        var sample = WheelPhysics.Sample(position.toDouble(), velocity, false)
        val steps = if (reduced) 1 else WheelPhysics.frameCount(elapsed)
        repeat(steps) {
            if (!sample.settled) sample = WheelPhysics.step(sample.position, sample.velocity, reduced)
        }
        velocity = sample.velocity
        updatePosition(sample.position.toFloat())
        if (sample.settled) {
            position = WheelPhysics.snap(sample.position).toFloat()
            invalidate()
            isInteracting = false
            onValueSettled?.invoke(value)
            return@Runnable
        }
        postFrame()
    }

    private fun describe(number: Int): String =
        context.getString(R.string.terminal_reel_value, reelLabel, number)

    private fun Int.withAlpha(alpha: Float): Int =
        (this and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).roundToInt() shl 24)
}
