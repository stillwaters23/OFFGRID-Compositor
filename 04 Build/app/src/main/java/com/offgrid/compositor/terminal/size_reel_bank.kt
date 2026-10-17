package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.offgrid.compositor.ui.wheel.DrumReelView
import com.offgrid.compositor.ui.wheel.LaserStrip

/**
 * Width and Height side by side. One laser runs through both selected rows,
 * including the gap, and each reel keeps a typed number for people who do not drag.
 */
internal class SizeReelBank(
    context: Context,
    widthItem: NumberItem,
    heightItem: NumberItem,
    widthValue: Int,
    heightValue: Int,
    private val laserSource: () -> Pair<Int, Int>,
    private val onChange: (key: String, value: Int, announce: Boolean) -> Unit,
    private val onNotch: (value: Int) -> Unit,
    onProblem: (String) -> Unit,
) : LinearLayout(context), Styled {
    private val widthTitle = rowTitle(context, widthItem.label)
    private val widthNote = rowNote(context, widthItem.help)
    private val heightTitle = rowTitle(context, heightItem.label)
    private val heightNote = rowNote(context, heightItem.help)
    private val stage = ReelStage(context)
    val widthReel: DrumReelView
    val heightReel: DrumReelView
    val widthBox: NumberBox
    val heightBox: NumberBox

    init {
        orientation = VERTICAL
        clipChildren = false
        setPadding(Hud.dpi(context, 4f), Hud.dpi(context, 4f), Hud.dpi(context, 4f), Hud.dpi(context, 4f))

        widthReel = reel(widthItem.label, widthValue, LookKeys.WIDTH)
        heightReel = reel(heightItem.label, heightValue, LookKeys.HEIGHT)
        widthBox = numberBox(widthItem, widthValue, onProblem)
        heightBox = numberBox(heightItem, heightValue, onProblem)

        val header = LinearLayout(context).apply { orientation = HORIZONTAL }
        header.addView(column(widthTitle, widthNote), weighted())
        header.addView(column(heightTitle, heightNote), weighted())
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val reels = LinearLayout(context).apply { orientation = HORIZONTAL }
        reels.addView(widthReel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        reels.addView(heightReel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Hud.dpi(context, 8f)
        })
        stage.addView(reels, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(stage, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val typed = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        typed.addView(widthBox, LayoutParams(0, Hud.dpi(context, 46f), 1f))
        typed.addView(heightBox, LayoutParams(0, Hud.dpi(context, 46f), 1f).apply {
            marginStart = Hud.dpi(context, 8f)
        })
        addView(typed, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = Hud.dpi(context, 4f)
        })
        val (color, level) = laserSource()
        applyLaser(color, level)
    }

    fun show(key: String, value: Int) {
        val reel = reelFor(key)
        val box = boxFor(key)
        if (!reel.isInteracting) reel.setValue(value)
        if (!box.hasFocus()) box.show(value)
    }

    fun isBusy(key: String): Boolean = reelFor(key).isInteracting

    /** The two selected rows sit on one horizontal line, and the laser crosses the gap between them. */
    fun sharesOneLaser(): Boolean {
        if (widthReel.height <= 0 || heightReel.height <= 0 || stage.width <= 0) return false
        val leftCenter = widthReel.top + widthReel.height / 2
        val rightCenter = heightReel.top + heightReel.height / 2
        val sameCenter = kotlin.math.abs(leftCenter - rightCenter) <= 1
        val row = stage.getChildAt(0) as? android.view.ViewGroup ?: return false
        val left = row.getChildAt(0) ?: return false
        val right = row.getChildAt(1) ?: return false
        return sameCenter && LaserStrip.coversGap(
            0f,
            stage.width.toFloat(),
            left.right.toFloat(),
            right.left.toFloat(),
        )
    }

    override fun applyLook(look: PanelLook) {
        styleRowWords(widthTitle, widthNote, look)
        styleRowWords(heightTitle, heightNote, look)
        val (color, level) = laserSource()
        applyLaser(color, level)
        for (reel in listOf(widthReel, heightReel)) {
            reel.setNumberColor(look.numberColor)
            reel.setSelectedNumberColor(NumberColorGuard.colorOnBeam(look.numberColor, look.skin, color, level))
            reel.setHapticsEnabled(look.tick)
            reel.setFontScale(look.fontScale)
        }
        stage.invalidate()
    }

    private fun applyLaser(color: Int, level: Int) {
        stage.laserColor = color
        stage.dimmer = level
        widthReel.setLaserColor(color)
        heightReel.setLaserColor(color)
        widthReel.setDimmer(level)
        heightReel.setDimmer(level)
    }

    private fun reel(label: String, value: Int, key: String) = DrumReelView(context).apply {
        drawsOwnLaser = false
        setReelLabel(label)
        setValue(value)
        onValueChanged = { number ->
            boxFor(key).let { box -> if (!box.hasFocus()) box.show(number) }
            onChange(key, number, false)
        }
        onValueSettled = { number -> onChange(key, number, true) }
        onNotch = onNotch
    }

    private fun numberBox(item: NumberItem, value: Int, onProblem: (String) -> Unit) = NumberBox(
        context,
        item.label,
        value,
        onChange = { number, announce ->
            reelFor(item.key).setValue(number)
            onChange(item.key, number, announce)
        },
        onProblem = onProblem,
    )

    private fun column(title: android.widget.TextView, note: android.widget.TextView) = LinearLayout(context).apply {
        orientation = VERTICAL
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun weighted() = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

    private fun reelFor(key: String): DrumReelView =
        if (key == LookKeys.HEIGHT) heightReel else widthReel

    private fun boxFor(key: String): NumberBox =
        if (key == LookKeys.HEIGHT) heightBox else widthBox

    /** The laser's view. It draws behind the digits and across the whole bank. */
    internal class ReelStage(context: Context) : FrameLayout(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.BUTT
        }
        var laserColor: Int = LaserStrip.DEFAULT_COLOR
        var dimmer: Int = 72

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setWillNotDraw(false)
        }

        override fun dispatchDraw(canvas: Canvas) {
            if (dimmer > 0 && width > 0 && height > 0) {
                val density = resources.displayMetrics.density
                val y = laserY()
                for (stroke in LaserStrip.strokes(laserColor, dimmer, density)) {
                    paint.color = stroke.color
                    paint.strokeWidth = stroke.widthPx
                    canvas.drawLine(0f, y, width.toFloat(), y, paint)
                }
            }
            super.dispatchDraw(canvas)
        }

        /** The selected row of the first reel, so the strip lines up with both drums. */
        private fun laserY(): Float {
            val row = getChildAt(0) as? android.view.ViewGroup
            val reel = row?.getChildAt(0)
            if (reel != null && reel.height > 0) {
                return row.top + reel.top + reel.height / 2f
            }
            return height / 2f
        }
    }
}
