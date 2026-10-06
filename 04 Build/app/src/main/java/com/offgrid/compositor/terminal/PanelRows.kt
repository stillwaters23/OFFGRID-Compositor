package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R

internal fun accentFor(kind: FolderKind): Int = when (kind) {
    FolderKind.SETTINGS, FolderKind.PREMIUM -> Hud.GOLD
    FolderKind.PLATFORM, FolderKind.EFFECTS, FolderKind.TESTS -> Hud.CYAN
}

internal fun itemsText(context: Context, count: Int): String {
    return context.resources.getQuantityString(R.plurals.terminal_items, count, count)
}

/** A soft cyan glow when a tile or row is pressed. */
internal fun pressGlow(context: Context): Drawable {
    val mask = GradientDrawable().apply {
        setColor(0xFFFFFFFF.toInt())
        cornerRadius = Hud.dp(context, 6f)
    }
    return RippleDrawable(ColorStateList.valueOf(Hud.withAlpha(Hud.CYAN, 0.28f)), null, mask)
}

/** A big glowing folder in a grid, with its name under it. */
internal class FolderTile(context: Context, folder: PanelFolder, iconDp: Float) : LinearLayout(context) {
    private val ring: HoloRingView
    private val name: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(0, 0, 0, Hud.dpi(context, 6f))
        val accent = accentFor(folder.kind)
        val ringSize = Hud.dpi(context, iconDp * 1.34f)
        val frame = FrameLayout(context)
        ring = HoloRingView(context, accent).apply { visibility = INVISIBLE }
        frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, accent, folder.itemCount, folder.tag, compact = false),
            FrameLayout.LayoutParams(Hud.dpi(context, iconDp), Hud.dpi(context, iconDp * 0.86f), Gravity.CENTER),
        )
        addView(frame, LayoutParams(ringSize, ringSize))
        name = TextView(context).apply {
            text = folder.title
            typeface = Hud.condensed
            textSize = 10.5f
            setTextColor(Hud.TEXT)
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(Hud.dp(context, 3f), 0f, 0f, Hud.INK)
        }
        addView(name, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isClickable = true
        isFocusable = true
        foreground = pressGlow(context)
        contentDescription = context.getString(
            R.string.terminal_folder_description,
            folder.title,
            itemsText(context, folder.itemCount),
        )
    }

    fun setActive(active: Boolean) {
        ring.visibility = if (active) VISIBLE else INVISIBLE
        ring.spinning = active
        name.setTextColor(if (active) Hud.GOLD else Hud.TEXT)
    }
}

/** One line in the side list: a small folder, its name, and how many items it holds. */
internal class SideRow(context: Context, folder: PanelFolder) : LinearLayout(context) {
    private val bar = View(context)
    private val ring: HoloRingView
    private val name: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 2f), Hud.dpi(context, 4f), Hud.dpi(context, 2f))
        val accent = accentFor(folder.kind)
        bar.setBackgroundColor(Hud.GOLD)
        bar.visibility = INVISIBLE
        addView(bar, LayoutParams(Hud.dpi(context, 2f), Hud.dpi(context, 22f)))
        val frame = FrameLayout(context)
        val ringSize = Hud.dpi(context, 27f)
        ring = HoloRingView(context, accent).apply { visibility = INVISIBLE }
        frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, accent, folder.itemCount, null, compact = true),
            FrameLayout.LayoutParams(Hud.dpi(context, 17f), Hud.dpi(context, 15f), Gravity.CENTER),
        )
        addView(frame, LayoutParams(ringSize, ringSize).apply { marginStart = Hud.dpi(context, 2f) })
        name = TextView(context).apply {
            text = folder.label
            typeface = Hud.condensed
            textSize = 10.5f
            setTextColor(Hud.TEXT)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        addView(name, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 3f) })
        val count = TextView(context).apply {
            text = folder.itemCount.toString()
            typeface = Hud.mono(context)
            textSize = 10f
            setTextColor(Hud.withAlpha(accent, 0.85f))
        }
        addView(count, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = Hud.dpi(context, 4f)
        })
        isClickable = true
        isFocusable = true
        foreground = pressGlow(context)
        contentDescription = context.getString(
            R.string.terminal_folder_description,
            folder.title,
            itemsText(context, folder.itemCount),
        )
    }

    fun setActive(active: Boolean) {
        bar.visibility = if (active) VISIBLE else INVISIBLE
        ring.visibility = if (active) VISIBLE else INVISIBLE
        ring.spinning = active
        name.setTextColor(if (active) Hud.GOLD else Hud.TEXT)
        setBackgroundColor(if (active) Hud.withAlpha(Hud.CYAN, 0.08f) else 0)
    }
}

/** A label, a short note, and the one shared toggle. */
internal class ToggleRow(
    context: Context,
    item: ToggleItem,
    note: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 3f), 0, Hud.dpi(context, 3f))
        val words = LinearLayout(context).apply { orientation = VERTICAL }
        words.addView(rowTitle(context, item.label))
        words.addView(rowNote(context, note))
        addView(words, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 6f) })
        val toggle = GlowToggleView(context)
        toggle.setOn(on)
        toggle.contentDescription = item.label
        toggle.onToggled = onChange
        addView(toggle)
    }
}

/**
 * A typed whole number from 0 to 100. Never a slider.
 * A good number is used right away. Done, or leaving the box, checks it and says the result.
 */
internal class NumberRow(
    context: Context,
    private val item: NumberItem,
    start: Int,
    private val onChange: (value: Int, announce: Boolean) -> Unit,
    private val onProblem: (String) -> Unit,
) : LinearLayout(context) {
    private val field = EditText(context)
    private var current = start
    private var announced = start

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Hud.dpi(context, 3f), 0, Hud.dpi(context, 3f))
        val words = LinearLayout(context).apply { orientation = VERTICAL }
        words.addView(rowTitle(context, item.label))
        words.addView(rowNote(context, item.help))
        addView(words, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 6f) })
        field.apply {
            setText(start.toString())
            typeface = Hud.monoBold(context)
            textSize = 15f
            setTextColor(Hud.CYAN)
            setHintTextColor(Hud.TEXT_DIM)
            highlightColor = Hud.withAlpha(Hud.CYAN, 0.35f)
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN
            filters = arrayOf(InputFilter.LengthFilter(3))
            maxLines = 1
            setSelectAllOnFocus(true)
            background = fieldBackground(context)
            textCursorDrawable = GradientDrawable().apply {
                setColor(Hud.CYAN)
                setSize(Hud.dpi(context, 2f), Hud.dpi(context, 18f))
            }
            setPadding(Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f), 0)
            contentDescription = context.getString(R.string.terminal_number_description, item.label)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    commit()
                    clearFocus()
                    context.getSystemService(InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(windowToken, 0)
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
                    val value = s?.toString()?.toIntOrNull() ?: return
                    if (value in 0..100 && value != current) {
                        current = value
                        onChange(value, false)
                    }
                }
            })
        }
        addView(field, LayoutParams(Hud.dpi(context, 68f), Hud.dpi(context, 46f)))
    }

    private fun commit() {
        val typed = field.text.toString().trim()
        val value = typed.toIntOrNull()
        when {
            value == null -> {
                field.setText(current.toString())
                onProblem(context.getString(R.string.terminal_number_empty))
            }
            value > 100 -> {
                field.setText(current.toString())
                onProblem(context.getString(R.string.terminal_number_too_big, value))
            }
            else -> {
                if (value != current) {
                    current = value
                    onChange(value, false)
                }
                if (typed != value.toString()) {
                    field.setText(value.toString())
                }
                if (current != announced) {
                    announced = current
                    onChange(current, true)
                }
            }
        }
    }

    private fun fieldBackground(context: Context): Drawable {
        fun box(stroke: Int) = GradientDrawable().apply {
            setColor(Hud.withAlpha(0xFF06141A.toInt(), 0.9f))
            cornerRadius = Hud.dp(context, 5f)
            setStroke(Hud.dpi(context, 1.3f), stroke)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), box(Hud.GOLD))
            addState(intArrayOf(), box(Hud.withAlpha(Hud.CYAN, 0.75f)))
        }
    }
}

private fun rowTitle(context: Context, text: String) = TextView(context).apply {
    this.text = text
    typeface = Hud.condensedBold
    textSize = 12.5f
    setTextColor(Hud.TEXT)
    setShadowLayer(Hud.dp(context, 3f), 0f, 0f, Hud.INK)
}

private fun rowNote(context: Context, text: String) = TextView(context).apply {
    this.text = text
    typeface = Hud.mono(context)
    textSize = 9.5f
    setTextColor(Hud.TEXT_DIM)
    setShadowLayer(Hud.dp(context, 3f), 0f, 0f, Hud.INK)
    visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
}

/** Lays tiles out in as many columns as fit. */
internal class TileGrid(context: Context, private val minCellDp: Float) : ViewGroup(context) {
    private var columns = 1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inner = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        columns = (inner / Hud.dp(context, minCellDp)).toInt().coerceAtLeast(1)
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
internal class GripView(context: Context) : View(context) {
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.withAlpha(Hud.CYAN, 0.8f) }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        val radius = Hud.dp(context, 1.6f)
        for (row in 0 until 3) {
            for (column in 0 until 2) {
                val x = width * (0.3f + 0.4f * column)
                val y = height * (0.2f + 0.3f * row)
                canvas.drawCircle(x, y, radius, dot)
            }
        }
    }
}

/** The small button that shrinks the panel to a glowing tag. */
internal class ShrinkButton(context: Context) : View(context) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        isClickable = true
        isFocusable = true
        foreground = pressGlow(context)
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width, height) * 0.62f
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        stroke.strokeWidth = Hud.dp(context, 1.2f)
        stroke.color = Hud.withAlpha(Hud.CYAN, 0.7f)
        canvas.drawRoundRect(left, top, left + size, top + size, Hud.dp(context, 3f), Hud.dp(context, 3f), stroke)
        stroke.strokeWidth = Hud.dp(context, 2f)
        stroke.color = Hud.GOLD
        canvas.drawLine(left + size * 0.25f, top + size * 0.62f, left + size * 0.75f, top + size * 0.62f, stroke)
    }
}
