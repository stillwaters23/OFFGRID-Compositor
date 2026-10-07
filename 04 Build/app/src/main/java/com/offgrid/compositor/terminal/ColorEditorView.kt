package com.offgrid.compositor.terminal

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.offgrid.compositor.R

/**
 * The color mixer inside each color folder of the theme office.
 *
 * A live swatch, a hex code box, nine typed numbers that mix like light,
 * "Use system color", and Reset. Every change is saved and shows on the panel at once.
 * Bad input gets a plain message and changes nothing.
 */
internal class ColorEditorView(
    context: Context,
    private val part: ColorPart,
    private val store: ThemeStore,
    private val onLookChanged: () -> Unit,
    private val say: (String) -> Unit,
) : LinearLayout(context) {
    private val swatch = SwatchView(context)
    private val hexBox = HexBox(context, ::onHex, say)
    private val systemRow: ToggleRow
    private val boxes = ArrayList<NumberBox>(RainbowMix.NAMES.size)

    init {
        orientation = VERTICAL
        setPadding(0, Hud.dpi(context, 2f), 0, Hud.dpi(context, 4f))
        // The typing glow around a box may reach a little past its row.
        clipChildren = false

        val top = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
        }
        top.addView(swatch, LayoutParams(Hud.dpi(context, 60f), Hud.dpi(context, 60f)))
        val code = LinearLayout(context).apply { orientation = VERTICAL }
        code.addView(label(R.string.terminal_color_code))
        code.addView(hexBox, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 46f)))
        top.addView(code, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = Hud.dpi(context, 8f)
        })
        addView(top, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val systemItem = ToggleItem(
            key = "system_color",
            label = context.getString(R.string.terminal_color_system),
            help = context.getString(R.string.terminal_color_system_help),
            default = true,
            isTest = false,
        )
        systemRow = ToggleRow(context, systemItem, systemItem.help, store.part(part).system) { on -> onSystem(on) }
        addView(systemRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = Hud.dpi(context, 6f)
        })

        addView(label(R.string.terminal_color_mix))
        val grid = TileGrid(context, MIX_CELL_DP, evenRows = true)
        grid.clipChildren = false
        val start = store.mix(part)
        RainbowMix.NAMES.forEachIndexed { index, name ->
            val cell = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(Hud.dpi(context, 3f), 0, Hud.dpi(context, 3f), Hud.dpi(context, 6f))
            }
            cell.addView(TextView(context).apply {
                text = dotted(name, RainbowMix.BASE_COLORS[index])
                typeface = Hud.condensedBold
                styled(TextRole.LABEL, 12f)
            })
            val box = NumberBox(
                context,
                name,
                start[index],
                onChange = { value, announce -> onNumber(index, value, announce) },
                onProblem = say,
            )
            boxes += box
            cell.addView(box, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 46f)))
            grid.addView(cell)
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val reset = ActionButton(context, context.getString(R.string.terminal_color_reset)).apply {
            contentDescription = context.getString(R.string.terminal_color_reset_description, part.title)
            setOnClickListener { onReset() }
        }
        addView(reset, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = Hud.dpi(context, 4f)
        })
        refresh()
    }

    private fun label(textRes: Int) = TextView(context).apply {
        text = context.getString(textRes)
        typeface = Hud.monoBold(context)
        letterSpacing = 0.12f
        setPadding(0, Hud.dpi(context, 8f), 0, Hud.dpi(context, 4f))
        styled(TextRole.SECTION, 9.5f)
    }

    /** "● Red", with the dot in that color. Black gets a hollow ring, so it shows on black glass. */
    private fun dotted(name: String, color: Int): CharSequence {
        val dark = color and 0x00FFFFFF == 0
        val text = SpannableStringBuilder(if (dark) "○ " else "● ").append(name)
        text.setSpan(ForegroundColorSpan(if (dark) RING_GRAY else color), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    /** Shows the saved color in the swatch, the hex box, the toggle, and all nine numbers. */
    private fun refresh() {
        val color = store.color(part)
        swatch.color = color
        hexBox.show(RainbowMix.hex(color))
        systemRow.toggle.setOn(store.part(part).system)
        val mix = store.mix(part)
        boxes.forEachIndexed { index, box -> box.show(mix[index]) }
    }

    private fun onHex(color: Int) {
        store.setCustom(part, color, RainbowMix.split(color))
        refresh()
        onLookChanged()
        say(context.getString(R.string.terminal_color_saved, part.title, RainbowMix.hex(color)))
    }

    private fun onNumber(index: Int, value: Int, announce: Boolean) {
        val weights = IntArray(RainbowMix.NAMES.size) { boxes.getOrNull(it)?.value ?: 0 }
        weights[index] = value
        val color = RainbowMix.mix(weights)
        store.setCustom(part, color, weights)
        swatch.color = color
        hexBox.show(RainbowMix.hex(color))
        systemRow.toggle.setOn(false)
        onLookChanged()
        if (announce) {
            say(context.getString(R.string.terminal_mix_saved, RainbowMix.NAMES[index], value, RainbowMix.hex(color)))
        }
    }

    private fun onSystem(on: Boolean) {
        store.setSystem(part, on)
        refresh()
        onLookChanged()
        val message = if (on) R.string.terminal_color_system_on else R.string.terminal_color_system_off
        say(context.getString(message, part.title))
    }

    private fun onReset() {
        store.resetPart(part)
        refresh()
        onLookChanged()
        say(context.getString(R.string.terminal_color_reset_done, part.title))
    }

    private companion object {
        const val MIX_CELL_DP = 76f
        const val RING_GRAY = 0xFFB0B0B0.toInt()
    }
}
