package com.offgrid.compositor.terminal

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.offgrid.compositor.ui.wheel.DrumReelView

internal class WheelNumberRow(
    context: Context,
    item: NumberItem,
    value: Int,
    private val laserSource: () -> Pair<Int, Int>,
    onChange: (value: Int, announce: Boolean) -> Unit,
    onNotch: (value: Int) -> Unit,
    onProblem: (String) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val note = rowNote(context, item.help)
    val reel = DrumReelView(context)
    val fallback = NumberBox(context, item.label, value, onChange = { number, announce ->
        reel.setValue(number)
        onChange(number, announce)
    }, onProblem = onProblem)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Hud.dpi(context, 6f), Hud.dpi(context, 4f), Hud.dpi(context, 6f), Hud.dpi(context, 4f))
        clipChildren = false
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        reel.setReelLabel(item.label)
        reel.setValue(value)
        reel.onValueChanged = { number ->
            if (!fallback.hasFocus()) fallback.show(number)
            onNotch(number)
            onChange(number, false)
        }
        reel.onValueSettled = { number ->
            onNotch(number)
            onChange(number, true)
        }
        reel.onNotch = onNotch
        addView(reel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(fallback, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 46f)).apply {
            topMargin = Hud.dpi(context, 4f)
        })
    }

    override fun applyLook(look: PanelLook) {
        styleRowWords(title, note, look)
        val (color, level) = laserSource()
        reel.setLaserColor(color)
        reel.setDimmer(level)
        reel.setNumberColor(look.numberColor)
        reel.setSelectedNumberColor(NumberColorGuard.colorOnBeam(look.numberColor, look.skin, color, level))
        reel.setHapticsEnabled(look.tick)
        reel.setFontScale(look.fontScale)
    }

    fun show(value: Int) {
        if (!reel.isInteracting) reel.setValue(value)
        if (!fallback.hasFocus()) fallback.show(value)
    }

}
