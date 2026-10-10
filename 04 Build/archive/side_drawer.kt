/**
 * Archived. The side panel was taken out of the terminal. This copy is kept so the
 * work is not deleted. It sits outside the app source set, so it is not compiled
 * and it is not shown.
 */

package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.offgrid.compositor.R
import kotlin.math.abs

/**
 * The side list as a drawer, for Side list: Swipe to open (Step 2.4).
 *
 * A thin glowing handle sits at the panel's left edge. Swipe right from it, or tap it,
 * and the side list slides out over the folders, following the finger. Swipe it left,
 * tap outside it, or press Back, and it tucks away. When the panel fills the screen, its
 * edge is the phone's edge too. Then only the glowing handle in the middle is kept for
 * the side list, and the rest of the edge still swipes Back as usual.
 */
internal class SideDrawer(context: Context) : FrameLayout(context), Styled {
    private val density = resources.displayMetrics.density
    private val scrim = View(context)
    private val handle = EdgeHandle(context)
    private val glass = PanelGlassDrawable(density, 0f, pane = true)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    /** Holds the side list while the drawer is in use. */
    val sheet = FrameLayout(context)

    /** Told when the drawer opens or closes, so Back can close it. */
    var onOpenChanged: ((Boolean) -> Unit)? = null

    var tick = true

    var isOpen = false
        private set

    private var downX = 0f
    private var downY = 0f
    private var startShift = 0f
    private var dragging = false
    private var sliding = false

    init {
        clipChildren = false
        // The handle sits under the dim layer and the sheet, so an open sheet's rows get every tap.
        handle.contentDescription = context.getString(R.string.terminal_side_open)
        handle.setOnClickListener { open(animate = true) }
        addView(handle, LayoutParams(Hud.dpi(context, HANDLE_DP), LayoutParams.MATCH_PARENT, Gravity.START))
        scrim.background = ColorDrawable(SCRIM)
        scrim.alpha = 0f
        scrim.visibility = GONE
        scrim.isClickable = true
        scrim.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        scrim.setOnClickListener { close(animate = true) }
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        sheet.background = LayerDrawable(arrayOf(ColorDrawable(BACKING), glass))
        sheet.isClickable = true
        sheet.visibility = INVISIBLE
        addView(sheet, LayoutParams(Hud.dpi(context, SHEET_DP), LayoutParams.MATCH_PARENT, Gravity.START))
        // It starts tucked away, so the first swipe slides it out from the edge.
        sheet.translationX = -sheetWidth()
    }

    /** How wide the sheet is. The panel sets it to fit the side list. */
    fun setSheetWidth(width: Int) {
        // Tucked away at the new width, unless a slide is still moving it.
        if (!isOpen && !sliding) sheet.translationX = -width.toFloat()
        val params = sheet.layoutParams
        if (params.width == width) return
        params.width = width
        sheet.layoutParams = params
    }

    /** How wide the handle is. The folders start past it, so a tap on a folder never opens the drawer. */
    fun handleWidth(): Int = Hud.dpi(context, HANDLE_DP)

    private fun sheetWidth(): Float = sheet.layoutParams.width.toFloat().coerceAtLeast(1f)

    fun open(animate: Boolean) {
        val was = isOpen
        isOpen = true
        handle.isClickable = false
        sheet.visibility = VISIBLE
        scrim.visibility = VISIBLE
        slideTo(0f, animate)
        if (!was) {
            if (tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onOpenChanged?.invoke(true)
            sheet.post { sheet.requestFocus() }
        }
    }

    fun close(animate: Boolean) {
        val was = isOpen
        isOpen = false
        handle.isClickable = true
        slideTo(-sheetWidth(), animate)
        if (was) onOpenChanged?.invoke(false)
    }

    /** [shift] 0 is open, minus the sheet's width is tucked away. */
    private fun slideTo(shift: Float, animate: Boolean) {
        sheet.animate().cancel()
        scrim.animate().cancel()
        val showing = 1f - (-shift / sheetWidth()).coerceIn(0f, 1f)
        val after = Runnable {
            sliding = false
            if (!isOpen) {
                sheet.visibility = INVISIBLE
                scrim.visibility = GONE
            }
        }
        if (animate && ValueAnimator.areAnimatorsEnabled()) {
            sliding = true
            sheet.animate().translationX(shift).setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator()).withEndAction(after).start()
            scrim.animate().alpha(showing).setDuration(SLIDE_MS).start()
        } else {
            sheet.translationX = shift
            scrim.alpha = showing
            after.run()
        }
        handle.alpha = 1f - showing
    }

    /** While a finger drags the drawer: the sheet follows it, and the outside dims to match. */
    private fun follow(shift: Float) {
        val clamped = shift.coerceIn(-sheetWidth(), 0f)
        sheet.visibility = VISIBLE
        scrim.visibility = VISIBLE
        sheet.translationX = clamped
        val showing = 1f + clamped / sheetWidth()
        scrim.alpha = showing
        handle.alpha = 1f - showing
    }

    private fun settle(shift: Float) {
        if (shift > -sheetWidth() * (1f - SETTLE_SHARE)) open(animate = true) else close(animate = true)
    }

    /** Opening: a swipe that starts on the handle. Closing: a sideways swipe on the open sheet. */
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                startShift = sheet.translationX
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val sideways = abs(dx) > slop && abs(dx) > abs(dy) * 1.2f
                val fromHandle = !isOpen && downX <= handle.width + slop && dx > 0f
                val onSheet = isOpen && downX <= sheetWidth() && dx < 0f
                if (sideways && (fromHandle || onSheet)) {
                    // A finger takes over from a slide that is still moving, right where it is.
                    sheet.animate().cancel()
                    scrim.animate().cancel()
                    sliding = false
                    startShift = sheet.translationX - dx
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(event)
        val dx = event.x - downX
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> follow(startShift + dx)
            MotionEvent.ACTION_UP -> {
                dragging = false
                settle(startShift + dx)
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                settle(startShift)
            }
        }
        return true
    }

    override fun applyLook(look: PanelLook) {
        glass.look = look
        handle.applyLook(look)
        sheet.elevation = Hud.dp(context, 10f)
    }

    private companion object {
        const val SHEET_DP = 132f
        const val HANDLE_DP = 16f
        const val SLIDE_MS = 200L
        const val SETTLE_SHARE = 0.35f
        const val SCRIM = 0x66000000
        const val BACKING = 0xF7000000.toInt()
    }
}

/** The thin glowing handle at the panel's left edge, with a small arrow that points right. */
private class EdgeHandle(context: Context) : View(context), Styled {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val chevron = Path()

    init {
        isClickable = true
        isFocusable = true
    }

    /** The middle of the handle is kept for the side list swipe, so the phone's Back swipe does not take it. */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val reach = minOf(h, Hud.dpi(context, KEPT_DP)) / 2
        systemGestureExclusionRects = listOf(Rect(0, h / 2 - reach, w, h / 2 + reach))
    }

    override fun applyLook(look: PanelLook) {
        line.color = Hud.withAlpha(look.edge, 0.55f)
        line.setShadowLayer(Hud.dp(context, 4f), 0f, 0f, look.edge)
        arrow.color = look.decor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val x = Hud.dp(context, 3f)
        val middle = height / 2f
        val reach = minOf(height * 0.18f, Hud.dp(context, 46f))
        canvas.drawRoundRect(x - Hud.dp(context, 1f), middle - reach, x + Hud.dp(context, 1f), middle + reach, x, x, line)
        val size = Hud.dp(context, 4f)
        arrow.strokeWidth = Hud.dp(context, 1.6f)
        chevron.reset()
        chevron.moveTo(x + size, middle - size)
        chevron.lineTo(x + size * 2f, middle)
        chevron.lineTo(x + size, middle + size)
        canvas.drawPath(chevron, arrow)
    }

    private companion object {
        /** Android lets an app keep up to 200 dp of each edge. The handle keeps less. */
        const val KEPT_DP = 160f
    }
}
