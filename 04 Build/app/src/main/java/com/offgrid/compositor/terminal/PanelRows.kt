package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.text.LineBreaker
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextPaint
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import com.offgrid.compositor.R
import kotlin.math.abs

/** A view that redraws itself in the current theme, without being rebuilt. */
internal interface Styled {
    fun applyLook(look: PanelLook)
}

/** What a plain text label is for. Each role takes a theme color, and Font size scales it. */
internal enum class TextRole { TITLE, PILL, SECTION, BACK, PATH, HEADING, COUNT, MESSAGE, LABEL }

internal class StyleTag(val role: TextRole, val baseSp: Float)

/** Marks a label so the panel can recolor and resize it when the theme changes. */
internal fun TextView.styled(role: TextRole, baseSp: Float): TextView {
    tag = StyleTag(role, baseSp)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp)
    return this
}

internal fun applyTextStyle(view: TextView, style: StyleTag, look: PanelLook) {
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.baseSp * look.fontScale)
    if (view is LaserTitleView) {
        applyLaserTitle(view, style.role, look)
        return
    }
    val glow = Hud.dp(view.context, 6f)
    when (style.role) {
        TextRole.TITLE -> {
            view.setTextColor(look.edge)
            view.setShadowLayer(glow, 0f, 0f, look.edge)
        }
        TextRole.PILL -> {
            view.setTextColor(look.decor)
            view.setShadowLayer(glow, 0f, 0f, look.decor)
        }
        TextRole.SECTION, TextRole.BACK -> view.setTextColor(look.decor)
        TextRole.PATH -> view.setTextColor(Hud.withAlpha(look.sideText, 0.6f))
        TextRole.HEADING -> {
            view.setTextColor(look.sideText)
            view.setShadowLayer(glow, 0f, 0f, look.folder)
        }
        TextRole.COUNT -> view.setTextColor(look.folder)
        TextRole.MESSAGE -> view.setTextColor(look.text)
        TextRole.LABEL -> view.setTextColor(look.sideText)
    }
}

/**
 * Titles are Laser Jet Bright: a bright core with layers of glow, in the typed text color.
 * Holo Classic keeps its Step 2 colors.
 */
private fun applyLaserTitle(view: LaserTitleView, role: TextRole, look: PanelLook) {
    val strength = look.glowStrength
    val boost = look.glowBoost
    when (role) {
        TextRole.TITLE -> view.setLaser(if (look.holo) look.edge else look.text, null, strength, boost)
        TextRole.PILL -> view.setLaser(if (look.holo) look.decor else look.text, null, strength, boost)
        TextRole.HEADING -> view.setLaser(
            if (look.holo) look.sideText else look.text,
            if (look.holo) look.folder else null,
            strength,
            boost,
        )
        TextRole.SECTION -> view.setLaser(if (look.holo) look.decor else look.text, null, strength * 0.6f, boost * 0.6f)
        else -> view.setLaser(look.text, null, strength, boost)
    }
}

/** How round boxes and buttons are. Corner roundness 0 is square. The start look is nearly square. */
internal fun chromeRadius(context: Context, look: PanelLook): Float = Hud.dp(context, 0.5f + 10f * look.roundness)

internal fun itemsText(context: Context, count: Int): String {
    return context.resources.getQuantityString(R.plurals.terminal_items, count, count)
}

/** A soft glow when a tile or row is pressed. */
internal fun pressGlow(context: Context, color: Int, radius: Float = Hud.dp(context, 2f)): Drawable {
    val mask = GradientDrawable().apply {
        setColor(Hud.WHITE)
        cornerRadius = radius
    }
    return RippleDrawable(ColorStateList.valueOf(Hud.withAlpha(color, 0.28f)), null, mask)
}

internal fun rowTitle(context: Context, text: String) = FitTextView(context).apply {
    this.text = text
    typeface = Hud.condensedBold
}

internal fun rowNote(context: Context, text: String) = FitTextView(context).apply {
    this.text = text
    typeface = Hud.mono(context)
    visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
}

internal fun styleRowWords(title: TextView, note: TextView, look: PanelLook) {
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * look.fontScale)
    title.setTextColor(look.sideText)
    note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f * look.fontScale)
    note.setTextColor(Hud.withAlpha(look.sideText, 0.62f))
}

/** A big glowing folder in a grid, with its name under it. */
internal class FolderTile(context: Context, private val folder: PanelFolder, iconDp: Float) : LinearLayout(context), Styled {
    private val ring = HoloRingView(context, folder.special)
    private val name = FitTextView(context)
    private var active = false
    private var look: PanelLook? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(0, 0, 0, Hud.dpi(context, 6f))
        val ringSize = Hud.dpi(context, iconDp * 1.34f)
        val frame = FrameLayout(context)
        ring.visibility = INVISIBLE
        frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, folder.itemCount, folder.tag, compact = false, special = folder.special),
            FrameLayout.LayoutParams(Hud.dpi(context, iconDp), Hud.dpi(context, iconDp * 0.86f), Gravity.CENTER),
        )
        addView(frame, LayoutParams(ringSize, ringSize))
        name.apply {
            text = folder.title
            typeface = Hud.condensed
            gravity = Gravity.CENTER_HORIZONTAL
        }
        addView(name, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(
            R.string.terminal_folder_description,
            folder.title,
            itemsText(context, folder.itemCount),
        )
    }

    fun setActive(value: Boolean) {
        active = value
        look?.let { applyLook(it) }
    }

    override fun applyLook(look: PanelLook) {
        this.look = look
        foreground = pressGlow(context, look.edge)
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f * look.fontScale)
        name.setTextColor(if (active) look.decor else look.sideText)
        val showRing = active && look.ring
        ring.visibility = if (showRing) VISIBLE else INVISIBLE
        ring.spinning = showRing
    }
}

/**
 * One line in the side list: a small folder, its name, and how many items it holds.
 * If a word in the name is wider than the room, the name gets a little smaller,
 * so words never break in the middle.
 */
internal class SideRow(context: Context, folder: PanelFolder) : LinearLayout(context), Styled {
    private val bar = View(context)
    private val ring = HoloRingView(context, folder.special)
    private val name = TextView(context)
    private val count = TextView(context)
    private var active = false
    private var look: PanelLook? = null
    private var fullTextPx = 0f
    private val probe = TextPaint()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 2f), Hud.dpi(context, 4f), Hud.dpi(context, 2f))
        bar.visibility = INVISIBLE
        addView(bar, LayoutParams(Hud.dpi(context, 2f), Hud.dpi(context, 22f)))
        val frame = FrameLayout(context)
        val ringSize = Hud.dpi(context, 27f)
        ring.visibility = INVISIBLE
        frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, folder.itemCount, null, compact = true, special = folder.special),
            FrameLayout.LayoutParams(Hud.dpi(context, 17f), Hud.dpi(context, 15f), Gravity.CENTER),
        )
        addView(frame, LayoutParams(ringSize, ringSize).apply { marginStart = Hud.dpi(context, 2f) })
        name.apply {
            text = folder.label
            typeface = Hud.condensed
            // Break lines only between words. Long names take another line instead of "…".
            breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        }
        addView(name, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 3f) })
        count.apply {
            text = folder.itemCount.toString()
            typeface = Hud.mono(context)
        }
        addView(count, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = Hud.dpi(context, 4f)
        })
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(
            R.string.terminal_folder_description,
            folder.title,
            itemsText(context, folder.itemCount),
        )
    }

    fun setActive(value: Boolean) {
        active = value
        look?.let { applyLook(it) }
    }

    override fun applyLook(look: PanelLook) {
        this.look = look
        foreground = pressGlow(context, look.edge)
        bar.setBackgroundColor(look.decor)
        bar.visibility = if (active) VISIBLE else INVISIBLE
        val full = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10.5f * look.fontScale, resources.displayMetrics)
        if (full != fullTextPx) {
            fullTextPx = full
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, full)
        }
        name.setTextColor(if (active) look.decor else look.sideText)
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f * look.fontScale)
        count.setTextColor(Hud.withAlpha(look.folder, 0.85f))
        setBackgroundColor(if (active) Hud.withAlpha(look.edge, 0.08f) else 0)
        val showRing = active && look.ring
        ring.visibility = if (showRing) VISIBLE else INVISIBLE
        ring.spinning = showRing
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val room = name.measuredWidth - name.totalPaddingLeft - name.totalPaddingRight
        if (fullTextPx <= 0f || room <= 0) return
        probe.set(name.paint)
        probe.textSize = fullTextPx
        val widest = widestWord(name.text)
        val fitted = if (widest <= room * ROOM_SHARE) {
            fullTextPx
        } else {
            maxOf(fullTextPx * room * ROOM_SHARE / widest, fullTextPx * SMALLEST_SHARE)
        }
        if (abs(fitted - name.textSize) > 0.5f) {
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, fitted)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun widestWord(text: CharSequence): Float {
        var widest = 0f
        var start = 0
        for (index in 0..text.length) {
            if (index == text.length || text[index] == ' ') {
                if (index > start) widest = maxOf(widest, probe.measureText(text, start, index))
                start = index + 1
            }
        }
        return widest
    }

    private companion object {
        /** A little room to spare, so a word never lands exactly on the edge. */
        const val ROOM_SHARE = 0.95f
        const val SMALLEST_SHARE = 0.7f
    }
}

/** A label, a short note, and the one shared toggle. */
internal class ToggleRow(
    context: Context,
    item: ToggleItem,
    note: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val words = rowNote(context, note)
    val toggle = PowerToggleView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 3f), 0, Hud.dpi(context, 3f))
        val column = LinearLayout(context).apply { orientation = VERTICAL }
        column.addView(title)
        column.addView(words)
        addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 6f) })
        toggle.setOn(on)
        toggle.contentDescription = item.label
        toggle.onToggled = onChange
        addView(toggle)
    }

    override fun applyLook(look: PanelLook) = styleRowWords(title, words, look)
}

/**
 * A typed whole number from 0 to 100. Never a slider.
 * A good number is used right away. Done, or leaving the box, checks it and says the result.
 * Wrong numbers get a plain message and change nothing.
 */
internal class NumberBox(
    context: Context,
    private val label: String,
    start: Int,
    private val onChange: (value: Int, announce: Boolean) -> Unit,
    private val onProblem: (String) -> Unit,
    private val baseSp: Float = 15f,
) : AppCompatEditText(context), Styled {
    var value = start
        private set
    private var announced = start
    private var quiet = false
    private var tick = false

    init {
        setText(start.toString())
        typeface = Hud.monoBold(context)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER
        imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        filters = arrayOf(InputFilter.LengthFilter(3))
        maxLines = 1
        setSelectAllOnFocus(true)
        setPadding(Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f), 0)
        contentDescription = context.getString(R.string.terminal_number_description, label)
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit()
                clearFocus()
                context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
                true
            } else {
                false
            }
        }
        setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commit() }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (quiet) return
                val typed = s?.toString()?.toIntOrNull() ?: return
                if (typed in 0..100 && typed != value) {
                    value = typed
                    onChange(typed, false)
                }
            }
        })
    }

    /** Shows a new value without telling anyone. */
    fun show(number: Int) {
        quiet = true
        value = number
        announced = number
        setText(number.toString())
        quiet = false
    }

    private fun commit() {
        val typed = text?.toString()?.trim().orEmpty()
        val number = typed.toIntOrNull()
        when {
            number == null -> {
                revert()
                onProblem(context.getString(R.string.terminal_number_empty))
            }
            number > 100 -> {
                revert()
                onProblem(context.getString(R.string.terminal_number_too_big, number))
            }
            else -> {
                if (number != value) {
                    value = number
                    onChange(number, false)
                }
                if (typed != number.toString()) {
                    quiet = true
                    setText(number.toString())
                    quiet = false
                }
                if (value != announced) {
                    announced = value
                    onChange(value, true)
                    if (tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
            }
        }
    }

    /**
     * Puts back the last saved number. Part of a wrong number, like 15 on the way to 150,
     * was already tried while typing, so that is undone too. Nothing changes.
     */
    private fun revert() {
        val tried = value
        show(announced)
        if (tried != announced) onChange(announced, false)
    }

    override fun applyLook(look: PanelLook) {
        tick = look.tick
        setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * look.fontScale)
        setTextColor(look.edge)
        highlightColor = Hud.withAlpha(look.edge, 0.35f)
        background = fieldBackground(context, look)
        textCursorDrawable = GradientDrawable().apply {
            setColor(look.cursor)
            setSize(Hud.dpi(context, 2f), Hud.dpi(context, 18f))
        }
    }
}

/** The dark box behind typed numbers and color codes. It brightens while typing. */
internal fun fieldBackground(context: Context, look: PanelLook): Drawable {
    fun box(stroke: Int, width: Float) = GradientDrawable().apply {
        setColor(Hud.withAlpha(Hud.blend(look.skin, Hud.INK, 0.6f), 0.92f))
        cornerRadius = chromeRadius(context, look)
        setStroke(Hud.dpi(context, width), stroke)
    }
    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), box(look.decor, 2f))
        addState(intArrayOf(), box(Hud.withAlpha(look.edge, 0.75f), 1.3f))
    }
}

/** A label, a short note, and a typed number box. */
internal class NumberRow(
    context: Context,
    item: NumberItem,
    start: Int,
    onChange: (value: Int, announce: Boolean) -> Unit,
    onProblem: (String) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val words = rowNote(context, item.help)

    /** The typed box. The panel updates it when the same number changes somewhere else. */
    val box = NumberBox(context, item.label, start, onChange, onProblem)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        // Room inside the edge, so the typing glow along the edge never touches the words.
        setPadding(Hud.dpi(context, 6f), Hud.dpi(context, 4f), Hud.dpi(context, 6f), Hud.dpi(context, 4f))
        val column = LinearLayout(context).apply { orientation = VERTICAL }
        column.addView(title)
        column.addView(words)
        addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 6f) })
        addView(box, LayoutParams(Hud.dpi(context, 68f), Hud.dpi(context, 46f)))
    }

    override fun applyLook(look: PanelLook) = styleRowWords(title, words, look)
}

/**
 * A color code box, like #00FF66. Any text can be typed. Done checks it.
 * A wrong code gets a plain message and changes nothing.
 */
internal class HexBox(
    context: Context,
    private val onColor: (Int) -> Unit,
    private val onProblem: (String) -> Unit,
) : AppCompatEditText(context), Styled {
    private var shownText = ""
    private var tick = false

    init {
        typeface = Hud.monoBold(context)
        gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        filters = arrayOf(InputFilter.LengthFilter(9))
        maxLines = 1
        setSelectAllOnFocus(true)
        setPadding(Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f), 0)
        contentDescription = context.getString(R.string.terminal_hex_description)
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit()
                clearFocus()
                context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
                true
            } else {
                false
            }
        }
        setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commit() }
    }

    fun show(code: String) {
        shownText = code
        setText(code)
    }

    private fun commit() {
        val typed = text?.toString()?.trim().orEmpty()
        if (typed.equals(shownText, ignoreCase = true)) {
            setText(shownText)
            return
        }
        val color = RainbowMix.parseHex(typed)
        if (color == null) {
            setText(shownText)
            onProblem(context.getString(R.string.terminal_hex_wrong))
            return
        }
        shownText = RainbowMix.hex(color)
        setText(shownText)
        onColor(color)
        if (tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    override fun applyLook(look: PanelLook) {
        tick = look.tick
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * look.fontScale)
        setTextColor(look.edge)
        highlightColor = Hud.withAlpha(look.edge, 0.35f)
        background = fieldBackground(context, look)
        textCursorDrawable = GradientDrawable().apply {
            setColor(look.cursor)
            setSize(Hud.dpi(context, 2f), Hud.dpi(context, 18f))
        }
    }
}

/** A plain HUD button, like Reset or Reset all. */
internal class ActionButton(context: Context, label: String) : androidx.appcompat.widget.AppCompatTextView(context), Styled {
    init {
        text = label
        typeface = Hud.monoBold(context)
        gravity = Gravity.CENTER
        minHeight = Hud.dpi(context, 46f)
        setPadding(Hud.dpi(context, 12f), Hud.dpi(context, 8f), Hud.dpi(context, 12f), Hud.dpi(context, 8f))
        isClickable = true
        isFocusable = true
    }

    override fun applyLook(look: PanelLook) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * look.fontScale)
        setTextColor(look.edge)
        val shape = GradientDrawable().apply {
            setColor(Hud.withAlpha(Hud.blend(look.skin, Hud.INK, 0.5f), 0.85f))
            cornerRadius = chromeRadius(context, look)
            setStroke(Hud.dpi(context, 1.4f), look.edge)
        }
        background = RippleDrawable(ColorStateList.valueOf(Hud.withAlpha(look.edge, 0.35f)), shape, null)
    }
}

/** A small square that shows a color, glowing a little in that color. */
internal class SwatchView(context: Context) : View(context), Styled {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private var edge = Hud.WHITE

    var color = Hud.INK
        set(value) {
            field = value
            invalidate()
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        edge = look.edge
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val pad = Hud.dp(context, 5f)
        box.set(pad, pad, width - pad, height - pad)
        val corner = Hud.dp(context, 2f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Hud.dp(context, 6f)
        paint.color = Hud.withAlpha(color, 0.25f)
        canvas.drawRoundRect(box, corner, corner, paint)
        paint.style = Paint.Style.FILL
        paint.color = color or 0xFF000000.toInt()
        canvas.drawRoundRect(box, corner, corner, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Hud.dp(context, 1.2f)
        paint.color = Hud.withAlpha(edge, 0.8f)
        canvas.drawRoundRect(box, corner, corner, paint)
    }
}

/** A tiny picture of a theme: its glass, edge, glow, and text colors. */
internal class ThemeChip(context: Context, private val colors: IntArray) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        val pad = Hud.dp(context, 6f)
        box.set(pad, pad, width - pad, height - pad)
        val corner = Hud.dp(context, 2f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Hud.dp(context, 7f)
        paint.color = Hud.withAlpha(colors[2], 0.3f)
        canvas.drawRoundRect(box, corner, corner, paint)
        paint.style = Paint.Style.FILL
        paint.color = colors[0]
        canvas.drawRoundRect(box, corner, corner, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Hud.dp(context, 1.5f)
        paint.color = colors[1]
        canvas.drawRoundRect(box, corner, corner, paint)
        paint.style = Paint.Style.FILL
        paint.color = colors[3]
        val line = box.height() / 6f
        for (row in 0 until 3) {
            val y = box.top + line * (1.5f + row * 1.4f)
            val end = box.left + box.width() * (0.75f - 0.18f * row)
            canvas.drawRect(box.left + line, y, end, y + line * 0.55f, paint)
        }
    }
}

/** One theme in the Themes folder. Tap it to apply it. */
internal class ThemeRow(context: Context, val theme: Theme, colors: IntArray, inUse: Boolean) : LinearLayout(context), Styled {
    private val title = rowTitle(context, theme.title)
    private val words = rowNote(context, theme.blurb)
    private val mark = TextView(context)

    /** True for the theme the panel started from. */
    var inUse = inUse
        set(value) {
            field = value
            mark.text = context.getString(if (value) R.string.terminal_theme_in_use else R.string.terminal_theme_tap)
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f))
        addView(ThemeChip(context, colors), LayoutParams(Hud.dpi(context, 56f), Hud.dpi(context, 46f)))
        val column = LinearLayout(context).apply { orientation = VERTICAL }
        column.addView(title)
        column.addView(words)
        mark.text = context.getString(if (inUse) R.string.terminal_theme_in_use else R.string.terminal_theme_tap)
        mark.typeface = Hud.mono(context)
        column.addView(mark)
        addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 8f) })
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.terminal_theme_description, theme.title, theme.blurb)
    }

    override fun applyLook(look: PanelLook) {
        styleRowWords(title, words, look)
        mark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f * look.fontScale)
        mark.setTextColor(look.decor)
        foreground = pressGlow(context, look.edge)
    }
}

/**
 * Lays tiles out in as many columns as fit.
 * [evenRows] uses a column count that fills every row, like 2 or 3 columns for 6 boxes.
 */
internal class TileGrid(
    context: Context,
    private val minCellDp: Float,
    private val evenRows: Boolean = false,
) : ViewGroup(context) {
    private var columns = 1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inner = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        columns = (inner / Hud.dp(context, minCellDp)).toInt().coerceAtLeast(1)
        if (evenRows && childCount > 0) {
            // Rows that are all full, like 3 by 3 for nine boxes. If only one column would
            // do that, keep the columns that fit, so nine boxes never become one long column.
            val even = (columns.coerceAtMost(childCount) downTo 1).first { childCount % it == 0 }
            columns = if (even == 1) columns.coerceAtMost(childCount) else even
        }
        val cell = inner / columns
        var height = paddingTop + paddingBottom
        var rowHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            child.measure(
                MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            rowHeight = maxOf(rowHeight, child.measuredHeight)
            if (index % columns == columns - 1 || index == childCount - 1) {
                height += rowHeight
                rowHeight = 0
            }
        }
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val inner = (r - l - paddingLeft - paddingRight).coerceAtLeast(0)
        val cell = inner / columns
        var y = paddingTop
        var rowHeight = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val column = index % columns
            val x = paddingLeft + column * cell
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            rowHeight = maxOf(rowHeight, child.measuredHeight)
            if (column == columns - 1 || index == childCount - 1) {
                y += rowHeight
                rowHeight = 0
            }
        }
    }
}

/** Six small dots that show where to grab the panel. */
internal class GripView(context: Context) : View(context), Styled {
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        dot.color = Hud.withAlpha(look.edge, 0.8f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val radius = Hud.dp(context, 1.6f)
        for (row in 0 until 3) {
            for (column in 0 until 2) {
                canvas.drawCircle(width * (0.3f + 0.4f * column), height * (0.2f + 0.3f * row), radius, dot)
            }
        }
    }
}

/** The small button that shrinks the panel to a glowing tag. */
internal class ShrinkButton(context: Context) : View(context), Styled {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private var frameColor = Hud.WHITE
    private var dashColor = Hud.WHITE

    init {
        isClickable = true
        isFocusable = true
    }

    override fun applyLook(look: PanelLook) {
        frameColor = Hud.withAlpha(look.edge, 0.7f)
        dashColor = look.decor
        foreground = pressGlow(context, look.edge)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height) * 0.62f
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        stroke.strokeWidth = Hud.dp(context, 1.2f)
        stroke.color = frameColor
        canvas.drawRoundRect(left, top, left + size, top + size, Hud.dp(context, 1f), Hud.dp(context, 1f), stroke)
        stroke.strokeWidth = Hud.dp(context, 2f)
        stroke.color = dashColor
        canvas.drawLine(left + size * 0.25f, top + size * 0.62f, left + size * 0.75f, top + size * 0.62f, stroke)
    }
}

/** Three short lines in the bottom right corner. Drag them to make the panel bigger or smaller. */
internal class ResizeGrip(context: Context) : View(context), Styled {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.terminal_resize)
    }

    override fun applyLook(look: PanelLook) {
        stroke.color = look.decor
        invalidate()
    }

    /** A tap on the corner says how to use it. A drag resizes the panel. */
    override fun performClick(): Boolean = super.performClick()

    override fun onDraw(canvas: Canvas) {
        val right = width - Hud.dp(context, 7f)
        val bottom = height - Hud.dp(context, 7f)
        stroke.strokeWidth = Hud.dp(context, 1.8f)
        for (step in 1..3) {
            val reach = Hud.dp(context, 5.5f) * step
            stroke.alpha = 120 + 45 * step
            canvas.drawLine(right - reach, bottom, right, bottom - reach, stroke)
        }
    }
}

/** A thin line of light between parts of the panel. It fades out at both ends. */
internal class GlowRule(context: Context, private val vertical: Boolean, private val decor: Boolean) : View(context), Styled {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        val color = Hud.withAlpha(if (decor) look.decor else look.edge, 0.75f)
        background = GradientDrawable(
            if (vertical) GradientDrawable.Orientation.TOP_BOTTOM else GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0, color, color, 0),
        )
    }
}

/** The band of light that sweeps down an opening folder once. */
internal class SweepBand(context: Context) : View(context), Styled {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun applyLook(look: PanelLook) {
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0, Hud.withAlpha(look.edge, 0.32f), 0),
        )
    }
}

/** A small typed box at the top of the panel, for Width or Height, with its name above it. */
internal class SizeBox(
    context: Context,
    caption: String,
    label: String,
    start: Int,
    onChange: (value: Int, announce: Boolean) -> Unit,
    onProblem: (String) -> Unit,
) : LinearLayout(context), Styled {
    private val name = TextView(context)

    /** The typed box, 0 to 100. */
    val box = NumberBox(context, label, start, onChange, onProblem, baseSp = 12f)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        name.text = caption
        name.typeface = Hud.monoBold(context)
        name.letterSpacing = 0.08f
        name.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        // A small gap, so the typing glow around the box never touches the name.
        addView(name, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = Hud.dpi(context, 2f)
        })
        addView(box, LayoutParams(Hud.dpi(context, 48f), Hud.dpi(context, 36f)))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // A tap on the name above the box opens the box too, so the whole spot is easy to hit.
        touchDelegate = TouchDelegate(Rect(0, 0, w, h), box)
    }

    override fun applyLook(look: PanelLook) {
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7.5f)
        name.setTextColor(Hud.withAlpha(look.sideText, 0.75f))
    }
}

/**
 * Typing glow: the edge of the setting being typed in lights softly.
 * A setting row glows along its own edge. A box with no row of its own, like Width at the
 * top or a color mixer box, gets a soft halo hugging the box. It never covers any words.
 */
internal class FocusGlowDrawable(
    private val density: Float,
    color: Int,
    private val radius: Float,
    private val target: View,
    private val wholeRow: Boolean,
) : Drawable() {
    private val box = RectF()
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
        this.color = Hud.withAlpha(color, 0.85f)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        this.color = Hud.withAlpha(color, 0.9f)
    }

    override fun draw(canvas: Canvas) {
        val corner: Float
        if (wholeRow) {
            val inset = ROW_INSET_DP * density
            box.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)
            corner = radius
        } else {
            val out = HALO_DP * density
            box.set(target.left - out, target.top - out, target.right + out, target.bottom + out)
            corner = radius + out
        }
        if (box.width() <= 0f || box.height() <= 0f) return
        canvas.drawRoundRect(box, corner, corner, glow)
        canvas.drawRoundRect(box, corner, corner, line)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    private companion object {
        /** How far the halo line sits outside the box. */
        const val HALO_DP = 2.5f

        /** How far the row's glow line sits inside the row's edge. Rows keep their words farther in. */
        const val ROW_INSET_DP = 1.5f
    }
}
