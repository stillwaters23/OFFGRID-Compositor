package com.offgrid.compositor.terminal

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.offgrid.compositor.R

private const val GRID_FADE_MS = 220L
private const val CLOSE_MS = 220L
private const val OPEN_MS = 420L
private const val SWEEP_MS = 520L

/** What the ⋮ menu does. Every choice works at once, is saved, and is said in plain words. */
internal fun TerminalPanel.menuListener() = object : PanelMenu.Listener {
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

    override fun onMenuClosed() {
        refreshBack()
    }

    override fun onProblem(message: String) {
        say(message)
    }
}

internal fun TerminalPanel.openMenu() {
    hideKeyboard()
    menu.open(topBar.height)
    refreshBack()
    say(context.getString(R.string.terminal_menu_opened))
}

internal fun TerminalPanel.savedTick() {
    if (look.tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}

/** Folders can be dragged only while Move folders is on, in your own order. */
internal fun TerminalPanel.canMoveFolders(): Boolean = folderSettings.unlocked && folderSettings.sort == FolderSort.CUSTOM

/** Shows folders in the chosen view, sort, and size, in the main area and the open folder. */
internal fun TerminalPanel.applyFolderLayout() {
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
}

/** A folder was dragged to a new spot. The new order is saved at once, and Undo can put it back. */
internal fun TerminalPanel.foldersMoved(parent: PanelFolder, order: List<PanelFolder>, before: List<PanelFolder>) {
    folderSettings.saveOrder(parent, order)
    lastMove = parent to before
    menu.setUndoAvailable(true)
    say(context.getString(R.string.terminal_folders_moved))
}

internal fun TerminalPanel.markActive(id: String) {
    activeId = id
    prefs.activeFolder = id
    mainFolders.setActive(id)
}

internal fun TerminalPanel.openFromTop(folder: PanelFolder, from: View) {
    markActive(folder.id)
    showFolder(folder, from)
}

internal fun TerminalPanel.showFolder(folder: PanelFolder, from: View?) {
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

internal fun TerminalPanel.goUp() {
    val folder = openFolder ?: return
    val up = folder.parent
    if (up == null || up === home) {
        closeDetail()
    } else {
        showFolder(up, null)
    }
}

internal fun TerminalPanel.closeDetail() {
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

internal fun TerminalPanel.clearDetail() {
    detailPane.visibility = INVISIBLE
    detailContent.removeAllViews()
    guidance = null
    headingRing = null
    themeRows.clear()
    sizeRows.clear()
    subFolders = null
}

/** True when folders should unfold like a hologram. Off, or reduced motion, opens them at once. */
internal fun TerminalPanel.unfolding(): Boolean = look.unfold && ValueAnimator.areAnimatorsEnabled()

internal fun TerminalPanel.pivotIn(target: View, from: View?): Pair<Float, Float> {
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
internal fun TerminalPanel.hologramIn(target: View, pivotX: Float, pivotY: Float, isDetail: Boolean) {
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

internal fun TerminalPanel.sweepOnce() {
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
