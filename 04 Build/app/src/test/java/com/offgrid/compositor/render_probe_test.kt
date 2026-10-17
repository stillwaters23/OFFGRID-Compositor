package com.offgrid.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Phase 0.5: the render probe logs exactly three frames per bind, and nothing on a release build. */
class RenderProbeTest {
    private val lines = ArrayList<String>()
    private val mainTasks = ArrayList<Runnable>()

    private fun probe(enabled: Boolean = true) = RenderProbe(
        enabled = enabled,
        sink = { lines.add(it) },
        onMain = { mainTasks.add(it) },
    ).also { it.previewSampler = { "previewView=1440x3120 scaleX=1.0 scaleY=1.0" } }

    @Test
    fun logsExactlyThreeFramesPerBind() {
        val probe = probe()
        probe.onBind(1, 1440, 3120, "6:13", 0)
        repeat(10) { probe.onPreviewFrame(590, 1280, 590, 1280) }
        val frames = lines.filter { it.contains("frame ") }
        assertEquals(3, frames.size)
        assertEquals(3, mainTasks.size)
        assertTrue(frames[0].contains("bind 1 frame 1/3"))
        assertTrue(frames[2].contains("bind 1 frame 3/3 output=590x1280 stage=590x1280"))

        probe.onBind(2, 1440, 3120, "6:13", 0)
        repeat(10) { probe.onPreviewFrame(590, 1280, 590, 1280) }
        val second = lines.filter { it.contains("bind 2 frame ") }
        assertEquals(3, second.size)
        assertEquals(6, mainTasks.size)
    }

    @Test
    fun mainThreadSampleCarriesTheTextureViewTransform() {
        val probe = probe()
        probe.onBind(7, 1440, 3120, "6:13", 0)
        probe.onPreviewFrame(590, 1280, 590, 1280)
        mainTasks.forEach(Runnable::run)
        assertTrue(lines.any { it == "bind 7 frame 1/3 previewView=1440x3120 scaleX=1.0 scaleY=1.0" })
    }

    @Test
    fun stageLineRepeatsOnlyWhenTheSizeChanges() {
        val probe = probe()
        probe.onBind(1, 1440, 3120, "6:13", 0)
        probe.onStage(590, 1280, true, 590, 1280)
        probe.onStage(590, 1280, true, 590, 1280)
        probe.onStage(720, 1280, false, 720, 1280)
        assertEquals(2, lines.count { it.contains("stage=") })
        assertTrue(lines.any { it.contains("main=590x1280 previewMaster=true stage=590x1280") })
    }

    @Test
    fun everyNumberTheReviewAsksForIsLogged() {
        val probe = probe()
        probe.onBind(3, 1440, 3120, "6:13", 0)
        probe.onInput(1280, 720)
        probe.onOutput(590, 1280, 1, true)
        probe.onOutput(720, 1280, 2, false)
        probe.onStage(590, 1280, true, 590, 1280)
        probe.onClipSize(1920, 1080, 1f)
        assertTrue(lines.any { it.contains("preview=1440x3120 viewPort=6:13 rotation=0") })
        assertTrue(lines.any { it.contains("surfaceRequest=1280x720") })
        assertTrue(lines.any { it.contains("output=590x1280 targets=1 preview=true") })
        assertTrue(lines.any { it.contains("output=720x1280 targets=2 preview=false") })
        assertTrue(lines.any { it.contains("videoSize=1920x1080 pixelRatio=1.0") })
    }

    @Test
    fun ringFeedsBindCapturesForColdStartAndResume() {
        val dir = File.createTempFile("probe", null).apply { delete(); mkdirs() }
        val probe = RenderProbe(
            enabled = true,
            sink = { lines.add(it) },
            onMain = { mainTasks.add(it) },
            captureDir = dir,
            ringCapacity = 5,
        )
        probe.onBind(1, 1440, 3120, "6:13", 0)
        val cold = File(dir, RenderProbe.COLD_START_FILE)
        assertTrue(cold.exists())
        assertTrue(cold.readText().contains("bind 1 preview="))
        probe.onBind(2, 1440, 3120, "6:13", 0)
        val resume = File(dir, RenderProbe.RESUME_FILE)
        assertTrue(resume.exists())
        assertTrue(resume.readText().contains("bind 2 preview="))
        dir.deleteRecursively()
    }

    @Test
    fun releaseBuildLogsNothing() {
        val probe = probe(enabled = false)
        probe.onBind(1, 1440, 3120, "6:13", 0)
        probe.onInput(1280, 720)
        probe.onOutput(590, 1280, 1, true)
        probe.onStage(590, 1280, true, 590, 1280)
        probe.onClipSize(1920, 1080, 1f)
        repeat(5) { probe.onPreviewFrame(590, 1280, 590, 1280) }
        assertTrue(lines.isEmpty())
        assertTrue(mainTasks.isEmpty())
    }
}
