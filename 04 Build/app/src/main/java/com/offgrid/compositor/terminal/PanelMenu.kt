package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import com.offgrid.compositor.R

/** The ⋮ button in the title bar: three dots, top to bottom. It opens the layout menu. */
internal class MoreButton(context: Context) : View(context), Styled {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameColor = Hud.WHITE

    init {
        isClickable = true
        isFocusable = true
    }

    override fun applyLook(look: PanelLook) {
        frameColor = Hud.withAlpha(look.edge, 0.7f)
        dot.color = look.decor
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
        val radius = Hud.dp(context, 1.7f)
        for (step in 0..2) {
            canvas.drawCircle(width / 2f, top + size * (0.27f + 0.23f * step), radius, dot)
        }
    }
}

/** A small picture of each choice in the ⋮ menu, drawn with lines. */
internal class ChoiceGlyph(context: Context, private val kind: Kind) : View(context), Styled {
    enum class Kind { LIST, BIG_LIST, GRID_2, GRID_3, IMPORTANT, A_TO_Z, CUSTOM, SIDE_SHOW, SIDE_SWIPE, SIDE_HIDDEN }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val words = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        words.typeface = Hud.monoBold(context)
    }

    override fun applyLook(look: PanelLook) {
        paint.color = look.edge
        words.color = look.edge
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        val x0 = (width - s) / 2f
        val y0 = (height - s) / 2f
        val line = s * 0.08f
        paint.style = Paint.Style.FILL
        fun bar(left: Float, top: Float, right: Float, bottom: Float) {
            box.set(x0 + s * left, y0 + s * top, x0 + s * right, y0 + s * bottom)
            canvas.drawRect(box, paint)
        }
        fun frame(left: Float, top: Float, right: Float, bottom: Float) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = line
            box.set(x0 + s * left, y0 + s * top, x0 + s * right, y0 + s * bottom)
            canvas.drawRect(box, paint)
            paint.style = Paint.Style.FILL
        }
        when (kind) {
            Kind.LIST -> for (row in 0..2) {
                bar(0.1f, 0.18f + row * 0.26f, 0.26f, 0.32f + row * 0.26f)
                bar(0.34f, 0.22f + row * 0.26f, 0.9f, 0.28f + row * 0.26f)
            }
            Kind.BIG_LIST -> for (row in 0..1) {
                bar(0.1f, 0.14f + row * 0.4f, 0.4f, 0.46f + row * 0.4f)
                bar(0.48f, 0.26f + row * 0.4f, 0.9f, 0.34f + row * 0.4f)
            }
            Kind.GRID_2 -> for (row in 0..1) for (col in 0..1) {
                bar(0.12f + col * 0.42f, 0.12f + row * 0.42f, 0.46f + col * 0.42f, 0.46f + row * 0.42f)
            }
            Kind.GRID_3 -> for (row in 0..2) for (col in 0..2) {
                bar(0.1f + col * 0.29f, 0.1f + row * 0.29f, 0.32f + col * 0.29f, 0.32f + row * 0.29f)
            }
            Kind.IMPORTANT -> {
                bar(0.1f, 0.16f, 0.9f, 0.3f)
                bar(0.1f, 0.43f, 0.7f, 0.55f)
                bar(0.1f, 0.68f, 0.5f, 0.78f)
            }
            Kind.A_TO_Z -> {
                words.textSize = s * 0.42f
                canvas.drawText("AZ", width / 2f, y0 + s * 0.66f, words)
            }
            Kind.CUSTOM -> for (row in 0..2) {
                bar(0.1f, 0.2f + row * 0.26f, 0.9f, 0.28f + row * 0.26f)
                canvas.drawCircle(x0 + s * 0.9f, y0 + s * (0.24f + row * 0.26f), line * 1.4f, paint)
            }
            Kind.SIDE_SHOW -> {
                frame(0.08f, 0.14f, 0.92f, 0.86f)
                bar(0.08f, 0.14f, 0.34f, 0.86f)
            }
            Kind.SIDE_SWIPE -> {
                frame(0.08f, 0.14f, 0.92f, 0.86f)
                bar(0.08f, 0.14f, 0.16f, 0.86f)
                bar(0.3f, 0.47f, 0.62f, 0.53f)
                bar(0.52f, 0.38f, 0.58f, 0.62f)
            }
            Kind.SIDE_HIDDEN -> frame(0.08f, 0.14f, 0.92f, 0.86f)
        }
    }
}

/** A round mark that lights up for the chosen line. */
private class ChoiceMark(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var chosen = false
        set(value) {
            field = value
            invalidate()
        }
    var color = Hud.WHITE
        set(value) {
            field = value
            invalidate()
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        val radius = minOf(width, height) * 0.32f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = Hud.dp(context, 1.5f)
        paint.color = Hud.withAlpha(color, if (chosen) 1f else 0.55f)
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        if (chosen) {
            paint.style = Paint.Style.FILL
            paint.setShadowLayer(radius, 0f, 0f, color)
            canvas.drawCircle(width / 2f, height / 2f, radius * 0.5f, paint)
            paint.clearShadowLayer()
        }
    }
}

/** One choice in the ⋮ menu: a small picture, a name, a short note, and a round mark. */
internal class ChoiceRow(context: Context, label: String, help: String, kind: ChoiceGlyph.Kind) :
    LinearLayout(context), Styled {
    private val glyph = ChoiceGlyph(context, kind)
    private val title = FitTextView(context)
    private val note = FitTextView(context)
    private val mark = ChoiceMark(context)
    private var look: PanelLook? = null

    var chosen = false
        set(value) {
            field = value
            mark.chosen = value
            look?.let { applyLook(it) }
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = Hud.dpi(context, 6f)
        setPadding(pad, Hud.dpi(context, 5f), pad, Hud.dpi(context, 5f))
        minimumHeight = Hud.dpi(context, 48f)
        addView(glyph, LayoutParams(Hud.dpi(context, 26f), Hud.dpi(context, 26f)))
        val column = LinearLayout(context).apply { orientation = VERTICAL }
        title.text = label
        title.typeface = Hud.condensedBold
        note.text = help
        note.typeface = Hud.mono(context)
        column.addView(title)
        column.addView(note)
        addView(column, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Hud.dpi(context, 10f)
            marginEnd = Hud.dpi(context, 6f)
        })
        addView(mark, LayoutParams(Hud.dpi(context, 22f), Hud.dpi(context, 22f)))
        isClickable = true
        isFocusable = true
        contentDescription = "$label. $help"
    }

    override fun applyLook(look: PanelLook) {
        this.look = look
        glyph.applyLook(look)
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * look.fontScale)
        title.setTextColor(if (chosen) look.decor else look.sideText)
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f * look.fontScale)
        note.setTextColor(Hud.withAlpha(look.sideText, 0.62f))
        mark.color = look.decor
        foreground = pressGlow(context, look.edge)
        setBackgroundColor(if (chosen) Hud.withAlpha(look.edge, 0.09f) else 0)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RadioButton::class.java.name
        info.isCheckable = true
        @Suppress("DEPRECATION")
        info.isChecked = chosen
    }
}

/**
 * The ⋮ menu (Step 2.4), like the three dots in Samsung My Files.
 *
 * View: List, Big list, Grid 2, or Grid 3. Sort: Important first, A to Z, or Custom, with
 * Move folders to unlock dragging. Folder size from 0 to 100. Side list: Show, Swipe to
 * open, or Hidden. Each choice works at once and is saved. A tap outside the card, Done,
 * or the phone's Back key closes it.
 */
internal class PanelMenu(
    context: Context,
    private val settings: FolderSettings,
    private val listener: Listener,
) : FrameLayout(context), Styled {

    interface Listener {
        fun onViewChosen(view: FolderView)
        fun onSortChosen(sort: FolderSort)
        fun onUnlockChanged(unlocked: Boolean)
        fun onFolderSize(value: Int, announce: Boolean)
        fun onSideChosen(mode: SideMode)
        fun onMenuClosed()
        fun onProblem(message: String)
    }

    private val density = resources.displayMetrics.density
    private val scrim = View(context)
    private val card = ScrollView(context)
    private val content = LinearLayout(context)
    private val glass = PanelGlassDrawable(density, 0f, pane = true)
    private val viewRows = LinkedHashMap<FolderView, ChoiceRow>()
    private val sortRows = LinkedHashMap<FolderSort, ChoiceRow>()
    private val sideRows = LinkedHashMap<SideMode, ChoiceRow>()
    private val unlockRow: ToggleRow
    private val sizeRow: NumberRow

    var isOpen = false
        private set

    init {
        visibility = GONE
        clipChildren = false
        scrim.isClickable = true
        scrim.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        scrim.setOnClickListener { close(animate = true) }
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        content.orientation = LinearLayout.VERTICAL
        val pad = Hud.dpi(context, 10f)
        content.setPadding(pad, Hud.dpi(context, 4f), pad, pad)

        content.addView(section(R.string.terminal_menu_view))
        val viewKinds = mapOf(
            FolderView.LIST to ChoiceGlyph.Kind.LIST,
            FolderView.BIG_LIST to ChoiceGlyph.Kind.BIG_LIST,
            FolderView.GRID_2 to ChoiceGlyph.Kind.GRID_2,
            FolderView.GRID_3 to ChoiceGlyph.Kind.GRID_3,
        )
        for (view in FolderView.entries) {
            val row = ChoiceRow(context, view.title, view.help, viewKinds.getValue(view))
            row.setOnClickListener { listener.onViewChosen(view) }
            viewRows[view] = row
            content.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        content.addView(section(R.string.terminal_menu_sort))
        val sortKinds = mapOf(
            FolderSort.IMPORTANT to ChoiceGlyph.Kind.IMPORTANT,
            FolderSort.A_TO_Z to ChoiceGlyph.Kind.A_TO_Z,
            FolderSort.CUSTOM to ChoiceGlyph.Kind.CUSTOM,
        )
        for (sort in FolderSort.entries) {
            val row = ChoiceRow(context, sort.title, sort.help, sortKinds.getValue(sort))
            row.setOnClickListener { listener.onSortChosen(sort) }
            sortRows[sort] = row
            content.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        val unlockItem = ToggleItem(
            key = "folders_unlocked",
            label = context.getString(R.string.terminal_menu_move),
            help = context.getString(R.string.terminal_menu_move_help),
            default = false,
            isTest = false,
        )
        unlockRow = ToggleRow(context, unlockItem, unlockItem.help, settings.unlocked) { on -> listener.onUnlockChanged(on) }
        unlockRow.setPadding(Hud.dpi(context, 6f), unlockRow.paddingTop, Hud.dpi(context, 2f), unlockRow.paddingBottom)
        content.addView(unlockRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        content.addView(section(R.string.terminal_menu_size))
        val sizeItem = NumberItem(
            key = "folder_size",
            label = context.getString(R.string.terminal_menu_size_label),
            help = context.getString(R.string.terminal_menu_size_help),
        )
        sizeRow = NumberRow(
            context,
            sizeItem,
            settings.size,
            onChange = { value, announce -> listener.onFolderSize(value, announce) },
            onProblem = { listener.onProblem(it) },
        )
        content.addView(sizeRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        content.addView(section(R.string.terminal_menu_side))
        val sideKinds = mapOf(
            SideMode.SHOW to ChoiceGlyph.Kind.SIDE_SHOW,
            SideMode.SWIPE to ChoiceGlyph.Kind.SIDE_SWIPE,
            SideMode.HIDDEN to ChoiceGlyph.Kind.SIDE_HIDDEN,
        )
        for (mode in SideMode.entries) {
            val row = ChoiceRow(context, mode.title, mode.help, sideKinds.getValue(mode))
            row.setOnClickListener { listener.onSideChosen(mode) }
            sideRows[mode] = row
            content.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        val done = ActionButton(context, context.getString(R.string.terminal_menu_done))
        done.setOnClickListener { close(animate = true) }
        content.addView(done, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = Hud.dpi(context, 10f)
        })

        card.isVerticalScrollBarEnabled = false
        card.overScrollMode = OVER_SCROLL_NEVER
        // A near solid black backing, so the folders behind never show through the menu.
        card.background = LayerDrawable(arrayOf(ColorDrawable(CARD_BACKING), glass))
        card.isClickable = true
        card.addView(content)
        addView(card, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END))
        refresh()
    }

    private fun section(textRes: Int) = LaserTitleView(context).apply {
        text = context.getString(textRes)
        typeface = Hud.monoBold(context)
        letterSpacing = 0.12f
        setPadding(Hud.dpi(context, 6f), Hud.dpi(context, 10f), 0, Hud.dpi(context, 2f))
        styled(TextRole.SECTION, 9.5f)
    }

    /** Extra room at the bottom while the keyboard covers the panel, so Folder size can scroll above it. */
    fun setKeyboardRoom(room: Int) {
        val pad = Hud.dpi(context, 10f)
        if (content.paddingBottom != pad + room) content.setPadding(pad, content.paddingTop, pad, pad + room)
    }

    /** Shows the saved choices. Called after any change, and after Reset all. */
    fun refresh() {
        val view = settings.view
        val sort = settings.sort
        val side = settings.side
        viewRows.forEach { (key, row) -> row.chosen = key == view }
        sortRows.forEach { (key, row) -> row.chosen = key == sort }
        sideRows.forEach { (key, row) -> row.chosen = key == side }
        if (unlockRow.toggle.isOn != settings.unlocked) unlockRow.toggle.setOn(settings.unlocked)
        if (!sizeRow.box.hasFocus() && sizeRow.box.value != settings.size) sizeRow.box.show(settings.size)
    }

    /** Opens the card under the title bar. [top] is where the card starts, from the top of this view. */
    fun open(top: Int) {
        refresh()
        val params = card.layoutParams as LayoutParams
        params.topMargin = top
        params.rightMargin = Hud.dpi(context, CARD_EDGE_DP)
        params.bottomMargin = Hud.dpi(context, CARD_EDGE_DP)
        card.layoutParams = params
        card.scrollTo(0, 0)
        isOpen = true
        visibility = VISIBLE
        if (ValueAnimator.areAnimatorsEnabled()) {
            card.pivotX = card.width.toFloat()
            card.pivotY = 0f
            card.alpha = 0f
            card.scaleX = 0.92f
            card.scaleY = 0.92f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(OPEN_MS).start()
        }
        card.post { (viewRows[settings.view] ?: card).requestFocus() }
    }

    fun close(animate: Boolean) {
        if (!isOpen) return
        isOpen = false
        val focused = findFocus()
        focused?.clearFocus()
        if (animate && ValueAnimator.areAnimatorsEnabled()) {
            card.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(CLOSE_MS).withEndAction {
                if (!isOpen) visibility = GONE
                card.alpha = 1f
                card.scaleX = 1f
                card.scaleY = 1f
            }.start()
        } else {
            visibility = GONE
        }
        listener.onMenuClosed()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val cardWidth = minOf(Hud.dpi(context, CARD_DP), width - Hud.dpi(context, CARD_EDGE_DP) * 2).coerceAtLeast(0)
        val params = card.layoutParams
        if (params.width != cardWidth) params.width = cardWidth
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun applyLook(look: PanelLook) {
        glass.look = look
        card.elevation = Hud.dp(context, 12f)
    }

    private companion object {
        const val CARD_DP = 320f
        const val CARD_EDGE_DP = 8f
        const val OPEN_MS = 150L
        const val CLOSE_MS = 110L
        const val CARD_BACKING = 0xF7000000.toInt()
    }
}
