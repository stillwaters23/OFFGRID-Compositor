package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

internal class NeonColorRow(
    context: Context,
    item: ColorItem,
    initialColor: Int,
    onColorChanged: (Int) -> Unit,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        val initialHex = String.format("#%06X", 0xFFFFFF and initialColor)
        var lastValidHex = initialHex
        addView(TextView(context).apply {
            text = item.label
            typeface = Hud.monoBold(context)
            textSize = 12f
            setTextColor(Hud.TEXT)
            contentDescription = item.help
        })
        addView(EditText(context).apply {
            setText(initialHex)
            typeface = Hud.monoBold(context)
            textSize = 13f
            setTextColor(Hud.TEXT)
            gravity = Gravity.CENTER_VERTICAL
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(InputFilter.LengthFilter(7))
            singleLine = true
            contentDescription = item.help
            background = GradientDrawable().apply {
                setColor(Hud.INK)
                cornerRadius = Hud.dp(context, 4f)
                setStroke(Hud.dpi(context, 1f), NeonPalette.UV_VIOLET)
            }
            setPadding(Hud.dpi(context, 8f), 0, Hud.dpi(context, 8f), 0)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val typed = s?.toString()?.removePrefix("#") ?: return
                    if (typed.matches(HEX_COLOR)) {
                        error = null
                        lastValidHex = "#$typed"
                        onColorChanged(Color.parseColor("#$typed"))
                    }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !text.toString().removePrefix("#").matches(HEX_COLOR)) {
                    error = "Use six hex digits."
                    setText(lastValidHex)
                }
            }
        }, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 48f)).apply {
            topMargin = Hud.dpi(context, 8f)
        })
        NeonPalette.swatches.chunked(4).forEach { group ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            group.forEachIndexed { index, swatch ->
                row.addView(TextView(context).apply {
                    text = swatch.name
                    typeface = Hud.monoBold(context)
                    textSize = 9f
                    gravity = Gravity.CENTER
                    setTextColor(if (Color.luminance(swatch.color) > 0.55f) Color.BLACK else Color.WHITE)
                    contentDescription = "${swatch.name}, ${String.format("#%06X", 0xFFFFFF and swatch.color)}"
                    background = GradientDrawable().apply {
                        setColor(swatch.color)
                        cornerRadius = Hud.dp(context, 4f)
                        setStroke(Hud.dpi(context, 1f), NeonPalette.BLACKLIGHT_WHITE)
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val field = (this@NeonColorRow.getChildAt(1) as EditText)
                        field.setText(String.format("#%06X", 0xFFFFFF and swatch.color))
                    }
                }, LayoutParams(0, Hud.dpi(context, 40f), 1f).apply {
                    if (index > 0) marginStart = Hud.dpi(context, 8f)
                })
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 48f)).apply {
                topMargin = Hud.dpi(context, 8f)
            })
        }
    }

    private companion object {
        val HEX_COLOR = Regex("[0-9A-Fa-f]{6}")
    }
}
