package com.offgrid.compositor.terminal

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import com.offgrid.compositor.effects.FxKeys
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The floating terminal panel (Step 2, polished in Steps 2.1 and 2.2).
 *
 * True black glass in a dark titanium frame, with a title bar on carbon fiber and a soft
 * living glow. A side list and a grid of big glowing folders. Folders unfold like holograms
 * onto thick sapphire glass. Titles and typed text are Laser Jet Bright.
 * Panel settings holds only folders: Size, Display, Themes, Colors, Glow effects,
 * and Reset.
 *
 * Drag the top bar to move it. A soft cross shows the middle while you drag.
 * Type Width and Height at the top, from 0 to 100, pinch with two fingers, or drag the
 * bottom right corner to resize. All three stay in step and are saved.
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
    private val header = LinearLayout(context)
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
    private val widthBox: SizeBox
    private val heightBox: SizeBox

    private val listRows = HashMap<String, SideRow>()

    /** The folder and its order from before the last drag, for Undo last move. */
    private var lastMove: Pair<PanelFolder, List<PanelFolder>>? = null

    /** Step 2.4: the ⋮ menu's choices, the folder views, the side list drawer, and the menu. */
    private val folderSettings = FolderSettings(prefs)
    private val mainFolders = FolderLayout(context, sub = false)
    private var subFolders: FolderLayout? = null
    private val sideRows = LinearLayout(context)
    private val body = LinearLayout(context)
    private val drawer = SideDrawer(context)
    private val moreButton = MoreButton(context)
    private val menu: PanelMenu
    private var sideMode = folderSettings.side

    /**
     * Final build, milestone 2: the bars, camera hole, gesture areas, and round screen corners.
     * The glass fills the screen edge to edge. Only its words and buttons move in from these.
     */
    private var edges = ScreenEdges()

    /** Extra room inside the glass where it reaches an edge of [edges], in pixels. */
    private val edgeRoom = Rect()

    /** The screen size while the keyboard is closed, so the keyboard never shrinks Height 100. */
    private var stableWidth = 0
    private var stableHeight = 0
    private val themeRows = ArrayList<ThemeRow>()

    /** Width and Height boxes inside the open Size folder, kept in step with the top boxes. */
    private val sizeRows = ArrayList<Pair<String, NumberBox>>()
    private var headingRing: HoloRingView? = null
    private var guidance: LaserTypeView? = null
    private var openFolder: PanelFolder? = null
    private var activeId = prefs.activeFolder?.takeIf { saved -> home.folders.any { it.id == saved } }
        ?: home.folders.first().id
    private var detailAnim: Animator? = null
    private var panelAnim: Animator? = null
    private var centerAnim: ValueAnimator? = null
    private var minimized = prefs.minimized

    private var placed = false
    private var userY = 0f
    private var keyboardLift = 0f
    private var keyboardHeight = 0
    private var keyboardCover = 0

    /** The glass size in pixels while a finger is resizing. Otherwise the saved Width and Height decide. */
    private var liveSize: Pair<Float, Float>? = null
    private var pinch: Pinch? = null
    private var pinchDone = false
    private var shownOnScreen = false
    private var ticking = false
    private val glassBox = RectF()

    /** The row that glows while its box is typed in, and the box. */
    private var focusHost: View? = null
    private var focusBox: EditText? = null

    /** Light ripples where a finger touched the edge. */
    private val ripples = ArrayList<Ripple>()
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val rippleEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        blendMode = BlendMode.PLUS
    }
    private var rippleBloom: RadialGradient? = null
    private var rippleLight: RadialGradient? = null
    private var rippleColor = 0
    private val rippleMatrix = Matrix()

    private var guides: SnapGuideView? = null

    private val barViews = ArrayList<View>()

    /** The bars the open panel covers now. They fade away, so their words never show through the glass. */
    private val coveredBars = HashSet<View>()
    private val barMoved = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshCoveredBars() }

    /** True while the record bar shows a button Royalty must tap, like Try again or Open settings. */
    private var barNeedsTap = false

    /** True when the panel shrank by itself to show that button. It comes back when the camera is live. */
    private var steppedAside = false
    private val barTexts = ArrayList<TextView>()
    private val barButtons = ArrayList<TextView>()

    /** Called with a setting's key whenever any setting is saved, so the camera screen can follow it. */
    var onSettingChanged: ((String) -> Unit)? = null

    /** Called when the Test diamond toggle changes. The camera screen passes it to the video painter. */
    var onTestDiamond: ((Boolean) -> Unit)? = null

    /** Called when a button asks the camera screen to do something, like Shatter now or Open TikTok. */
    internal var onAction: ((PanelAction, String?) -> Unit)? = null

    /** Called when any of Royalty's own toggles flips (not test toggles), so fireworks can burst. */
    var onToggled: ((String, Boolean) -> Unit)? = null

    /** Final build: the saved looks in 📁 Presets. */
    private val presets = LookPresets(context, prefs)

    /** Every effect setting right now, for the camera's effects. */
    fun effectSnapshot(): com.offgrid.compositor.effects.EffectSnapshot = EffectSettings.snapshot(store)

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

    /** The phone's Back key closes the ⋮ menu or the side drawer first, then one folder at a time. */
    val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                menu.isOpen -> menu.close(animate = true)
                drawer.isOpen -> drawer.close(animate = true)
                else -> goUp()
            }
        }
    }

    private val focusWatcher = ViewTreeObserver.OnGlobalFocusChangeListener { old, now ->
        showTypingGlow(now)
        // A top box that was kept for typing may now tuck away on a narrow panel.
        if (old === widthBox.box || old === heightBox.box) requestLayout()
        if (now is EditText && keyboardHeight > 0) post { keepFocusAboveKeyboard() }
    }

    /** A two finger pinch, from where it started. */
    private class Pinch(
        val spanX: Float,
        val spanY: Float,
        val width: Float,
        val height: Float,
        val centerX: Float,
        val centerY: Float,
    )

    private class Ripple(val x: Float, val y: Float) {
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

        widthBox = SizeBox(
            context,
            context.getString(R.string.terminal_width_caption),
            context.getString(R.string.terminal_width),
            store.number(LookKeys.WIDTH),
            onChange = { value, announce -> setSizePercent(LookKeys.WIDTH, value, announce) },
            onProblem = { say(it) },
        )
        heightBox = SizeBox(
            context,
            context.getString(R.string.terminal_height_caption),
            context.getString(R.string.terminal_height),
            store.number(LookKeys.HEIGHT),
            onChange = { value, announce -> setSizePercent(LookKeys.HEIGHT, value, announce) },
            onProblem = { say(it) },
        )

        expanded.orientation = LinearLayout.VERTICAL
        expanded.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, WRAP))
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
        body.addView(buildSideList(), LinearLayout.LayoutParams(dpi(SIDE_MAX_DP), MATCH))
        body.addView(
            sideRule,
            LinearLayout.LayoutParams(dpi(1f), MATCH).apply {
                topMargin = dpi(8f)
                bottomMargin = dpi(14f)
            },
        )
        body.addView(buildMainArea(), LinearLayout.LayoutParams(0, MATCH, 1f))
        // The body, with the side list drawer over it for Swipe to open.
        val bodyFrame = FrameLayout(context)
        bodyFrame.addView(body, LayoutParams(MATCH, MATCH))
        drawer.visibility = GONE
        drawer.onOpenChanged = { open ->
            refreshBack()
            if (open) say(context.getString(R.string.terminal_side_opened))
        }
        bodyFrame.addView(drawer, LayoutParams(MATCH, MATCH))
        expanded.addView(bodyFrame, LinearLayout.LayoutParams(MATCH, 0, 1f))
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

        // The ⋮ menu sits over everything inside the glass.
        addView(menu, LayoutParams(MATCH, MATCH))

        applyLook()
        applySideMode(announce = false)
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
            drawer.close(animate = false)
        }
        visibility = if (hidden) GONE else VISIBLE
        refreshBack()
        refreshCoveredBars()
    }

    /** Back is on only while there is something to close. */
    private fun refreshBack() {
        backCallback.isEnabled = visibility == VISIBLE && !minimized &&
            (menu.isOpen || drawer.isOpen || openFolder != null)
    }

    /** The widths the glass may take on this screen, in pixels: smallest readable, then the whole screen. */
    private fun widthRange(areaWidth: Int): Pair<Int, Int> {
        val most = areaWidth.coerceAtLeast(0)
        return minOf(dpi(MIN_WIDTH_DP), most) to most
    }

    /** The heights the glass may take: smallest readable, then the whole screen, edge to edge. */
    private fun heightRange(areaHeight: Int): Pair<Int, Int> {
        val most = areaHeight.coerceAtLeast(0)
        return minOf(dpi(MIN_HEIGHT_DP), most) to most
    }

    /** Where the glass's top left corner goes to sit in the exact middle of the screen. */
    private fun middleFor(glassWidth: Int, glassHeight: Int): Pair<Float, Float> {
        return (areaWidth() - glassWidth) / 2f to (areaHeight() - glassHeight) / 2f
    }

    private fun sizeFor(percent: Int, range: Pair<Int, Int>): Int =
        range.first + ((range.second - range.first) * percent.coerceIn(0, 100) / 100f).roundToInt()

    private fun percentFor(size: Float, range: Pair<Int, Int>): Int {
        if (range.second <= range.first) return 100
        return ((size - range.first) / (range.second - range.first) * 100f).roundToInt().coerceIn(0, 100)
    }

    /** The screen width. While the keyboard is open, the size from before it opened. */
    private fun areaWidth(): Int {
        val now = (parent as? View)?.width ?: 0
        return if (keyboardHeight > 0 && stableWidth > 0) stableWidth else now
    }

    /** The screen height. While the keyboard is open, the size from before it opened, so nothing jumps. */
    private fun areaHeight(): Int {
        val now = (parent as? View)?.height ?: 0
        return if (keyboardHeight > 0 && stableHeight > 0) maxOf(stableHeight, now) else now
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (minimized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
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
        } else {
            glassWidth = sizeFor(store.number(LookKeys.WIDTH), widths)
            glassHeight = sizeFor(store.number(LookKeys.HEIGHT), heights)
        }
        // The side list shrinks a little with the panel, so the open folder keeps its room.
        // A very narrow panel hides it. The big folders still show every top folder.
        val glassDp = glassWidth / density
        val sideWidth = dpi((glassDp * SIDE_SHARE).coerceIn(SIDE_MIN_DP, SIDE_MAX_DP))
        when (sideMode) {
            SideMode.SHOW -> {
                val showSide = glassDp >= SIDE_SHOW_DP
                if (sideList.isVisible != showSide) sideList.isVisible = showSide
                if (sideRule.isVisible != showSide) sideRule.isVisible = showSide
                sideList.layoutParams.width = sideWidth
            }
            SideMode.SWIPE -> {
                if (!sideList.isVisible) sideList.isVisible = true
                if (sideRule.isVisible) sideRule.isVisible = false
                drawer.setSheetWidth(sideWidth + dpi(DRAWER_EXTRA_DP))
            }
            SideMode.HIDDEN -> {
                if (sideList.isVisible) sideList.isVisible = false
                if (sideRule.isVisible) sideRule.isVisible = false
            }
        }
        // A narrow panel keeps its title readable. Width and Height are still in 📁 Size.
        // A box being typed in stays until the typing is done.
        val typing = widthBox.box.hasFocus() || heightBox.box.hasFocus()
        val showBoxes = glassDp >= HEADER_BOXES_DP || typing
        if (widthBox.isVisible != showBoxes) {
            widthBox.isVisible = showBoxes
            heightBox.isVisible = showBoxes
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(glassWidth + margin * 2, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(glassHeight + margin * 2, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // The title bar's band reaches up under the status bar when the glass does.
        glass.headerHeight = if (minimized) 0f else (header.height + edgeRoom.top).toFloat()
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pinch = null
                pinchDone = false
                glassArea(glassBox)
                if (!glassBox.contains(event.x, event.y)) {
                    // A tap on the glow, outside the glass, goes to whatever is under the panel.
                    return false
                }
                val focused = findFocus()
                if (focused is EditText && !isInside(focused, event)) {
                    hideKeyboard()
                }
                touchGlow(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pinch == null && !pinchDone && !minimized && event.pointerCount == 2 && bothOnGlass(event)) {
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

    /** The glass edges inside this view. They follow the size at once, before the next frame is drawn. */
    private fun glassArea(out: RectF) {
        out.set(margin.toFloat(), margin.toFloat(), (width - margin).toFloat(), (height - margin).toFloat())
    }

    private fun bothOnGlass(event: MotionEvent): Boolean {
        glassArea(glassBox)
        return glassBox.contains(event.getX(0), event.getY(0)) && glassBox.contains(event.getX(1), event.getY(1))
    }

    /** Two fingers: the pinch takes over until both fingers lift, and the panel's buttons let go. */
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
        // The pinch is set first, so a corner drag that lets go here does not save a size of its own.
        val cancel = MotionEvent.obtain(event)
        cancel.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        hideKeyboard()
        glass.holdMasks = true
    }

    /** Sideways pinch changes width, up and down changes height, and a slant changes both. */
    private fun handlePinch(event: MotionEvent) {
        val started = pinch
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (started == null || event.pointerCount < 2) return
                val spanX = kotlin.math.abs(event.getX(1) - event.getX(0))
                val spanY = kotlin.math.abs(event.getY(1) - event.getY(0))
                val widths = widthRange(areaWidth())
                val heights = heightRange(areaHeight())
                val newWidth = (started.width + spanX - started.spanX).coerceIn(widths.first.toFloat(), widths.second.toFloat())
                val newHeight = (started.height + spanY - started.spanY).coerceIn(heights.first.toFloat(), heights.second.toFloat())
                liveSize = newWidth to newHeight
                showSizeNumbers(percentFor(newWidth, widths), percentFor(newHeight, heights))
                requestLayout()
            }
            MotionEvent.ACTION_POINTER_UP -> if (started != null) finishPinch()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (started != null) finishPinch()
                pinchDone = false
            }
        }
    }

    private fun finishPinch() {
        pinch = null
        pinchDone = true
        finishLiveSize()
    }

    /** Sets the breathing glow for this moment, then draws. */
    override fun draw(canvas: Canvas) {
        val current = look
        val live = ticking && GlowClock.motionAllowed
        glass.breath = if (live && current.pulse) breathAt(GlowClock.seconds, current.pulseSpeed) else STILL_BREATH
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
        super.onDetachedFromWindow()
    }

    private fun isInside(view: View, event: MotionEvent): Boolean {
        val spot = IntArray(2)
        view.getLocationOnScreen(spot)
        val x = event.rawX
        val y = event.rawY
        return x >= spot[0] && x <= spot[0] + view.width && y >= spot[1] && y <= spot[1] + view.height
    }

    /**
     * Touch glow: a touch on the frame, the title bar, or near the edge lights that spot
     * with a short ripple of living light, then fades. Never with less motion asked for.
     */
    private fun touchGlow(x: Float, y: Float) {
        if (!look.touchGlow || !ValueAnimator.areAnimatorsEnabled()) return
        glassArea(glassBox)
        val band = dp(EDGE_TOUCH_DP)
        val nearEdge = x - glassBox.left < band || glassBox.right - x < band ||
            y - glassBox.top < band || glassBox.bottom - y < band
        val onTitleBar = !minimized && y < glassBox.top + glass.frameInset + edgeRoom.top + header.height
        if (!nearEdge && !onTitleBar && !minimized) return
        val ripple = Ripple(x, y)
        ripples += ripple
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = RIPPLE_MS
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                ripple.progress = it.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                ripples.remove(ripple)
                invalidate()
            }
            start()
        }
    }

    /** Each ripple: a soft bloom that grows and fades, and the nearby edge line burning brighter. */
    private fun drawRipples(canvas: Canvas) {
        if (ripples.isEmpty()) return
        val color = look.edge
        if (rippleBloom == null || rippleLight == null || rippleColor != color) {
            rippleColor = color
            fun gradient() = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(Hud.withAlpha(Hud.lighten(color, 0.6f), 0.9f), Hud.withAlpha(color, 0.5f), Hud.withAlpha(color, 0f)),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            rippleBloom = gradient()
            rippleLight = gradient()
        }
        val bloom = rippleBloom ?: return
        val light = rippleLight ?: return
        ripplePaint.shader = bloom
        rippleEdge.shader = light
        rippleEdge.strokeWidth = dp(2.4f)
        val corner = chromeRadius(context, look) * 3f
        glassArea(glassBox)
        for (ripple in ripples) {
            val fade = (1f - ripple.progress)
            val radius = dp(12f) + dp(52f) * ripple.progress
            ripplePaint.alpha = (255f * fade * fade * 0.85f).toInt()
            canvas.save()
            canvas.translate(ripple.x, ripple.y)
            canvas.scale(radius, radius)
            canvas.drawCircle(0f, 0f, 1f, ripplePaint)
            canvas.restore()
            rippleMatrix.setScale(radius * 1.6f, radius * 1.6f)
            rippleMatrix.postTranslate(ripple.x, ripple.y)
            light.setLocalMatrix(rippleMatrix)
            rippleEdge.alpha = (255f * fade).toInt()
            canvas.drawRoundRect(glassBox, corner, corner, rippleEdge)
        }
    }

    /** Typing glow: the edge of the setting being typed in lights softly. Its row draws it. */
    private fun showTypingGlow(now: View?) {
        val box = if (now is EditText && look.typingGlow && isDescendant(now)) now else null
        val next = box?.parent as? View
        if (next === focusHost && box === focusBox) return
        focusHost?.foreground = null
        focusHost = next
        focusBox = box
        if (next != null) next.foreground = typingGlow(next, box, look)
    }

    private fun typingGlow(host: View, box: EditText, current: PanelLook) =
        FocusGlowDrawable(density, current.edge, chromeRadius(context, current), box, wholeRow = host is NumberRow)

    private fun isDescendant(view: View): Boolean {
        var parentView = view.parent
        while (parentView != null) {
            if (parentView === this) return true
            parentView = parentView.parent
        }
        return false
    }

    private fun buildHeader(): View {
        header.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpi(14f), dpi(6f), dpi(6f), dpi(4f))
            contentDescription = context.getString(R.string.terminal_drag)
            clipChildren = false
            clipToPadding = false
        }
        header.addView(GripView(context), LinearLayout.LayoutParams(dpi(12f), dpi(18f)).apply {
            marginEnd = dpi(9f)
        })
        val title = LaserTitleView(context).apply {
            text = context.getString(R.string.terminal_title)
            typeface = Hud.monoBold(context)
            letterSpacing = 0.12f
            fitWholeLine = true
            setPadding(0, dpi(4f), 0, dpi(4f))
            styled(TextRole.TITLE, 12.5f)
        }
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        header.addView(widthBox, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dpi(4f) })
        header.addView(heightBox, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dpi(4f) })
        moreButton.contentDescription = context.getString(R.string.terminal_menu_button)
        moreButton.setOnClickListener { if (menu.isOpen) menu.close(animate = true) else openMenu() }
        header.addView(moreButton, LinearLayout.LayoutParams(dpi(TARGET_DP), dpi(TARGET_DP)).apply { marginStart = dpi(2f) })
        val shrink = ShrinkButton(context).apply {
            contentDescription = context.getString(R.string.terminal_shrink)
            setOnClickListener { showMinimized(true, animate = true) }
        }
        header.addView(shrink, LinearLayout.LayoutParams(dpi(TARGET_DP), dpi(TARGET_DP)))
        header.setOnTouchListener(dragHandle())
        return header
    }

    private fun buildSideList(): View {
        sideRows.orientation = LinearLayout.VERTICAL
        sideRows.setPadding(dpi(6f), dpi(6f), dpi(2f), dpi(12f))
        for (folder in home.folders) {
            val row = SideRow(context, folder)
            row.setOnClickListener { openFromTop(folder, row) }
            listRows[folder.id] = row
        }
        sideList.isVerticalScrollBarEnabled = false
        sideList.overScrollMode = OVER_SCROLL_NEVER
        sideList.addView(sideRows)
        return sideList
    }

    private fun buildMainArea(): View {
        val area = FrameLayout(context)
        mainFolders.setPadding(dpi(4f), dpi(6f), dpi(4f), dpi(GRIP_DP))
        mainFolders.onOpen = { folder, from -> openFromTop(folder, from) }
        mainFolders.onMoved = { order, before -> foldersMoved(home, order, before) }
        mainFolders.styler = { restyle(it, look) }
        gridScroll.isVerticalScrollBarEnabled = false
        gridScroll.overScrollMode = OVER_SCROLL_NEVER
        gridScroll.addView(mainFolders)
        area.addView(gridScroll, LayoutParams(MATCH, MATCH))

        detailPane.background = paneGlass
        detailPane.isClickable = true
        detailPane.visibility = INVISIBLE
        detailContent.orientation = LinearLayout.VERTICAL
        detailContent.clipChildren = false
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
        menu.setKeyboardRoom(keyboardCover)
    }

    private fun buildPill() {
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

    /**
     * Drag to move the panel. A short tap counts as a normal tap.
     * With Center guide on, a soft cross shows the middle, and the panel clicks into the
     * middle when it comes close.
     */
    private fun dragHandle(): OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var dragging = false
        var snappedX = false
        var snappedY = false
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        return OnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopCentering()
                    downX = event.rawX
                    downY = event.rawY
                    startX = translationX + margin
                    startY = userY + margin
                    dragging = false
                    snappedX = false
                    snappedY = false
                    view.isPressed = true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        view.isPressed = false
                        parent?.requestDisallowInterceptTouchEvent(true)
                        if (look.centerGuide) {
                            guides?.setMiddle(areaWidth() / 2f, areaHeight() / 2f)
                            guides?.showGuides()
                        }
                    }
                    if (dragging) {
                        var x = startX + dx
                        var y = startY + dy
                        if (look.centerGuide) {
                            val glassWidth = width - margin * 2
                            val glassHeight = height - margin * 2
                            val (middleX, middleY) = middleFor(glassWidth, glassHeight)
                            val near = dp(SNAP_DP)
                            val nowX = kotlin.math.abs(x - middleX) < near
                            val nowY = kotlin.math.abs(y - middleY) < near
                            if (nowX) x = middleX
                            if (nowY) y = middleY
                            if ((nowX && !snappedX) || (nowY && !snappedY)) {
                                if (look.tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            snappedX = nowX
                            snappedY = nowY
                            guides?.setSnapped(nowX, nowY)
                        }
                        moveGlassTo(x, y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.isPressed = false
                    if (dragging) savePosition() else view.performClick()
                    dragging = false
                    guides?.hideGuides()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    if (dragging) savePosition()
                    dragging = false
                    guides?.hideGuides()
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
                    stopCentering()
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
                    if (resizing) finishLiveSize() else view.performClick()
                    resizing = false
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    if (resizing && pinch == null) finishLiveSize()
                    resizing = false
                }
            }
            true
        }
    }

    /** Sets a new glass size in pixels. It never goes past the screen, or below the smallest size. */
    private fun resizeGlassTo(newWidth: Float, newHeight: Float) {
        if (parent !is View) return
        val widths = widthRange(areaWidth())
        val heights = heightRange(areaHeight())
        val roomWidth = (areaWidth() - (translationX + margin)).coerceIn(widths.first.toFloat(), widths.second.toFloat())
        val roomHeight = (areaHeight() - (userY + margin)).coerceIn(heights.first.toFloat(), heights.second.toFloat())
        val glassWidth = newWidth.coerceIn(widths.first.toFloat(), roomWidth)
        val glassHeight = newHeight.coerceIn(heights.first.toFloat(), roomHeight)
        liveSize = glassWidth to glassHeight
        showSizeNumbers(percentFor(glassWidth, widths), percentFor(glassHeight, heights))
        requestLayout()
    }

    /** Saves the size a finger chose as Width and Height, from 0 to 100, and says so. */
    private fun finishLiveSize() {
        val live = liveSize
        glass.holdMasks = false
        if (live != null) {
            val widthPercent = percentFor(live.first, widthRange(areaWidth()))
            val heightPercent = percentFor(live.second, heightRange(areaHeight()))
            store.setNumber(LookKeys.WIDTH, widthPercent)
            store.setNumber(LookKeys.HEIGHT, heightPercent)
            liveSize = null
            showSizeNumbers(widthPercent, heightPercent)
            requestLayout()
            post { savePosition() }
            say(context.getString(R.string.terminal_size_saved, widthPercent, heightPercent))
            if (look.tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    /** For screen readers: the Bigger and Smaller actions on the resize corner. */
    private fun resizeBy(step: Float) {
        if (minimized) return
        resizeGlassTo((width - margin * 2) + step, (height - margin * 2) + step)
        finishLiveSize()
    }

    /** A typed Width or Height, from the top boxes or the Size folder. */
    private fun setSizePercent(key: String, value: Int, announce: Boolean) {
        store.setNumber(key, value)
        liveSize = null
        showSizeNumbers(store.number(LookKeys.WIDTH), store.number(LookKeys.HEIGHT))
        requestLayout()
        if (announce) {
            val label = context.getString(if (key == LookKeys.WIDTH) R.string.terminal_width else R.string.terminal_height)
            say(context.getString(R.string.terminal_number_saved, label, value))
            post { savePosition() }
        }
    }

    /** Shows Width and Height in every box that shows them, except one being typed in. */
    private fun showSizeNumbers(widthPercent: Int, heightPercent: Int) {
        fun show(box: NumberBox, value: Int) {
            if (!box.hasFocus() && box.value != value) box.show(value)
        }
        show(widthBox.box, widthPercent)
        show(heightBox.box, heightPercent)
        for ((key, box) in sizeRows) {
            show(box, if (key == LookKeys.WIDTH) widthPercent else heightPercent)
        }
    }

    /** Center panel: glides the panel to the middle of the screen, then saves the place. */
    private fun centerPanel() {
        val area = parent as? View ?: return
        stopCentering()
        val fromX = translationX + margin
        val fromY = userY + margin
        if (area.width == 0) return
        val (toX, toY) = middleFor(width - margin * 2, height - margin * 2)
        if (!ValueAnimator.areAnimatorsEnabled()) {
            moveGlassTo(toX, toY)
            savePosition()
        } else {
            centerAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CENTER_MS
                interpolator = DecelerateInterpolator(1.4f)
                addUpdateListener {
                    val t = it.animatedValue as Float
                    moveGlassTo(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t)
                }
                doOnEnd {
                    if (centerAnim === it) {
                        centerAnim = null
                        savePosition()
                    }
                }
                start()
            }
        }
        say(context.getString(R.string.terminal_centered))
    }

    private fun stopCentering() {
        val running = centerAnim ?: return
        centerAnim = null
        running.cancel()
        savePosition()
    }

    /** Moves the glass so its top left corner is at x, y. The glass always stays on the screen. */
    private fun moveGlassTo(x: Float, y: Float) {
        if (parent !is View) return
        val glassWidth = (width - margin * 2).toFloat()
        val glassHeight = (height - margin * 2).toFloat()
        val glassX = x.coerceIn(0f, (areaWidth() - glassWidth).coerceAtLeast(0f))
        val glassY = y.coerceIn(0f, (areaHeight() - glassHeight).coerceAtLeast(0f))
        translationX = glassX - margin
        userY = glassY - margin
        translationY = userY - keyboardLift
        refreshEdgeRoom()
        refreshCoveredBars()
    }

    /**
     * Where the glass reaches an edge of the screen, its words and buttons move in, out of
     * the bars, the camera hole, and the round corners. The resize corner also stays out of
     * the Back swipe area. The glass itself still fills the screen.
     */
    private fun refreshEdgeRoom() {
        if (parent !is View || minimized) {
            if (edgeRoom.left != 0 || edgeRoom.top != 0 || edgeRoom.right != 0 || edgeRoom.bottom != 0) {
                edgeRoom.set(0, 0, 0, 0)
                applyContentPadding()
            }
            return
        }
        val areaWidth = areaWidth()
        val areaHeight = areaHeight()
        if (areaWidth == 0 || areaHeight == 0) return
        val glassLeft = (translationX + margin).roundToInt()
        val glassTop = (userY + margin).roundToInt()
        val glassRight = glassLeft + width - margin * 2
        val glassBottom = glassTop + height - margin * 2
        // A round screen corner cuts in about this far. The buttons stay clear of it.
        val corner = (edges.cornerRadius * CORNER_SHARE).roundToInt()
        val top = (edges.top - glassTop).coerceAtLeast(0)
        val bottom = (glassBottom - (areaHeight - maxOf(edges.bottom, edges.gestureBottom))).coerceAtLeast(0)
        val left = (maxOf(edges.left, corner) - glassLeft).coerceAtLeast(0)
        val right = (glassRight - (areaWidth - maxOf(edges.right, corner))).coerceAtLeast(0)
        if (edgeRoom.left == left && edgeRoom.top == top && edgeRoom.right == right && edgeRoom.bottom == bottom) {
            placeResizeGrip(glassRight, areaWidth, right)
            return
        }
        edgeRoom.set(left, top, right, bottom)
        applyContentPadding()
        placeResizeGrip(glassRight, areaWidth, right)
    }

    /** The resize corner stays out of the Back swipe area on the right edge. */
    private fun placeResizeGrip(glassRight: Int, areaWidth: Int, roomRight: Int) {
        val params = resizeGrip.layoutParams as? LayoutParams ?: return
        val gesture = (glassRight - (areaWidth - edges.gestureRight)).coerceAtLeast(0)
        val extra = (gesture - roomRight).coerceAtLeast(0)
        if (params.rightMargin != extra) {
            params.rightMargin = extra
            resizeGrip.layoutParams = params
        }
    }

    /** The glow margin, the titanium frame, and any room for the screen's edges. */
    private fun applyContentPadding() {
        val base = margin + glass.frameInset.roundToInt()
        val left = base + edgeRoom.left
        val top = base + edgeRoom.top
        val right = base + edgeRoom.right
        val bottom = base + edgeRoom.bottom
        if (paddingLeft != left || paddingTop != top || paddingRight != right || paddingBottom != bottom) {
            setPadding(left, top, right, bottom)
        }
    }

    /**
     * A bar under the open panel fades away while the glass covers it, and comes back when
     * the panel moves off it, shrinks to its tag, or hides. Taps there already go to the panel.
     */
    private fun refreshCoveredBars() {
        if (barViews.isEmpty()) return
        val open = isAttachedToWindow && visibility == VISIBLE && !minimized && !barNeedsTap
        val here = IntArray(2)
        val there = IntArray(2)
        if (open) getLocationInWindow(here)
        val glassLeft = here[0] + margin
        val glassTop = here[1] + margin
        val glassRight = here[0] + width - margin
        val glassBottom = here[1] + height - margin
        for (bar in barViews) {
            var covered = false
            if (open && bar.height > 0) {
                bar.getLocationInWindow(there)
                val across = minOf(glassRight, there[0] + bar.width) - maxOf(glassLeft, there[0])
                val down = minOf(glassBottom, there[1] + bar.height) - maxOf(glassTop, there[1])
                // Only a bar the glass hides completely fades. A bar that peeks out stays,
                // so a button you can tap is always a button you can see.
                val slack = dpi(COVER_SLACK_DP)
                covered = across >= bar.width - slack && down >= bar.height - slack
            }
            if (covered == bar in coveredBars) continue
            if (covered) coveredBars += bar else coveredBars -= bar
            bar.importantForAccessibility =
                if (covered) IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else IMPORTANT_FOR_ACCESSIBILITY_AUTO
            val target = if (covered) 0f else 1f
            bar.animate().cancel()
            if (ValueAnimator.areAnimatorsEnabled()) {
                bar.animate().alpha(target).setDuration(COVER_MS).start()
            } else {
                bar.alpha = target
            }
        }
    }

    private fun savePosition() {
        val areaWidth = areaWidth()
        val areaHeight = areaHeight()
        if (areaWidth > 0 && areaHeight > 0) {
            prefs.setPosition((translationX + margin) / areaWidth, (userY + margin) / areaHeight)
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
            (glassBottom - visibleBottom).coerceAtMost((glassTop - edges.top).coerceAtLeast(0f))
        } else {
            0f
        }
        translationY = userY - keyboardLift
        refreshCoveredBars()
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
            // The box's own list scrolls: an open folder, or the ⋮ menu card.
            val list = generateSequence(focused.parent) { it.parent }.filterIsInstance<ScrollView>().firstOrNull() ?: detailScroll
            list.smoothScrollBy(0, hidden.toInt())
        }
    }

    private fun showMinimized(value: Boolean, animate: Boolean, save: Boolean = true) {
        minimized = value
        if (save) prefs.minimized = value
        // Royalty's own tap decides from now on.
        if (save) steppedAside = false
        if (value) {
            hideKeyboard()
            menu.close(animate = false)
            drawer.close(animate = false)
        }
        expanded.isVisible = !value
        pill.isVisible = value
        refreshEdgeRoom()
        resizeGrip.isVisible = !value
        scanLine.isVisible = !value && look.scanLine
        pillRing.isVisible = look.ring
        pillRing.spinning = value && look.ring
        refreshBack()
        requestLayout()
        refreshCoveredBars()
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
        // The titanium frame is drawn inside the glass edge, so the content starts inside it.
        applyContentPadding()
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
        guides?.applyLook(current)
        mainFolders.tick = current.tick
        subFolders?.tick = current.tick
        drawer.tick = current.tick
        val host = focusHost
        val box = focusBox
        if (host != null && box != null) {
            host.foreground = if (current.typingGlow) typingGlow(host, box, current) else null
        }
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
        typer.cursorGlow = current.cursorGlow
        typer.glowStrength = current.glowStrength
        typer.glowBoost = current.glowBoost
        typer.speed = current.lettersPerSecond
        typer.animateTyping = current.laserTyping
    }

    private fun styleGuidance(typer: LaserTypeView) {
        val current = look
        styleTyper(typer, current.text, GUIDE_SP)
        typer.background = if (current.sapphire) {
            // The typed guidance sits on its own thick sapphire pane.
            PanelGlassDrawable(density, 0f, pane = true).apply { look = current }
        } else {
            GradientDrawable().apply {
                setColor(Hud.withAlpha(Hud.blend(current.skin, Hud.INK, 0.6f), 0.55f))
                cornerRadius = chromeRadius(context, current)
                setStroke(dpi(1f), Hud.withAlpha(current.text, 0.22f))
            }
        }
    }

    /** The glow moves only while it is on screen, and never when the phone asks for less motion. */
    private fun refreshClock() {
        val current = look
        val glowing = current.glowStrength > 0f && (current.outerGlowOn || current.edgeGlow)
        val want = shownOnScreen && glowing && current.pulse && GlowClock.motionAllowed
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

    private fun say(message: String) {
        statusLine.type(PanelText.line(message))
    }

    /** What the ⋮ menu does. Every choice works at once, is saved, and is said in plain words. */
    private fun menuListener() = object : PanelMenu.Listener {
        override fun onViewChosen(view: FolderView) {
            folderSettings.view = view
            menu.refresh()
            applyFolderLayout()
            savedTick()
            say(context.getString(R.string.terminal_view_chosen, view.title))
        }

        override fun onSortChosen(sort: FolderSort) {
            folderSettings.sort = sort
            // Folders move only in your own order, so another sort locks them again.
            if (sort != FolderSort.CUSTOM) folderSettings.unlocked = false
            menu.refresh()
            applyFolderLayout()
            savedTick()
            val message = if (sort == FolderSort.CUSTOM && !folderSettings.unlocked) {
                R.string.terminal_sort_custom
            } else {
                R.string.terminal_sort_chosen
            }
            say(context.getString(message, sort.title))
        }

        override fun onUnlockChanged(unlocked: Boolean) {
            if (unlocked && folderSettings.sort != FolderSort.CUSTOM) {
                if (folderSettings.hasSavedOrder(home)) {
                    // Your own saved order is never replaced without asking.
                    menu.askReplaceOrder()
                    say(context.getString(R.string.terminal_menu_replace_question))
                    return
                }
                // Moving starts from the order on screen now, in every folder, so nothing jumps.
                folderSettings.keepEveryOrder(home)
                folderSettings.sort = FolderSort.CUSTOM
            }
            folderSettings.unlocked = unlocked
            menu.refresh()
            applyFolderLayout()
            say(context.getString(if (unlocked) R.string.terminal_folders_unlocked else R.string.terminal_folders_locked))
        }

        override fun onReplaceOrder(replace: Boolean) {
            if (replace) folderSettings.keepEveryOrder(home)
            folderSettings.sort = FolderSort.CUSTOM
            folderSettings.unlocked = true
            menu.refresh()
            applyFolderLayout()
            say(context.getString(if (replace) R.string.terminal_order_replaced else R.string.terminal_order_kept))
        }

        override fun onUndoMove() {
            val last = lastMove ?: return
            lastMove = null
            menu.setUndoAvailable(false)
            folderSettings.saveOrder(last.first, last.second)
            applyFolderLayout()
            savedTick()
            say(context.getString(R.string.terminal_move_undone))
        }

        override fun onFolderSize(value: Int, announce: Boolean) {
            folderSettings.size = value
            applyFolderLayout()
            if (announce) {
                say(context.getString(R.string.terminal_number_saved, context.getString(R.string.terminal_menu_size_label), value))
            }
        }

        override fun onSideChosen(mode: SideMode) {
            folderSettings.side = mode
            menu.refresh()
            applySideMode(announce = true)
            savedTick()
        }

        override fun onMenuClosed() {
            refreshBack()
        }

        override fun onProblem(message: String) {
            say(message)
        }
    }

    private fun openMenu() {
        hideKeyboard()
        if (drawer.isOpen) drawer.close(animate = false)
        menu.open(header.height)
        refreshBack()
        say(context.getString(R.string.terminal_menu_opened))
    }

    private fun savedTick() {
        if (look.tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** Folders can be dragged only while Move folders is on, in your own order. */
    private fun canMoveFolders(): Boolean = folderSettings.unlocked && folderSettings.sort == FolderSort.CUSTOM


    /** Shows the folders in the chosen view, sort, and size, in the main area, the open folder, and the side list. */
    private fun applyFolderLayout() {
        val view = folderSettings.view
        val size = folderSettings.size
        val moving = canMoveFolders()
        mainFolders.dragOn = moving
        mainFolders.show(folderSettings.arrange(home), view, size)
        mainFolders.setActive(activeId)
        val open = openFolder
        val inside = subFolders
        if (open != null && inside != null) {
            inside.dragOn = moving
            inside.show(folderSettings.arrange(open), view, size)
        }
        arrangeSideRows()
    }

    /** The side list follows the same order as the folders. */
    private fun arrangeSideRows() {
        val wanted = folderSettings.arrange(home).mapNotNull { listRows[it.id] }
        val same = sideRows.childCount == wanted.size && wanted.indices.all { sideRows.getChildAt(it) === wanted[it] }
        if (same) return
        sideRows.removeAllViews()
        for (row in wanted) sideRows.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        // Rows join the list here, so they take the current look here too.
        restyle(sideRows, look)
    }

    /** A folder was dragged to a new spot. The new order is saved at once, and Undo can put it back. */
    private fun foldersMoved(parent: PanelFolder, order: List<PanelFolder>, before: List<PanelFolder>) {
        folderSettings.saveOrder(parent, order)
        lastMove = parent to before
        menu.setUndoAvailable(true)
        if (parent === home) arrangeSideRows()
        say(context.getString(R.string.terminal_folders_moved))
    }

    /** Puts the side list where the ⋮ menu says: always there, in a swipe drawer, or hidden. */
    private fun applySideMode(announce: Boolean) {
        val mode = folderSettings.side
        sideMode = mode
        val holder = sideList.parent as? ViewGroup
        if (mode == SideMode.SWIPE) {
            if (holder !== drawer.sheet) {
                holder?.removeView(sideList)
                drawer.sheet.addView(sideList, LayoutParams(MATCH, MATCH))
            }
            drawer.visibility = VISIBLE
            // The folders start just past the drawer's handle, so a tap on a folder never opens the drawer.
            body.setPadding(drawer.handleWidth(), 0, 0, 0)
        } else {
            drawer.close(animate = false)
            drawer.visibility = GONE
            if (holder !== body) {
                holder?.removeView(sideList)
                body.addView(sideList, 0, LinearLayout.LayoutParams(dpi(SIDE_MAX_DP), MATCH))
            }
            body.setPadding(0, 0, 0, 0)
        }
        refreshBack()
        requestLayout()
        if (announce) {
            val message = when (mode) {
                SideMode.SHOW -> R.string.terminal_side_show
                SideMode.SWIPE -> R.string.terminal_side_swipe
                SideMode.HIDDEN -> R.string.terminal_side_hidden
            }
            say(context.getString(message))
        }
    }

    private fun markActive(id: String) {
        activeId = id
        prefs.activeFolder = id
        mainFolders.setActive(id)
        listRows.forEach { (key, row) -> row.setActive(key == id) }
    }

    private fun openFromTop(folder: PanelFolder, from: View) {
        if (drawer.isOpen) drawer.close(animate = true)
        markActive(folder.id)
        showFolder(folder, from)
    }

    private fun showFolder(folder: PanelFolder, from: View?) {
        hideKeyboard()
        val (pivotX, pivotY) = pivotIn(detailPane, from)
        val wasOpen = openFolder != null
        openFolder = folder
        refreshBack()
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
        refreshBack()
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
        val (pivotX, pivotY) = pivotIn(detailPane, mainFolders.cellFor(folder.topLevel().id))
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
        sizeRows.clear()
        subFolders = null
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
        sizeRows.clear()
        subFolders = null
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
            // Folders inside folders follow the ⋮ menu too: the same view, sort, and size.
            val folders = FolderLayout(context, sub = true)
            folders.onOpen = { child, from -> showFolder(child, from) }
            folders.onMoved = { order, before -> foldersMoved(folder, order, before) }
            folders.styler = { restyle(it, look) }
            folders.tick = look.tick
            folders.dragOn = canMoveFolders()
            folders.show(folderSettings.arrange(folder), folderSettings.view, folderSettings.size)
            subFolders = folders
            detailContent.addView(folders, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        if (folder.items.isNotEmpty()) {
            detailContent.addView(sectionLabel(sectionFor(folder)))
            for (item in folder.items) {
                detailContent.addView(itemView(folder, item), LinearLayout.LayoutParams(MATCH, WRAP))
            }
        }
        // Final build: every folder with settings has its own Reset at the bottom.
        if (folder.id != RESET_FOLDER_ID && FolderReset.hasSettings(folder)) {
            val reset = ActionItem(
                PanelAction.RESET_FOLDER,
                context.getString(R.string.terminal_folder_reset),
                context.getString(R.string.terminal_folder_reset_help),
                folder.id,
            )
            detailContent.addView(actionRow(reset), LinearLayout.LayoutParams(MATCH, WRAP))
        }
        // Paint the new views in the current theme before the typing starts.
        restyle(detailContent, look)
        styleGuidance(typer)
        typer.type(PanelText.guidance(folder, folder.results(::markOf)))
    }

    private fun sectionFor(folder: PanelFolder): Int = when {
        folder.kind == FolderKind.TESTS && folder.items.all { it is ToggleItem } -> R.string.terminal_section_toggles
        folder.items.all { it is ThemeItem } -> R.string.terminal_section_themes
        folder.items.all { it is ColorItem } -> R.string.terminal_section_color
        else -> R.string.terminal_section_settings
    }

    private fun itemView(folder: PanelFolder, item: PanelItem): View = when (item) {
        is NumberItem -> if (item.key == LookKeys.WIDTH || item.key == LookKeys.HEIGHT) {
            NumberRow(
                context,
                item,
                store.number(item.key),
                onChange = { value, announce -> setSizePercent(item.key, value, announce) },
                onProblem = { say(it) },
            ).also { sizeRows += item.key to it.box }
        } else {
            NumberRow(
                context,
                item,
                store.number(item.key),
                onChange = { value, announce -> saveNumber(item, value, announce) },
                onProblem = { say(it) },
            )
        }
        is ToggleItem -> {
            val note = if (item.isTest) markLine(folder, item) else item.help
            val on = if (item.isTest) prefs.toggle(testKey(folder, item), item.default) else store.toggle(item.key)
            ToggleRow(context, item, note, on) { value -> saveToggle(folder, item, value) }
        }
        is ActionItem -> actionRow(item)
        is ThemeItem -> ThemeRow(context, item.theme, store.preview(item.theme), store.base() == item.theme).also { row ->
            themeRows += row
            row.setOnClickListener { applyTheme(item.theme) }
        }
        is ColorItem -> ColorEditorView(context, item.part, store, onLookChanged = ::refreshLook, say = ::say)
        is ChoiceItem -> ChoiceGroup(context, item, store.choice(item.key)) { choice -> saveChoice(item, choice) }
        is TextItem -> TextRow(
            context,
            item,
            store.text(item.key),
            onSave = { value -> saveText(item, value) },
            onProblem = { say(it) },
        )
        is PresetsItem -> PresetRows(context, presets, onLoaded = { lookChanged(folderToShow = openFolder) }, say = ::say)
    }

    /** A pick-one setting changed. It is saved and used at once. */
    private fun saveChoice(item: ChoiceItem, choice: Choice) {
        store.setChoice(item.key, choice.value)
        if (item.key.startsWith(TestMarks.MARK)) {
            // A test result keeps the day it was marked.
            val day = TestMarks.today()
            store.setText(TestMarks.DATE + item.key.removePrefix(TestMarks.MARK), day)
            say(context.getString(R.string.terminal_mark_saved, item.label, choice.label, day))
            return
        }
        refreshLook()
        settingChanged(item.key)
        say(context.getString(R.string.terminal_choice_saved, item.label, choice.label))
    }

    /** A typed text setting changed. It is saved and used at once. */
    private fun saveText(item: TextItem, value: String) {
        store.setText(item.key, value)
        if (item.key.startsWith(TestMarks.SEEN)) {
            checkTestCode(item.key.removePrefix(TestMarks.SEEN), value)
            return
        }
        settingChanged(item.key)
        say(context.getString(R.string.terminal_text_saved, item.label, value))
    }

    /** Feature 37: checks the code a viewer saw against the code the video showed. */
    private fun checkTestCode(platform: String, seen: String) {
        val day = TestMarks.today()
        val message = when (TestMarks.check(store, platform, seen, day)) {
            TestMarks.Check.NO_CODE -> context.getString(R.string.terminal_code_none)
            TestMarks.Check.MATCH -> context.getString(R.string.terminal_code_match, day)
            TestMarks.Check.MISMATCH -> context.getString(R.string.terminal_code_mismatch, day)
        }
        say(message)
    }

    /** Buttons the panel handles itself. The rest go to the camera screen. */
    private fun runAction(item: ActionItem) {
        when (item.action) {
            PanelAction.TEST_CODE -> {
                val platform = item.arg ?: return
                val code = TestMarks.newCode()
                store.setText(TestMarks.SHOWN + platform, code)
                onAction?.invoke(PanelAction.TEST_CODE, code)
                say(context.getString(R.string.terminal_code_shown, code))
            }
            PanelAction.SAFE_START -> safeStart(item.arg ?: return)
            else -> onAction?.invoke(item.action, item.arg)
        }
    }

    /** A platform's safe start: the face diamond follows the face, and the red sweep runs on a kick. */
    private fun safeStart(platform: String) {
        store.setToggle(FxKeys.DIAMOND_ON, true)
        store.setToggle(FxKeys.DIAMOND_FOLLOW, true)
        store.setToggle(FxKeys.SWEEP_GO_PREFIX + platform, true)
        settingChanged(FxKeys.DIAMOND_ON)
        openFolder?.let { buildDetail(it) }
        val title = home.folders.firstOrNull { it.id == platform }?.title ?: platform
        say(context.getString(R.string.terminal_safe_start, title))
    }

    /** Puts one folder's settings back to the start. Test marks and the camera mode stay. */
    private fun resetFolder(id: String?) {
        val folder = findFolder(home, id ?: return) ?: return
        hideKeyboard()
        FolderReset.reset(store, folder)
        lookChanged(folderToShow = openFolder)
        say(context.getString(R.string.terminal_folder_reset_done, folder.title))
    }

    private fun findFolder(from: PanelFolder, id: String): PanelFolder? {
        if (from.id == id) return from
        for (child in from.folders) findFolder(child, id)?.let { return it }
        return null
    }

    /** After a preset loads or a folder resets, the panel, the size boxes, and the camera follow. */
    private fun lookChanged(folderToShow: PanelFolder?) {
        stopCentering()
        liveSize = null
        refreshLook()
        showSizeNumbers(store.number(LookKeys.WIDTH), store.number(LookKeys.HEIGHT))
        onTestDiamond?.invoke(testDiamondOn)
        requestLayout()
        settingChanged(ALL_SETTINGS)
        folderToShow?.let {
            val scroll = detailScroll.scrollY
            buildDetail(it)
            detailScroll.post { detailScroll.scrollTo(0, scroll) }
        }
    }

    /** Tells the camera screen a setting changed, so effects follow at once. */
    private fun settingChanged(key: String) {
        onSettingChanged?.invoke(key)
    }

    /** A button with a short note. Center panel works at once. Reset all asks first, in plain words. */
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
        val question = if (item.action == PanelAction.RESET_FOLDER) {
            context.getString(R.string.terminal_folder_reset_question)
        } else {
            context.getString(R.string.terminal_reset_question)
        }
        confirm.addView(TextView(context).apply {
            text = question
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
            when (item.action) {
                PanelAction.RESET_ALL, PanelAction.RESET_FOLDER -> {
                    button.visibility = GONE
                    confirm.visibility = VISIBLE
                    say(question)
                }
                PanelAction.CENTER_PANEL -> centerPanel()
                else -> runAction(item)
            }
        }
        no.setOnClickListener {
            confirm.visibility = GONE
            button.visibility = VISIBLE
            say(context.getString(R.string.terminal_reset_kept))
        }
        yes.setOnClickListener {
            if (item.action == PanelAction.RESET_FOLDER) resetFolder(item.arg) else resetAll()
        }
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
        stopCentering()
        store.resetAll()
        liveSize = null
        savePosition()
        prefs.activeFolder = activeId
        prefs.minimized = minimized
        refreshLook()
        showSizeNumbers(store.number(LookKeys.WIDTH), store.number(LookKeys.HEIGHT))
        onTestDiamond?.invoke(testDiamondOn)
        // The ⋮ menu's choices are back to the start too: List, Important first, side list shown.
        lastMove = null
        menu.setUndoAvailable(false)
        menu.refresh()
        applySideMode(announce = false)
        applyFolderLayout()
        requestLayout()
        openFolder?.let {
            buildDetail(it)
            detailScroll.scrollTo(0, 0)
        }
        settingChanged(ALL_SETTINGS)
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
        // The whole path shows. A long one wraps between words instead of being cut.
        val path = FitTextView(context).apply {
            text = (listOf(context.getString(R.string.terminal_home)) + folder.trail().map { it.label })
                .joinToString(" › ")
            typeface = Hud.mono(context)
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
            clipChildren = false
            clipToPadding = false
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
            clipChildren = false
            clipToPadding = false
        }
        names.addView(LaserTitleView(context).apply {
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
        return LaserTitleView(context).apply {
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
        settingChanged(item.key)
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
        settingChanged(item.key)
        onToggled?.invoke(item.key, on)
        if (item.key == LookKeys.TEST_DIAMOND) {
            onTestDiamond?.invoke(on)
            say(context.getString(if (on) R.string.terminal_diamond_on else R.string.terminal_diamond_off))
            return
        }
        val message = if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off
        say(context.getString(message, item.label))
    }

    private fun testKey(folder: PanelFolder, item: ToggleItem): String = "${PanelPrefs.TEST_PREFIX}${folder.id}.${item.key}"

    /** The mark Royalty gave a test in 📁 Results, or by the success code. */
    private fun markOf(folder: PanelFolder, item: ToggleItem): TestMark {
        val platform = TestMarks.platformOf(folder) ?: return TestMark.NOT_TESTED
        return TestMarks.mark(store, platform, item.key)
    }

    /** A test toggle's note: its mark, and the day it was marked. */
    private fun markLine(folder: PanelFolder, item: ToggleItem): String {
        val mark = markOf(folder, item)
        val platform = TestMarks.platformOf(folder) ?: return markText(mark)
        val day = TestMarks.date(store, platform, item.key)
        return if (mark == TestMark.NOT_TESTED || day.isEmpty()) markText(mark) else "${markText(mark)} ($day)"
    }

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
        /** Told to the camera screen when many settings changed at once, like Reset all. */
        const val ALL_SETTINGS = "*"

        /** 📁 Reset already holds Reset all, so it gets no folder Reset of its own. */
        const val RESET_FOLDER_ID = "reset"

        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val GLOW_MARGIN_DP = 18f
        const val EDGE_DP = 8f
        const val MIN_WIDTH_DP = 280f
        const val MIN_HEIGHT_DP = 260f

        /** How close the panel comes to the middle before it clicks into place. */
        const val SNAP_DP = 12f

        /** How far in from the glass edge a touch still lights the edge. */
        const val EDGE_TOUCH_DP = 26f
        const val RIPPLE_MS = 650L
        const val CENTER_MS = 420L
        const val STEP_DP = 40f
        const val GRIP_DP = 30f

        /** How far in a round screen corner cuts, as a share of its radius (1 minus the sine of 45 degrees). */
        const val CORNER_SHARE = 0.3f

        /** The smallest comfortable touch target, for the ⋮ and − buttons. */
        const val TARGET_DP = 48f
        const val DETAIL_BOTTOM_DP = 30f
        const val SIDE_SHARE = 0.29f
        const val SIDE_SHOW_DP = 340f
        const val SIDE_MIN_DP = 100f
        const val SIDE_MAX_DP = 116f

        /** Below this glass width, the title bar hides the Width and Height boxes so the title stays whole. */
        const val HEADER_BOXES_DP = 340f

        /** The swipe drawer is this much wider than the side list, for breathing room. */
        const val DRAWER_EXTRA_DP = 16f
        const val STATUS_SP = 11f
        const val GUIDE_SP = 10.5f
        const val STILL_BREATH = 0.8f
        const val BREATH_SLOW_S = 6f
        const val BREATH_FAST_S = 1.2f
        const val OPEN_MS = 420L
        const val CLOSE_MS = 220L
        const val SWEEP_MS = 520L
        const val GRID_FADE_MS = 220L

        /** A bar counts as covered when the glass hides all of it but this much. */
        const val COVER_SLACK_DP = 2f
        const val COVER_MS = 180L
    }
}
