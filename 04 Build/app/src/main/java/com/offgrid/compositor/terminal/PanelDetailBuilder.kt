package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R

internal class PanelDetailBuilder(
    private val context: Context,
    private val prefs: PanelPrefs,
    private val onNumber: (NumberItem, Int, Boolean) -> Unit,
    private val onToggle: (PanelFolder, ToggleItem, Boolean) -> Unit,
    private val onChoice: (ChoiceItem, String) -> Unit,
    private val onColor: (ColorItem, Int) -> Unit,
    private val onAction: (PanelAction) -> Unit,
    private val onFolder: (PanelFolder, View) -> Unit,
    private val onProblem: (String) -> Unit,
    private val typing: () -> Pair<Float, Boolean>,
) {
    private val density = context.resources.displayMetrics.density
    private var currentGuide: LaserTypeView? = null

    fun updateTyping(speed: Float, animateTyping: Boolean) {
        currentGuide?.let {
            it.speed = speed
            it.animateTyping = animateTyping
        }
    }

    fun clear() {
        currentGuide = null
    }

    fun build(folder: PanelFolder, content: LinearLayout) {
        content.removeAllViews()
        content.addView(pathBar(folder), LinearLayout.LayoutParams(MATCH, WRAP))
        content.addView(folderHeading(folder), LinearLayout.LayoutParams(MATCH, WRAP))

        val (speed, animateTyping) = typing()
        val guide = LaserTypeView(context).apply {
            textSize = 10.5f
            setPadding(dpi(9f), dpi(7f), dpi(9f), dpi(8f))
            background = GradientDrawable().apply {
                setColor(Hud.withAlpha(Hud.INK, 0.5f))
                cornerRadius = dp(6f)
                setStroke(dpi(1f), Hud.withAlpha(Hud.LASER, 0.2f))
            }
            this.speed = speed
            this.animateTyping = animateTyping
        }
        currentGuide = guide
        content.addView(guide, LinearLayout.LayoutParams(MATCH, WRAP))
        guide.type(PanelText.guidance(folder, folder.results { _, _ -> TestMark.NOT_TESTED }))

        if (folder.folders.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_folders))
            val tiles = TileGrid(context, 84f, singleColumn = true)
            for (child in folder.folders) {
                val tile = FolderTile(context, child, 50f, bigTile = true)
                tile.setOnClickListener { onFolder(child, tile) }
                tiles.addView(tile)
            }
            content.addView(tiles, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        if (folder.numbers.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_numbers))
            folder.numbers.forEach { item ->
                content.addView(
                    NumberRow(
                        context,
                        item,
                        prefs.number(item.key, item.default),
                        onChange = { value, announce -> onNumber(item, value, announce) },
                        onProblem = onProblem,
                    ),
                    LinearLayout.LayoutParams(MATCH, WRAP),
                )
            }
        }
        if (folder.toggles.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_toggles))
            folder.toggles.forEach { item ->
                val note = if (item.isTest) context.getString(R.string.terminal_not_tested) else item.help
                content.addView(
                    ToggleRow(context, item, note, prefs.toggle(toggleKey(folder, item), item.default)) {
                        onToggle(folder, item, it)
                    },
                    LinearLayout.LayoutParams(MATCH, WRAP),
                )
            }
        }
        if (folder.choices.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_choices))
            folder.choices.forEach { item ->
                content.addView(
                    ChoiceRow(context, item, prefs.choice(item.key, item.default)) { onChoice(item, it) },
                    LinearLayout.LayoutParams(MATCH, WRAP),
                )
            }
        }
        if (folder.colors.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_colors))
            folder.colors.forEach { item ->
                content.addView(
                    NeonColorRow(context, item, prefs.color(item.key, item.default)) { onColor(item, it) },
                    LinearLayout.LayoutParams(MATCH, WRAP),
                )
            }
        }
        if (folder.actions.isNotEmpty()) {
            content.addView(sectionLabel(R.string.terminal_section_actions))
            folder.actions.forEach { item ->
                content.addView(
                    ActionRow(context, item) { onAction(item.action) },
                    LinearLayout.LayoutParams(MATCH, WRAP),
                )
            }
        }
    }

    private fun pathBar(folder: PanelFolder): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
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
        frame.addView(HoloRingView(context, accent).apply { spinning = true }, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
        frame.addView(
            FolderIconView(context, accent, folder.itemCount, folder.tag, compact = false),
            FrameLayout.LayoutParams(dpi(42f), dpi(36f), Gravity.CENTER),
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

    private fun sectionLabel(textRes: Int) = TextView(context).apply {
        text = context.getString(textRes)
        typeface = Hud.monoBold(context)
        textSize = 9.5f
        letterSpacing = 0.12f
        setTextColor(Hud.GOLD)
        setPadding(0, dpi(12f), 0, dpi(4f))
    }

    private fun toggleKey(folder: PanelFolder, item: ToggleItem) =
        if (item.isTest) "test.${folder.id}.${item.key}" else item.key

    private fun dp(value: Float) = value * density
    private fun dpi(value: Float) = (dp(value) + 0.5f).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
