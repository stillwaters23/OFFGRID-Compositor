package com.offgrid.compositor.terminal

import android.widget.PopupMenu
import android.widget.PopupWindow

/**
 * Phase 1.3: menus and pop-ups the panel opens are windows of their own. Setting the panel GONE
 * does not take them down, so [TerminalPanel.applyHidden] closes them here when the Broadcast
 * Lock, playback, or the shrink hides the panel. Each one leaves this list when it is dismissed.
 */
internal object PanelPopups {
    private val windows = ArrayList<PopupWindow>()
    private val menus = ArrayList<PopupMenu>()

    fun track(window: PopupWindow) {
        windows += window
        window.setOnDismissListener { windows -= window }
    }

    fun track(menu: PopupMenu) {
        menus += menu
        menu.setOnDismissListener { menus -= menu }
    }

    fun dismissAll() {
        for (window in windows.toList()) window.dismiss()
        for (menu in menus.toList()) menu.dismiss()
        windows.clear()
        menus.clear()
    }
}
