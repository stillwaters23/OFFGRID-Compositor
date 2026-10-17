package com.offgrid.compositor

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.offgrid.compositor.terminal.Hud

/**
 * Phase 1.2 (V2 section 7.2): the pill is gone from the stage, so the first time Royalty shrinks
 * the panel one line in the top-left zone says how to bring it back. It shows once per install,
 * never while recording, locked, or armed, and any lock takes it off the stage at once.
 * White text on a black strip, nothing else, and it is not a touch target: the hold works through it.
 */
internal class CoachMark(private val root: FrameLayout, private val prefs: SharedPreferences) {
    private val main = Handler(Looper.getMainLooper())
    private var view: View? = null
    private val hideTask = Runnable { hide() }

    /** [zone] is the panel-open zone (left, top, right, bottom in pixels). */
    fun showOnce(zone: IntArray, allowed: Boolean) {
        if (!allowed || view != null || prefs.getBoolean(KEY_SHOWN, false)) return
        prefs.edit().putBoolean(KEY_SHOWN, true).apply()
        val context = root.context
        val text = TextView(context).apply {
            text = context.getString(R.string.coach_panel_open)
            typeface = Hud.mono(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            val pad = Hud.dpi(context, 10f)
            setPadding(pad, pad, pad, pad)
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val params = FrameLayout.LayoutParams(zone[2] - zone[0], FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = zone[0]
            topMargin = zone[1]
        }
        root.addView(text, params)
        view = text
        main.postDelayed(hideTask, SHOW_MS)
    }

    fun hide() {
        main.removeCallbacks(hideTask)
        val shown = view ?: return
        view = null
        root.removeView(shown)
    }

    companion object {
        const val SHOW_MS = 6_000L
        private const val KEY_SHOWN = "coach_panel_open_shown"
        private const val FILE_NAME = "coach_mark"

        fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }
}
