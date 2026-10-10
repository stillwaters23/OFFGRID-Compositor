package com.offgrid.compositor.terminal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.offgrid.compositor.R
import kotlin.math.hypot

internal fun folderColumnCount(innerWidth: Int, minimumCellWidth: Float, requestedColumns: Int): Int {
    val minimum = minimumCellWidth.coerceAtLeast(1f)
    val fits = (innerWidth.coerceAtLeast(0) / minimum).toInt().coerceAtLeast(1)
    return requestedColumns.coerceAtLeast(1).coerceAtMost(fits)
}

/** How folders are laid out (Step 2.4, the ⋮ menu). */
internal enum class FolderView(val key: String, val title: String, val help: String, val columns: Int) {
    LIST("list", "List", "One folder per row, top to bottom.", 1),
    BIG_LIST("big_list", "Big tiles", "One big folder per row.", 1),
    GRID_2("grid_2", "Grid 2", "Two folders side by side.", 2),
    GRID_3("grid_3", "Grid 3", "Three folders across.", 3),
    ;

    companion object {
        fun from(key: String?): FolderView = entries.firstOrNull { it.key == key } ?: GRID_2
    }
}

/** The order folders show in. */
internal enum class FolderSort(val key: String, val title: String, val help: String) {
    IMPORTANT("important", "Important first", "The most used folders are at the top."),
    A_TO_Z("a_to_z", "A to Z", "By name, from A to Z."),
    CUSTOM("custom", "Custom", "Your own order. Unlock to drag folders."),
    ;

    companion object {
        fun from(key: String?): FolderSort = entries.firstOrNull { it.key == key } ?: IMPORTANT
    }
}

/**
 * The ⋮ menu's choices, saved on the phone. Reset all clears them, which brings back the
 * start: Big tiles, Important first, folder size 50, folders locked.
 */
internal class FolderSettings(private val prefs: PanelPrefs) {
    var view: FolderView
        get() = FolderView.from(prefs.text(VIEW))
        set(value) = prefs.write { text(VIEW, value.key) }

    var sort: FolderSort
        get() = FolderSort.from(prefs.text(SORT))
        set(value) = prefs.write { text(SORT, value.key) }

    /** Folder size from 0 to 100. 50 is the normal size. */
    var size: Int
        get() = prefs.number(SIZE, START_SIZE)
        set(value) = prefs.setNumber(SIZE, value)

    /** True while folders can be dragged into a new order. */
    var unlocked: Boolean
        get() = prefs.toggle(UNLOCKED, false)
        set(value) = prefs.setToggle(UNLOCKED, value)

    /** The folders inside [parent], in the order the chosen sort gives. */
    fun arrange(parent: PanelFolder): List<PanelFolder> = when (sort) {
        FolderSort.IMPORTANT -> parent.folders
        FolderSort.A_TO_Z -> parent.folders.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { sortName(it) })
        FolderSort.CUSTOM -> custom(parent)
    }

    fun saveOrder(parent: PanelFolder, folders: List<PanelFolder>) {
        prefs.write { text(ORDER + parent.id, folders.joinToString(",") { it.id }) }
    }

    /** True when any folder under [root] already has your own saved order. */
    fun hasSavedOrder(root: PanelFolder): Boolean {
        fun walk(folder: PanelFolder): Boolean =
            prefs.text(ORDER + folder.id) != null || folder.folders.any { walk(it) }
        return walk(root)
    }

    /** Saves the order on screen now as your own order, for every folder that holds folders. */
    fun keepEveryOrder(root: PanelFolder) {
        val orders = LinkedHashMap<String, String>()
        fun walk(folder: PanelFolder) {
            if (folder.folders.size > 1) orders[ORDER + folder.id] = arrange(folder).joinToString(",") { it.id }
            folder.folders.forEach { walk(it) }
        }
        walk(root)
        prefs.write { orders.forEach { (key, value) -> text(key, value) } }
    }

    private fun custom(parent: PanelFolder): List<PanelFolder> {
        val saved = prefs.text(ORDER + parent.id)?.split(",").orEmpty()
        val byId = parent.folders.associateBy { it.id }
        val placed = saved.mapNotNull { byId[it] }.distinct()
        return placed + parent.folders.filter { it !in placed }
    }

    companion object {
        const val START_SIZE = 50
        private const val VIEW = "folder_view"
        private const val SORT = "folder_sort"
        private const val SIZE = "folder_size"
        private const val UNLOCKED = "folders_unlocked"
        private const val ORDER = "folder_order."

        /** The name to sort by, without the 📁 in front. */
        fun sortName(folder: PanelFolder): String = folder.title.trimStart { !it.isLetterOrDigit() }

        /** Folder size 0 is 70% as big, 50 is normal, and 100 is 130%. */
        fun scaleOf(size: Int): Float = 0.7f + 0.6f * size.coerceIn(0, 100) / 100f
    }
}

/**
 * One folder as a row, for the List and Big list views: the glowing folder, its name, and
 * how many items it holds. Big list shows a big folder and the item count in words.
 */
internal class FolderRow(
    context: Context,
    private val folder: PanelFolder,
    private val big: Boolean,
    scale: Float,
) : LinearLayout(context), Styled {
    private val bar = View(context)
    private val ring = HoloRingView(context, folder.special)
    private val name = FitTextView(context)
    private val detail = FitTextView(context)
    private val count = TextView(context)
    private var active = false
    private var look: PanelLook? = null
    private var endPad = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = Hud.dpi(context, if (big) 6f else 3f)
        setPadding(0, pad, Hud.dpi(context, 8f), pad)
        endPad = Hud.dpi(context, 8f)
        bar.visibility = INVISIBLE
        addView(bar, LayoutParams(Hud.dpi(context, 2f), Hud.dpi(context, (if (big) 44f else 26f) * scale)))
        val iconDp = (if (big) BIG_ICON_DP else ICON_DP) * scale
        val ringSize = Hud.dpi(context, iconDp * 1.3f)
        val frame = FrameLayout(context)
        ring.visibility = INVISIBLE
        frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, folder.itemCount, if (big) folder.tag else null, compact = !big, special = folder.special),
            FrameLayout.LayoutParams(Hud.dpi(context, iconDp), Hud.dpi(context, iconDp * 0.86f), Gravity.CENTER),
        )
        addView(frame, LayoutParams(ringSize, ringSize).apply { marginStart = Hud.dpi(context, 4f) })
        val words = LinearLayout(context).apply { orientation = VERTICAL }
        name.text = if (big) folder.title else folder.label
        name.typeface = if (big) Hud.condensedBold else Hud.condensed
        words.addView(name)
        if (big) {
            detail.text = if (folder.itemCount == 0) context.getString(R.string.terminal_folder_empty_short) else itemsText(context, folder.itemCount)
            detail.typeface = Hud.mono(context)
            words.addView(detail)
        }
        addView(words, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 8f) })
        if (!big) {
            count.text = folder.itemCount.toString()
            count.typeface = Hud.mono(context)
            addView(count, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = Hud.dpi(context, 6f)
            })
        }
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.terminal_folder_description, folder.title, itemsText(context, folder.itemCount))
    }

    fun setActive(value: Boolean) {
        active = value
        look?.let { applyLook(it) }
    }

    /** Extra room at the right end for the drag handle while folders can move. */
    fun setHandleRoom(room: Int) {
        val wanted = endPad + room
        if (paddingRight != wanted) setPadding(paddingLeft, paddingTop, wanted, paddingBottom)
    }

    override fun applyLook(look: PanelLook) {
        this.look = look
        foreground = pressGlow(context, look.edge)
        bar.setBackgroundColor(look.decor)
        bar.visibility = if (active) VISIBLE else INVISIBLE
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, (if (big) 16f else 13f) * look.fontScale)
        val ink = if (look.base == Theme.SYSTEM) look.text else if (active) look.decor else look.secondaryText
        val note = if (look.base == Theme.SYSTEM) look.text else Hud.withAlpha(look.folder, 0.85f)
        name.setTextColor(ink)
        detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f * look.fontScale)
        detail.setTextColor(note)
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * look.fontScale)
        count.setTextColor(note)
        setBackgroundColor(if (active) Hud.withAlpha(look.edge, 0.08f) else 0)
        val showRing = active && look.ring
        ring.visibility = if (showRing) VISIBLE else INVISIBLE
        ring.spinning = showRing
    }

    private companion object {
        const val ICON_DP = 30f
        const val BIG_ICON_DP = 58f
    }
}

/**
 * Folders laid out as a list, a big list, or a grid of 2 or 3 across (Step 2.4).
 *
 * With [dragOn], a folder moves after a long press, or right away from its drag handle
 * (the three short lines). Anywhere else, a swipe still scrolls the list (final build,
 * Rex's review). The other folders slide out of the way, the phone ticks softly as the
 * folder lands on each new spot, and the new order is handed to [onMoved] when the finger
 * lifts, only if it really changed. Near the top or bottom of the scrolling area, the list
 * scrolls by itself so a folder can travel all the way. A tap still opens the folder.
 */
internal class FolderLayout(context: Context, private val sub: Boolean) : ViewGroup(context) {
    /** Opens a folder. The view is where the hologram unfolds from. */
    var onOpen: ((PanelFolder, View) -> Unit)? = null

    /** The new order and the order from before, after a drag that changed something. */
    var onMoved: ((List<PanelFolder>, List<PanelFolder>) -> Unit)? = null

    /** Paints new folder views in the current look. */
    var styler: ((View) -> Unit)? = null

    /** Follows Tick when saved. */
    var tick = true

    var dragOn = false
        set(value) {
            if (field == value) return
            field = value
            dragHandleRoom()
            invalidate()
        }

    private val order = ArrayList<PanelFolder>()
    private val cells = HashMap<String, View>()
    private var view = FolderView.GRID_2
    private var size = FolderSettings.START_SIZE
    private var activeId: String? = null
    private var columns = 1
    private var cellWidth = 0
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val slot = Rect()

    // The drag in progress.
    private var candidate: View? = null
    private var dragged: View? = null
    private var downX = 0f
    private var downY = 0f
    private var fingerX = 0f
    private var fingerY = 0f
    private var grabX = 0f
    private var grabY = 0f
    private var slideNext = false
    private var grabbedByHandle = false
    private var orderBefore: List<PanelFolder> = emptyList()
    private val dragHandle = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = android.graphics.Paint.Cap.ROUND
    }
    private val longPress = Runnable {
        val held = candidate ?: return@Runnable
        if (dragged != null) return@Runnable
        // A long press lifts the folder. Its own tap is cancelled, so it doesn't open.
        held.cancelPendingInputEvents()
        held.isPressed = false
        if (tick) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        lift(held, downX, downY)
    }
    private val autoScroll = object : Runnable {
        override fun run() {
            val scroller = scroller() ?: return
            val held = dragged ?: return
            val step = edgeStep()
            if (step == 0) return
            val before = scroller.scrollY
            scroller.scrollBy(0, step)
            val moved = scroller.scrollY - before
            if (moved != 0) {
                fingerY += moved
                follow(held)
                // The list moved under a still finger, so the folder can land on a new spot too.
                moveTo(held)
            }
            postOnAnimation(this)
        }
    }

    init {
        clipChildren = false
    }

    /** Shows [folders] in [view] at [size]. Builds the folder views again only when needed. */
    fun show(folders: List<PanelFolder>, view: FolderView, size: Int) {
        val rebuild = view != this.view || size != this.size || cells.keys != folders.map { it.id }.toSet()
        val reordered = folders.map { it.id } != order.map { it.id }
        if (!rebuild && !reordered) return
        this.view = view
        this.size = size
        order.clear()
        order.addAll(folders)
        if (rebuild) {
            removeAllViews()
            cells.clear()
            for (folder in folders) {
                val cell = makeCell(folder)
                cells[folder.id] = cell
                addView(cell)
                styler?.invoke(cell)
            }
            setActive(activeId)
            dragHandleRoom()
            if (ValueAnimator.areAnimatorsEnabled() && isAttachedToWindow) {
                alpha = 0f
                animate().alpha(1f).setDuration(FADE_MS).start()
            }
        } else {
            slideNext = true
        }
        requestLayout()
    }

    fun setActive(id: String?) {
        activeId = id
        for ((key, cell) in cells) {
            when (cell) {
                is FolderTile -> cell.setActive(key == id)
                is FolderRow -> cell.setActive(key == id)
            }
        }
    }

    fun cellFor(id: String): View? = cells[id]

    private fun makeCell(folder: PanelFolder): View {
        val scale = FolderSettings.scaleOf(size)
        val cell = when (view) {
            FolderView.LIST -> FolderRow(context, folder, big = false, scale)
            FolderView.BIG_LIST -> FolderRow(context, folder, big = true, scale)
            FolderView.GRID_2 -> FolderTile(context, folder, (if (sub) SUB_ICON_DP else GRID_ICON_DP) * GRID_2_SHARE * scale)
            FolderView.GRID_3 -> FolderTile(context, folder, (if (sub) SUB_ICON_DP else GRID_ICON_DP) * scale)
        }
        cell.setOnClickListener { onOpen?.invoke(folder, cell) }
        return cell
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inner = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        // A grid never squeezes folders smaller than they can be read. It uses fewer columns instead.
        columns = folderColumnCount(inner, Hud.dp(context, MIN_CELL_DP), view.columns)
        cellWidth = inner / columns
        var height = paddingTop + paddingBottom
        var rowHeight = 0
        order.forEachIndexed { index, folder ->
            val cell = cells[folder.id] ?: return@forEachIndexed
            cell.measure(
                MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            rowHeight = maxOf(rowHeight, cell.measuredHeight)
            if (index % columns == columns - 1 || index == order.size - 1) {
                height += rowHeight
                rowHeight = 0
            }
        }
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val slide = slideNext && ValueAnimator.areAnimatorsEnabled()
        slideNext = false
        var y = paddingTop
        var rowHeight = 0
        order.forEachIndexed { index, folder ->
            val cell = cells[folder.id] ?: return@forEachIndexed
            val column = index % columns
            val x = paddingLeft + column * cellWidth
            // Where the folder shows now, counting a slide that is still under way.
            val seenX = cell.left + cell.translationX
            val seenY = cell.top + cell.translationY
            val placed = cell.width > 0
            cell.layout(x, y, x + cell.measuredWidth, y + cell.measuredHeight)
            when {
                cell === dragged -> follow(cell)
                slide && placed && (seenX != x.toFloat() || seenY != y.toFloat()) -> {
                    cell.translationX = seenX - x
                    cell.translationY = seenY - y
                    cell.animate().translationX(0f).translationY(0f).setDuration(SLIDE_MS).start()
                }
            }
            rowHeight = maxOf(rowHeight, cell.measuredHeight)
            if (column == columns - 1 || index == order.size - 1) {
                y += rowHeight
                rowHeight = 0
            }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!dragOn) return false
        val held = dragged
        if (held != null) {
            // A folder lifted by a long press: this view handles the rest of the touch.
            handleDrag(held, event)
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                val cell = cellAt(event.x, event.y)
                candidate = cell
                grabbedByHandle = cell != null && onDragHandle(cell, event.x, event.y)
                if (cell != null) {
                    if (grabbedByHandle) {
                        // The drag handle moves the folder right away. The list doesn't scroll from it.
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val cell = candidate
                if (cell != null && hypot(event.x - downX, event.y - downY) > slop) {
                    if (grabbedByHandle) {
                        lift(cell, event.x, event.y)
                        return true
                    }
                    // A swipe before the long press: it scrolls the list as usual.
                    removeCallbacks(longPress)
                    candidate = null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                candidate = null
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val held = dragged ?: return false
        handleDrag(held, event)
        return true
    }

    private fun handleDrag(held: View, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                fingerX = event.x
                fingerY = event.y
                follow(held)
                moveTo(held)
                removeCallbacks(autoScroll)
                if (edgeStep() != 0) postOnAnimation(autoScroll)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endDrag(held)
        }
    }

    /** The drag handle: the right end of a row, or the top right corner of a tile. */
    private fun onDragHandle(cell: View, x: Float, y: Float): Boolean {
        val reach = Hud.dp(context, DRAG_HANDLE_TOUCH_DP)
        return if (cell is FolderRow) {
            x >= cell.right - reach
        } else {
            x >= cell.right - reach && y <= cell.top + reach
        }
    }

    /** Rows leave room at their right end for the drag handle while folders can move. */
    private fun dragHandleRoom() {
        val room = if (dragOn) Hud.dpi(context, DRAG_HANDLE_ROOM_DP) else 0
        for (cell in cells.values) if (cell is FolderRow) cell.setHandleRoom(room)
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        if (!dragOn) return
        val look = PanelLook.current ?: return
        dragHandle.color = Hud.withAlpha(look.decor, 0.85f)
        dragHandle.strokeWidth = Hud.dp(context, 2f)
        val half = Hud.dp(context, 7f)
        val gap = Hud.dp(context, 4.5f)
        for (cell in cells.values) {
            if (cell === dragged) continue
            val cx: Float
            val cy: Float
            if (cell is FolderRow) {
                cx = cell.right - Hud.dp(context, DRAG_HANDLE_ROOM_DP) / 2f + cell.translationX
                cy = (cell.top + cell.bottom) / 2f + cell.translationY
            } else {
                cx = cell.right - Hud.dp(context, 16f) + cell.translationX
                cy = cell.top + Hud.dp(context, 16f) + cell.translationY
            }
            for (step in -1..1) canvas.drawLine(cx - half, cy + step * gap, cx + half, cy + step * gap, dragHandle)
        }
    }

    private fun cellAt(x: Float, y: Float): View? {
        for (cell in cells.values) {
            if (x >= cell.left && x < cell.right && y >= cell.top && y < cell.bottom) return cell
        }
        return null
    }

    /** Lifts [cell] under the finger at [x], [y]. */
    private fun lift(cell: View, x: Float, y: Float) {
        removeCallbacks(longPress)
        dragged = cell
        candidate = null
        orderBefore = order.toList()
        parent?.requestDisallowInterceptTouchEvent(true)
        fingerX = x
        fingerY = y
        grabX = downX - cell.left
        grabY = downY - cell.top
        cell.animate().cancel()
        cell.isPressed = false
        cell.translationZ = Hud.dp(context, LIFT_DP)
        if (ValueAnimator.areAnimatorsEnabled()) {
            cell.animate().scaleX(LIFT_SCALE).scaleY(LIFT_SCALE).alpha(LIFT_ALPHA).setDuration(LIFT_MS).start()
        } else {
            cell.scaleX = LIFT_SCALE
            cell.scaleY = LIFT_SCALE
            cell.alpha = LIFT_ALPHA
        }
        follow(cell)
    }

    /** Keeps the lifted folder under the finger, wherever its spot is. */
    private fun follow(cell: View) {
        cell.translationX = fingerX - grabX - cell.left
        cell.translationY = fingerY - grabY - cell.top
    }

    /** When the lifted folder's middle is over another spot, it takes that spot. */
    private fun moveTo(cell: View) {
        val from = order.indexOfFirst { cells[it.id] === cell }
        if (from < 0) return
        val centerX = (fingerX - grabX + cell.width / 2f).toInt()
        val centerY = (fingerY - grabY + cell.height / 2f).toInt()
        var target = -1
        order.forEachIndexed { index, folder ->
            val other = cells[folder.id] ?: return@forEachIndexed
            slot.set(other.left, other.top, other.right, other.bottom)
            // Only the middle of a spot counts, so folders of different heights never
            // swap back and forth on the line between them.
            slot.inset(other.width / 4, other.height / 4)
            if (other !== cell && slot.contains(centerX, centerY)) target = index
        }
        if (target < 0 || target == from) return
        order.add(target, order.removeAt(from))
        slideNext = true
        requestLayout()
        if (tick) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun endDrag(cell: View) {
        removeCallbacks(autoScroll)
        dragged = null
        parent?.requestDisallowInterceptTouchEvent(false)
        cell.translationZ = 0f
        if (ValueAnimator.areAnimatorsEnabled()) {
            cell.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(LIFT_MS).start()
        } else {
            cell.translationX = 0f
            cell.translationY = 0f
            cell.scaleX = 1f
            cell.scaleY = 1f
            cell.alpha = 1f
        }
        if (tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        // Only a drag that really changed the order is saved and said.
        if (order.map { it.id } != orderBefore.map { it.id }) onMoved?.invoke(order.toList(), orderBefore)
        invalidate()
    }

    private fun scroller(): ScrollView? {
        var at = parent
        while (at != null && at !is ScrollView) at = at.parent
        return at as? ScrollView
    }

    /** How far to scroll this frame: up near the top edge, down near the bottom edge, else 0. */
    private fun edgeStep(): Int {
        val scroller = scroller() ?: return 0
        val box = IntArray(2)
        val mine = IntArray(2)
        scroller.getLocationInWindow(box)
        getLocationInWindow(mine)
        val y = mine[1] + fingerY - box[1]
        val zone = Hud.dp(context, EDGE_ZONE_DP)
        val step = Hud.dpi(context, SCROLL_STEP_DP)
        return when {
            y < zone && scroller.canScrollVertically(-1) -> -step
            y > scroller.height - zone && scroller.canScrollVertically(1) -> step
            else -> 0
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPress)
        removeCallbacks(autoScroll)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val GRID_ICON_DP = 54f
        const val SUB_ICON_DP = 50f
        const val GRID_2_SHARE = 1.3f
        const val MIN_CELL_DP = 72f
        const val FADE_MS = 160L
        const val SLIDE_MS = 180L
        const val LIFT_MS = 120L
        const val LIFT_DP = 8f
        const val LIFT_SCALE = 1.06f
        const val LIFT_ALPHA = 0.92f
        const val EDGE_ZONE_DP = 48f

        /** The drag handle's touch area and the room rows leave for it. */
        const val DRAG_HANDLE_TOUCH_DP = 48f
        const val DRAG_HANDLE_ROOM_DP = 40f
        const val SCROLL_STEP_DP = 6f
    }
}
