package com.offgrid.compositor.terminal

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.offgrid.compositor.ui.wheel.DrumReelView

internal class WheelNumberRow(
    context: Context,
    item: NumberItem,
    value: Int,
    laserColor: Int,
    dimmer: Int,
    onChange: (value: Int, announce: Boolean) -> Unit,
    onNotch: (value: Int) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val note = rowNote(context, item.help)
    val reel = DrumReelView(context)
    private var currentLaserColor = laserColor
    private var currentDimmer = dimmer

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(Hud.dpi(context, 6f), Hud.dpi(context, 4f), Hud.dpi(context, 6f), Hud.dpi(context, 4f))
        clipChildren = false
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(note, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        reel.setValue(value)
        reel.onValueChanged = { onChange(it, false) }
        reel.onValueSettled = { onChange(it, true) }
        reel.onNotch = onNotch
        addView(reel, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 112f)))
    }

    override fun applyLook(look: PanelLook) {
        styleRowWords(title, note, look)
        reel.setLaserColor(currentLaserColor)
        reel.setNumberColor(look.numberColor)
        reel.setDimmer(currentDimmer)
        reel.setHapticsEnabled(look.tick)
    }

    fun show(value: Int) = reel.setValue(value)

}
