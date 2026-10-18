package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.text.InputFilter
import android.text.InputType
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
import com.offgrid.compositor.ui.wheel.DrumReelView

internal class TopBar(
    context: Context,
    prefs: PanelPrefs,
    width: Int,
    height: Int,
    private val backOrb: BackOrbView,
    onShrink: () -> Unit,
    onToggle: (String, Boolean, String) -> Unit,
    private val onDimensionChanged: (String, Int) -> Unit,
    onViewModeChanged: (Int) -> Unit,
    onLockTapped: () -> Unit = {},
    onLockHeld: () -> Unit = {},
    private val onNotch: (Int) -> Unit = {},
    private val laserSource: () -> Pair<Int, Int> = { NeonPalette.CYAN_GLOW to 72 },
) : LinearLayout(context), Styled {
    val widthInput: EditText
    val heightInput: EditText
    val widthReel: DrumReelView
    val heightReel: DrumReelView
    private val widthControl: FrameLayout
    private val heightControl: FrameLayout
    private val titleView: FitTextView
    private val camera = QuickToggleView(context, false, prefs.toggle(FxKeys.CAMERA_ON, true))
    private val microphone = QuickToggleView(context, true, prefs.toggle(FxKeys.MIC_ON, false))
    private val moreButton: View
    private val shrinkButton: View
    private val lockButton: LockGlyphButton
    private val widthCaption: TextView
    private val heightCaption: TextView
    private val widthPlate: GradientDrawable
    private val heightPlate: GradientDrawable
    private var currentWidth = width
    private var currentHeight = height
    private var laidOutWidth = 0
    private val sideGap = Hud.dpi(context, 8f)
    private val controlWidth = Hud.dpi(context, 40f)
    private val sizeBoxWidth = Hud.dpi(context, 48f)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(Hud.dpi(context, 8f), 0, Hud.dpi(context, 8f), 0)
        // A group description hides Lock, Minus, and the menu from TalkBack and UI tests.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO

        backOrb.layoutParams = LayoutParams(controlWidth, Hud.dpi(context, CONTROL_HEIGHT_DP))
        addView(backOrb)
        titleView = FitTextView(context).apply {
            text = context.getString(R.string.terminal_title)
            typeface = Hud.monoBold(context)
            textSize = 12f
            includeFontPadding = false
            letterSpacing = 0.08f
            fitWholeLine = false
            maxLines = 2
            setShadowLayer(Hud.dp(context, 1.5f), 0f, 0f, Hud.withAlpha(NeonPalette.ELECTRIC_PURPLE, 0.2f))
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = context.getString(R.string.terminal_title)
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
        widthReel = sizeReel(context, "Width", width, LookKeys.WIDTH)
        widthCaption = dimensionCaption(context, "W")
        widthPlate = dimensionPlate(context)
        widthControl = dimensionControl(context, widthCaption, widthPlate, widthReel, widthInput)
        addControl(widthControl, sizeBoxWidth)
        heightInput = dimensionInput(context, "Height", height) { onDimensionChanged(LookKeys.HEIGHT, it) }
        heightReel = sizeReel(context, "Height", height, LookKeys.HEIGHT)
        heightCaption = dimensionCaption(context, "H")
        heightPlate = dimensionPlate(context)
        heightControl = dimensionControl(context, heightCaption, heightPlate, heightReel, heightInput)
        addControl(heightControl, sizeBoxWidth)

        // Phase 1.3: the Broadcast Lock glyph. Tap locks now. Hold arms or disarms.
        lockButton = LockGlyphButton(context).apply {
            onTap = onLockTapped
            onHold = onLockHeld
        }
        addControl(lockButton, controlWidth)
        moreButton = MoreButton(context).apply {
            contentDescription = context.getString(R.string.terminal_menu_button)
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
        // Prefer a slightly wider W/H box, but keep the title on a narrow phone.
        val paddingH = paddingLeft + paddingRight
        val gaps = sideGap * 8
        val room = available - paddingH - gaps
        // Phase 1.3 added the Lock glyph. When the six fixed controls, two boxes, and a readable
        // title no longer fit, the fixed controls and boxes narrow together, never the gaps.
        val minTitle = Hud.dpi(context, MIN_TITLE_DP)
        var control = controlWidth
        if (room - FIXED_CONTROLS * control - 2 * control < minTitle) {
            control = ((room - minTitle) / (FIXED_CONTROLS + 2)).coerceIn(Hud.dpi(context, MIN_CONTROL_DP), controlWidth)
        }
        for (view in fixedControls) resizeBox(view, control)
        val roomForTitleAndBoxes = room - FIXED_CONTROLS * control
        val box = minOf(sizeBoxWidth, maxOf(control, (roomForTitleAndBoxes - 1) / 2))
        resizeBox(widthControl, box)
        resizeBox(heightControl, box)
        widthControl.visibility = VISIBLE
        heightControl.visibility = VISIBLE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        laidOutWidth = available
    }

    private val fixedControls: List<View>
        get() = listOf(backOrb, camera, microphone, lockButton, moreButton, shrinkButton)

    private fun resizeBox(view: View, width: Int) {
        val params = view.layoutParams ?: return
        if (params.width == width) return
        params.width = width
        view.layoutParams = params
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        laidOutWidth = right - left
        // Never change textSize here. TextView.textSize is px, the setter is sp,
        // so assigning 9f/12f every pass asks for another layout forever.
        // FitTextView already shrinks the title to the reserved width.
    }

    fun setDimensions(width: Int, height: Int) {
        currentWidth = width
        currentHeight = height
        if (!widthReel.isInteracting) widthReel.setValue(width)
        if (!heightReel.isInteracting) heightReel.setValue(height)
        showTyped(widthInput, width)
        showTyped(heightInput, height)
    }

    fun setQuickToggle(key: String, on: Boolean) {
        when (key) {
            FxKeys.CAMERA_ON -> camera.setOn(on)
            FxKeys.MIC_ON -> microphone.setOn(on)
        }
    }

    fun setMicBlocked(blocked: Boolean) {
        microphone.setBlocked(blocked)
    }

    fun setLockWord(word: String) {
        lockButton.word = word
    }

    override fun applyLook(look: PanelLook) {
        titleView.setTextColor(look.text)
        widthInput.setTextColor(look.text)
        heightInput.setTextColor(look.text)
        widthCaption.setTextColor(look.text)
        heightCaption.setTextColor(look.text)
        widthPlate.setColor(look.skin)
        heightPlate.setColor(look.skin)
        val stroke = Hud.dpi(context, 1f).coerceAtLeast(1)
        widthPlate.setStroke(stroke, look.edge)
        heightPlate.setStroke(stroke, look.edge)
        val (laser, level) = laserSource()
        for (reel in listOf(widthReel, heightReel)) {
            reel.setLaserColor(laser)
            reel.setDimmer(level)
            reel.setNumberColor(look.numberColor)
            reel.setSelectedNumberColor(NumberColorGuard.colorOnBeam(look.numberColor, look.skin, laser, level))
            reel.setHapticsEnabled(look.tick)
            reel.setFontScale(1f)
        }
        camera.applyAccent(look.edge, look.outerGlow)
        microphone.applyAccent(look.edge, look.outerGlow)
    }

    private fun showViewMenu(anchor: View, onViewModeChanged: (Int) -> Unit) {
        PopupMenu(context, anchor).apply {
            menu.add("Big tiles").setOnMenuItemClickListener { onViewModeChanged(0); true }
            menu.add("List").setOnMenuItemClickListener { onViewModeChanged(1); true }
            menu.add("Grid 2").setOnMenuItemClickListener { onViewModeChanged(2); true }
            menu.add("Grid 3").setOnMenuItemClickListener { onViewModeChanged(3); true }
            PanelPopups.track(this)
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
        setTextColor(Hud.WHITE)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER
        imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
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

    private fun dimensionCaption(context: Context, caption: String) = TextView(context).apply {
        text = caption
        typeface = Hud.monoBold(context)
        textSize = 9f
        includeFontPadding = false
        setTextColor(Hud.WHITE)
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = VISIBLE
    }

    private fun dimensionPlate(context: Context) = GradientDrawable().apply {
        setColor(Hud.INK)
        cornerRadius = Hud.dp(context, 4f)
        setStroke(Hud.dpi(context, 1f).coerceAtLeast(1), NeonPalette.CYAN_GLOW)
    }

    private fun showTyped(field: EditText, value: Int) {
        if (field.hasFocus() || field.text.toString() == value.toString()) return
        field.setText(value.toString())
    }

    private fun sizeReel(context: Context, label: String, value: Int, key: String) = DrumReelView(context).apply {
        drawsOwnLaser = true
        setReelLabel(label)
        setValue(value)
        onValueChanged = { onDimensionChanged(key, it) }
        onValueSettled = { number -> onDimensionChanged(key, number) }
        onNotch = onNotch
    }

    private fun dimensionControl(
        context: Context,
        caption: TextView,
        plate: GradientDrawable,
        reel: DrumReelView,
        field: EditText,
    ) = FrameLayout(context).apply {
        // Compact tap-to-type W / H boxes on the header. Laser scroll reels stay in settings folders only.
        clipChildren = false
        val inset = Hud.dpi(context, 2f)
        background = InsetDrawable(plate, inset, inset, inset, inset)
        reel.visibility = GONE
        addView(reel, FrameLayout.LayoutParams(1, 1))
        caption.visibility = VISIBLE
        addView(
            caption,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ),
        )
        field.visibility = VISIBLE
        field.isFocusable = true
        field.isFocusableInTouchMode = true
        addView(
            field,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = Hud.dpi(context, 11f)
                bottomMargin = Hud.dpi(context, 1f)
            },
        )
        setOnClickListener {
            field.requestFocus()
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private companion object {
        const val CONTROL_HEIGHT_DP = 40f
        const val FIXED_CONTROLS = 6
        const val MIN_TITLE_DP = 72f
        const val MIN_CONTROL_DP = 28f
    }
}
