package com.offgrid.compositor.terminal

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.offgrid.compositor.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/**
 * The floating terminal panel (Step 2, polished in Step 2.1).
 *
 * True black glass with a soft living glow in the phone's own colors. A side list and
 * a grid of big glowing folders. Folders unfold like holograms. Text is laser typed.
 * Panel settings holds the theme office: themes, colors, glow effects, and toggle style.
 *
 * Drag the top bar to move it. Drag the bottom right corner to resize it.
 * Every setting is a typed number from 0 to 100 or a toggle. There are no sliders.
 * It is Royalty's private control, so it is never painted into the saved video.
 */
class TerminalPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val prefs = PanelPrefs(context)
    private val store = ThemeStore(context, prefs)
    private val home = PanelTree.build()
    private val density = resources.displayMetrics.density

    /** Room around the glass for the outer glow. Taps there go through to the camera screen. */
    private val margin = dpi(GLOW_MARGIN_DP)
    private val glass = PanelGlassDrawable(density, margin.toFloat())
    private val paneGlass = PanelGlassDrawable(density, 0f, pane = true)
    private var look = store.look()

    private val focusSink = View(context)
    private val expanded = LinearLayout(context)
    private val statusLine = LaserTypeView(context)
    private val sideList = ScrollView(context)
    private val sideRule = GlowRule(context, vertical = true, decor = false)
    private val gridScroll = ScrollView(context)
    private val detailPane = FrameLayout(context)
    private val detailScroll = ScrollView(context)
    private val detailContent = LinearLayout(context)
    private val holoSweep = SweepBand(context)
    private val scanLine = ScanLineView(context)
    private val pill = LinearLayout(context)
    private val pillRing = HoloRingView(context, special = true)
    private val resizeGrip = ResizeGrip(context)

    private val gridTiles = HashMap<String, FolderTile>()
    private val listRows = HashMap<String, SideRow>()
    private val themeRows = ArrayList<ThemeRow>()
    private var headingRing: HoloRingView? = null
    private var guidance: LaserTypeView? = null
    private var openFolder: PanelFolder? = null
    private var activeId = prefs.activeFolder?.takeIf { saved -> home.folders.any { it.id == saved } }
        ?: home.folders.first().id
    private var detailAnim: Animator? = null
    private var panelAnim: Animator? = null
    private var minimized = prefs.minimized

    private var placed = false
    private var userY = 0f
    private var keyboardLift = 0f
    private var keyboardHeight = 0
    private var keyboardCover = 0

    /** The glass size in dp, once Royalty drags the corner. Null means the start size. */
    private var chosenSize: Pair<Float, Float>? = prefs.size()
    private var shownOnScreen = false
    private var ticking = false
    private val glassBox = RectF()

    private val barViews = ArrayList<View>()
    private val barTexts = ArrayList<TextView>()
    private val barButtons = ArrayList<TextView>()

    /** Called when the Test diamond toggle changes. The camera screen passes it to the video painter. */
    var onTestDiamond: ((Boolean) -> Unit)? = null

    /** True when the Step 1 test diamond is painted into the video. It starts off. */
    val testDiamondOn: Boolean get() = store.toggle(LookKeys.TEST_DIAMOND)

    /** The phone's Back key closes one folder at a time. */
    val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = goUp()
    }

    private val focusWatcher = ViewTreeObserver.OnGlobalFocusChangeListener { _, now ->
        if (now is EditText && keyboardHeight > 0) post { keepFocusAboveKeyboard() }
    }

    init {
        setPadding(margin, margin, margin, margin)
        background = glass

        // A tiny spot that takes focus when a typed box lets go, so the keyboard stays shut.
        focusSink.isFocusable = true
        focusSink.isFocusableInTouchMode = true
        focusSink.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(focusSink, LayoutParams(1, 1))

        expanded.orientation = LinearLayout.VERTICAL
        expanded.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, WRAP))
        statusLine.minLines = 2
        statusLine.setPadding(dpi(14f), 0, dpi(12f), dpi(6f))
        expanded.addView(statusLine, LinearLayout.LayoutParams(MATCH, WRAP))
        expanded.addView(
            GlowRule(context, vertical = false, decor = true),
            LinearLayout.LayoutParams(MATCH, dpi(1f)).apply {
                marginStart = dpi(12f)
                marginEnd = dpi(12f)
            },
        )
        val body = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(buildSideList(), LinearLayout.LayoutParams(dpi(SIDE_MAX_DP), MATCH))
        body.addView(
            sideRule,
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

        resizeGrip.setOnTouchListener(resizeHandle())
        resizeGrip.setOnClickListener { say(context.getString(R.string.terminal_resize_hint)) }
        ViewCompat.addAccessibilityAction(resizeGrip, context.getString(R.string.terminal_bigger)) { _, _ ->
            resizeBy(dp(STEP_DP))
            true
        }
        ViewCompat.addAccessibilityAction(resizeGrip, context.getString(R.string.terminal_smaller)) { _, _ ->
            resizeBy(-dp(STEP_DP))
            true
        }
        addView(resizeGrip, LayoutParams(dpi(GRIP_DP), dpi(GRIP_DP), Gravity.BOTTOM or Gravity.END))

        applyLook()
        markActive(activeId)
        showMinimized(minimized, animate = false)
        say(context.getString(R.string.terminal_welcome))
    }

    /**
     * Paints the record bar to match the panel. The panel repaints it whenever the theme changes.
     * [bars] are the bar backgrounds, [texts] the messages, and [buttons] the bar's buttons.
     */
    fun matchLook(bars: List<View>, texts: List<TextView>, buttons: List<TextView>) {
        barViews.clear()
        barViews += bars
        barTexts.clear()
        barTexts += texts
        barButtons.clear()
        barButtons += buttons
        RecordBarLook.apply(look, barViews, barTexts, barButtons)
    }

    /** Lifts the panel above the keyboard while a number or color code is typed. */
    fun onKeyboardInset(bottomInset: Int) {
        keyboardHeight = bottomInset
        fitAboveKeyboard()
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
        val areaWidth = MeasureSpec.getSize(widthMeasureSpec)
        val areaHeight = MeasureSpec.getSize(heightMeasureSpec)
        val edges = dpi(EDGE_DP) * 2
        val saved = chosenSize
        val glassWidth: Int
        val glassHeight: Int
        if (saved != null) {
            glassWidth = dpi(saved.first).coerceIn(minOf(dpi(MIN_WIDTH_DP), areaWidth), areaWidth.coerceAtLeast(0))
            glassHeight = dpi(saved.second).coerceIn(minOf(dpi(MIN_HEIGHT_DP), areaHeight), areaHeight.coerceAtLeast(0))
        } else {
            glassWidth = minOf(areaWidth - edges, dpi(START_MAX_WIDTH_DP)).coerceAtLeast(0)
            glassHeight = (areaHeight * START_HEIGHT_SHARE).toInt()
                .coerceIn(dpi(START_MIN_HEIGHT_DP), dpi(START_MAX_HEIGHT_DP))
                .coerceAtMost(areaHeight - edges)
                .coerceAtLeast(0)
        }
        // The side list shrinks a little with the panel, so the open folder keeps its room.
        // A very narrow panel hides it. The big folders still show every top folder.
        val glassDp = glassWidth / density
        val showSide = glassDp >= SIDE_SHOW_DP
        if (sideList.isVisible != showSide) {
            sideList.isVisible = showSide
            sideRule.isVisible = showSide
        }
        sideList.layoutParams.width = dpi((glassDp * SIDE_SHARE).coerceIn(SIDE_MIN_DP, SIDE_MAX_DP))
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(glassWidth + margin * 2, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(glassHeight + margin * 2, MeasureSpec.EXACTLY),
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
                moveGlassTo(saved.first * area.width, saved.second * area.height)
            } else {
                // Start low in the middle, above the record bar, so the face stays easy to see.
                val glassWidth = width - margin * 2
                val glassHeight = height - margin * 2
                moveGlassTo((area.width - glassWidth) / 2f, area.height * START_BOTTOM_SHARE - glassHeight - dp(EDGE_DP))
            }
        } else {
            moveGlassTo(translationX + margin, userY + margin)
        }
        if (keyboardHeight > 0) fitAboveKeyboard()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val down = event.actionMasked == MotionEvent.ACTION_DOWN
        if (down) {
            glass.glassRect(glassBox)
            if (!glassBox.contains(event.x, event.y)) {
                // A tap on the glow, outside the glass, goes to whatever is under the panel.
                return false
            }
            val focused = findFocus()
            if (focused is EditText && !isInside(focused, event)) {
                hideKeyboard()
            }
        }
        val handled = super.dispatchTouchEvent(event)
        // The glass keeps its own taps, so they never press the record bar under it.
        return handled || down
    }

    /** Sets the breathing glow and the swirl for this moment, then draws. */
    override fun draw(canvas: Canvas) {
        val current = look
        val live = ticking && GlowClock.motionAllowed
        val seconds = GlowClock.seconds
        glass.breath = if (live && current.pulse) breathAt(seconds, current.pulseSpeed) else STILL_BREATH
        glass.swirlAngle = if (live && current.swirl) (seconds * swirlDegreesPerSecond(current.swirlSpeed)) % 360f else 0f
        super.draw(canvas)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        shownOnScreen = isVisible
        refreshClock()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalFocusChangeListener(focusWatcher)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalFocusChangeListener(focusWatcher)
        shownOnScreen = false
        refreshClock()
        super.onDetachedFromWindow()
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
            letterSpacing = 0.12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            styled(TextRole.TITLE, 12.5f)
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
        sideList.isVerticalScrollBarEnabled = false
        sideList.overScrollMode = OVER_SCROLL_NEVER
        sideList.addView(rows)
        return sideList
    }

    private fun buildMainArea(): View {
        val area = FrameLayout(context)
        val grid = TileGrid(context, GRID_CELL_DP).apply {
            setPadding(dpi(4f), dpi(6f), dpi(4f), dpi(GRIP_DP))
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

        detailPane.background = paneGlass
        detailPane.isClickable = true
        detailPane.visibility = INVISIBLE
        detailContent.orientation = LinearLayout.VERTICAL
        setDetailPadding()
        detailScroll.isVerticalScrollBarEnabled = false
        detailScroll.overScrollMode = OVER_SCROLL_NEVER
        detailScroll.addView(detailContent)
        detailPane.addView(detailScroll, LayoutParams(MATCH, MATCH))
        holoSweep.visibility = INVISIBLE
        detailPane.addView(holoSweep, LayoutParams(MATCH, dpi(44f)))
        area.addView(detailPane, LayoutParams(MATCH, MATCH).apply {
            setMargins(dpi(4f), dpi(4f), dpi(8f), dpi(10f))
        })
        return area
    }

    /** The bottom gap leaves room for the resize corner, and for the keyboard when it covers the panel. */
    private fun setDetailPadding() {
        detailContent.setPadding(dpi(10f), dpi(6f), dpi(10f), dpi(DETAIL_BOTTOM_DP) + keyboardCover)
    }

    private fun buildPill() {
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.setPadding(dpi(10f), dpi(7f), dpi(16f), dpi(7f))
        val frame = FrameLayout(context)
        frame.addView(pillRing, LayoutParams(dpi(32f), dpi(32f), Gravity.CENTER))
        frame.addView(
            FolderIconView(context, home.folders.size, null, compact = true, special = true),
            LayoutParams(dpi(17f), dpi(15f), Gravity.CENTER),
        )
        pill.addView(frame, LinearLayout.LayoutParams(dpi(32f), dpi(32f)))
        pill.addView(
            TextView(context).apply {
                text = context.getString(R.string.terminal_pill)
                typeface = Hud.monoBold(context)
                letterSpacing = 0.1f
                styled(TextRole.PILL, 12.5f)
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
                    startX = translationX + margin
                    startY = userY + margin
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
                        moveGlassTo(startX + dx, startY + dy)
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

    /** Drag the bottom right corner to make the panel bigger or smaller. The top left corner stays put. */
    private fun resizeHandle(): OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startWidth = 0f
        var startHeight = 0f
        var resizing = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        return OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startWidth = (width - margin * 2).toFloat()
                    startHeight = (height - margin * 2).toFloat()
                    resizing = false
                    view.isPressed = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!resizing && hypot(dx, dy) > slop) {
                        resizing = true
                        hideKeyboard()
                        // Stretch the old glow while dragging. It is redrawn once at the end.
                        glass.holdMasks = true
                    }
                    if (resizing) {
                        resizeGlassTo(startWidth + dx, startHeight + dy)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                    if (resizing) finishResize() else view.performClick()
                    resizing = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    if (resizing) finishResize()
                    resizing = false
                }
            }
            true
        }
    }

    /** Sets a new glass size in pixels. It never goes past the screen, or below the smallest size. */
    private fun resizeGlassTo(newWidth: Float, newHeight: Float) {
        val area = parent as? View ?: return
        val roomWidth = (area.width - (translationX + margin)).coerceAtLeast(0f)
        val roomHeight = (area.height - (userY + margin)).coerceAtLeast(0f)
        val glassWidth = newWidth.coerceIn(minOf(dp(MIN_WIDTH_DP), roomWidth), roomWidth)
        val glassHeight = newHeight.coerceIn(minOf(dp(MIN_HEIGHT_DP), roomHeight), roomHeight)
        chosenSize = (glassWidth / density) to (glassHeight / density)
        requestLayout()
    }

    private fun finishResize() {
        glass.holdMasks = false
        chosenSize?.let { prefs.setSize(it.first, it.second) }
        savePosition()
        say(context.getString(R.string.terminal_resized))
    }

    /** For screen readers: the Bigger and Smaller actions on the resize corner. */
    private fun resizeBy(step: Float) {
        if (minimized) return
        resizeGlassTo((width - margin * 2) + step, (height - margin * 2) + step)
        finishResize()
    }

    /** Moves the glass so its top left corner is at x, y. The glass always stays on the screen. */
    private fun moveGlassTo(x: Float, y: Float) {
        val area = parent as? View ?: return
        val glassWidth = (width - margin * 2).toFloat()
        val glassHeight = (height - margin * 2).toFloat()
        val glassX = x.coerceIn(0f, (area.width - glassWidth).coerceAtLeast(0f))
        val glassY = y.coerceIn(0f, (area.height - glassHeight).coerceAtLeast(0f))
        translationX = glassX - margin
        userY = glassY - margin
        translationY = userY - keyboardLift
    }

    private fun savePosition() {
        val area = parent as? View ?: return
        if (area.width > 0 && area.height > 0) {
            prefs.setPosition((translationX + margin) / area.width, (userY + margin) / area.height)
        }
    }

    /**
     * Lifts the panel so its bottom clears the keyboard. A very tall panel can't lift that far.
     * Then the open folder scrolls the box being typed in up above the keyboard.
     */
    private fun fitAboveKeyboard() {
        val area = parent as? View ?: return
        val visibleBottom = area.height - keyboardHeight - dp(EDGE_DP)
        val glassTop = userY + margin
        val glassBottom = userY + height - margin
        keyboardLift = if (keyboardHeight > 0 && glassBottom > visibleBottom) {
            (glassBottom - visibleBottom).coerceAtMost(glassTop.coerceAtLeast(0f))
        } else {
            0f
        }
        translationY = userY - keyboardLift
        val cover = if (keyboardHeight > 0) (glassBottom - keyboardLift - visibleBottom).coerceAtLeast(0f) else 0f
        if (cover.toInt() != keyboardCover) {
            keyboardCover = cover.toInt()
            setDetailPadding()
        }
        if (keyboardCover > 0) post { keepFocusAboveKeyboard() }
    }

    private fun keepFocusAboveKeyboard() {
        val focused = findFocus() as? EditText ?: return
        val area = parent as? View ?: return
        if (keyboardHeight <= 0) return
        val spot = IntArray(2)
        area.getLocationOnScreen(spot)
        val keyboardTop = spot[1] + area.height - keyboardHeight - dp(EDGE_DP)
        focused.getLocationOnScreen(spot)
        val hidden = spot[1] + focused.height - keyboardTop
        if (hidden > 0f) {
            detailScroll.smoothScrollBy(0, hidden.toInt())
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
        resizeGrip.isVisible = !value
        scanLine.isVisible = !value && look.scanLine
        pillRing.isVisible = look.ring
        pillRing.spinning = value && look.ring
        backCallback.isEnabled = !value && openFolder != null && isVisible
        requestLayout()
        if (animate && !value) {
            post { hologramIn(this, margin.toFloat(), margin.toFloat(), isDetail = false) }
            say(context.getString(R.string.terminal_panel_open))
        }
    }

    /** Reads the saved theme settings again, then repaints the whole panel. */
    private fun refreshLook() {
        look = store.look()
        applyLook()
    }

    private fun applyLook() {
        val current = look
        glass.look = current
        paneGlass.look = current
        restyle(this, current)
        // The words dim a little with the glass, but never fade away.
        val contentAlpha = 0.7f + 0.3f * current.opacity
        expanded.alpha = contentAlpha
        pill.alpha = contentAlpha
        scanLine.isVisible = current.scanLine && !minimized
        pillRing.isVisible = current.ring
        pillRing.spinning = minimized && current.ring
        headingRing?.let {
            it.isVisible = current.ring
            it.spinning = current.ring
        }
        styleTyper(statusLine, current.status, STATUS_SP)
        guidance?.let { styleGuidance(it) }
        RecordBarLook.apply(current, barViews, barTexts, barButtons)
        refreshClock()
        invalidate()
    }

    /** Walks every view in the panel and repaints the ones that follow the theme. */
    private fun restyle(view: View, current: PanelLook) {
        if (view is Styled) view.applyLook(current)
        if (view is TextView) {
            (view.tag as? StyleTag)?.let { applyTextStyle(view, it, current) }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                restyle(view.getChildAt(index), current)
            }
        }
    }

    private fun styleTyper(typer: LaserTypeView, color: Int, baseSp: Float) {
        val current = look
        typer.setTextColor(color)
        typer.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * current.fontScale)
        typer.cursorColor = current.cursor
        typer.speed = current.lettersPerSecond
        typer.animateTyping = current.laserTyping
    }

    private fun styleGuidance(typer: LaserTypeView) {
        val current = look
        styleTyper(typer, current.text, GUIDE_SP)
        typer.background = GradientDrawable().apply {
            setColor(Hud.withAlpha(Hud.blend(current.skin, Hud.INK, 0.6f), 0.55f))
            cornerRadius = dp(6f)
            setStroke(dpi(1f), Hud.withAlpha(current.text, 0.22f))
        }
    }

    /** The glow moves only while it is on screen, and never when the phone asks for less motion. */
    private fun refreshClock() {
        val current = look
        val glowing = current.glowStrength > 0f && (current.outerGlowOn || current.edgeGlow)
        val want = shownOnScreen && glowing && (current.pulse || current.swirl) && GlowClock.motionAllowed
        if (want && !ticking) {
            ticking = true
            GlowClock.join(this)
        } else if (!want && ticking) {
            ticking = false
            GlowClock.leave(this)
        }
    }

    /** 0 to 1, rising and falling smoothly like slow breathing. Speed 0 is about 6 seconds a breath. */
    private fun breathAt(seconds: Float, speed: Float): Float {
        val period = BREATH_SLOW_S - (BREATH_SLOW_S - BREATH_FAST_S) * speed
        return 0.5f - 0.5f * cos(seconds / period * 2f * PI.toFloat())
    }

    private fun swirlDegreesPerSecond(speed: Float): Float = 6f + 84f * speed

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
            if (unfolding()) {
                gridScroll.animate().alpha(0f).setDuration(GRID_FADE_MS).withEndAction {
                    if (openFolder != null) gridScroll.visibility = INVISIBLE
                }.start()
            } else {
                gridScroll.alpha = 0f
                gridScroll.visibility = INVISIBLE
            }
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
        detailAnim?.cancel()
        detailPane.animate().cancel()
        say(context.getString(R.string.terminal_home_again))
        if (!unfolding()) {
            gridScroll.alpha = 1f
            clearDetail()
            return
        }
        gridScroll.animate().alpha(1f).setDuration(GRID_FADE_MS).start()
        val (pivotX, pivotY) = pivotIn(detailPane, gridTiles[folder.topLevel().id])
        detailPane.pivotX = pivotX
        detailPane.pivotY = pivotY
        detailPane.animate()
            .scaleX(0.12f)
            .scaleY(0.03f)
            .alpha(0f)
            .setDuration(CLOSE_MS)
            .withLayer()
            .withEndAction {
                if (openFolder == null) clearDetail()
                detailPane.scaleX = 1f
                detailPane.scaleY = 1f
                detailPane.alpha = 1f
            }
            .start()
    }

    private fun clearDetail() {
        detailPane.visibility = INVISIBLE
        detailContent.removeAllViews()
        guidance = null
        headingRing = null
        themeRows.clear()
    }

    /** True when folders should unfold like a hologram. Off, or reduced motion, opens them at once. */
    private fun unfolding(): Boolean = look.unfold && ValueAnimator.areAnimatorsEnabled()

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
        if (!unfolding()) {
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
        if (!unfolding()) return
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
        themeRows.clear()
        headingRing = null
        detailContent.addView(pathBar(folder), LinearLayout.LayoutParams(MATCH, WRAP))
        detailContent.addView(folderHeading(folder), LinearLayout.LayoutParams(MATCH, WRAP))

        val typer = LaserTypeView(context).apply {
            setPadding(dpi(9f), dpi(7f), dpi(9f), dpi(8f))
        }
        guidance = typer
        detailContent.addView(typer, LinearLayout.LayoutParams(MATCH, WRAP))

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
        if (folder.items.isNotEmpty()) {
            detailContent.addView(sectionLabel(sectionFor(folder)))
            for (item in folder.items) {
                detailContent.addView(itemView(folder, item), LinearLayout.LayoutParams(MATCH, WRAP))
            }
        }
        // Paint the new views in the current theme before the typing starts.
        restyle(detailContent, look)
        styleGuidance(typer)
        typer.type(PanelText.guidance(folder, folder.results(::markOf)))
    }

    private fun sectionFor(folder: PanelFolder): Int = when {
        folder.kind == FolderKind.TESTS -> R.string.terminal_section_toggles
        folder.items.all { it is ThemeItem } -> R.string.terminal_section_themes
        folder.items.all { it is ColorItem } -> R.string.terminal_section_color
        else -> R.string.terminal_section_settings
    }

    private fun itemView(folder: PanelFolder, item: PanelItem): View = when (item) {
        is NumberItem -> NumberRow(
            context,
            item,
            store.number(item.key),
            onChange = { value, announce -> saveNumber(item, value, announce) },
            onProblem = { say(it) },
        )
        is ToggleItem -> {
            val note = if (item.isTest) markText(markOf(folder, item)) else item.help
            val on = if (item.isTest) prefs.toggle(testKey(folder, item), item.default) else store.toggle(item.key)
            ToggleRow(context, item, note, on) { value -> saveToggle(folder, item, value) }
        }
        is ActionItem -> actionRow(item)
        is ThemeItem -> ThemeRow(context, item.theme, store.preview(item.theme), store.base() == item.theme).also { row ->
            themeRows += row
            row.setOnClickListener { applyTheme(item.theme) }
        }
        is ColorItem -> ColorEditorView(context, item.part, store, onLookChanged = ::refreshLook, say = ::say)
    }

    /** A button with a short note. Reset all asks first, in plain words. */
    private fun actionRow(item: ActionItem): View {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpi(8f), 0, dpi(4f))
        }
        val button = ActionButton(context, item.label)
        box.addView(button, LinearLayout.LayoutParams(MATCH, WRAP))
        box.addView(TextView(context).apply {
            text = item.help
            typeface = Hud.mono(context)
            setPadding(0, dpi(4f), 0, 0)
            styled(TextRole.PATH, 9.5f)
        })
        val confirm = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = GONE
        }
        confirm.addView(TextView(context).apply {
            text = context.getString(R.string.terminal_reset_question)
            typeface = Hud.monoBold(context)
            setPadding(0, dpi(4f), 0, dpi(6f))
            styled(TextRole.MESSAGE, 11f)
        })
        val choices = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val yes = ActionButton(context, context.getString(R.string.terminal_reset_yes))
        val no = ActionButton(context, context.getString(R.string.terminal_reset_no))
        choices.addView(yes, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dpi(4f) })
        choices.addView(no, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dpi(4f) })
        confirm.addView(choices, LinearLayout.LayoutParams(MATCH, WRAP))
        box.addView(confirm, 1, LinearLayout.LayoutParams(MATCH, WRAP))
        button.setOnClickListener {
            if (item.action == PanelAction.RESET_ALL) {
                button.visibility = GONE
                confirm.visibility = VISIBLE
                say(context.getString(R.string.terminal_reset_question))
            }
        }
        no.setOnClickListener {
            confirm.visibility = GONE
            button.visibility = VISIBLE
            say(context.getString(R.string.terminal_reset_kept))
        }
        yes.setOnClickListener { resetAll() }
        return box
    }

    private fun applyTheme(theme: Theme) {
        hideKeyboard()
        store.applyTheme(theme)
        refreshLook()
        themeRows.forEach { it.inUse = it.theme == theme }
        sweepOnce()
        say(context.getString(R.string.terminal_theme_applied, theme.title))
    }

    /** Puts every panel setting back to the start. The test marks and the panel's place stay. */
    private fun resetAll() {
        hideKeyboard()
        store.resetAll()
        chosenSize = null
        savePosition()
        prefs.activeFolder = activeId
        prefs.minimized = minimized
        refreshLook()
        onTestDiamond?.invoke(testDiamondOn)
        requestLayout()
        openFolder?.let {
            buildDetail(it)
            detailScroll.scrollTo(0, 0)
        }
        say(context.getString(R.string.terminal_reset_done))
    }

    private fun pathBar(folder: PanelFolder): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(context).apply {
            text = context.getString(R.string.terminal_back)
            typeface = Hud.monoBold(context)
            setPadding(dpi(6f), dpi(9f), dpi(10f), dpi(9f))
            isClickable = true
            isFocusable = true
            foreground = pressGlow(context, look.edge)
            contentDescription = context.getString(R.string.terminal_back_label)
            setOnClickListener { goUp() }
            styled(TextRole.BACK, 11f)
        }
        bar.addView(back)
        val path = TextView(context).apply {
            text = (listOf(context.getString(R.string.terminal_home)) + folder.trail().map { it.label })
                .joinToString(" › ")
            typeface = Hud.mono(context)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            styled(TextRole.PATH, 9.5f)
        }
        bar.addView(path, LinearLayout.LayoutParams(0, WRAP, 1f))
        return bar
    }

    private fun folderHeading(folder: PanelFolder): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpi(6f))
        }
        val frame = FrameLayout(context)
        val ringSize = dpi(60f)
        val ring = HoloRingView(context, folder.special)
        headingRing = ring
        frame.addView(ring, LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, folder.itemCount, folder.tag, compact = false, special = folder.special),
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
            styled(TextRole.HEADING, 16f)
        })
        names.addView(TextView(context).apply {
            text = itemsText(context, folder.itemCount)
            typeface = Hud.mono(context)
            styled(TextRole.COUNT, 10.5f)
        })
        row.addView(names, LinearLayout.LayoutParams(0, WRAP, 1f))
        ring.isVisible = look.ring
        ring.spinning = look.ring
        return row
    }

    private fun sectionLabel(textRes: Int): View {
        return TextView(context).apply {
            text = context.getString(textRes)
            typeface = Hud.monoBold(context)
            letterSpacing = 0.12f
            setPadding(0, dpi(12f), 0, dpi(4f))
            styled(TextRole.SECTION, 9.5f)
        }
    }

    private fun saveNumber(item: NumberItem, value: Int, announce: Boolean) {
        store.setNumber(item.key, value)
        refreshLook()
        if (announce) {
            say(context.getString(R.string.terminal_number_saved, item.label, value))
        }
    }

    private fun saveToggle(folder: PanelFolder, item: ToggleItem, on: Boolean) {
        if (item.isTest) {
            prefs.setToggle(testKey(folder, item), on)
            val message = if (on) R.string.terminal_test_on else R.string.terminal_test_off
            say(context.getString(message, item.label))
            return
        }
        store.setToggle(item.key, on)
        refreshLook()
        if (item.key == LookKeys.TEST_DIAMOND) {
            onTestDiamond?.invoke(on)
            say(context.getString(if (on) R.string.terminal_diamond_on else R.string.terminal_diamond_off))
            return
        }
        val message = if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off
        say(context.getString(message, item.label))
    }

    private fun testKey(folder: PanelFolder, item: ToggleItem): String = "${PanelPrefs.TEST_PREFIX}${folder.id}.${item.key}"

    /** There are no real checks yet, so every test starts as not tested. A later step adds the checks. */
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

    private fun dp(value: Float): Float = value * density

    private fun dpi(value: Float): Int = (value * density + 0.5f).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val GLOW_MARGIN_DP = 18f
        const val EDGE_DP = 8f
        const val START_MAX_WIDTH_DP = 440f
        const val START_MIN_HEIGHT_DP = 300f
        const val START_MAX_HEIGHT_DP = 470f
        const val START_HEIGHT_SHARE = 0.44f
        const val START_BOTTOM_SHARE = 0.82f
        const val MIN_WIDTH_DP = 280f
        const val MIN_HEIGHT_DP = 260f
        const val STEP_DP = 40f
        const val GRIP_DP = 30f
        const val DETAIL_BOTTOM_DP = 30f
        const val SIDE_SHARE = 0.29f
        const val SIDE_SHOW_DP = 340f
        const val SIDE_MIN_DP = 100f
        const val SIDE_MAX_DP = 116f
        const val GRID_CELL_DP = 76f
        const val GRID_ICON_DP = 54f
        const val SUB_CELL_DP = 84f
        const val SUB_ICON_DP = 50f
        const val STATUS_SP = 11f
        const val GUIDE_SP = 10.5f
        const val STILL_BREATH = 0.8f
        const val BREATH_SLOW_S = 6f
        const val BREATH_FAST_S = 1.2f
        const val OPEN_MS = 420L
        const val CLOSE_MS = 220L
        const val SWEEP_MS = 520L
        const val GRID_FADE_MS = 220L
    }
}
