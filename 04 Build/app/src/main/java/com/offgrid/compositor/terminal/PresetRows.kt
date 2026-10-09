package com.offgrid.compositor.terminal

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R

/**
 * The 📁 Presets folder's rows (final build, M4): a name box, Save, and one row per saved
 * look with Load, Rename, and Delete. Delete asks first. Nothing here is a slider.
 */
internal class PresetRows(
    context: Context,
    private val presets: LookPresets,
    private val onLoaded: () -> Unit,
    private val say: (String) -> Unit,
) : LinearLayout(context), Styled {
    private var typed = ""
    private val list = LinearLayout(context).apply { orientation = VERTICAL }
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
        addView(list, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 8f) })
        refresh()
    }

    private fun save() {
        val name = typed.ifEmpty { context.getString(R.string.terminal_preset_default_name, presets.names().size + 1) }
        when (presets.save(name)) {
            LookPresets.Result.DONE -> say(context.getString(R.string.terminal_preset_saved, name))
            LookPresets.Result.REPLACED -> say(context.getString(R.string.terminal_preset_replaced, name))
            LookPresets.Result.FULL -> say(context.getString(R.string.terminal_preset_full, LookPresets.MAX_PRESETS))
            else -> say(context.getString(R.string.terminal_text_empty))
        }
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        titles.clear()
        notes.clear()
        buttons.clear()
        val names = presets.names()
        if (names.isEmpty()) {
            list.addView(rowNote(context, context.getString(R.string.terminal_preset_none)).also { notes += it })
        }
        for (name in names) list.addView(presetRow(name), MATCH_WRAP().apply { topMargin = Hud.dpi(context, 6f) })
        look?.let { applyLook(it) }
    }

    private fun presetRow(name: String): View {
        val box = LinearLayout(context).apply { orientation = VERTICAL }
        box.addView(rowTitle(context, name).also { titles += it })
        val buttons = LinearLayout(context).apply { orientation = HORIZONTAL }
        val load = ActionButton(context, context.getString(R.string.terminal_preset_load))
        val rename = ActionButton(context, context.getString(R.string.terminal_preset_rename))
        val delete = ActionButton(context, context.getString(R.string.terminal_preset_delete))
        load.contentDescription = context.getString(R.string.terminal_preset_load_description, name)
        rename.contentDescription = context.getString(R.string.terminal_preset_rename_description, name)
        delete.contentDescription = context.getString(R.string.terminal_preset_delete_description, name)
        for ((index, button) in listOf(load, rename, delete).withIndex()) {
            buttons.addView(button, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = Hud.dpi(context, 4f)
            })
        }
        box.addView(buttons, MATCH_WRAP())
        val confirm = LinearLayout(context).apply {
            orientation = HORIZONTAL
            visibility = GONE
        }
        val yes = ActionButton(context, context.getString(R.string.terminal_preset_delete_yes))
        val no = ActionButton(context, context.getString(R.string.terminal_reset_no))
        confirm.addView(yes, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = Hud.dpi(context, 4f) })
        confirm.addView(no, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = Hud.dpi(context, 4f) })
        box.addView(confirm, MATCH_WRAP().apply { topMargin = Hud.dpi(context, 4f) })
        this.buttons += listOf(load, rename, delete, yes, no)

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
        delete.setOnClickListener {
            buttons.visibility = GONE
            confirm.visibility = VISIBLE
            say(context.getString(R.string.terminal_preset_delete_question, name))
        }
        no.setOnClickListener {
            confirm.visibility = GONE
            buttons.visibility = VISIBLE
            say(context.getString(R.string.terminal_preset_kept, name))
        }
        yes.setOnClickListener {
            presets.delete(name)
            say(context.getString(R.string.terminal_preset_deleted, name))
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
