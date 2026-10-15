package com.offgrid.compositor

import android.content.Intent
import androidx.media3.common.Player
import com.offgrid.compositor.effects.EffectSnapshot
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.StagePlan
import com.offgrid.compositor.terminal.ActionItem
import com.offgrid.compositor.terminal.ChoiceItem
import com.offgrid.compositor.terminal.NumberItem
import com.offgrid.compositor.terminal.PanelAction
import com.offgrid.compositor.terminal.PanelTree
import com.offgrid.compositor.terminal.ToggleItem
import com.offgrid.live.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Catalog, stage mix, and the panel entry for the headless AR Video Deck. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArDeckTest {
    @Test
    fun catalogKeepsLocalClipsAndCycles() {
        val catalog = ArDeckCatalog()
        assertNull(catalog.next())
        assertNull(catalog.resolve("https://example.com/a.mp4"))
        val first = catalog.add("One", "content://clips/one")
        val second = catalog.add("Two", "content://clips/two")
        assertEquals("c1", first.id)
        assertEquals("c2", second.id)
        assertEquals(second, catalog.current())
        assertEquals("content://clips/two", catalog.resolve("c2"))
        assertEquals(first, catalog.next())
        assertEquals(second, catalog.next())
        catalog.restore("c2", "content://clips/two")
        assertEquals(2, catalog.clips().size)
    }

    @Test
    fun pickIntentIsTheDocumentPickerWithoutALibraryPermission() {
        val intent = ArDeckCatalog.pickIntent()
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertEquals("video/*", intent.type)
        assertTrue(intent.categories.contains(Intent.CATEGORY_OPENABLE))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
    }

    @Test
    fun loopUsesOneClipRepeat() {
        assertEquals(Player.REPEAT_MODE_ONE, ArDeckPlayer.repeatMode(true))
        assertEquals(Player.REPEAT_MODE_OFF, ArDeckPlayer.repeatMode(false))
    }

    @Test
    fun stagePlanMixesTheDeckOverAPlainCamera() {
        val off = StagePlan().update(1f, EffectSnapshot.EMPTY, emptyList(), null, -100f, FxKeys.MODE_LIVE)
        assertFalse(off.arOn)
        assertTrue(off.arLoop)
        assertTrue(off.plain)

        val on = StagePlan().update(
            1f,
            EffectSnapshot(
                numbers = mapOf(FxKeys.AR_OPACITY to 40),
                toggles = mapOf(FxKeys.AR_ON to true, FxKeys.AR_LOOP to false),
                choices = emptyMap(),
                texts = mapOf(FxKeys.AR_CLIP to "c1"),
            ),
            emptyList(),
            null,
            -100f,
            FxKeys.MODE_LIVE,
        )
        assertTrue(on.arOn)
        assertFalse(on.arLoop)
        assertEquals(0.4f, on.arOpacity, 0.001f)
        assertEquals("c1", on.arClip)
        assertFalse(on.plain)
        assertFalse(on.arFit)
    }

    @Test
    fun clipShapeDefaultsToFillAndCanBeFit() {
        val folder = PanelTree.build().folders.single { it.id == "ar_video_deck" }
        val shape = folder.items.filterIsInstance<ChoiceItem>().single { it.key == FxKeys.AR_FIT }
        assertEquals(FxKeys.AR_FIT_FILL, shape.default)
        assertEquals(listOf(FxKeys.AR_FIT_FILL, FxKeys.AR_FIT_FIT), shape.choices.map { it.value })

        val fit = StagePlan().update(
            1f,
            EffectSnapshot(
                numbers = emptyMap(),
                toggles = mapOf(FxKeys.AR_ON to true),
                choices = mapOf(FxKeys.AR_FIT to FxKeys.AR_FIT_FIT),
                texts = emptyMap(),
            ),
            emptyList(),
            null,
            -100f,
            FxKeys.MODE_LIVE,
        )
        assertTrue(fit.arFit)
    }

    @Test
    fun panelFolderOffersSafPickLoopAndOpacity() {
        val folder = PanelTree.build().folders.single { it.id == "ar_video_deck" }
        assertTrue(folder.title.contains("AR Video Deck"))
        assertTrue(folder.items.filterIsInstance<ActionItem>().any { it.action == PanelAction.AR_PICK_CLIP })
        assertTrue(folder.items.filterIsInstance<ToggleItem>().single { it.key == FxKeys.AR_LOOP }.default)
        assertFalse(folder.items.filterIsInstance<ToggleItem>().single { it.key == FxKeys.AR_ON }.default)
        assertEquals(100, folder.items.filterIsInstance<NumberItem>().single { it.key == FxKeys.AR_OPACITY }.default)
        assertTrue(
            RemoteMessage.FIRE_ACTIONS.containsAll(
                listOf(ArDeckCatalog.PLAY, ArDeckCatalog.PAUSE, ArDeckCatalog.NEXT),
            ),
        )
    }
}
