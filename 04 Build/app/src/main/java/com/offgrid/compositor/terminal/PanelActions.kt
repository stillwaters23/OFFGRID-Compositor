package com.offgrid.compositor.terminal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R
import com.offgrid.compositor.effects.FxKeys

private const val RESET_FOLDER_ID = "reset"
private const val ALL_SETTINGS = "*"
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** A pick-one setting changed. It is saved and used at once. */
internal fun TerminalPanel.saveChoice(item: ChoiceItem, choice: Choice) {
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
internal fun TerminalPanel.saveText(item: TextItem, value: String) {
    store.setText(item.key, value)
    if (item.key.startsWith(TestMarks.SEEN)) {
        checkTestCode(item.key.removePrefix(TestMarks.SEEN), value)
        return
    }
    settingChanged(item.key)
    say(context.getString(R.string.terminal_text_saved, item.label, value))
}

/** Feature 37: checks the code a viewer saw against the code the video showed. */
internal fun TerminalPanel.checkTestCode(platform: String, seen: String) {
    val day = TestMarks.today()
    val message = when (TestMarks.check(store, platform, seen, day)) {
        TestMarks.Check.NO_CODE -> context.getString(R.string.terminal_code_none)
        TestMarks.Check.MATCH -> context.getString(R.string.terminal_code_match, day)
        TestMarks.Check.MISMATCH -> context.getString(R.string.terminal_code_mismatch, day)
    }
    say(message)
}

/** Buttons the panel handles itself. The rest go to the camera screen. */
internal fun TerminalPanel.runAction(item: ActionItem) {
    when (item.action) {
        PanelAction.TEST_CODE -> {
            val platform = item.arg ?: return
            val code = TestMarks.newCode()
            store.setText(TestMarks.SHOWN + platform, code)
            onAction?.invoke(PanelAction.TEST_CODE, code)
            say(context.getString(R.string.terminal_code_shown, code))
        }
        PanelAction.SAFE_START -> safeStart(item.arg ?: return)
        PanelAction.OPEN_EFFECT_LINK -> openEffectLink(item.arg ?: return)
        else -> onAction?.invoke(item.action, item.arg)
    }
}

internal fun TerminalPanel.openEffectLink(id: String) {
    val parts = id.split(':')
    val category = parts.getOrNull(0)?.toIntOrNull() ?: return
    val index = parts.getOrNull(1)?.toIntOrNull() ?: return
    val link = EffectLinks.categories.getOrNull(category)?.links?.getOrNull(index) ?: return
    val uri = Uri.parse(link.url)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) {
        say("This link is not available.")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        say("No browser is available.")
    }
}

/** A platform's safe start: the face diamond follows the face, and the red sweep runs on a kick. */
internal fun TerminalPanel.safeStart(platform: String) {
    store.setToggle(FxKeys.DIAMOND_ON, true)
    store.setToggle(FxKeys.DIAMOND_FOLLOW, true)
    store.setToggle(FxKeys.SWEEP_GO_PREFIX + platform, true)
    settingChanged(FxKeys.DIAMOND_ON)
    openFolder?.let { buildDetail(it) }
    val title = home.folders.firstOrNull { it.id == platform }?.title ?: platform
    say(context.getString(R.string.terminal_safe_start, title))
}

/** Puts one folder's settings back to the start. Test marks and the camera mode stay. */
internal fun TerminalPanel.resetFolder(id: String?) {
    val folder = findFolder(home, id ?: return) ?: return
    hideKeyboard()
    FolderReset.reset(store, folder)
    lookChanged(folderToShow = openFolder)
    say(context.getString(R.string.terminal_folder_reset_done, folder.title))
}

internal fun TerminalPanel.findFolder(from: PanelFolder, id: String): PanelFolder? {
    if (from.id == id) return from
    for (child in from.folders) findFolder(child, id)?.let { return it }
    return null
}

/** After a preset loads or a folder resets, the panel, the size boxes, and the camera follow. */
internal fun TerminalPanel.lookChanged(folderToShow: PanelFolder?) {
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
internal fun TerminalPanel.settingChanged(key: String) {
    onSettingChanged?.invoke(key)
}

/** A button with a short note. Center panel works at once. Reset all asks first, in plain words. */
internal fun TerminalPanel.actionRow(item: ActionItem): View {
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

internal fun TerminalPanel.applyTheme(theme: Theme) {
    hideKeyboard()
    store.applyTheme(theme)
    refreshLook()
    themeRows.forEach { it.inUse = it.theme == theme }
    sweepOnce()
    say(context.getString(R.string.terminal_theme_applied, theme.title))
}

/** Puts every panel setting back to the start. The test marks and the panel's place stay. */
internal fun TerminalPanel.resetAll() {
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
    // The ⋮ menu's choices are back to the start too: Big tiles, Important first.
    lastMove = null
    menu.setUndoAvailable(false)
    menu.refresh()
    applyFolderLayout()
    requestLayout()
    openFolder?.let {
        buildDetail(it)
        detailScroll.scrollTo(0, 0)
    }
    settingChanged(ALL_SETTINGS)
    say(context.getString(R.string.terminal_reset_done))
}

internal fun TerminalPanel.saveNumber(item: NumberItem, value: Int, announce: Boolean) {
    store.setNumber(item.key, value)
    refreshLook()
    settingChanged(item.key)
    if (announce) {
        say(context.getString(R.string.terminal_number_saved, item.label, value))
    }
}

internal fun TerminalPanel.saveToggle(folder: PanelFolder, item: ToggleItem, on: Boolean) {
    if (item.isTest) {
        prefs.setToggle(testKey(folder, item), on)
        val message = if (on) R.string.terminal_test_on else R.string.terminal_test_off
        say(context.getString(message, item.label))
        return
    }
    if (item.key == FxKeys.MIC_ON && on && store.toggle(FxKeys.MIC_BLOCKED)) {
        say(context.getString(R.string.mic_toggle_blocked))
        return
    }
    if (item.key == FxKeys.CAMERA_ON || item.key == FxKeys.MIC_ON) {
        setToggleSetting(item.key, on)
        val message = if (on) R.string.terminal_toggle_on else R.string.terminal_toggle_off
        say(context.getString(message, item.label))
        return
    }
    store.setToggle(item.key, on)
    refreshLook()
    if (item.key == LookKeys.FULL_SCREEN || item.key == LookKeys.SEE_THROUGH_FULL_SIZE) {
        requestLayout()
    }
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

internal fun TerminalPanel.testKey(folder: PanelFolder, item: ToggleItem): String = "${PanelPrefs.TEST_PREFIX}${folder.id}.${item.key}"

/** The mark Royalty gave a test in 📁 Results, or by the success code. */
internal fun TerminalPanel.markOf(folder: PanelFolder, item: ToggleItem): TestMark {
    val platform = TestMarks.platformOf(folder) ?: return TestMark.NOT_TESTED
    return TestMarks.mark(store, platform, item.key)
}

/** A test toggle's note: its mark, and the day it was marked. */
internal fun TerminalPanel.markLine(folder: PanelFolder, item: ToggleItem): String {
    val mark = markOf(folder, item)
    val platform = TestMarks.platformOf(folder) ?: return markText(mark)
    val day = TestMarks.date(store, platform, item.key)
    return if (mark == TestMark.NOT_TESTED || day.isEmpty()) markText(mark) else "${markText(mark)} ($day)"
}

internal fun TerminalPanel.markText(mark: TestMark): String = context.getString(
    when (mark) {
        TestMark.NOT_TESTED -> R.string.terminal_not_tested
        TestMark.SUCCESS -> R.string.terminal_success
        TestMark.FAILED -> R.string.terminal_failed
    },
)
