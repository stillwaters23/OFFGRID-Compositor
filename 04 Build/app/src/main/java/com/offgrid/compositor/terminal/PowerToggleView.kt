package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Switch
import com.offgrid.compositor.R

/** The two toggle styles (final build, milestone 1). */
internal enum class ToggleStyle(val key: String, val title: String, val help: String) {
    WHITE("white", "Pure White", "On is a solid white circle. Off is a dim white outline."),
    BLACK("black", "Pure Black", "On is a black circle with a bright white ring. Off is dim gray."),
    ;

    companion object {
        fun from(key: String?): ToggleStyle = entries.firstOrNull { it.key == key } ?: WHITE
    }
}

/**
 * The one toggle used everywhere in the panel: a circle with the power symbol inside,
 * drawn in code, so it is sharp at every size.
 *
 * Pure White: on is a solid white circle with a black power symbol. Off is a dim white
 * outline. Pure Black: on is a black circle with a bright white ring and symbol, like a
 * lit power button. Off is dim gray. The optional toggle glow, off at the start, adds a
 * soft halo in the panel's glow color to toggles that are on.
 *
 * Switching takes a short fade, and none when the phone asks for less motion.
 */
internal class PowerToggleView(context: Context) : View(context), Styled {
    var isOn = false
        private set

    var onToggled: ((Boolean) -> Unit)? = null

    /** Shows one style no matter what the look says, for the Toggle style choices. */
    var fixedStyle: ToggleStyle? = null
        set(value) {
            field = value
            invalidate()
        }

    private var style = ToggleStyle.WHITE
    private var glowOn = false
    private var glowColor = Hud.WHITE
    private var mix = 0f
    private var fade: ValueAnimator? = null
    private var tick = true
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val symbol = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcBox = RectF()

    init {
        isClickable = true
        isFocusable = true
        setOnClickListener { setOn(!isOn, fromUser = true) }
        stateDescription = context.getString(R.string.terminal_off)
    }

    fun setOn(on: Boolean, fromUser: Boolean = false) {
        if (on == isOn && fromUser) return
        isOn = on
        stateDescription = context.getString(if (on) R.string.terminal_on else R.string.terminal_off)
        val end = if (on) 1f else 0f
        fade?.cancel()
        if (fromUser && ValueAnimator.areAnimatorsEnabled()) {
            fade = ValueAnimator.ofFloat(mix, end).apply {
                duration = FADE_MS
                addUpdateListener {
                    mix = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            mix = end
            invalidate()
        }
        if (fromUser) {
            if (tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onToggled?.invoke(on)
        }
    }

    override fun applyLook(look: PanelLook) {
        tick = look.tick
        style = look.toggleStyle
        glowOn = look.toggleGlow && look.glowStrength > 0f
        glowColor = look.outerGlow
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = Hud.dpi(context, SIZE_DP)
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val current = fixedStyle ?: style
        val t = mix
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2f - Hud.dp(context, EDGE_ROOM_DP)
        if (radius <= 0f) return
        val ringWidth = Hud.dp(context, RING_DP)
        val onFill: Int
        val offFill: Int
        val onRing: Int
        val offRing: Int
        val onSymbol: Int
        val offSymbol: Int
        when (current) {
            ToggleStyle.WHITE -> {
                onFill = Hud.WHITE
                offFill = Color.TRANSPARENT
                onRing = Hud.WHITE
                offRing = Hud.withAlpha(Hud.WHITE, DIM)
                onSymbol = Hud.INK
                offSymbol = Hud.withAlpha(Hud.WHITE, DIM)
            }
            ToggleStyle.BLACK -> {
                onFill = Hud.INK
                offFill = Hud.INK
                onRing = Hud.WHITE
                offRing = DIM_GRAY
                onSymbol = Hud.WHITE
                offSymbol = DIM_GRAY
            }
        }
        // The optional halo, in the glow color, only on toggles that are on.
        if (glowOn && t > 0f) {
            halo.color = Hud.withAlpha(glowColor, 0.9f * t)
            halo.strokeWidth = ringWidth * 2f
            halo.setShadowLayer(Hud.dp(context, HALO_DP) * t, 0f, 0f, Hud.withAlpha(glowColor, t))
            canvas.drawCircle(cx, cy, radius, halo)
        }
        disc.color = lerp(offFill, onFill, t)
        canvas.drawCircle(cx, cy, radius, disc)
        // A faint dark edge keeps Pure White visible on a white background too.
        if (current == ToggleStyle.WHITE) {
            edge.color = Hud.withAlpha(Hud.INK, EDGE_ALPHA)
            edge.strokeWidth = Hud.dp(context, 1f)
            canvas.drawCircle(cx, cy, radius + edge.strokeWidth / 2f, edge)
        }
        ring.color = lerp(offRing, onRing, t)
        ring.strokeWidth = ringWidth
        canvas.drawCircle(cx, cy, radius - ringWidth / 2f, ring)
        // The power symbol: an open circle with a gap at the top, and a line down into the gap.
        symbol.color = lerp(offSymbol, onSymbol, t)
        symbol.strokeWidth = radius * SYMBOL_WIDTH
        val arcRadius = radius * ARC_SHARE
        arcBox.set(cx - arcRadius, cy - arcRadius + radius * 0.06f, cx + arcRadius, cy + arcRadius + radius * 0.06f)
        canvas.drawArc(arcBox, ARC_START, ARC_SWEEP, false, symbol)
        canvas.drawLine(cx, cy - radius * LINE_TOP, cx, cy - radius * LINE_BOTTOM, symbol)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Switch::class.java.name
        info.isCheckable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            info.setChecked(
                if (isOn) AccessibilityNodeInfo.CHECKED_STATE_TRUE else AccessibilityNodeInfo.CHECKED_STATE_FALSE,
            )
        } else {
            @Suppress("DEPRECATION")
            info.isChecked = isOn
        }
    }

    override fun onDetachedFromWindow() {
        fade?.cancel()
        fade = null
        super.onDetachedFromWindow()
    }

    private companion object {
        /** A full 52 dp square, above the 48 dp smallest comfortable touch target. */
        const val SIZE_DP = 52f

        /** Room around the circle for the optional halo. */
        const val EDGE_ROOM_DP = 6f
        const val RING_DP = 2f
        const val HALO_DP = 7f
        const val DIM = 0.42f
        const val EDGE_ALPHA = 0.35f
        const val DIM_GRAY = 0xFF5C5C5C.toInt()
        const val SYMBOL_WIDTH = 0.13f
        const val ARC_SHARE = 0.5f
        const val ARC_START = 305f
        const val ARC_SWEEP = 290f
        const val LINE_TOP = 0.62f
        const val LINE_BOTTOM = 0.1f
        const val FADE_MS = 160L

        /** Mixes two colors, alpha included. */
        fun lerp(from: Int, to: Int, t: Float): Int {
            val share = t.coerceIn(0f, 1f)
            fun mix(a: Int, b: Int) = (a + (b - a) * share + 0.5f).toInt()
            return Color.argb(
                mix(Color.alpha(from), Color.alpha(to)),
                mix(Color.red(from), Color.red(to)),
                mix(Color.green(from), Color.green(to)),
                mix(Color.blue(from), Color.blue(to)),
            )
        }
    }
}
