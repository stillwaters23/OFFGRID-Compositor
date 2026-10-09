package com.offgrid.compositor.terminal

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.offgrid.compositor.R
import kotlin.math.roundToInt

/**
 * The floating terminal panel (Step 2, polished in Steps 2.1 and 2.2).
 *
 * True black glass in a dark titanium frame, with a title bar on carbon fiber and a soft
 * living glow. Big glowing folders unfold like holograms
 * onto thick sapphire glass. Titles and typed text are Laser Jet Bright.
 * Panel settings holds only folders: Size, Display, Themes, Colors, Glow effects,
 * and Reset.
 *
 * Drag the top bar to move it. A soft cross shows the middle while you drag.
 * Type Width and Height at the top, from 0 to 100, pinch with two fingers, or drag the
 * bottom edge or lower corners to resize. All three stay in step and are saved.
 * Every setting is a typed number from 0 to 100 or a toggle. There are no sliders.
 * It is Royalty's private control, so it is never painted into the saved video.
 */
class TerminalPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    internal val prefs = PanelPrefs(context)
    internal val store = ThemeStore(context, prefs)
    internal val home = PanelTree.build()
    internal val density = resources.displayMetrics.density

    /** Room around the glass for the outer glow. Taps there go through to the camera screen. */
    internal val glowMargin = dpi(GLOW_MARGIN_DP)
    internal var margin = glowMargin
    internal val glass = PanelGlassDrawable(density, margin.toFloat())
    internal val paneGlass = PanelGlassDrawable(density, 0f, pane = true)
    internal var look = store.look()

    internal val focusSink = View(context)
    internal val expanded = LinearLayout(context)
    internal val statusLine = LaserTypeView(context)
    internal val gridScroll = ScrollView(context)
    internal val detailPane = FrameLayout(context)
    internal val detailScroll = ScrollView(context)
    internal val detailContent = LinearLayout(context)
    internal val holoSweep = SweepBand(context)
    internal val scanLine = ScanLineView(context)
    internal val pill = LinearLayout(context)
    internal val pillRing = HoloRingView(context, special = true)
    internal val backOrb = BackOrbView(context)
    internal lateinit var topBar: TopBar

    /** The folder and its order from before the last drag, for Undo last move. */
    internal var lastMove: Pair<PanelFolder, List<PanelFolder>>? = null

    /** The ⋮ menu's choices and the folder views. */
    internal val folderSettings = FolderSettings(prefs)
    internal val mainFolders = FolderLayout(context, sub = false)
    internal var subFolders: FolderLayout? = null
    internal val body = LinearLayout(context)
    internal val menu: PanelMenu

    /**
     * Final build, milestone 2: the bars, camera hole, gesture areas, and round screen corners.
     * The glass fills the screen edge to edge. Only its words and buttons move in from these.
     */
    internal var edges = ScreenEdges()

    /** Extra room inside the glass where it reaches an edge of [edges], in pixels. */
    internal val edgeRoom = Rect()

    /** The screen size while the keyboard is closed, so the keyboard never shrinks Height 100. */
    internal var stableWidth = 0
    internal var stableHeight = 0
    internal val themeRows = ArrayList<ThemeRow>()

    /** Width and Height boxes inside the open Size folder, kept in step with the top boxes. */
    internal val sizeRows = ArrayList<Pair<String, NumberBox>>()
    internal var headingRing: HoloRingView? = null
    internal var guidance: LaserTypeView? = null
    internal var openFolder: PanelFolder? = null
    internal var activeId = prefs.activeFolder?.takeIf { saved -> home.folders.any { it.id == saved } }
        ?: home.folders.first().id
    internal var detailAnim: Animator? = null
    internal var panelAnim: Animator? = null
    internal var centerAnim: ValueAnimator? = null
    internal var minimized = prefs.minimized
    internal var fullBleed = false
    internal var seeThroughAtFullSize = false
    internal var visibleWindowEdges: ScreenEdges? = null

    internal var placed = false
    internal var userY = 0f
    internal var keyboardLift = 0f
    internal var keyboardHeight = 0
    internal var keyboardCover = 0

    /** The glass size in pixels while a finger is resizing. Otherwise the saved Width and Height decide. */
    internal var liveSize: Pair<Float, Float>? = null
    internal var pinch: Pinch? = null
    internal var pinchDone = false
    internal var resizeZone: PanelResizeZone? = null
    internal var resizeDownX = 0f
    internal var resizeDownY = 0f
    internal var resizeStartWidth = 0f
    internal var resizeStartHeight = 0f
    internal var resizeMoved = false
    internal var resizePulseAnimator: ValueAnimator? = null
    internal var lastLoggedGlassWidth = -1
    internal var lastLoggedGlassHeight = -1
    internal var lastLoggedDisplayWidth = -1
    internal var lastLoggedDisplayHeight = -1
    internal var shownOnScreen = false
    internal var ticking = false
    internal val glassBox = RectF()

    /** The row that glows while its box is typed in, and the box. */
    internal var focusHost: View? = null
    internal var focusBox: EditText? = null

    /** Light ripples where a finger touched the edge. */
    internal val ripples = ArrayList<Ripple>()
    internal val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    internal val rippleEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        blendMode = BlendMode.PLUS
    }
    internal var rippleBloom: RadialGradient? = null
    internal var rippleLight: RadialGradient? = null
    internal var rippleColor = 0
    internal val rippleMatrix = Matrix()

    internal var guides: SnapGuideView? = null

    internal val barViews = ArrayList<View>()

    /** The bars the open panel covers now. They fade away, so their words never show through the glass. */
    internal val coveredBars = HashSet<View>()
    internal val barMoved = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshCoveredBars() }

    /** True while the record bar shows a button Royalty must tap, like Try again or Open settings. */
    internal var barNeedsTap = false

    /** True when the panel shrank by itself to show that button. It comes back when the camera is live. */
    internal var steppedAside = false
    internal val barTexts = ArrayList<TextView>()
    internal val barButtons = ArrayList<TextView>()

    /** Called with a setting's key whenever any setting is saved, so the camera screen can follow it. */
    var onSettingChanged: ((String) -> Unit)? = null

    /** Called when the Test diamond toggle changes. The camera screen passes it to the video painter. */
    var onTestDiamond: ((Boolean) -> Unit)? = null

    /** Called when a button asks the camera screen to do something, like Shatter now or Open TikTok. */
    internal var onAction: ((PanelAction, String?) -> Unit)? = null

    /** Called when any of Royalty's own toggles flips (not test toggles), so fireworks can burst. */
    var onToggled: ((String, Boolean) -> Unit)? = null

    /** Saved profiles include the look, position, camera mode, and effect settings. */
    internal val presets = LookPresets(context, prefs)

    /** Every effect setting right now, for the camera's effects. */
    fun effectSnapshot(): com.offgrid.compositor.effects.EffectSnapshot = EffectSettings.snapshot(store)

    /** Updates an effect toggle from its owner and keeps the visible panel row in sync. */
    fun setToggleSetting(key: String, on: Boolean) {
        val changed = store.toggle(key) != on
        store.setToggle(key, on)
        if (::topBar.isInitialized) topBar.setQuickToggle(key, on)
        refreshLook()
        openFolder?.takeIf { folder -> folder.items.any { it is ToggleItem && it.key == key } }?.let { folder ->
            val scroll = detailScroll.scrollY
            buildDetail(folder)
            detailScroll.post { detailScroll.scrollTo(0, scroll) }
        }
        if (changed) {
            settingChanged(key)
            onToggled?.invoke(key, on)
        }
    }

    /** The panel's glow color, for effects that follow the panel's look. */
    val glowColor: Int get() = look.outerGlow

    /** The title of the folder with [id], like "TikTok", or null. */
    fun folderTitle(id: String): String? = home.folders.firstOrNull { it.id == id }?.title

    /** Types [message] on the panel's status line, so the camera screen can answer in the same place. */
    fun announce(message: String) {
        say(message)
    }

    /** True when the Step 1 test diamond is painted into the video. It starts off. */
    val testDiamondOn: Boolean get() = store.toggle(LookKeys.TEST_DIAMOND)

    /** The phone's Back key closes the ⋮ menu first, then goes up one folder at a time. */
    val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                menu.isOpen -> menu.close(animate = true)
                else -> goUp()
            }
        }
    }

    internal val focusWatcher = ViewTreeObserver.OnGlobalFocusChangeListener { old, now ->
        showTypingGlow(now)
        // A top box that was kept for typing may now tuck away on a narrow panel.
        if (::topBar.isInitialized && (old === topBar.widthInput || old === topBar.heightInput)) requestLayout()
        if (now is EditText && keyboardHeight > 0) post { keepFocusAboveKeyboard() }
    }

    /** A two finger pinch, from where it started. */
    internal class Pinch(
        val spanX: Float,
        val spanY: Float,
        val width: Float,
        val height: Float,
        val centerX: Float,
        val centerY: Float,
    )

    internal class Ripple(val x: Float, val y: Float) {
        var progress = 0f
    }

    init {
        setPadding(margin, margin, margin, margin)
        background = glass

        // A tiny spot that takes focus when a typed box lets go, so the keyboard stays shut.
        focusSink.isFocusable = true
        focusSink.isFocusableInTouchMode = true
        focusSink.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(focusSink, LayoutParams(1, 1))

        menu = PanelMenu(context, folderSettings, menuListener())
        backOrb.setOnClickListener { goUp() }

        expanded.orientation = LinearLayout.VERTICAL
        expanded.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, dpi(TOP_BAR_HEIGHT_DP)))
        statusLine.minLines = 2
        statusLine.setPadding(dpi(14f), dpi(4f), dpi(12f), dpi(6f))
        expanded.addView(statusLine, LinearLayout.LayoutParams(MATCH, WRAP))
        expanded.addView(
            GlowRule(context, vertical = false, decor = true),
            LinearLayout.LayoutParams(MATCH, dpi(1f)).apply {
                marginStart = dpi(12f)
                marginEnd = dpi(12f)
            },
        )
        body.orientation = LinearLayout.HORIZONTAL
        body.addView(buildMainArea(), LinearLayout.LayoutParams(0, MATCH, 1f))
        expanded.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))
        addView(expanded, LayoutParams(MATCH, MATCH))

        addView(scanLine, LayoutParams(MATCH, dpi(22f), Gravity.TOP).apply {
            leftMargin = dpi(18f)
            rightMargin = dpi(18f)
        })

        buildPill()
        addView(pill, LayoutParams(WRAP, WRAP))

        ViewCompat.addAccessibilityAction(this, context.getString(R.string.terminal_bigger)) { _, _ ->
            resizeBy(dp(STEP_DP))
            true
        }
        ViewCompat.addAccessibilityAction(this, context.getString(R.string.terminal_smaller)) { _, _ ->
            resizeBy(-dp(STEP_DP))
            true
        }

        // The ⋮ menu sits over everything inside the glass.
        addView(menu, LayoutParams(MATCH, MATCH))

        applyLook()
        applyFolderLayout()
        markActive(activeId)
        showMinimized(minimized, animate = false)
        say(context.getString(R.string.terminal_welcome))
    }

    /**
     * Paints the record bar to match the panel. The panel repaints it whenever the theme changes.
     * [bars] are the bar backgrounds, [texts] the messages, and [buttons] the bar's buttons.
     */
    fun matchLook(bars: List<View>, texts: List<TextView>, buttons: List<TextView>) {
        barViews.forEach { it.removeOnLayoutChangeListener(barMoved) }
        barViews.clear()
        barViews += bars
        // A bar that grows or shrinks, like when the Record buttons show, is checked again.
        bars.forEach { it.addOnLayoutChangeListener(barMoved) }
        barTexts.clear()
        barTexts += texts
        barButtons.clear()
        barButtons += buttons
        RecordBarLook.apply(look, barViews, barTexts, barButtons)
        post { refreshCoveredBars() }
    }

    /** The full-screen view that draws the soft center cross while the panel is dragged. */
    fun useGuides(view: SnapGuideView) {
        guides = view
        view.applyLook(look)
    }

    /**
     * The phone's bars, camera hole, gesture areas, and round corners. Width 100 and Height 100
     * fill the whole screen, edge to edge. The words and buttons stay out of these parts.
     */
    fun onScreenEdges(next: ScreenEdges) {
        if (next == edges) return
        edges = next
        refreshEdgeRoom()
        requestLayout()
    }

    /** The same as [onScreenEdges] with only the bars and the camera hole. */
    fun onSystemInsets(left: Int, top: Int, right: Int, bottom: Int) {
        onScreenEdges(edges.copy(left = left, top = top, right = right, bottom = bottom))
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        val cutout = insets.getInsets(WindowInsets.Type.displayCutout())
        val gestures = insets.getInsets(WindowInsets.Type.systemGestures())
        val positions = intArrayOf(
            RoundedCorner.POSITION_TOP_LEFT,
            RoundedCorner.POSITION_TOP_RIGHT,
            RoundedCorner.POSITION_BOTTOM_RIGHT,
            RoundedCorner.POSITION_BOTTOM_LEFT,
        )
        val radii = FloatArray(8)
        var cornerRadius = 0
        positions.forEachIndexed { index, position ->
            val radius = insets.getRoundedCorner(position)?.radius?.toFloat() ?: 0f
            radii[index * 2] = radius
            radii[index * 2 + 1] = radius
            cornerRadius = maxOf(cornerRadius, radius.toInt())
        }
        visibleWindowEdges = ScreenEdges(
            top = maxOf(bars.top, cutout.top),
            bottom = maxOf(bars.bottom, cutout.bottom),
            left = maxOf(bars.left, cutout.left),
            right = maxOf(bars.right, cutout.right),
            gestureLeft = gestures.left,
            gestureRight = gestures.right,
            gestureBottom = gestures.bottom,
            cornerRadius = cornerRadius,
        )
        glass.setDisplayCornerRadii(radii)
        refreshEdgeRoom()
        requestLayout()
        return super.onApplyWindowInsets(insets)
    }

    /** Lifts the panel above the keyboard while a number or color code is typed. */
    fun onKeyboardInset(bottomInset: Int) {
        keyboardHeight = bottomInset
        fitAboveKeyboard()
    }

    /**
     * The record bar shows a button to tap, like Try again or Open settings, when the camera
     * can't start (final build, Rex's review). The bar never fades then, and the panel shrinks
     * to its tag so the button can be tapped. When the camera is live, the panel comes back.
     */
    fun setBarNeedsTap(needed: Boolean) {
        if (needed == barNeedsTap) return
        barNeedsTap = needed
        if (needed && !minimized && visibility == VISIBLE) {
            steppedAside = true
            showMinimized(true, animate = false, save = false)
            say(context.getString(R.string.terminal_stepped_aside))
        } else if (!needed && steppedAside) {
            steppedAside = false
            if (minimized) showMinimized(false, animate = true, save = false)
        }
        refreshCoveredBars()
    }

    /** Hides the panel while the saved video plays, so the whole video can be checked. */
    fun setHiddenForPlayback(hidden: Boolean) {
        if (hidden) {
            hideKeyboard()
        }
        if (hidden) {
            menu.close(animate = false)
        }
        visibility = if (hidden) GONE else VISIBLE
        refreshBack()
        refreshCoveredBars()
    }

    /** Back is on only while there is something to close. */
    internal fun refreshBack() {
        backCallback.isEnabled = visibility == VISIBLE && !minimized &&
            (menu.isOpen || openFolder != null)
        backOrb.enabledState = visibility == VISIBLE && !minimized && openFolder != null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (minimized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val nextFullBleed = PanelGeometry.isFullBleed(
            store.number(LookKeys.WIDTH),
            store.number(LookKeys.HEIGHT),
            store.toggle(LookKeys.FULL_SCREEN),
        )
        val nextSeeThrough = store.toggle(LookKeys.SEE_THROUGH_FULL_SIZE)
        if (nextFullBleed != fullBleed || nextSeeThrough != seeThroughAtFullSize) {
            val wasFullBleed = fullBleed
            fullBleed = nextFullBleed
            seeThroughAtFullSize = nextSeeThrough
            margin = if (fullBleed) 0 else glowMargin
            glass.margin = margin.toFloat()
            glass.fullBleed = fullBleed
            glass.seeThroughAtFullSize = seeThroughAtFullSize
            if (wasFullBleed && !fullBleed) placed = false
            if (fullBleed) {
                translationX = 0f
                userY = 0f
                translationY = 0f
            }
            applyContentPadding()
            refreshEdgeRoom()
        }
        var areaWidth = MeasureSpec.getSize(widthMeasureSpec)
        var areaHeight = MeasureSpec.getSize(heightMeasureSpec)
        // On older phones the keyboard can shrink the window. Height 100 keeps the size from before.
        if (keyboardHeight == 0) {
            stableWidth = areaWidth
            stableHeight = areaHeight
        } else if (stableHeight > areaHeight) {
            areaWidth = stableWidth
            areaHeight = stableHeight
        }
        val widths = widthRange(areaWidth)
        val heights = heightRange(areaHeight)
        val live = liveSize
        val glassWidth: Int
        val glassHeight: Int
        if (live != null) {
            glassWidth = live.first.roundToInt().coerceIn(widths.first, widths.second)
            glassHeight = live.second.roundToInt().coerceIn(heights.first, heights.second)
        } else if (fullBleed) {
            glassWidth = areaWidth
            glassHeight = areaHeight
        } else {
            glassWidth = sizeFor(store.number(LookKeys.WIDTH), widths)
            glassHeight = sizeFor(store.number(LookKeys.HEIGHT), heights)
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(glassWidth + margin * 2, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(glassHeight + margin * 2, MeasureSpec.EXACTLY),
        )
        if (glassWidth != lastLoggedGlassWidth || glassHeight != lastLoggedGlassHeight ||
            areaWidth != lastLoggedDisplayWidth || areaHeight != lastLoggedDisplayHeight
        ) {
            lastLoggedGlassWidth = glassWidth
            lastLoggedGlassHeight = glassHeight
            lastLoggedDisplayWidth = areaWidth
            lastLoggedDisplayHeight = areaHeight
            Log.d("OFFGRID", "OFFGRID size: glass=${glassWidth}x$glassHeight display=${areaWidth}x$areaHeight")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshLook()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // The title bar's band reaches up under the status bar when the glass does.
        glass.headerHeight = if (minimized) 0f else (topBar.height + edgeRoom.top).toFloat()
        if (fullBleed) {
            translationX = 0f
            userY = 0f
            translationY = 0f
            refreshEdgeRoom()
            return
        }
        val area = parent as? View ?: return
        if (area.width == 0 || area.height == 0) return
        val glassWidth = width - margin * 2
        val glassHeight = height - margin * 2
        val pinching = pinch
        if (!placed) {
            placed = true
            val saved = prefs.position()
            if (saved != null) {
                moveGlassTo(saved.first * areaWidth(), saved.second * areaHeight())
            } else {
                // The first time, the panel opens in the exact middle of the screen.
                val (middleX, middleY) = middleFor(glassWidth, glassHeight)
                moveGlassTo(middleX, middleY)
            }
        } else if (pinching != null) {
            // A pinch grows and shrinks the panel around its middle.
            moveGlassTo(pinching.centerX - glassWidth / 2f, pinching.centerY - glassHeight / 2f)
        } else {
            moveGlassTo(translationX + margin, userY + margin)
        }
        if (keyboardHeight > 0) fitAboveKeyboard()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (resizeZone != null) {
            handleBottomResize(event)
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pinch = null
                pinchDone = false
                glassArea(glassBox)
                if (!glassBox.contains(event.x, event.y)) {
                    // A tap on the glow, outside the glass, goes to whatever is under the panel.
                    return false
                }
                val zone = resizeZoneAt(event.x, event.y)
                if (zone != PanelResizeZone.NONE) {
                    startBottomResize(zone, event)
                    return true
                }
                val focused = findFocus()
                if (focused is EditText && !isInside(focused, event)) {
                    hideKeyboard()
                }
                touchGlow(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pinch == null && !pinchDone && !minimized && !fullBleed && event.pointerCount == 2 && bothOnGlass(event)) {
                    startPinch(event)
                    return true
                }
            }
        }
        if (pinch != null || pinchDone) {
            handlePinch(event)
            return true
        }
        val handled = super.dispatchTouchEvent(event)
        // The glass keeps its own taps, so they never press the record bar under it.
        return handled || event.actionMasked == MotionEvent.ACTION_DOWN
    }

    private fun startPinch(event: MotionEvent) {
        stopCentering()
        val glassWidth = (width - margin * 2).toFloat()
        val glassHeight = (height - margin * 2).toFloat()
        pinch = Pinch(
            spanX = kotlin.math.abs(event.getX(1) - event.getX(0)),
            spanY = kotlin.math.abs(event.getY(1) - event.getY(0)),
            width = glassWidth,
            height = glassHeight,
            centerX = translationX + margin + glassWidth / 2f,
            centerY = userY + margin + glassHeight / 2f,
        )
        val cancel = MotionEvent.obtain(event)
        cancel.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        hideKeyboard()
        glass.holdMasks = true
    }






    /** Sets the breathing glow for this moment, then draws. */
    override fun draw(canvas: Canvas) {
        val current = look
        val live = ticking && GlowClock.motionAllowed
        glass.breath = if (live && current.pulse) breathAt(GlowClock.seconds, current.pulseSpeed) else STILL_BREATH
        backOrb.breath = glass.breath
        super.draw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawRipples(canvas)
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
        stopCentering()
        resizePulseAnimator?.cancel()
        resizePulseAnimator = null
        glass.resizeBarPulse = 1f
        super.onDetachedFromWindow()
    }







    internal fun buildHeader(): View {
        topBar = TopBar(
            context = context,
            prefs = prefs,
            width = store.number(LookKeys.WIDTH),
            height = store.number(LookKeys.HEIGHT),
            backOrb = backOrb,
            onShrink = { showMinimized(true, animate = true) },
            onToggle = { key, on, label ->
                setToggleSetting(key, on)
                say(context.getString(if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off, label))
            },
            onDimensionChanged = { key, value -> setSizePercent(key, value, announce = false) },
            onViewModeChanged = { mode ->
                folderSettings.view = when (mode) {
                    0 -> FolderView.BIG_LIST
                    1 -> FolderView.LIST
                    2 -> FolderView.GRID_2
                    else -> FolderView.GRID_3
                }
                applyFolderLayout()
            },
        ).apply {
            setOnTouchListener(dragHandle())
        }
        return topBar
    }

    internal fun buildMainArea(): View {
        val area = FrameLayout(context)
        mainFolders.setPadding(dpi(4f), dpi(6f), dpi(4f), dpi(6f))
        mainFolders.onOpen = { folder, from -> openFromTop(folder, from) }
        mainFolders.onMoved = { order, before -> foldersMoved(home, order, before) }
        mainFolders.styler = { restyle(it, look) }
        gridScroll.isVerticalScrollBarEnabled = false
        gridScroll.overScrollMode = OVER_SCROLL_NEVER
        gridScroll.addView(mainFolders)
        area.addView(gridScroll, LayoutParams(MATCH, MATCH).apply { marginEnd = dpi(24f) })
        area.addView(GlowScrollBar(context, gridScroll), LayoutParams(dpi(24f), MATCH, Gravity.END))

        detailPane.background = paneGlass
        detailPane.isClickable = true
        detailPane.visibility = INVISIBLE
        detailContent.orientation = LinearLayout.VERTICAL
        detailContent.clipChildren = false
        setDetailPadding()
        detailScroll.isVerticalScrollBarEnabled = false
        detailScroll.overScrollMode = OVER_SCROLL_NEVER
        detailScroll.addView(detailContent)
        detailPane.addView(detailScroll, LayoutParams(MATCH, MATCH).apply { marginEnd = dpi(24f) })
        detailPane.addView(GlowScrollBar(context, detailScroll), LayoutParams(dpi(24f), MATCH, Gravity.END))
        holoSweep.visibility = INVISIBLE
        detailPane.addView(holoSweep, LayoutParams(MATCH, dpi(44f)))
        area.addView(detailPane, LayoutParams(MATCH, MATCH).apply {
            setMargins(dpi(4f), dpi(4f), dpi(8f), dpi(10f))
        })
        return area
    }

    /** The bottom gap leaves room for the keyboard when it covers the panel. */
    internal fun setDetailPadding() {
        detailContent.setPadding(dpi(10f), dpi(6f), dpi(10f), dpi(8f) + keyboardCover)
        menu.setKeyboardRoom(keyboardCover)
    }

    internal fun buildPill() {
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.setPadding(dpi(10f), dpi(7f), dpi(16f), dpi(7f))
        pill.clipChildren = false
        val frame = FrameLayout(context)
        frame.addView(pillRing, LayoutParams(dpi(32f), dpi(32f), Gravity.CENTER))
        frame.addView(
            FolderIconView(context, home.folders.size, null, compact = true, special = true),
            LayoutParams(dpi(19f), dpi(17f), Gravity.CENTER),
        )
        pill.addView(frame, LinearLayout.LayoutParams(dpi(32f), dpi(32f)))
        pill.addView(
            LaserTitleView(context).apply {
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



























    internal fun say(message: String) {
        statusLine.type(PanelText.line(message))
    }









































    internal fun hideKeyboard() {
        val focused = findFocus() ?: return
        if (focused is EditText) {
            focused.clearFocus()
            context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    internal fun dp(value: Float): Float = value * density

    internal fun dpi(value: Float): Int = (value * density + 0.5f).toInt()

    private companion object {
        /** Told to the camera screen when many settings changed at once, like Reset all. */
        const val ALL_SETTINGS = "*"

        /** 📁 Reset already holds Reset all, so it gets no folder Reset of its own. */
        const val RESET_FOLDER_ID = "reset"

        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val GLOW_MARGIN_DP = 32f
        const val MIN_WIDTH_DP = 280f
        const val MIN_HEIGHT_DP = 260f

        /** How close the panel comes to the middle before it clicks into place. */
        const val SNAP_DP = 12f

        /** How far in from the glass edge a touch still lights the edge. */
        const val EDGE_TOUCH_DP = 26f
        const val RIPPLE_MS = 650L
        const val CENTER_MS = 420L
        const val STEP_DP = 40f
        const val RESIZE_ZONE_DP = 24f

        /** How far in a round screen corner cuts, as a share of its radius (1 minus the sine of 45 degrees). */
        const val CORNER_SHARE = 0.3f

        const val STATUS_SP = 11f
        const val GUIDE_SP = 10.5f
        const val STILL_BREATH = 0.8f
        const val BREATH_SLOW_S = 6f
        const val BREATH_FAST_S = 1.2f
        const val OPEN_MS = 420L
        const val CLOSE_MS = 220L
        const val SWEEP_MS = 520L
        const val GRID_FADE_MS = 220L
        const val TOP_BAR_HEIGHT_DP = 56f

        /** A bar counts as covered when the glass hides all of it but this much. */
        const val COVER_SLACK_DP = 2f
        const val COVER_MS = 180L
    }
}
