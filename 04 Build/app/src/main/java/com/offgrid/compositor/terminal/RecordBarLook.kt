package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

/**
 * Paints the record bar at the bottom of the screen in the panel's theme,
 * so the whole screen matches. The bar is never part of the saved video.
 */
internal object RecordBarLook {
    fun apply(look: PanelLook, bars: List<View>, texts: List<TextView>, buttons: List<TextView>) {
        bars.forEach { it.background = bar(it.context, look) }
        texts.forEach { it.setTextColor(look.status) }
        buttons.forEach {
            it.background = button(it.context, look)
            it.setTextColor(buttonText(look))
        }
    }

    /** Dark glass, a thin line of light on top, and a short brighter mark. */
    private fun bar(context: Context, look: PanelLook): Drawable {
        val glass = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Hud.withAlpha(Hud.blend(look.skin, Hud.INK, 0.35f), 0.85f),
                Hud.withAlpha(Hud.blend(look.skin, Hud.INK, 0.8f), 0.95f),
            ),
        )
        val line = GradientDrawable().apply { setColor(Hud.withAlpha(look.edge, 0.6f)) }
        val mark = GradientDrawable().apply { setColor(look.decor) }
        return LayerDrawable(arrayOf(glass, line, mark)).apply {
            setLayerGravity(1, Gravity.TOP or Gravity.FILL_HORIZONTAL)
            setLayerHeight(1, Hud.dpi(context, 1f))
            setLayerGravity(2, Gravity.TOP or Gravity.START)
            setLayerSize(2, Hud.dpi(context, 72f), Hud.dpi(context, 2f))
            setLayerInsetStart(2, Hud.dpi(context, 16f))
        }
    }

    private fun button(context: Context, look: PanelLook): Drawable {
        fun shape(fill: Int, stroke: Int, width: Float) = GradientDrawable().apply {
            setColor(fill)
            setStroke(Hud.dpi(context, width), stroke)
            cornerRadius = Hud.dp(context, 5f)
        }
        val dark = Hud.blend(look.skin, Hud.INK, 0.5f)
        return StateListDrawable().apply {
            addState(
                intArrayOf(-android.R.attr.state_enabled),
                shape(Hud.withAlpha(dark, 0.6f), Hud.withAlpha(look.edge, 0.25f), 1f),
            )
            addState(
                intArrayOf(android.R.attr.state_pressed),
                shape(Hud.withAlpha(look.edge, 0.33f), pressed(look), 1.5f),
            )
            addState(intArrayOf(), shape(Hud.withAlpha(dark, 0.85f), look.edge, 1.5f))
        }
    }

    private fun buttonText(look: PanelLook) = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf(),
        ),
        intArrayOf(Hud.withAlpha(look.edge, 0.4f), pressed(look), look.edge),
    )

    /** Holo Classic presses in gold, like Step 2. Other themes press brighter. */
    private fun pressed(look: PanelLook): Int = if (look.holo) look.decor else Hud.lighten(look.edge, 0.45f)
}
