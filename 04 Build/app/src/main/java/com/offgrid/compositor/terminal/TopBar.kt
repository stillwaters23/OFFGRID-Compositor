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
    private val backOrb: BackOrbView,
    onShrink: () -> Unit,
    onToggle: (String, Boolean, String) -> Unit,
    onDimensionChanged: (String, Int) -> Unit,
    onViewModeChanged: (Int) -> Unit,
) : LinearLayout(context), Styled {
    val widthInput: EditText
    val heightInput: EditText
    private val widthControl: FrameLayout
    private val heightControl: FrameLayout
    private val titleView: TextView
    private val camera = QuickToggleView(context, false, prefs.toggle(FxKeys.CAMERA_ON, true))
    private val microphone = QuickToggleView(context, true, prefs.toggle(FxKeys.MIC_ON, false))
    private val moreButton: View
    private val shrinkButton: View
    private var currentWidth = width
    private var currentHeight = height
    private var laidOutWidth = 0
    private val sideGap = Hud.dpi(context, 8f)
    private val controlWidth = Hud.dpi(context, 40f)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(Hud.dpi(context, 8f), 0, Hud.dpi(context, 8f), 0)
        contentDescription = context.getString(R.string.terminal_drag)

        backOrb.layoutParams = LayoutParams(controlWidth, Hud.dpi(context, CONTROL_HEIGHT_DP))
        addView(backOrb)
        titleView = TextView(context).apply {
            text = context.getString(R.string.terminal_title)
            typeface = Hud.monoBold(context)
            textSize = 12f
            includeFontPadding = false
            letterSpacing = 0.08f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(Hud.dp(context, 1.5f), 0f, 0f, Hud.withAlpha(NeonPalette.ELECTRIC_PURPLE, 0.2f))
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(titleView, LayoutParams(0, Hud.dpi(context, CONTROL_HEIGHT_DP), 1f).apply {
            marginStart = sideGap
            marginEnd = sideGap
        })

        camera.onChanged = { onToggle(FxKeys.CAMERA_ON, it, "Camera") }
        addControl(camera, controlWidth, gap = false)
        microphone.onChanged = { onToggle(FxKeys.MIC_ON, it, "Mic") }
        addControl(microphone, controlWidth)

        widthInput = dimensionInput(context, "Width", width) { onDimensionChanged(LookKeys.WIDTH, it) }
        widthControl = dimensionControl(context, "W", widthInput)
        addControl(widthControl, controlWidth)
        heightInput = dimensionInput(context, "Height", height) { onDimensionChanged(LookKeys.HEIGHT, it) }
        heightControl = dimensionControl(context, "H", heightInput)
        addControl(heightControl, controlWidth)

        moreButton = MoreButton(context).apply {
            contentDescription = context.getString(R.string.terminal_view_options)
            setOnClickListener { showViewMenu(this, onViewModeChanged) }
        }
        addControl(moreButton, controlWidth)
        shrinkButton = ShrinkButton(context).apply {
            contentDescription = context.getString(R.string.terminal_shrink)
            setOnClickListener { onShrink() }
        }
        addControl(shrinkButton, controlWidth)
    }

    private fun addControl(view: View, width: Int, gap: Boolean = true) {
        addView(view, LayoutParams(width, Hud.dpi(context, CONTROL_HEIGHT_DP)).apply {
            if (gap) marginStart = sideGap
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        widthControl.visibility = VISIBLE
        heightControl.visibility = VISIBLE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        laidOutWidth = available
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        laidOutWidth = right - left
        titleView.textSize = if (laidOutWidth < Hud.dpi(context, 480f)) 9f else 12f
    }

    fun setDimensions(width: Int, height: Int) {
        currentWidth = width
        currentHeight = height
        if (!widthInput.hasFocus()) widthInput.setText(width.toString())
        if (!heightInput.hasFocus()) heightInput.setText(height.toString())
    }

    fun setQuickToggle(key: String, on: Boolean) {
        when (key) {
            FxKeys.CAMERA_ON -> camera.setOn(on)
            FxKeys.MIC_ON -> microphone.setOn(on)
        }
    }

    override fun applyLook(look: PanelLook) {
        titleView.setTextColor(look.text)
        widthInput.setTextColor(look.text)
        heightInput.setTextColor(look.text)
    }

    private fun showViewMenu(anchor: View, onViewModeChanged: (Int) -> Unit) {
        PopupMenu(context, anchor).apply {
            menu.add("Big tiles").setOnMenuItemClickListener { onViewModeChanged(0); true }
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
        fontFeatureSettings = "tnum"
        includeFontPadding = false
        textSize = 11f
        setTextColor(NeonPalette.BLACKLIGHT_WHITE)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER
        filters = arrayOf(InputFilter.LengthFilter(3))
        maxLines = 1
        contentDescription = label
        background = null
        setPadding(Hud.dpi(context, 2f), 0, Hud.dpi(context, 2f), 0)
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
            foreground = InsetDrawable(GradientDrawable().apply {
                setColor(Hud.INK)
                cornerRadius = Hud.dp(context, 4f)
                setStroke(Hud.dpi(context, 1f), NeonPalette.UV_VIOLET)
            }, Hud.dpi(context, 4f), Hud.dpi(context, 4f), Hud.dpi(context, 4f), Hud.dpi(context, 4f))
            addView(TextView(context).apply {
                text = caption
                typeface = Hud.monoBold(context)
                textSize = 9f
                includeFontPadding = false
                setTextColor(NeonPalette.CYAN_GLOW)
                gravity = Gravity.CENTER
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(Hud.dpi(context, 12f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START))
            addView(field, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = Hud.dpi(context, 12f)
            })
        }

    private companion object {
        const val CONTROL_HEIGHT_DP = 40f
    }
}
