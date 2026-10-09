package com.offgrid.compositor.terminal

import com.offgrid.compositor.effects.EffectSnapshot

/**
 * Reads every effect setting ("fx." keys) from the panel's saved settings in one go, for the
 * camera screen (final build, M4). The keys come from the folders themselves, so a new
 * effect setting in a folder reaches the effects with no extra wiring.
 */
internal object EffectSettings {
    private class Keys(val numbers: List<String>, val toggles: List<String>, val choices: List<String>, val texts: List<String>)

    private val keys: Keys by lazy {
        val numbers = LinkedHashSet<String>()
        val toggles = LinkedHashSet<String>()
        val choices = LinkedHashSet<String>()
        val texts = LinkedHashSet<String>()
        fun walk(folder: PanelFolder) {
            for (item in folder.items) {
                when (item) {
                    is NumberItem -> if (item.key.startsWith(PREFIX)) numbers += item.key
                    is ToggleItem -> if (item.key.startsWith(PREFIX) && !item.isTest) toggles += item.key
                    is ChoiceItem -> if (item.key.startsWith(PREFIX)) choices += item.key
                    is TextItem -> if (item.key.startsWith(PREFIX)) texts += item.key
                    else -> Unit
                }
            }
            folder.folders.forEach { walk(it) }
        }
        walk(PanelTree.build())
        Keys(numbers.toList(), toggles.toList(), choices.toList(), texts.toList())
    }

    fun snapshot(store: ThemeStore): EffectSnapshot = EffectSnapshot(
        numbers = keys.numbers.associateWith { store.number(it) },
        toggles = keys.toggles.associateWith { store.toggle(it) },
        choices = keys.choices.associateWith { store.choice(it) },
        texts = keys.texts.associateWith { store.text(it) },
    )

    private const val PREFIX = "fx."
}
