package com.offgrid.compositor

import com.offgrid.compositor.terminal.EffectFolders
import com.offgrid.compositor.terminal.PanelTree
import com.offgrid.compositor.terminal.RenderProbeViewerItem
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderProbePanelTest {
    @Test
    fun systemFolderHasDiagnosticsRenderProbe() {
        val diagnostics = EffectFolders.system().folders.single { it.id == "system.diagnostics" }
        val probe = diagnostics.folders.single { it.id == RenderProbeIds.VIEWER_FOLDER }
        assertTrue(probe.items.any { it is RenderProbeViewerItem })
    }

    @Test
    fun panelTreeOmitsSystemWithoutDebuggableContext() {
        val home = PanelTree.build(null)
        assertTrue(home.folders.none { it.id == "system" })
    }
}
