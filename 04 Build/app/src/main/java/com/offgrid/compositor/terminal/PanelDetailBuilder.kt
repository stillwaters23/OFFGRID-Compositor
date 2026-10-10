package com.offgrid.compositor.terminal

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.offgrid.compositor.R

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val RESET_FOLDER_ID = "reset"

internal fun TerminalPanel.buildDetail(folder: PanelFolder) {
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

internal fun TerminalPanel.sectionFor(folder: PanelFolder): Int = when {
    folder.kind == FolderKind.TESTS && folder.items.all { it is ToggleItem } -> R.string.terminal_section_toggles
    folder.items.all { it is ThemeItem } -> R.string.terminal_section_themes
    folder.items.all { it is ColorItem } -> R.string.terminal_section_color
    else -> R.string.terminal_section_settings
}

internal fun TerminalPanel.itemView(folder: PanelFolder, item: PanelItem): View = when (item) {
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

internal fun TerminalPanel.pathBar(folder: PanelFolder): View {
    val bar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val path = FitTextView(context).apply {
        text = (listOf(context.getString(R.string.terminal_home)) + folder.trail().map { it.label })
            .joinToString(" › ")
        typeface = Hud.mono(context)
        styled(TextRole.PATH, 9.5f)
    }
    bar.addView(path, LinearLayout.LayoutParams(0, WRAP, 1f))
    return bar
}

internal fun TerminalPanel.folderHeading(folder: PanelFolder): View {
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
    frame.addView(ring, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
    frame.addView(
        FolderIconView(context, folder.itemCount, folder.tag, compact = false, special = folder.special),
        FrameLayout.LayoutParams(dpi(42f), dpi(36f), Gravity.CENTER),
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

internal fun TerminalPanel.sectionLabel(textRes: Int): View {
    return LaserTitleView(context).apply {
        text = context.getString(textRes)
        typeface = Hud.monoBold(context)
        letterSpacing = 0.12f
        setPadding(0, dpi(12f), 0, dpi(4f))
        styled(TextRole.SECTION, 9.5f)
    }
}
