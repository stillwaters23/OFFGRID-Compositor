package com.offgrid.compositor.terminal

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.view.RoundedCorner
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
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
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.R
import kotlin.math.hypot

/**
 * Step 2. The floating terminal panel (features 3, 4, 5, and 13).
 *
 * Dark glass with a one-column folder list. Folders slide out like holograms. A soft ring turns around the
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
    private var topBar: TopBar? = null
    private var homeGrid: TileGrid? = null

    private val gridTiles = HashMap<String, FolderTile>()
    private var openFolder: PanelFolder? = null
    private var activeId = prefs.activeFolder ?: home.folders.first().id
    private var detailAnim: Animator? = null
    private var panelAnim: Animator? = null

    private var opacity = prefs.number(PanelTree.OPACITY, 50)
    private var glowLevel = prefs.number(PanelTree.GLOW, 50)
    private var typingSpeed = prefs.number(PanelTree.TYPING, 50)
    private var widthPercent = prefs.number(PanelTree.WIDTH, 80)
    private var heightPercent = prefs.number(PanelTree.HEIGHT, 60)
    private var fullScreen = prefs.toggle(PanelTree.FULL_SCREEN, false)
    private var seeThroughAtFullSize = prefs.toggle(PanelTree.SEE_THROUGH_FULL_SIZE, false)
    private var borderThickness = prefs.number(PanelTree.BORDER_THICKNESS, 50)
    private var tickOnSave = prefs.toggle(PanelTree.TICK_ON_SAVE, true)
    private var fullBleed = false
    private var cornerSafeTop = 0
    private var gestureInsets = android.graphics.Insets.NONE
    private var contentInsets = android.graphics.Insets.NONE
    var onSettingChanged: ((String, Boolean) -> Unit)? = null
    var onChoiceChanged: ((String, String) -> Unit)? = null
    var onPanelAction: ((PanelAction) -> Unit)? = null
    private var lastSizeLog: String? = null
    private var resizeEdge = ResizeEdge.NONE
    private var resizeStartX = 0f
    private var resizeStartY = 0f
    private var resizeStartWidth = 0
    private var resizeStartWidthPercent = 0
    private var resizeStartHeightPercent = 0
    private var resizeStartTranslationX = 0f
    private var pinchWidthPercent = 0
    private var pinchHeightPercent = 0
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (minimized || detector.currentSpan <= 0f) return false
            resizeEdge = ResizeEdge.NONE
            fullScreen = false
            prefs.setToggle(PanelTree.FULL_SCREEN, false)
            pinchWidthPercent = widthPercent
            pinchHeightPercent = heightPercent
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            pinchWidthPercent = (pinchWidthPercent * detector.scaleFactor).toInt().coerceIn(1, 100)
            pinchHeightPercent = (pinchHeightPercent * detector.scaleFactor).toInt().coerceIn(1, 100)
            widthPercent = pinchWidthPercent
            heightPercent = pinchHeightPercent
            prefs.setNumber(PanelTree.WIDTH, widthPercent)
            prefs.setNumber(PanelTree.HEIGHT, heightPercent)
            topBar?.setDimensions(widthPercent, heightPercent)
            requestLayout()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (tickOnSave) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            savePosition()
        }
    })
    private var scanLineOn = prefs.toggle(PanelTree.SCAN_LINE, true)
    private var laserTypingOn = prefs.toggle(PanelTree.LASER_TYPING, true)
    private var minimized = prefs.minimized
    private val detailBuilder by lazy(LazyThreadSafetyMode.NONE) {
        PanelDetailBuilder(
            context = context,
            prefs = prefs,
            onNumber = ::saveNumber,
            onToggle = ::saveToggle,
            onChoice = ::saveChoice,
            onAction = ::performAction,
            onFolder = ::showFolder,
            onProblem = ::say,
            typing = { typingLettersPerSecond() to laserTypingOn },
        )
    }

    private var placed = false
    private var userY = 0f

    /** The phone's Back key closes one folder at a time. */
    val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = goUp()
    }

    private enum class ResizeEdge { NONE, BOTTOM, LEFT, RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private fun updateQuickToggle(key: String, value: Boolean, label: String) {
        prefs.setToggle(key, value)
        onSettingChanged?.invoke(key, value)
        say(
            if (key == FxKeys.MIC_ON && value) {
                "Mic is on. Your voice goes into the video."
            } else {
                "$label is ${if (value) "on" else "off"}."
            },
        )
    }

    fun setToggleSetting(key: String, on: Boolean) {
        prefs.setToggle(key, on)
        onSettingChanged?.invoke(key, on)
        when (key) {
            FxKeys.CAMERA_ON -> updateQuickToggleState(camera = on)
            FxKeys.MIC_ON -> updateQuickToggleState(microphone = on)
        }
    }

    private fun updateQuickToggleState(camera: Boolean? = null, microphone: Boolean? = null) {
        camera?.let { topBar?.setQuickToggle(FxKeys.CAMERA_ON, it) }
        microphone?.let { topBar?.setQuickToggle(FxKeys.MIC_ON, it) }
    }

    init {
        Hud.updateSystemTheme(resources.configuration)
        statusLine.applySystemTextColor()
        // A tiny spot that takes focus when a number box lets go, so the keyboard stays shut.
        focusSink.isFocusable = true
        focusSink.isFocusableInTouchMode = true
        focusSink.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(focusSink, LayoutParams(1, 1))
        ViewCompat.addAccessibilityAction(this, "Bigger", AccessibilityViewCommand { _, _ ->
            adjustSize(5, 5)
            true
        })
        ViewCompat.addAccessibilityAction(this, "Smaller", AccessibilityViewCommand { _, _ ->
            adjustSize(-5, -5)
            true
        })

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

    /** Keeps the panel in place when the keyboard opens. */
    fun onKeyboardInset(bottomInset: Int) {
        if (bottomInset < 0) return
        translationY = if (fullBleed) 0f else userY
    }

    /** Hides the panel while the saved video plays, so the whole video can be checked. */
    fun setHiddenForPlayback(hidden: Boolean) {
        if (hidden) {
            hideKeyboard()
        }
        visibility = if (hidden) GONE else VISIBLE
        backCallback.isEnabled = !hidden && !minimized && openFolder != null
        topBar?.setBackEnabled(!hidden && !minimized && openFolder != null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (minimized) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        val wasFullBleed = fullBleed
        fullBleed = fullScreen || (widthPercent == 100 && heightPercent == 100)
        if (wasFullBleed != fullBleed) {
            updateContentInsets()
            glass.fullBleed = fullBleed
            glass.seeThroughAtFullSize = seeThroughAtFullSize
            glass.lightSkin = Hud.isLightTheme
        }
        val panelWidth = if (fullBleed) availableWidth else PanelGeometry.panelDimension(
            availableWidth,
            widthPercent,
            dpi(MAX_WIDTH_DP),
        )
        val panelHeight = if (fullBleed) availableHeight else PanelGeometry.panelDimension(
            availableHeight,
            heightPercent,
            availableHeight,
        ).let { if (heightPercent == 0) 0 else it.coerceAtLeast(minOf(dpi(MIN_HEIGHT_DP), availableHeight)) }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(panelWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(panelHeight, MeasureSpec.EXACTLY),
        )
        val measured = "${measuredWidth}x$measuredHeight/${availableWidth}x$availableHeight"
        if (measured != lastSizeLog) {
            lastSizeLog = measured
            Log.d("OFFGRID size", "glass=${measuredWidth}x$measuredHeight display=${availableWidth}x$availableHeight")
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val area = parent as? View ?: return
        if (area.width == 0 || area.height == 0) return
        if (fullBleed) {
            userY = 0f
            translationX = 0f
            translationY = 0f
            return
        }
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

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        val cutout = insets.getInsets(WindowInsets.Type.displayCutout())
        gestureInsets = insets.getInsets(WindowInsets.Type.systemGestures())
        contentInsets = android.graphics.Insets.of(
            maxOf(bars.left, cutout.left),
            maxOf(bars.top, cutout.top),
            maxOf(bars.right, cutout.right),
            maxOf(bars.bottom, cutout.bottom),
        )
        val positions = listOf(
            RoundedCorner.POSITION_TOP_LEFT,
            RoundedCorner.POSITION_TOP_RIGHT,
            RoundedCorner.POSITION_BOTTOM_RIGHT,
            RoundedCorner.POSITION_BOTTOM_LEFT,
        )
        val windowLocation = IntArray(2)
        getLocationInWindow(windowLocation)
        val corners = positions.map { position ->
            val rounded = insets.getRoundedCorner(position) ?: return@map DisplayCorner(0f, 0f, 0f)
            val center = rounded.center
            DisplayCorner(
                rounded.radius.toFloat(),
                center.x - windowLocation[0].toFloat(),
                center.y - windowLocation[1].toFloat(),
            )
        }
        glass.setDisplayCornerGeometry(corners)
        cornerSafeTop = maxOf(corners[0].radius, corners[1].radius).toInt()
        updateContentInsets()
        requestLayout()
        return super.onApplyWindowInsets(insets)
    }

    private fun updateContentInsets() {
        if (fullBleed) {
            expanded.setPadding(contentInsets.left, contentInsets.top, contentInsets.right, contentInsets.bottom)
        } else {
            expanded.setPadding(0, 0, 0, 0)
        }
        val header = expanded.getChildAt(0) ?: return
        header.setPadding(
            dpi(16f),
            dpi(8f) + if (fullBleed) cornerSafeTop else 0,
            0,
            dpi(2f),
        )
        val detailParams = detailPane.layoutParams as? FrameLayout.LayoutParams
        if (detailParams != null) {
            val margin = if (fullBleed) 0 else dpi(4f)
            detailParams.setMargins(margin, margin, if (fullBleed) 0 else dpi(8f), if (fullBleed) 0 else dpi(10f))
            detailPane.layoutParams = detailParams
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1 && resizeEdge != ResizeEdge.NONE) {
            resizeEdge = ResizeEdge.NONE
            glass.resizeGlow = 0f
        }
        scaleDetector.onTouchEvent(event)
        if (scaleDetector.isInProgress) return true
        if (handleResizeTouch(event)) return true
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val focused = findFocus()
            if (focused is EditText && !isInside(focused, event)) {
                hideKeyboard()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun handleResizeTouch(event: MotionEvent): Boolean {
        val area = parent as? View ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resizeEdge = findResizeEdge(event.x, event.y)
                if (resizeEdge == ResizeEdge.NONE) return false
                resizeStartX = event.rawX
                resizeStartY = event.rawY
                resizeStartWidth = width
                resizeStartWidthPercent = widthPercent
                resizeStartHeightPercent = heightPercent
                resizeStartTranslationX = translationX
                if (fullScreen) {
                    fullScreen = false
                    prefs.setToggle(PanelTree.FULL_SCREEN, false)
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (resizeEdge == ResizeEdge.NONE) return false
                val dx = event.rawX - resizeStartX
                val dy = event.rawY - resizeStartY
                val changeWidth = resizeEdge == ResizeEdge.LEFT || resizeEdge == ResizeEdge.RIGHT ||
                    resizeEdge == ResizeEdge.BOTTOM_LEFT || resizeEdge == ResizeEdge.BOTTOM_RIGHT
                val changeHeight = resizeEdge == ResizeEdge.BOTTOM || resizeEdge == ResizeEdge.BOTTOM_LEFT ||
                    resizeEdge == ResizeEdge.BOTTOM_RIGHT
                if (changeWidth) {
                    val direction = if (resizeEdge == ResizeEdge.LEFT || resizeEdge == ResizeEdge.BOTTOM_LEFT) -1 else 1
                    widthPercent = (resizeStartWidthPercent + direction * dx * 100f / area.width.coerceAtLeast(1)).toInt().coerceIn(1, 100)
                    prefs.setNumber(PanelTree.WIDTH, widthPercent)
                    topBar?.setDimensions(widthPercent, heightPercent)
                }
                if (changeHeight) {
                    heightPercent = (resizeStartHeightPercent + dy * 100f / area.height.coerceAtLeast(1)).toInt().coerceIn(1, 100)
                    prefs.setNumber(PanelTree.HEIGHT, heightPercent)
                    topBar?.setDimensions(widthPercent, heightPercent)
                }
                if (resizeEdge == ResizeEdge.LEFT || resizeEdge == ResizeEdge.BOTTOM_LEFT) {
                    val nextWidth = PanelGeometry.panelDimension(area.width, widthPercent, dpi(MAX_WIDTH_DP))
                    translationX = (resizeStartTranslationX + resizeStartWidth - nextWidth)
                        .coerceIn(0f, (area.width - nextWidth).toFloat().coerceAtLeast(0f))
                }
                glass.resizeGlow = 1f
                requestLayout()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (resizeEdge == ResizeEdge.NONE) return false
                resizeEdge = ResizeEdge.NONE
                glass.resizeGlow = 0f
                if (event.actionMasked == MotionEvent.ACTION_UP && tickOnSave) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                savePosition()
                return true
            }
        }
        return false
    }

    private fun findResizeEdge(x: Float, y: Float): ResizeEdge {
        if (minimized || width == 0 || height == 0) return ResizeEdge.NONE
        val area = parent as? View ?: return ResizeEdge.NONE
        val touch = dpi(24f)
        val atBottom = userY + height >= area.height - 1
        val atLeft = translationX <= 1f
        val atRight = translationX + width >= area.width - 1
        val bottom = height - if (atBottom) gestureInsets.bottom else 0
        val left = if (atLeft) maxOf(touch, gestureInsets.left) else touch
        val right = if (atRight) minOf(width - touch, width - gestureInsets.right) else width - touch
        val inBottom = y >= bottom - touch && y <= bottom
        if (inBottom) {
            if (x <= left + touch) return ResizeEdge.BOTTOM_LEFT
            if (x >= right - touch) return ResizeEdge.BOTTOM_RIGHT
            return ResizeEdge.BOTTOM
        }
        if (y >= height / 2f) {
            if (x >= left && x <= left + touch) return ResizeEdge.LEFT
            if (x >= right - touch && x <= right) return ResizeEdge.RIGHT
        }
        return ResizeEdge.NONE
    }

    private fun adjustSize(widthChange: Int, heightChange: Int) {
        widthPercent = (widthPercent + widthChange).coerceIn(1, 100)
        heightPercent = (heightPercent + heightChange).coerceIn(1, 100)
        prefs.setNumber(PanelTree.WIDTH, widthPercent)
        prefs.setNumber(PanelTree.HEIGHT, heightPercent)
        topBar?.setDimensions(widthPercent, heightPercent)
        requestLayout()
    }

    private fun isInside(view: View, event: MotionEvent): Boolean {
        val spot = IntArray(2)
        view.getLocationOnScreen(spot)
        val x = event.rawX
        val y = event.rawY
        return x >= spot[0] && x <= spot[0] + view.width && y >= spot[1] && y <= spot[1] + view.height
    }

    private fun buildHeader(): View {
        val bar = TopBar(
            context = context,
            prefs = prefs,
            width = widthPercent,
            height = heightPercent,
            onBack = ::goUp,
            onShrink = { showMinimized(true, animate = true) },
            onToggle = ::updateQuickToggle,
            onDimensionChanged = ::onDimensionChanged,
            onViewModeChanged = { homeGrid?.setColumnCount(it) },
        )
        topBar = bar
        bar.setOnTouchListener(dragHandle())
        return bar
    }

    private fun onDimensionChanged(key: String, value: Int) {
        if (key == PanelTree.WIDTH) widthPercent = value else heightPercent = value
        prefs.setNumber(key, value)
        requestLayout()
    }

    private fun buildMainArea(): View {
        val area = FrameLayout(context)
        val grid = TileGrid(context, GRID_CELL_DP, singleColumn = true).apply {
            setPadding(dpi(4f), dpi(6f), dpi(4f), dpi(14f))
        }
        homeGrid = grid
        for (folder in home.folders) {
            val tile = FolderTile(context, folder, GRID_ICON_DP, bigTile = true)
            tile.setOnClickListener { openFromTop(folder, tile) }
            gridTiles[folder.id] = tile
            grid.addView(tile)
        }
        gridScroll.isVerticalScrollBarEnabled = false
        gridScroll.overScrollMode = OVER_SCROLL_NEVER
        gridScroll.addView(grid)
        area.addView(gridScroll, LayoutParams(MATCH, MATCH))
        area.addView(GlowScrollBar(context, gridScroll), LayoutParams(dpi(24f), MATCH, Gravity.END))

        detailPane.background = holoGlass
        detailPane.isClickable = true
        detailPane.visibility = INVISIBLE
        detailContent.orientation = LinearLayout.VERTICAL
        detailContent.setPadding(dpi(10f), dpi(6f), dpi(10f), dpi(16f))
        detailScroll.isVerticalScrollBarEnabled = false
        detailScroll.overScrollMode = OVER_SCROLL_NEVER
        detailScroll.addView(detailContent)
        detailPane.addView(detailScroll, LayoutParams(MATCH, MATCH))
        detailPane.addView(GlowScrollBar(context, detailScroll), LayoutParams(dpi(24f), MATCH, Gravity.END))
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
        translationY = userY
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
        topBar?.setBackEnabled(!value && openFolder != null)
        requestLayout()
        if (animate && !value) {
            post { hologramIn(this, 0f, 0f, isDetail = false) }
            say(context.getString(R.string.terminal_panel_open))
        }
    }

    private fun applyLook() {
        val share = opacity / 100f
        glass.opacity = if (fullBleed && !seeThroughAtFullSize) 1f else share
        glass.glow = glowLevel / 100f
        glass.fullBleed = fullBleed
        glass.seeThroughAtFullSize = seeThroughAtFullSize
        glass.borderThickness = PanelGeometry.borderThickness(borderThickness)
        glass.lightSkin = Hud.isLightTheme
        glass.uvTheme = prefs.choice(PanelTree.THEME, "OLED Bioluminescent") == "UV Black Light"
        for (drawable in listOf(pillGlass, holoGlass)) {
            drawable.opacity = if (fullBleed && !seeThroughAtFullSize && drawable === holoGlass) 1f else share
            drawable.glow = glowLevel / 100f
            drawable.lightSkin = Hud.isLightTheme
        }
        holoGlass.solidSkin = fullBleed && !seeThroughAtFullSize
        requestLayout()
        // The words dim a little with the glass, but never fade away.
        val contentAlpha = if (fullBleed && !seeThroughAtFullSize) 1f else 0.7f + 0.3f * share
        expanded.alpha = contentAlpha
        pill.alpha = contentAlpha
        scanLine.isVisible = scanLineOn && !minimized
        statusLine.speed = typingLettersPerSecond()
        statusLine.animateTyping = laserTypingOn
        detailBuilder.updateTyping(typingLettersPerSecond(), laserTypingOn)
    }

    private fun typingLettersPerSecond(): Float = 12f + typingSpeed * 1.38f

    private fun say(message: String) {
        statusLine.type(PanelText.line(message))
    }

    private fun markActive(id: String) {
        activeId = id
        prefs.activeFolder = id
        gridTiles.forEach { (key, tile) -> tile.setActive(key == id) }
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
        topBar?.setBackEnabled(true)
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
        topBar?.setBackEnabled(false)
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
                    detailBuilder.clear()
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

    private fun buildDetail(folder: PanelFolder) = detailBuilder.build(folder, detailContent)

    private fun performAction(action: PanelAction) {
        when (action) {
            PanelAction.RESET_ALL -> resetSettings(home)
            PanelAction.RESET_FOLDER -> resetSettings(openFolder ?: home)
            PanelAction.LIVE_OPEN_OVERLAY_SETTINGS,
            PanelAction.LIVE_CLEAN_OUTPUT,
            PanelAction.REMOTE_OPEN_PAIRING,
            -> onPanelAction?.invoke(action)
        }
    }

    private fun resetSettings(folder: PanelFolder) {
        fun reset(current: PanelFolder) {
            current.numbers.forEach { prefs.setNumber(it.key, it.default) }
            current.toggles.forEach {
                val key = toggleKey(current, it)
                prefs.setToggle(key, it.default)
                if (it.key.startsWith("fx.")) onSettingChanged?.invoke(it.key, it.default)
            }
            current.choices.forEach {
                prefs.setChoice(it.key, it.default)
                onChoiceChanged?.invoke(it.key, it.default)
            }
            current.folders.forEach(::reset)
        }
        reset(folder)
        opacity = prefs.number(PanelTree.OPACITY, 50)
        glowLevel = prefs.number(PanelTree.GLOW, 50)
        typingSpeed = prefs.number(PanelTree.TYPING, 50)
        widthPercent = prefs.number(PanelTree.WIDTH, 80)
        heightPercent = prefs.number(PanelTree.HEIGHT, 60)
        fullScreen = prefs.toggle(PanelTree.FULL_SCREEN, false)
        seeThroughAtFullSize = prefs.toggle(PanelTree.SEE_THROUGH_FULL_SIZE, false)
        borderThickness = prefs.number(PanelTree.BORDER_THICKNESS, 50)
        tickOnSave = prefs.toggle(PanelTree.TICK_ON_SAVE, true)
        scanLineOn = prefs.toggle(PanelTree.SCAN_LINE, true)
        laserTypingOn = prefs.toggle(PanelTree.LASER_TYPING, true)
        topBar?.setDimensions(widthPercent, heightPercent)
        applyLook()
        openFolder?.let(::buildDetail)
        say("Settings reset.")
    }

    private fun saveNumber(item: NumberItem, value: Int, announce: Boolean) {
        prefs.setNumber(item.key, value)
        when (item.key) {
            PanelTree.OPACITY -> opacity = value
            PanelTree.GLOW -> glowLevel = value
            PanelTree.TYPING -> typingSpeed = value
            PanelTree.WIDTH -> widthPercent = value
            PanelTree.HEIGHT -> heightPercent = value
            PanelTree.BORDER_THICKNESS -> borderThickness = value
        }
        if (item.key == PanelTree.WIDTH || item.key == PanelTree.HEIGHT) {
            topBar?.setDimensions(widthPercent, heightPercent)
        }
        applyLook()
        if (announce) {
            say(context.getString(R.string.terminal_number_saved, item.label, value))
        }
    }

    private fun saveToggle(folder: PanelFolder, item: ToggleItem, on: Boolean) {
        prefs.setToggle(toggleKey(folder, item), on)
        onSettingChanged?.invoke(item.key, on)
        if (item.isTest) {
            val message = if (on) R.string.terminal_test_on else R.string.terminal_test_off
            say(context.getString(message, item.label))
            return
        }
        when (item.key) {
            PanelTree.SCAN_LINE -> scanLineOn = on
            PanelTree.LASER_TYPING -> laserTypingOn = on
            PanelTree.FULL_SCREEN -> fullScreen = on
            PanelTree.SEE_THROUGH_FULL_SIZE -> seeThroughAtFullSize = on
            PanelTree.TICK_ON_SAVE -> tickOnSave = on
        }
        applyLook()
        val message = if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off
        say(context.getString(message, item.label))
    }

    private fun saveChoice(item: ChoiceItem, value: String) {
        prefs.setChoice(item.key, value)
        onChoiceChanged?.invoke(item.key, value)
        applyLook()
        say("${item.label} is $value.")
    }

    private fun toggleKey(folder: PanelFolder, item: ToggleItem): String {
        return if (item.isTest) "test.${folder.id}.${item.key}" else item.key
    }

    private fun hideKeyboard() {
        val focused = findFocus() ?: return
        if (focused is EditText) {
            focused.clearFocus()
            context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Hud.updateSystemTheme(newConfig)
        applyLook()
        openFolder?.let(::buildDetail)
        applyTextTheme(expanded)
        applyTextTheme(pill)
    }

    private fun applyTextTheme(view: View) {
        if (view is TextView && view !is EditText) {
            val current = view.currentTextColor
            if (current == 0xFFDDF8FF.toInt() || current == 0xFFFFFFFF.toInt() ||
                current == 0xFF000000.toInt() || current == 0xFF8DB4BF.toInt() ||
                current == 0xFFB8B8C0.toInt()
            ) {
                view.setTextColor(Hud.TEXT)
            }
            if (view is LaserTypeView) view.applySystemTextColor()
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyTextTheme(view.getChildAt(index))
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
        const val DEFAULT_BOTTOM_SHARE = 0.82f
        const val GRID_CELL_DP = 76f
        const val GRID_ICON_DP = 54f
        const val OPEN_MS = 420L
        const val CLOSE_MS = 220L
        const val SWEEP_MS = 520L
        const val GRID_FADE_MS = 220L
    }
}
