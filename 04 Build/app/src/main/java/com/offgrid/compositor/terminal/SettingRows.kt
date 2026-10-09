package com.offgrid.compositor.terminal

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import com.offgrid.compositor.R

/**
 * A pick-one list (final build): a name, then one row per choice with a round mark.
 * Tapping a row picks it at once. The choice is saved by the panel.
 */
internal class ChoiceGroup(
    context: Context,
    private val item: ChoiceItem,
    current: String,
    private val onChoose: (Choice) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val rows = LinkedHashMap<String, ChoiceRow>()

    init {
        orientation = VERTICAL
        setPadding(0, Hud.dpi(context, 4f), 0, Hud.dpi(context, 4f))
        addView(title)
        for (choice in item.choices) {
            val preview = when (item.preview) {
                ChoicePreview.TOGGLE -> PowerToggleView(context).apply {
                    fixedStyle = ToggleStyle.from(choice.value)
                    setOn(true)
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                ChoicePreview.NONE -> null
            }
            val row = ChoiceRow(context, choice.label, choice.help, preview)
            row.setOnClickListener {
                if (rows[choice.value]?.chosen == true) return@setOnClickListener
                select(choice.value)
                onChoose(choice)
            }
            rows[choice.value] = row
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        select(current)
    }

    /** Lights the mark of [value]'s row. An unknown value lights the first row. */
    fun select(value: String) {
        val known = if (rows.containsKey(value)) value else item.choices.first().value
        rows.forEach { (key, row) -> row.chosen = key == known }
    }

    override fun applyLook(look: PanelLook) {
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f * look.fontScale)
        title.setTextColor(look.sideText)
    }
}

/**
 * A short typed text (final build), like the status word or a message for the laser.
 * Done, or leaving the box, saves it. An empty box is refused in plain words and
 * nothing changes. It is never a slider.
 */
internal class TextRow(
    context: Context,
    item: TextItem,
    start: String,
    private val onSave: (String) -> Unit,
    private val onProblem: (String) -> Unit,
) : LinearLayout(context), Styled {
    private val title = rowTitle(context, item.label)
    private val words = rowNote(context, item.help)
    val box = AppCompatEditText(context)
    private var saved = start
    private var tick = true

    init {
        orientation = VERTICAL
        setPadding(0, Hud.dpi(context, 4f), 0, Hud.dpi(context, 6f))
        addView(title)
        addView(words)
        box.setText(start)
        box.typeface = Hud.monoBold(context)
        box.gravity = Gravity.CENTER_VERTICAL
        box.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        box.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        box.filters = arrayOf(InputFilter.LengthFilter(item.maxLength))
        box.maxLines = 1
        box.isSingleLine = true
        box.setPadding(Hud.dpi(context, 10f), 0, Hud.dpi(context, 10f), 0)
        box.minimumHeight = Hud.dpi(context, 48f)
        box.contentDescription = context.getString(R.string.terminal_text_description, item.label)
        box.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit()
                box.clearFocus()
                true
            } else {
                false
            }
        }
        box.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commit() }
        addView(box, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = Hud.dpi(context, 4f)
        })
    }

    private fun commit() {
        val typed = box.text?.toString().orEmpty().trim()
        when {
            typed.isEmpty() -> {
                box.setText(saved)
                onProblem(context.getString(R.string.terminal_text_empty))
            }
            typed != saved -> {
                saved = typed
                if (typed != box.text?.toString()) box.setText(typed)
                onSave(typed)
                if (tick) performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
    }

    /** Shows [value] without saving it, like after Reset all. */
    fun show(value: String) {
        saved = value
        if (!box.hasFocus()) box.setText(value)
    }

    override fun applyLook(look: PanelLook) {
        tick = look.tick
        styleRowWords(title, words, look)
        box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * look.fontScale)
        box.setTextColor(look.edge)
        box.highlightColor = Hud.withAlpha(look.edge, 0.35f)
        box.background = fieldBackground(context, look)
    }
}
