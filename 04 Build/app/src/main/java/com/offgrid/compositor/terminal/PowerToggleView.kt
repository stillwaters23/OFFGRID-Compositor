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
    private var extraHalo = false
    private var accent = NeonPalette.CYAN_GLOW
    private var glowColor = NeonPalette.ELECTRIC_PURPLE
    private var mix = 0f
    private var fade: ValueAnimator? = null
    private var tick = true
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stamp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
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
        glowOn = look.glowStrength > 0f
        extraHalo = look.toggleGlow
        accent = look.edge
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
        val size = minOf(width, height)
        if (size <= 0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = size * BioluminescentPowerIcon.DISC_SHARE
        val layers = if (glowOn) {
            BioluminescentPowerIcon.layers(size, accent, glowColor, resources.displayMetrics.density)
        } else {
            null
        }
        if (layers != null) {
            val left = (width - size) / 2f
            val top = (height - size) / 2f
            val halo = when {
                t <= 0f -> 0.22f
                extraHalo -> t
                else -> 0.72f * t
            }
            BioluminescentPowerIcon.paintLayers(canvas, left, top, layers, halo, stamp)
        }
        val onFill: Int
        val offFill: Int
        val onSymbol: Int
        val offSymbol: Int
        when (current) {
            ToggleStyle.WHITE -> {
                onFill = Hud.WHITE
                offFill = Color.TRANSPARENT
                onSymbol = Hud.INK
                offSymbol = Hud.withAlpha(Hud.WHITE, DIM)
            }
            ToggleStyle.BLACK -> {
                onFill = Hud.INK
                offFill = Hud.INK
                onSymbol = Hud.WHITE
                offSymbol = DIM_GRAY
            }
        }
        disc.color = lerp(offFill, onFill, t)
        canvas.drawCircle(cx, cy, radius, disc)
        if (current == ToggleStyle.WHITE) {
            edge.color = Hud.withAlpha(Hud.INK, EDGE_ALPHA)
            edge.strokeWidth = Hud.dp(context, 1f)
            canvas.drawCircle(cx, cy, radius + edge.strokeWidth / 2f, edge)
        }
        symbol.color = lerp(offSymbol, onSymbol, t)
        BioluminescentPowerIcon.drawPowerSymbol(canvas, cx, cy, radius, symbol, arcBox)
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

        const val DIM = 0.42f
        const val EDGE_ALPHA = 0.35f
        const val DIM_GRAY = 0xFF5C5C5C.toInt()
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
