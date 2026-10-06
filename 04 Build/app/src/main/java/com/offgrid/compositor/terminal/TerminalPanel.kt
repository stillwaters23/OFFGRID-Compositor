package com.offgrid.compositor.terminal

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import com.offgrid.compositor.R
import kotlin.math.hypot

/**
 * Step 2. The floating terminal panel (features 3, 4, 5, and 13).
 *
 * Dark frosted glass with cyan and gold glow lines. A side list and a grid of big
 * glowing folders. Folders slide out like holograms. A soft ring turns around the
 * active folder. A soft scan line sweeps the glass. Text is laser typed.
 *
 * Drag the top bar to move it. Opacity is a typed number from 0 to 100.
 * There are no sliders. It is Royalty's private control, so it is not painted
 * into the saved video.
 */
class TerminalPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val prefs = PanelPrefs(context)
    private val home = PanelTree.build()
    private val density = resources.displayMetrics.density
    private val glass = GlassPanelDrawable(density)
    private val pillGlass = GlassPanelDrawable(density, cutDp = 9f)
    private val holoGlass = GlassPanelDrawable(density, cutDp = 12f, holo = true)

    private val focusSink = View(context)
    private val expanded = LinearLayout(context)
    private val statusLine = LaserTypeView(context)
    private val gridScroll = ScrollView(context)
    private val detailPane = FrameLayout(context)
    private val detailScroll = ScrollView(context)
    private val detailContent = LinearLayout(context)
    private val holoSweep = View(context)
    private val scanLine = ScanLineView(context)
    private val pill = LinearLayout(context)
    private val pillRing = HoloRingView(context, Hud.GOLD)

    private val gridTiles = HashMap<String, FolderTile>()
    private val listRows = HashMap<String, SideRow>()
    private var guidance: LaserTypeView? = null
    private var openFolder: PanelFolder? = null
    private var activeId = prefs.activeFolder ?: home.folders.first().id
    private var detailAnim: Animator? = null
    private var panelAnim: Animator? = null

    private var opacity = prefs.number(PanelTree.OPACITY, 50)
    private var glowLevel = prefs.number(PanelTree.GLOW, 70)
    private var typingSpeed = prefs.number(PanelTree.TYPING, 50)
    private var scanLineOn = prefs.toggle(PanelTree.SCAN_LINE, true)
    private var laserTypingOn = prefs.toggle(PanelTree.LASER_TYPING, true)
    private var minimized = prefs.minimized

    private var placed = false
    private var userY = 0f
    private var keyboardLift = 0f

    /** The phone's Back key closes one folder at a time. */
    val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = goUp()
    }

    init {
        // A tiny spot that takes focus when a number box lets go, so the keyboard stays shut.
        focusSink.isFocusable = true
        focusSink.isFocusableInTouchMode = true
        focusSink.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(focusSink, LayoutParams(1, 1))

        expanded.orientation = LinearLayout.VERTICAL
        expanded.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, WRAP))
        statusLine.textSize = 11f
        statusLine.minLines = 2
        statusLine.setPadding(dpi(14f), 0, dpi(12f), dpi(6f))
        expanded.addView(statusLine, LinearLayout.LayoutParams(MATCH, WRAP))
        expanded.addView(
            glowRule(Hud.GOLD, GradientDrawable.Orientation.LEFT_RIGHT),
            LinearLayout.LayoutParams(MATCH, dpi(1f)).apply {
                marginStart = dpi(12f)
                marginEnd = dpi(12f)
            },
        )
        val body = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(buildSideList(), LinearLayout.LayoutParams(dpi(SIDE_LIST_DP), MATCH))
        body.addView(
            glowRule(Hud.CYAN, GradientDrawable.Orientation.TOP_BOTTOM),
            LinearLayout.LayoutParams(dpi(1f), MATCH).apply {
                topMargin = dpi(8f)
                bottomMargin = dpi(14f)
            },
        )
        body.addView(buildMainArea(), LinearLayout.LayoutParams(0, MATCH, 1f))
        expanded.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))
        addView(expanded, LayoutParams(MATCH, MATCH))

        addView(scanLine, LayoutParams(MATCH, dpi(22f), Gravity.TOP).apply {
            leftMargin = dpi(18f)
            rightMargin = dpi(18f)
        })

        buildPill()
        addView(pill, LayoutParams(WRAP, WRAP))

        applyLook()
        markActive(activeId)
        showMinimized(minimized, animate = false)
        say(context.getString(R.string.terminal_welcome))
    }

    /** Lifts the panel above the keyboard while a number is typed. */
    fun onKeyboardInset(bottomInset: Int) {
        val area = parent as? View ?: return
        val visibleBottom = area.height - bottomInset
        val panelBottom = userY + height
        val edge = dp(EDGE_DP)
        keyboardLift = if (bottomInset > 0 && panelBottom > visibleBottom - edge) {
            (panelBottom - (visibleBottom - edge)).coerceAtMost(userY)
        } else {
            0f
        }
        translationY = userY - keyboardLift
    }

    /** Hides the panel while the saved video plays, so the whole video can be checked. */
    fun setHiddenForPlayback(hidden: Boolean) {
        if (hidden) {
            hideKeyboard()
        }
        visibility = if (hidden) GONE else VISIBLE
        backCallback.isEnabled = !hidden && !minimized && openFolder != null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (minimized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        val edges = dpi(EDGE_DP) * 2
        val panelWidth = minOf(availableWidth - edges, dpi(MAX_WIDTH_DP)).coerceAtLeast(0)
        val panelHeight = (availableHeight * HEIGHT_SHARE).toInt()
            .coerceIn(dpi(MIN_HEIGHT_DP), dpi(MAX_HEIGHT_DP))
            .coerceAtMost(availableHeight - edges)
            .coerceAtLeast(0)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(panelWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(panelHeight, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val area = parent as? View ?: return
        if (area.width == 0 || area.height == 0) return
        if (!placed) {
            placed = true
            val saved = prefs.position()
            if (saved != null) {
                moveTo(saved.first * area.width, saved.second * area.height)
            } else {
                // Start low in the middle, above the record bar, so the yellow diamond stays easy to see.
                moveTo((area.width - width) / 2f, area.height * DEFAULT_BOTTOM_SHARE - height - dp(EDGE_DP))
            }
        } else {
            moveTo(translationX, userY)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val focused = findFocus()
            if (focused is EditText && !isInside(focused, event)) {
                hideKeyboard()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun isInside(view: View, event: MotionEvent): Boolean {
        val spot = IntArray(2)
        view.getLocationOnScreen(spot)
        val x = event.rawX
        val y = event.rawY
        return x >= spot[0] && x <= spot[0] + view.width && y >= spot[1] && y <= spot[1] + view.height
    }

    private fun buildHeader(): View {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpi(16f), dpi(8f), dpi(8f), dpi(2f))
            contentDescription = context.getString(R.string.terminal_drag)
        }
        header.addView(GripView(context), LinearLayout.LayoutParams(dpi(12f), dpi(18f)).apply {
            marginEnd = dpi(10f)
        })
        val title = TextView(context).apply {
            text = context.getString(R.string.terminal_title)
            typeface = Hud.monoBold(context)
            textSize = 12.5f
            letterSpacing = 0.12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Hud.CYAN)
            setShadowLayer(dp(6f), 0f, 0f, Hud.CYAN)
        }
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        val shrink = ShrinkButton(context).apply {
            contentDescription = context.getString(R.string.terminal_shrink)
            setOnClickListener { showMinimized(true, animate = true) }
        }
        header.addView(shrink, LinearLayout.LayoutParams(dpi(48f), dpi(36f)))
        header.setOnTouchListener(dragHandle())
        return header
    }

    private fun buildSideList(): View {
        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpi(6f), dpi(6f), dpi(2f), dpi(12f))
        }
        for (folder in home.folders) {
            val row = SideRow(context, folder)
            row.setOnClickListener { openFromTop(folder, row) }
            listRows[folder.id] = row
            rows.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        return ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            addView(rows)
        }
    }

    private fun buildMainArea(): View {
        val area = FrameLayout(context)
        val grid = TileGrid(context, GRID_CELL_DP).apply {
            setPadding(dpi(4f), dpi(6f), dpi(4f), dpi(14f))
        }
        for (folder in home.folders) {
            val tile = FolderTile(context, folder, GRID_ICON_DP)
            tile.setOnClickListener { openFromTop(folder, tile) }
            gridTiles[folder.id] = tile
            grid.addView(tile)
        }
        gridScroll.isVerticalScrollBarEnabled = false
        gridScroll.overScrollMode = OVER_SCROLL_NEVER
        gridScroll.addView(grid)
        area.addView(gridScroll, LayoutParams(MATCH, MATCH))

        detailPane.background = holoGlass
        detailPane.isClickable = true
        detailPane.visibility = INVISIBLE
        detailContent.orientation = LinearLayout.VERTICAL
        detailContent.setPadding(dpi(10f), dpi(6f), dpi(10f), dpi(16f))
        detailScroll.isVerticalScrollBarEnabled = false
        detailScroll.overScrollMode = OVER_SCROLL_NEVER
        detailScroll.addView(detailContent)
        detailPane.addView(detailScroll, LayoutParams(MATCH, MATCH))
        holoSweep.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0, Hud.withAlpha(Hud.CYAN, 0.32f), 0),
        )
        holoSweep.visibility = INVISIBLE
        detailPane.addView(holoSweep, LayoutParams(MATCH, dpi(44f)))
        area.addView(detailPane, LayoutParams(MATCH, MATCH).apply {
            setMargins(dpi(4f), dpi(4f), dpi(8f), dpi(10f))
        })
        return area
    }

    private fun buildPill() {
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.setPadding(dpi(10f), dpi(7f), dpi(16f), dpi(7f))
        pill.background = pillGlass
        val frame = FrameLayout(context)
        frame.addView(pillRing, LayoutParams(dpi(32f), dpi(32f), Gravity.CENTER))
        frame.addView(
            FolderIconView(context, Hud.GOLD, home.folders.size, null, compact = true),
            LayoutParams(dpi(17f), dpi(15f), Gravity.CENTER),
        )
        pill.addView(frame, LinearLayout.LayoutParams(dpi(32f), dpi(32f)))
        pill.addView(
            TextView(context).apply {
                text = context.getString(R.string.terminal_pill)
                typeface = Hud.monoBold(context)
                textSize = 12.5f
                letterSpacing = 0.1f
                setTextColor(Hud.GOLD)
                setShadowLayer(dp(5f), 0f, 0f, Hud.GOLD)
            },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dpi(8f) },
        )
        pill.contentDescription = context.getString(R.string.terminal_open)
        pill.isClickable = true
        pill.isFocusable = true
        pill.setOnClickListener { showMinimized(false, animate = true) }
        pill.setOnTouchListener(dragHandle())
    }

    /** Drag to move the panel. A short tap counts as a normal tap. */
    private fun dragHandle(): OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var dragging = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        return OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = translationX
                    startY = userY
                    dragging = false
                    view.isPressed = true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        view.isPressed = false
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) {
                        moveTo(startX + dx, startY + dy)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                    if (dragging) savePosition() else view.performClick()
                    dragging = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    if (dragging) savePosition()
                    dragging = false
                }
            }
            true
        }
    }

    /** Keeps the whole panel on the screen. */
    private fun moveTo(x: Float, y: Float) {
        val area = parent as? View ?: return
        translationX = x.coerceIn(0f, (area.width - width).toFloat().coerceAtLeast(0f))
        userY = y.coerceIn(0f, (area.height - height).toFloat().coerceAtLeast(0f))
        translationY = userY - keyboardLift
    }

    private fun savePosition() {
        val area = parent as? View ?: return
        if (area.width > 0 && area.height > 0) {
            prefs.setPosition(translationX / area.width, userY / area.height)
        }
    }

    private fun showMinimized(value: Boolean, animate: Boolean) {
        minimized = value
        prefs.minimized = value
        if (value) {
            hideKeyboard()
        }
        expanded.isVisible = !value
        pill.isVisible = value
        pillRing.spinning = value
        scanLine.isVisible = !value && scanLineOn
        background = if (value) null else glass
        backCallback.isEnabled = !value && openFolder != null
        requestLayout()
        if (animate && !value) {
            post { hologramIn(this, 0f, 0f, isDetail = false) }
            say(context.getString(R.string.terminal_panel_open))
        }
    }

    private fun applyLook() {
        val share = opacity / 100f
        for (drawable in listOf(glass, pillGlass, holoGlass)) {
            drawable.opacity = share
            drawable.glow = glowLevel / 100f
        }
        // The words dim a little with the glass, but never fade away.
        val contentAlpha = 0.7f + 0.3f * share
        expanded.alpha = contentAlpha
        pill.alpha = contentAlpha
        scanLine.isVisible = scanLineOn && !minimized
        for (typer in listOfNotNull(statusLine, guidance)) {
            typer.speed = typingLettersPerSecond()
            typer.animateTyping = laserTypingOn
        }
    }

    private fun typingLettersPerSecond(): Float = 12f + typingSpeed * 1.38f

    private fun say(message: String) {
        statusLine.type(PanelText.line(message))
    }

    private fun markActive(id: String) {
        activeId = id
        prefs.activeFolder = id
        gridTiles.forEach { (key, tile) -> tile.setActive(key == id) }
        listRows.forEach { (key, row) -> row.setActive(key == id) }
    }

    private fun openFromTop(folder: PanelFolder, from: View) {
        markActive(folder.id)
        showFolder(folder, from)
    }

    private fun showFolder(folder: PanelFolder, from: View?) {
        hideKeyboard()
        val (pivotX, pivotY) = pivotIn(detailPane, from)
        val wasOpen = openFolder != null
        openFolder = folder
        backCallback.isEnabled = true
        buildDetail(folder)
        detailScroll.scrollTo(0, 0)
        detailPane.visibility = VISIBLE
        hologramIn(detailPane, pivotX, pivotY, isDetail = true)
        if (!wasOpen) {
            gridScroll.animate().cancel()
            gridScroll.animate().alpha(0f).setDuration(GRID_FADE_MS).withEndAction {
                if (openFolder != null) gridScroll.visibility = INVISIBLE
            }.start()
        }
        val message = if (folder.itemCount == 0) R.string.terminal_folder_empty else R.string.terminal_folder_open
        say(context.getString(message, folder.title))
    }

    private fun goUp() {
        val folder = openFolder ?: return
        val up = folder.parent
        if (up == null || up === home) {
            closeDetail()
        } else {
            showFolder(up, null)
        }
    }

    private fun closeDetail() {
        val folder = openFolder ?: return
        hideKeyboard()
        openFolder = null
        backCallback.isEnabled = false
        gridScroll.animate().cancel()
        gridScroll.visibility = VISIBLE
        gridScroll.animate().alpha(1f).setDuration(GRID_FADE_MS).start()
        val (pivotX, pivotY) = pivotIn(detailPane, gridTiles[folder.topLevel().id])
        detailAnim?.cancel()
        detailPane.pivotX = pivotX
        detailPane.pivotY = pivotY
        detailPane.animate().cancel()
        detailPane.animate()
            .scaleX(0.12f)
            .scaleY(0.03f)
            .alpha(0f)
            .setDuration(CLOSE_MS)
            .withLayer()
            .withEndAction {
                if (openFolder == null) {
                    detailPane.visibility = INVISIBLE
                    detailContent.removeAllViews()
                    guidance = null
                }
                detailPane.scaleX = 1f
                detailPane.scaleY = 1f
                detailPane.alpha = 1f
            }
            .start()
        say(context.getString(R.string.terminal_home_again))
    }

    private fun pivotIn(target: View, from: View?): Pair<Float, Float> {
        if (from == null || !from.isAttachedToWindow || target.width == 0) {
            return target.width / 2f to 0f
        }
        val fromSpot = IntArray(2)
        val targetSpot = IntArray(2)
        from.getLocationInWindow(fromSpot)
        target.getLocationInWindow(targetSpot)
        return (fromSpot[0] - targetSpot[0] + from.width / 2f) to (fromSpot[1] - targetSpot[1] + from.height / 2f)
    }

    /** The hologram: a thin line of light that unfolds into the panel, with one soft flicker. */
    private fun hologramIn(target: View, pivotX: Float, pivotY: Float, isDetail: Boolean) {
        val running = if (isDetail) detailAnim else panelAnim
        running?.cancel()
        target.animate().cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            target.alpha = 1f
            target.scaleX = 1f
            target.scaleY = 1f
            return
        }
        target.pivotX = pivotX
        target.pivotY = pivotY
        val fade = PropertyValuesHolder.ofKeyframe(
            View.ALPHA,
            Keyframe.ofFloat(0f, 0f),
            Keyframe.ofFloat(0.3f, 1f),
            Keyframe.ofFloat(0.45f, 0.6f),
            Keyframe.ofFloat(0.65f, 1f),
            Keyframe.ofFloat(1f, 1f),
        )
        val wide = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.12f, 1f)
        val tall = PropertyValuesHolder.ofKeyframe(
            View.SCALE_Y,
            Keyframe.ofFloat(0f, 0.03f),
            Keyframe.ofFloat(0.35f, 0.08f),
            Keyframe.ofFloat(1f, 1f),
        )
        val unfold = ObjectAnimator.ofPropertyValuesHolder(target, fade, wide, tall).apply {
            duration = OPEN_MS
            interpolator = DecelerateInterpolator(1.4f)
            doOnStart { target.setLayerType(LAYER_TYPE_HARDWARE, null) }
            doOnEnd {
                target.setLayerType(LAYER_TYPE_NONE, null)
                target.alpha = 1f
                target.scaleX = 1f
                target.scaleY = 1f
            }
            start()
        }
        if (isDetail) {
            detailAnim = unfold
            sweepOnce()
        } else {
            panelAnim = unfold
        }
    }

    private fun sweepOnce() {
        holoSweep.animate().cancel()
        holoSweep.visibility = VISIBLE
        holoSweep.alpha = 0.9f
        holoSweep.translationY = -holoSweep.height.toFloat()
        holoSweep.animate()
            .translationY(detailPane.height.toFloat())
            .alpha(0f)
            .setInterpolator(LinearInterpolator())
            .setDuration(SWEEP_MS)
            .withEndAction { holoSweep.visibility = INVISIBLE }
            .start()
    }

    private fun buildDetail(folder: PanelFolder) {
        detailContent.removeAllViews()
        detailContent.addView(pathBar(folder), LinearLayout.LayoutParams(MATCH, WRAP))
        detailContent.addView(folderHeading(folder), LinearLayout.LayoutParams(MATCH, WRAP))

        val typer = LaserTypeView(context).apply {
            textSize = 10.5f
            setPadding(dpi(9f), dpi(7f), dpi(9f), dpi(8f))
            background = GradientDrawable().apply {
                setColor(Hud.withAlpha(Hud.INK, 0.5f))
                cornerRadius = dp(6f)
                setStroke(dpi(1f), Hud.withAlpha(Hud.LASER, 0.2f))
            }
            speed = typingLettersPerSecond()
            animateTyping = laserTypingOn
        }
        guidance = typer
        detailContent.addView(typer, LinearLayout.LayoutParams(MATCH, WRAP))
        typer.type(PanelText.guidance(folder, folder.results(::markOf)))

        if (folder.folders.isNotEmpty()) {
            detailContent.addView(sectionLabel(R.string.terminal_section_folders))
            val tiles = TileGrid(context, SUB_CELL_DP)
            for (child in folder.folders) {
                val tile = FolderTile(context, child, SUB_ICON_DP)
                tile.setOnClickListener { showFolder(child, tile) }
                tiles.addView(tile)
            }
            detailContent.addView(tiles, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        if (folder.numbers.isNotEmpty()) {
            detailContent.addView(sectionLabel(R.string.terminal_section_numbers))
            for (item in folder.numbers) {
                val row = NumberRow(
                    context,
                    item,
                    prefs.number(item.key, item.default),
                    onChange = { value, announce -> saveNumber(item, value, announce) },
                    onProblem = { say(it) },
                )
                detailContent.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
            }
        }
        if (folder.toggles.isNotEmpty()) {
            detailContent.addView(sectionLabel(R.string.terminal_section_toggles))
            for (item in folder.toggles) {
                val note = if (item.isTest) markText(markOf(folder, item)) else item.help
                val on = prefs.toggle(toggleKey(folder, item), item.default)
                val row = ToggleRow(context, item, note, on) { value -> saveToggle(folder, item, value) }
                detailContent.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
            }
        }
    }

    private fun pathBar(folder: PanelFolder): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(context).apply {
            text = context.getString(R.string.terminal_back)
            typeface = Hud.monoBold(context)
            textSize = 11f
            setTextColor(Hud.GOLD)
            setPadding(dpi(6f), dpi(9f), dpi(10f), dpi(9f))
            isClickable = true
            isFocusable = true
            foreground = pressGlow(context)
            contentDescription = context.getString(R.string.terminal_back_label)
            setOnClickListener { goUp() }
        }
        bar.addView(back)
        val path = TextView(context).apply {
            text = (listOf(context.getString(R.string.terminal_home)) + folder.trail().map { it.label })
                .joinToString(" › ")
            typeface = Hud.mono(context)
            textSize = 9.5f
            setTextColor(Hud.TEXT_DIM)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }
        bar.addView(path, LinearLayout.LayoutParams(0, WRAP, 1f))
        return bar
    }

    private fun folderHeading(folder: PanelFolder): View {
        val accent = accentFor(folder.kind)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpi(6f))
        }
        val frame = FrameLayout(context)
        val ringSize = dpi(60f)
        frame.addView(
            HoloRingView(context, accent).apply { spinning = true },
            LayoutParams(ringSize, ringSize, Gravity.CENTER),
        )
        frame.addView(
            FolderIconView(context, accent, folder.itemCount, folder.tag, compact = false),
            LayoutParams(dpi(42f), dpi(36f), Gravity.CENTER),
        )
        row.addView(frame, LinearLayout.LayoutParams(ringSize, ringSize))
        val names = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpi(8f), 0, 0, 0)
        }
        names.addView(TextView(context).apply {
            text = folder.title
            typeface = Hud.condensedBold
            textSize = 16f
            setTextColor(Hud.TEXT)
            setShadowLayer(dp(6f), 0f, 0f, accent)
        })
        names.addView(TextView(context).apply {
            text = itemsText(context, folder.itemCount)
            typeface = Hud.mono(context)
            textSize = 10.5f
            setTextColor(accent)
        })
        row.addView(names, LinearLayout.LayoutParams(0, WRAP, 1f))
        return row
    }

    private fun sectionLabel(textRes: Int): View {
        return TextView(context).apply {
            text = context.getString(textRes)
            typeface = Hud.monoBold(context)
            textSize = 9.5f
            letterSpacing = 0.12f
            setTextColor(Hud.GOLD)
            setPadding(0, dpi(12f), 0, dpi(4f))
        }
    }

    private fun saveNumber(item: NumberItem, value: Int, announce: Boolean) {
        prefs.setNumber(item.key, value)
        when (item.key) {
            PanelTree.OPACITY -> opacity = value
            PanelTree.GLOW -> glowLevel = value
            PanelTree.TYPING -> typingSpeed = value
        }
        applyLook()
        if (announce) {
            say(context.getString(R.string.terminal_number_saved, item.label, value))
        }
    }

    private fun saveToggle(folder: PanelFolder, item: ToggleItem, on: Boolean) {
        prefs.setToggle(toggleKey(folder, item), on)
        if (item.isTest) {
            val message = if (on) R.string.terminal_test_on else R.string.terminal_test_off
            say(context.getString(message, item.label))
            return
        }
        when (item.key) {
            PanelTree.SCAN_LINE -> scanLineOn = on
            PanelTree.LASER_TYPING -> laserTypingOn = on
        }
        applyLook()
        val message = if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off
        say(context.getString(message, item.label))
    }

    private fun toggleKey(folder: PanelFolder, item: ToggleItem): String {
        return if (item.isTest) "test.${folder.id}.${item.key}" else item.key
    }

    /** Step 2 has no real checks yet, so every test starts as not tested. Step 10 adds the checks. */
    @Suppress("UNUSED_PARAMETER")
    private fun markOf(folder: PanelFolder, item: ToggleItem): TestMark = TestMark.NOT_TESTED

    private fun markText(mark: TestMark): String = context.getString(
        when (mark) {
            TestMark.NOT_TESTED -> R.string.terminal_not_tested
            TestMark.SUCCESS -> R.string.terminal_success
            TestMark.FAILED -> R.string.terminal_failed
        },
    )

    private fun hideKeyboard() {
        val focused = findFocus() ?: return
        if (focused is EditText) {
            focused.clearFocus()
            context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun glowRule(color: Int, direction: GradientDrawable.Orientation): View {
        return View(context).apply {
            background = GradientDrawable(
                direction,
                intArrayOf(0, Hud.withAlpha(color, 0.75f), Hud.withAlpha(color, 0.75f), 0),
            )
        }
    }

    private fun dp(value: Float): Float = value * density

    private fun dpi(value: Float): Int = (value * density + 0.5f).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val EDGE_DP = 8f
        const val MAX_WIDTH_DP = 440f
        const val MIN_HEIGHT_DP = 300f
        const val MAX_HEIGHT_DP = 470f
        const val HEIGHT_SHARE = 0.44f
        const val DEFAULT_BOTTOM_SHARE = 0.82f
        const val SIDE_LIST_DP = 116f
        const val GRID_CELL_DP = 76f
        const val GRID_ICON_DP = 54f
        const val SUB_CELL_DP = 84f
        const val SUB_ICON_DP = 50f
        const val OPEN_MS = 420L
        const val CLOSE_MS = 220L
        const val SWEEP_MS = 520L
        const val GRID_FADE_MS = 220L
    }
}
