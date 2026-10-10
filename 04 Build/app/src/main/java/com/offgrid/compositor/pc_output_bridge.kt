package com.offgrid.compositor

import android.app.Activity
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.live.LiveService
import com.offgrid.live.PcOutputConfig
import com.offgrid.live.PcPublisher

internal class PcOutputBridge(
    private val host: LiveHost,
    private val activity: () -> Activity?,
    private val remoteStillRunning: () -> Boolean,
    private val onStatus: (String) -> Unit,
) : AutoCloseable {
    private var publisher: PcPublisher? = null
    private var runningKey: String? = null

    fun startFromSettings() {
        val config = try {
            PcOutputConfig.create(
                host.readSetting(FxKeys.LIVE_PC_PROTOCOL) ?: "srt",
                host.readSetting(FxKeys.LIVE_PC_HOST).orEmpty(),
                host.readSetting(FxKeys.LIVE_PC_PORT) ?: PcOutputConfig.DEFAULT_PORT.toString(),
                host.readSetting(FxKeys.LIVE_PC_RES) ?: "720p",
                host.readSetting(FxKeys.LIVE_PC_BITRATE) ?: PcOutputConfig.DEFAULT_BITRATE.toString(),
            )
        } catch (error: IllegalArgumentException) {
            onStatus(error.message ?: "Check the PC output settings.")
            return
        }
        val key = config.url + ":" + config.width + ":" + config.bitrate
        if (publisher != null && runningKey == key) return
        stopPublisher()
        val owner = activity()
        if (owner == null) {
            onStatus("Open OFFGRID before sending to a PC.")
            return
        }
        try {
            LiveService.startStreaming(owner, camera = true, microphone = false)
        } catch (error: SecurityException) {
            onStatus("Camera streaming permission is not available.")
            return
        }
        runningKey = key
        publisher = PcPublisher(config, host, onStatus).also { it.start() }
        if (host.readSetting(FxKeys.LIVE_PC_ON) != "true") {
            host.writeSetting(FxKeys.LIVE_PC_ON, "true")
        }
    }

    fun stop() {
        val wasRunning = publisher != null
        stopPublisher()
        if (wasRunning && !remoteStillRunning()) LiveService.stopStreaming(host.applicationContext())
    }

    override fun close() {
        stopPublisher()
    }

    private fun stopPublisher() {
        publisher?.close()
        publisher = null
        runningKey = null
    }
}
