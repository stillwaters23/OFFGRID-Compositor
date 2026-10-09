package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

internal class NeonColorRow(
    context: Context,
    onColorChanged: (Int) -> Unit,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        NeonPalette.swatches.chunked(4).forEach { group ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            group.forEachIndexed { index, swatch ->
                val code = RainbowMix.hex(swatch.color)
                row.addView(TextView(context).apply {
                    text = swatch.name
                    typeface = Hud.monoBold(context)
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(if (Color.luminance(swatch.color) > 0.55f) Color.BLACK else Color.WHITE)
                    contentDescription = "${swatch.name}, $code"
                    background = GradientDrawable().apply {
                        setColor(swatch.color)
                        cornerRadius = Hud.dp(context, 4f)
                        setStroke(Hud.dpi(context, 1f), NeonPalette.UV_VIOLET)
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onColorChanged(swatch.color) }
                }, LayoutParams(0, Hud.dpi(context, 40f), 1f).apply {
                    if (index > 0) marginStart = Hud.dpi(context, 8f)
                })
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 48f)).apply {
                topMargin = Hud.dpi(context, 8f)
            })
        }
    }
}
