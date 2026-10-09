package com.offgrid.compositor.ui.wheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.floor
import kotlin.math.pow

/** A native, five-row numeric drum with a continuous laser guide through its selected row. */
internal class DrumReelView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val laserGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var velocityTracker: VelocityTracker? = null
    private var position = 50f
    private var lastNotch = 50
    private var lastY = 0f
    private var downY = 0f
    private var dragging = false
    private var velocity = 0f
    private var lastFrame = 0L
    private var animationPosted = false
    private var dimmer = 72
    private var hapticsOn = true
    private var laserColor = 0xFF2BF5E6.toInt()
    private var numberColor = 0xFFFFFFFF.toInt()

    var isInteracting: Boolean = false
        private set

    var onValueChanged: ((Int) -> Unit)? = null
    var onValueSettled: ((Int) -> Unit)? = null
    var onNotch: ((Int) -> Unit)? = null

    val value: Int get() = WheelSnapMath.snap(position)

    init {
        isFocusable = true
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Value 50. Scroll from 0 to 100."
    }

    fun setValue(value: Int) {
        val safe = WheelSnapMath.clamp(value)
        position = safe.toFloat()
        lastNotch = safe
        contentDescription = "Value $safe. Scroll from 0 to 100."
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

    fun setDimmer(value: Int) {
        dimmer = WheelSnapMath.clamp(value)
        invalidate()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        hapticsOn = enabled
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (240f * density).roundToInt()
        val desiredHeight = (112f * density).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (height <= 0 || width <= 0) return
        val pitch = height / VISIBLE_ROWS.toFloat()
        val centerY = height / 2f
        val radius = pitch / sin(ROW_ANGLE)
        textPaint.textSize = min(20f * density, pitch * 0.72f)
        val base = floor(position).toInt()
        for (row in base - 2..base + 3) {
            if (row !in MIN_VALUE..MAX_VALUE) continue
            val offset = row - position
            if (abs(offset) > 2.5f) continue
            val angle = offset * ROW_ANGLE
            val depth = cos(angle).coerceAtLeast(0f)
            val y = centerY + sin(angle) * radius
            textPaint.color = if (row == value) numberColor else numberColor.withAlpha(depth * 0.56f)
            textPaint.textSize = min(20f * density, pitch * 0.72f) * (0.72f + 0.28f * depth)
            canvas.drawText(row.toString(), width / 2f, y - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        }
        val alpha = (dimmer / 100f * 0.95f).coerceIn(0f, 0.95f)
        val centerColor = laserColor.withAlpha(alpha)
        laserGlowPaint.color = laserColor.withAlpha(alpha * 0.28f)
        laserGlowPaint.strokeWidth = 8f * density
        canvas.drawLine(0f, centerY, width.toFloat(), centerY, laserGlowPaint)
        laserPaint.color = centerColor
        laserPaint.strokeWidth = 1.5f * density
        canvas.drawLine(0f, centerY, width.toFloat(), centerY, laserPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(frame)
                animationPosted = false
                velocity = 0f
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                downY = event.y
                lastY = event.y
                dragging = false
                isInteracting = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val delta = lastY - event.y
                if (!dragging && abs(event.y - downY) > touchSlop) dragging = true
                if (dragging) {
                    updatePosition(position + delta / rowPitch())
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                if (!dragging) {
                    performClick()
                    if (abs(position - value) > SNAP_EPSILON) startSnap() else isInteracting = false
                } else {
                    velocityTracker?.computeCurrentVelocity(1000)
                    velocity = -(velocityTracker?.yVelocity ?: 0f) / rowPitch() / 60f
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
                if (dragging || abs(position - value) > SNAP_EPSILON) startSnap() else isInteracting = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
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
            MIN_VALUE.toFloat(),
            MAX_VALUE.toFloat(),
            value.toFloat(),
        )
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        val step = when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> -1
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> 1
            else -> return super.performAccessibilityAction(action, arguments)
        }
        val next = WheelSnapMath.clamp(value + step)
        if (next != value) {
            isInteracting = true
            updatePosition(next.toFloat())
            isInteracting = false
            onValueSettled?.invoke(next)
        }
        return true
    }

    private fun updatePosition(next: Float) {
        position = next.coerceIn(MIN_VALUE.toFloat(), MAX_VALUE.toFloat())
        val snapped = WheelSnapMath.snap(position)
        if (snapped != lastNotch) {
            val direction = if (snapped > lastNotch) 1 else -1
            var notch = lastNotch + direction
            while (if (direction > 0) notch <= snapped else notch >= snapped) {
                onNotch?.invoke(notch)
                if (hapticsOn && isHapticFeedbackEnabled) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                onValueChanged?.invoke(notch)
                notch += direction
            }
            lastNotch = snapped
            contentDescription = "Value $snapped. Scroll from 0 to 100."
        }
        invalidate()
    }

    private fun startSnap() {
        velocity = 0f
        lastFrame = SystemClock.uptimeMillis()
        postFrame()
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
        val frames = ((now - lastFrame).coerceAtLeast(1L).toFloat() / FRAME_MS).coerceIn(0.25f, 3f)
        lastFrame = now
        if (abs(velocity) > FLING_STOP) {
            val next = position + velocity * frames
            val clamped = next.coerceIn(MIN_VALUE.toFloat(), MAX_VALUE.toFloat())
            updatePosition(clamped)
            velocity *= FRICTION_PER_FRAME.pow(frames.toDouble()).toFloat()
            if (clamped != next) velocity = 0f
        } else {
            val target = WheelSnapMath.snap(position).toFloat()
            val amount = min(1f, frames * SNAP_RATE)
            updatePosition(position + (target - position) * amount)
            if (abs(target - position) < SNAP_EPSILON) {
                position = target
                invalidate()
                isInteracting = false
                onValueSettled?.invoke(target.roundToInt())
                return@Runnable
            }
        }
        postFrame()
    }

    private fun rowPitch(): Float = max(height / VISIBLE_ROWS.toFloat(), 1f)

    private fun Int.withAlpha(alpha: Float): Int =
        (this and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).roundToInt() shl 24)

    private companion object {
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100
        const val VISIBLE_ROWS = 5
        val ROW_ANGLE = Math.PI.toFloat() * 26f / 180f
        const val FRICTION_PER_FRAME = 0.94
        const val FLING_STOP = 0.018f
        const val SNAP_RATE = 0.34f
        const val SNAP_EPSILON = 0.002f
        const val FRAME_MS = 16L
    }
}
