package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import com.offgrid.compositor.R
import com.offgrid.compositor.effects.FxKeys

internal class TopBar(
    context: Context,
    prefs: PanelPrefs,
    width: Int,
    height: Int,
    onBack: () -> Unit,
    onShrink: () -> Unit,
    onToggle: (String, Boolean, String) -> Unit,
    onDimensionChanged: (String, Int) -> Unit,
    onViewModeChanged: (Int) -> Unit,
) : LinearLayout(context) {
    val widthInput: EditText
    val heightInput: EditText
    private val widthControl: FrameLayout
    private val heightControl: FrameLayout
    private val back = BackOrbView(context)
    private val camera = QuickToggleView(context, false, prefs.toggle(FxKeys.CAMERA_ON, true))
    private val microphone = QuickToggleView(context, true, prefs.toggle(FxKeys.MIC_ON, false))
    private var currentWidth = width
    private var currentHeight = height

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(Hud.dpi(context, 16f), Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f))
        contentDescription = context.getString(R.string.terminal_drag)
        back.setOnClickListener { onBack() }
        addView(back, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 48f)))
        addView(TextView(context).apply {
            text = context.getString(R.string.terminal_title)
            typeface = Hud.monoBold(context)
            textSize = 12.5f
            letterSpacing = 0.12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(NeonPalette.BLACKLIGHT_WHITE)
            setShadowLayer(Hud.dp(context, 6f), 0f, 0f, NeonPalette.ELECTRIC_PURPLE)
            setPadding(Hud.dpi(context, 8f), 0, Hud.dpi(context, 8f), 0)
        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        camera.onChanged = { onToggle(FxKeys.CAMERA_ON, it, "Camera") }
        addView(camera, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 48f)).withStartGap(context))
        microphone.onChanged = { onToggle(FxKeys.MIC_ON, it, "Mic") }
        addView(microphone, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 48f)).withStartGap(context))
        widthInput = dimensionInput(context, "Width", width) { onDimensionChanged(PanelTree.WIDTH, it) }
        widthControl = dimensionControl(context, "W", widthInput)
        addView(widthControl, LayoutParams(Hud.dpi(context, 56f), Hud.dpi(context, 48f)).withStartGap(context))
        heightInput = dimensionInput(context, "Height", height) { onDimensionChanged(PanelTree.HEIGHT, it) }
        heightControl = dimensionControl(context, "H", heightInput)
        addView(heightControl, LayoutParams(Hud.dpi(context, 56f), Hud.dpi(context, 48f)).withStartGap(context))
        addView(TextView(context).apply {
            text = "⋮"
            typeface = Hud.monoBold(context)
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(NeonPalette.HOT_MAGENTA)
            contentDescription = context.getString(R.string.terminal_view_options)
            isClickable = true
            isFocusable = true
            setOnClickListener { showViewMenu(this, onViewModeChanged) }
        }, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 48f)).withStartGap(context))
        addView(ShrinkButton(context).apply {
            contentDescription = context.getString(R.string.terminal_shrink)
            setOnClickListener { onShrink() }
        }, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 48f)).withStartGap(context))
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val showBoxes = view.width >= Hud.dpi(context, 480f)
            widthControl.visibility = if (showBoxes) VISIBLE else GONE
            heightControl.visibility = if (showBoxes) VISIBLE else GONE
        }
    }

    fun setDimensions(width: Int, height: Int) {
        currentWidth = width
        currentHeight = height
        if (!widthInput.hasFocus()) widthInput.setText(width.toString())
        if (!heightInput.hasFocus()) heightInput.setText(height.toString())
    }

    fun setBackEnabled(enabled: Boolean) {
        back.enabledState = enabled
    }

    fun setQuickToggle(key: String, on: Boolean) {
        when (key) {
            FxKeys.CAMERA_ON -> camera.setOn(on)
            FxKeys.MIC_ON -> microphone.setOn(on)
        }
    }

    private fun showViewMenu(anchor: View, onViewModeChanged: (Int) -> Unit) {
        PopupMenu(context, anchor).apply {
            menu.add("Big tiles").setOnMenuItemClickListener { onViewModeChanged(1); true }
            menu.add("List").setOnMenuItemClickListener { onViewModeChanged(1); true }
            menu.add("Grid 2").setOnMenuItemClickListener { onViewModeChanged(2); true }
            menu.add("Grid 3").setOnMenuItemClickListener { onViewModeChanged(3); true }
            show()
        }
    }

    private fun dimensionInput(
        context: Context,
        label: String,
        value: Int,
        onChange: (Int) -> Unit,
    ) = EditText(context).apply {
        setText(value.toString())
        typeface = Hud.monoBold(context)
        textSize = 12f
        setTextColor(NeonPalette.BLACKLIGHT_WHITE)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER
        filters = arrayOf(InputFilter.LengthFilter(3))
        maxLines = 1
        contentDescription = label
        background = null
        setPadding(0, 0, Hud.dpi(context, 3f), 0)
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val typed = s?.toString()?.toIntOrNull() ?: return
                if (typed in 0..100) onChange(typed)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val fallback = if (label == "Width") currentWidth else currentHeight
                val typed = text.toString().toIntOrNull()?.coerceIn(0, 100) ?: fallback
                setText(typed.toString())
                onChange(typed)
            }
        }
    }

    private fun dimensionControl(context: Context, caption: String, field: EditText) =
        FrameLayout(context).apply {
            background = null
            foreground = InsetDrawable(GradientDrawable().apply {
                setColor(Hud.INK)
                cornerRadius = Hud.dp(context, 4f)
                setStroke(Hud.dpi(context, 1f), NeonPalette.UV_VIOLET)
            }, 0, Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f))
            addView(TextView(context).apply {
                text = caption
                typeface = Hud.monoBold(context)
                textSize = 9f
                setTextColor(NeonPalette.CYAN_GLOW)
                gravity = Gravity.CENTER
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(Hud.dpi(context, 16f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START))
            addView(field, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = Hud.dpi(context, 16f)
            })
        }

    private fun LayoutParams.withStartGap(context: Context) = apply {
        marginStart = Hud.dpi(context, 8f)
    }
}
