package com.offgrid.compositor.terminal

import com.offgrid.compositor.effects.FxKeys

/**
 * Every folder's Reset (final build, M5). It puts the settings in a folder, and in the
 * folders inside it, back to the start. Test marks, the camera mode, and the body look never
 * change, so a reset never shows Royalty's face by surprise.
 */
internal object FolderReset {
    /** True when [folder] or a folder inside it has a setting that Reset puts back. */
    fun hasSettings(folder: PanelFolder): Boolean =
        folder.items.any { resettable(it) } || folder.folders.any { hasSettings(it) }

    /** Puts every setting in [folder] and its subfolders back to the start. */
    fun reset(store: ThemeStore, folder: PanelFolder) {
        val numbers = ArrayList<String>()
        val toggles = ArrayList<String>()
        val texts = ArrayList<String>()
        val parts = ArrayList<ColorPart>()
        fun walk(at: PanelFolder) {
            for (item in at.items) {
                if (!resettable(item)) continue
                when (item) {
                    is NumberItem -> numbers += item.key
                    is ToggleItem -> toggles += item.key
                    is ChoiceItem -> texts += item.key
                    is TextItem -> texts += item.key
                    is ColorItem -> parts += item.part
                    else -> Unit
                }
            }
            at.folders.forEach { walk(it) }
        }
        walk(folder)
        store.clear(numbers, toggles, texts)
        parts.forEach { store.resetPart(it) }
    }

    private fun resettable(item: PanelItem): Boolean = when (item) {
        is NumberItem -> true
        is ToggleItem -> !item.isTest
        is ChoiceItem -> kept(item.key).not()
        is TextItem -> kept(item.key).not()
        is ColorItem -> true
        else -> false
    }

    private fun kept(key: String): Boolean =
        key == FxKeys.CAMERA_MODE || key == FxKeys.BODY_MODE || key.startsWith(PanelPrefs.TEST_PREFIX)
}
