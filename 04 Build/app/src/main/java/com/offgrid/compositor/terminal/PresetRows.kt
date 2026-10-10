package com.offgrid.compositor.terminal

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R

/**
 * Named profiles hold the look, panel position, camera mode, and active effect settings.
 */
internal class PresetRows(
    context: Context,
    private val presets: LookPresets,
    private val onLoaded: () -> Unit,
    private val say: (String) -> Unit,
) : LinearLayout(context), Styled {
    private var typed = ""
    private val list = LinearLayout(context).apply { orientation = VERTICAL }
    private val saveConfirm = LinearLayout(context).apply {
        orientation = VERTICAL
        visibility = GONE
    }
    private val saveQuestion = rowNote(context, "")
    private val titles = ArrayList<TextView>()
    private val notes = ArrayList<TextView>()
    private val buttons = ArrayList<ActionButton>()
    private var look: PanelLook? = null

    init {
        orientation = VERTICAL
        val nameItem = TextItem(
            NAME_KEY,
            context.getString(R.string.terminal_preset_name),
            context.getString(R.string.terminal_preset_name_help),
            "",
            LookPresets.NAME_MAX,
        )
        addView(TextRow(context, nameItem, "", onSave = { typed = it }, onProblem = say), MATCH_WRAP())
        val save = ActionButton(context, context.getString(R.string.terminal_preset_save))
        save.setOnClickListener { save() }
        addView(save, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 6f) })
        saveConfirm.addView(saveQuestion, MATCH_WRAP())
        val saveChoices = LinearLayout(context).apply { orientation = HORIZONTAL }
        val replace = ActionButton(context, "Overwrite")
        val cancel = ActionButton(context, context.getString(R.string.terminal_reset_no))
        saveChoices.addView(replace, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = Hud.dpi(context, 4f)
        })
        saveChoices.addView(cancel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Hud.dpi(context, 4f)
        })
        saveConfirm.addView(saveChoices, MATCH_WRAP())
        buttons += listOf(replace, cancel)
        addView(saveConfirm, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 4f) })
        replace.setOnClickListener {
            val name = pendingSaveName ?: return@setOnClickListener
            val result = presets.save(name, overwrite = true)
            pendingSaveName = null
            saveConfirm.visibility = GONE
            say(resultMessage(result, name))
            refresh()
        }
        cancel.setOnClickListener {
            pendingSaveName = null
            saveConfirm.visibility = GONE
        }
        addView(list, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 8f) })
        refresh()
    }

    private var pendingSaveName: String? = null

    private fun save() {
        val name = typed
        when (val result = presets.save(name)) {
            LookPresets.Result.TAKEN -> {
                pendingSaveName = name
                saveQuestion.text = "Overwrite the saved folder \"$name\"?"
                saveConfirm.visibility = VISIBLE
            }
            else -> say(resultMessage(result, name))
        }
        refresh()
    }

    private fun resultMessage(result: LookPresets.Result, name: String): String = when (result) {
        LookPresets.Result.DONE -> context.getString(R.string.terminal_preset_saved, name)
        LookPresets.Result.REPLACED -> context.getString(R.string.terminal_preset_replaced, name)
        LookPresets.Result.FULL -> context.getString(R.string.terminal_preset_full, LookPresets.MAX_PRESETS)
        LookPresets.Result.NO_NAME -> context.getString(R.string.terminal_text_empty)
        LookPresets.Result.TAKEN -> context.getString(R.string.terminal_preset_taken, name)
        LookPresets.Result.INVALID_NAME -> "Use a name with 24 characters or fewer."
        else -> context.getString(R.string.terminal_preset_type_name)
    }

    private fun refresh() {
        list.removeAllViews()
        titles.clear()
        notes.clear()
        buttons.clear()
        buttons += saveConfirmButtons()
        val names = presets.names()
        if (names.isEmpty()) {
            list.addView(rowNote(context, context.getString(R.string.terminal_preset_none)).also { notes += it })
        }
        for (name in names) list.addView(presetRow(name), MATCH_WRAP().apply { topMargin = Hud.dpi(context, 6f) })
        val archived = presets.archivedNames()
        if (archived.isNotEmpty()) {
            list.addView(rowTitle(context, "ARCHIVED").also { titles += it }, MATCH_WRAP().apply {
                topMargin = Hud.dpi(context, 12f)
            })
            archived.forEach { name ->
                val row = LinearLayout(context).apply { orientation = HORIZONTAL }
                row.addView(rowTitle(context, name).also { titles += it }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                val restore = ActionButton(context, "Restore")
                buttons += restore
                restore.setOnClickListener {
                    say(resultMessage(presets.restore(name), name))
                    refresh()
                }
                row.addView(restore, LayoutParams(Hud.dpi(context, 88f), LayoutParams.WRAP_CONTENT))
                list.addView(row, MATCH_WRAP())
            }
        }
        look?.let { applyLook(it) }
    }

    private fun saveConfirmButtons(): List<ActionButton> =
        if (saveConfirm.childCount > 1) {
            val choices = saveConfirm.getChildAt(1) as LinearLayout
            listOf(choices.getChildAt(0), choices.getChildAt(1)).filterIsInstance<ActionButton>()
        } else emptyList()

    private fun presetRow(name: String): View {
        val box = LinearLayout(context).apply { orientation = VERTICAL }
        box.addView(rowTitle(context, name).also { titles += it })
        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL }
        val load = ActionButton(context, context.getString(R.string.terminal_preset_load))
        val rename = ActionButton(context, context.getString(R.string.terminal_preset_rename))
        val duplicate = ActionButton(context, "Duplicate")
        val archive = ActionButton(context, "Archive")
        load.contentDescription = context.getString(R.string.terminal_preset_load_description, name)
        rename.contentDescription = context.getString(R.string.terminal_preset_rename_description, name)
        duplicate.contentDescription = "Duplicate $name"
        archive.contentDescription = "Archive $name"
        for ((index, button) in listOf(load, duplicate, rename, archive).withIndex()) {
            buttons.addView(button, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = Hud.dpi(context, 4f)
            })
        }
        box.addView(buttons, MATCH_WRAP())
        val confirm = LinearLayout(context).apply {
            orientation = HORIZONTAL
            visibility = GONE
        }
        val yes = ActionButton(context, "Archive")
        val no = ActionButton(context, context.getString(R.string.terminal_reset_no))
        confirm.addView(yes, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 4f) })
        confirm.addView(no, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 4f) })
        box.addView(confirm, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 4f) })
        this.buttons += listOf(load, duplicate, rename, archive, yes, no)

        load.setOnClickListener {
            if (presets.load(name) == LookPresets.Result.DONE) {
                onLoaded()
                say(context.getString(R.string.terminal_preset_loaded, name))
            }
        }
        rename.setOnClickListener {
            val result = presets.rename(name, typed)
            say(
                when (result) {
                    LookPresets.Result.DONE -> context.getString(R.string.terminal_preset_renamed, name, typed)
                    LookPresets.Result.TAKEN -> context.getString(R.string.terminal_preset_taken, typed)
                    else -> context.getString(R.string.terminal_preset_type_name)
                },
            )
            refresh()
        }
        duplicate.setOnClickListener {
            val result = presets.duplicate(name, typed)
            say(resultMessage(result, typed))
            refresh()
        }
        archive.setOnClickListener {
            buttons.visibility = GONE
            confirm.visibility = VISIBLE
            say("Archive \"$name\"? It can be restored later.")
        }
        no.setOnClickListener {
            confirm.visibility = GONE
            buttons.visibility = VISIBLE
            say(context.getString(R.string.terminal_preset_kept, name))
        }
        yes.setOnClickListener {
            presets.archive(name)
            say("Archived \"$name\".")
            refresh()
        }
        return box
    }

    /** The preset names and buttons follow the panel's look, including rows made after a save. */
    override fun applyLook(look: PanelLook) {
        this.look = look
        for (title in titles) {
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP * look.fontScale)
            title.setTextColor(look.secondaryText)
        }
        for (note in notes) {
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, NOTE_SP * look.fontScale)
            note.setTextColor(Hud.withAlpha(look.secondaryText, NOTE_ALPHA))
        }
        buttons.forEach { it.applyLook(look) }
    }

    @Suppress("FunctionName")
    private fun MATCH_WRAP() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    private companion object {
        const val NAME_KEY = "preset_name"
        const val TITLE_SP = 12.5f
        const val NOTE_SP = 9.5f
        const val NOTE_ALPHA = 0.62f
    }
}
