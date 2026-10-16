package com.offgrid.compositor.terminal

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.offgrid.compositor.R
import com.offgrid.compositor.RenderProbe

internal object RenderProbeCaptures {
    fun readFile(context: Context, name: String): String {
        if (!RenderProbe.isDebugBuild(context)) return ""
        val file = RenderProbe.captureFile(context, name)
        return try {
            if (file.exists()) file.readText() else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun combinedText(context: Context): String {
        val cold = readFile(context, RenderProbe.COLD_START_FILE)
        val resume = readFile(context, RenderProbe.RESUME_FILE)
        return buildString {
            appendLine(context.getString(R.string.render_probe_section_cold))
            appendLine()
            append(if (cold.isEmpty()) context.getString(R.string.render_probe_empty) else cold)
            appendLine()
            appendLine()
            appendLine(context.getString(R.string.render_probe_section_resume))
            appendLine()
            append(if (resume.isEmpty()) context.getString(R.string.render_probe_empty) else resume)
        }
    }
}

internal fun TerminalPanel.renderProbeViewer(): LinearLayout {
    val box = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dpi(8f), 0, dpi(4f))
    }
    val body = TextView(context).apply {
        typeface = Hud.mono(context)
        textSize = 9.5f
        setTextIsSelectable(true)
        movementMethod = ScrollingMovementMethod.getInstance()
        text = RenderProbeCaptures.combinedText(context)
    }
    box.addView(
        ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpi(220f))
            addView(body, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        },
    )
    val actions = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dpi(10f), 0, 0)
    }
    val copy = ActionButton(context, context.getString(R.string.render_probe_copy))
    val share = ActionButton(context, context.getString(R.string.render_probe_share))
    actions.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginEnd = dpi(4f)
    })
    actions.addView(share, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginStart = dpi(4f)
    })
    box.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    copy.setOnClickListener {
        val text = RenderProbeCaptures.combinedText(context)
        val clip = ClipData.newPlainText(context.getString(R.string.render_probe_clip_label), text)
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
    }
    share.setOnClickListener {
        val text = RenderProbeCaptures.combinedText(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.render_probe_share_chooser)))
        } catch (_: ActivityNotFoundException) {
            // No share target; stay silent (no toast).
        }
    }
    restyle(box, look)
    for (i in 0 until box.childCount) {
        val child = box.getChildAt(i)
        if (child is Styled) child.applyLook(look)
        if (child is ViewGroup) {
            for (j in 0 until child.childCount) {
                val inner = child.getChildAt(j)
                if (inner is TextView) inner.styled(TextRole.PATH, 9.5f)
            }
        }
    }
    body.styled(TextRole.PATH, 9.5f)
    return box
}
