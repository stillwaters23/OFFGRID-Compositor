package com.offgrid.compositor

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.offgrid.compositor.terminal.TerminalPanel
import com.offgrid.live.CameraRemoteServer
import com.offgrid.live.LiveHostRegistry
import com.offgrid.live.LiveOverlayController
import com.offgrid.live.LiveService
import com.offgrid.live.LocalAddressPolicy
import com.offgrid.live.OffgridHost
import com.offgrid.live.PairingActivity
import com.offgrid.live.PairingWorkflow
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.live.LiveActions
import com.offgrid.live.RemoteMessage
import com.offgrid.live.RemotePacket
import com.offgrid.live.RemotePreviewActivity
import com.offgrid.live.RemotePreviewEncoder
import com.offgrid.live.RemoteServiceDiscovery
import java.util.concurrent.Executors

internal class LiveIntegration(
    activity: Activity,
    effects: EffectsController,
    processor: () -> CompositorProcessor?,
    controlBar: View,
    panel: TerminalPanel,
) : DefaultLifecycleObserver {
    private val appContext = activity.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val pairing = PairingWorkflow(appContext)
    private var effects: EffectsController? = effects
    private var controlBar: View? = controlBar
    private var panel: TerminalPanel? = panel
    private val host = LiveHost(appContext).also {
        it.attach(effects, processor)
        it.onRoleChanged = ::changeRole
        it.onOverlayChanged = ::overlayChanged
    }
    private val discovery = RemoteServiceDiscovery(appContext, ::showStatus)
    private var remoteServer: CameraRemoteServer? = null
    private var remoteEncoder: RemotePreviewEncoder? = null
    private var streamServiceStarted = false
    private val pc = PcOutputBridge(
        host = host,
        activity = { this.activity },
        remoteStillRunning = { remoteServer != null || remoteEncoder != null || streamServiceStarted },
        onStatus = ::showStatus,
    )
    private var cleanOutput = false
    private var activity: Activity? = activity
    private var activeRole = "off"

    init {
        host.onPcOutputChanged = { enabled -> if (enabled) pc.startFromSettings() else pc.stop() }
        host.onRestoreControls = ::restoreCleanOutput
        LiveHostRegistry.host = host
        effects.onLiveAction = ::onAction
        effects.onLiveSettings = ::syncSettings
        syncSettings()
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(this)
    }

    private fun onResume() {
        if (cleanOutput) restoreCleanOutput()
        if (host.readSetting(FxKeys.LIVE_PC_ON) == "true") pc.startFromSettings()
        if (host.readSetting(LIVE_OVERLAY_KEY) == "true") {
            LiveOverlayController.openOverlay(appContext)
        }
    }

    private fun onPause() {
        pc.stop()
        if (host.readSetting(LIVE_OVERLAY_KEY) == "true") {
            LiveOverlayController.openOverlay(appContext)
        } else if (remoteServer == null && remoteEncoder == null && !streamServiceStarted) {
            appContext.stopService(Intent(appContext, LiveService::class.java))
        }
    }

    fun close() {
        effects?.onLiveAction = null
        effects?.onLiveSettings = null
        effects = null
        host.detach()
        activity = null
        controlBar = null
        panel = null
        pairing.close()
        pc.close()
        worker.shutdownNow()
    }

    override fun onResume(owner: LifecycleOwner) = onResume()

    override fun onPause(owner: LifecycleOwner) = onPause()

    override fun onDestroy(owner: LifecycleOwner) {
        close()
        owner.lifecycle.removeObserver(this)
    }

    private fun onAction(action: String) {
        when (action) {
            LiveActions.OPEN_OVERLAY -> host.writeSetting(LIVE_OVERLAY_KEY, "true")
            LiveActions.CLEAN_OUTPUT -> {
                controlBar?.visibility = View.GONE
                panel?.setHiddenForPlayback(true)
                cleanOutput = true
                LiveOverlayController.hideOverlay(appContext)
            }
            LiveActions.OPEN_PAIRING -> launch(PairingActivity::class.java)
            LiveActions.OPEN_PREVIEW -> launch(RemotePreviewActivity::class.java)
            LiveActions.SEND_TO_PC -> pc.startFromSettings()
        }
    }

    private fun restoreCleanOutput() {
        cleanOutput = false
        controlBar?.visibility = View.VISIBLE
        panel?.setHiddenForPlayback(false)
    }

    private fun launch(destination: Class<out Activity>) {
        val owner = activity ?: return
        owner.startActivity(Intent(owner, destination))
    }

    private fun overlayChanged(enabled: Boolean) {
        if (enabled) {
            val owner = activity
            if (owner != null) LiveOverlayController.askForPermission(owner)
            else showStatus("Open OFFGRID to allow display over other apps.")
        } else {
            if (remoteServer == null && remoteEncoder == null && !streamServiceStarted) {
                appContext.stopService(Intent(appContext, LiveService::class.java))
            }
        }
    }

    private fun changeRole(value: String) {
        if (activeRole == value) return
        activeRole = value
        when (value) {
            "camera" -> startCameraRole()
            "remote" -> {
                stopCameraRole()
                launch(RemotePreviewActivity::class.java)
            }
            else -> stopCameraRole()
        }
    }

    private fun syncSettings() {
        syncRole()
        if (host.readSetting(FxKeys.LIVE_PC_ON) == "true") pc.startFromSettings() else pc.stop()
    }

    private fun syncRole() {
        changeRole(host.readSetting("fx.remote.role") ?: "off")
    }

    private fun startCameraRole() {
        stopCameraRole()
        val owner = activity
        if (owner == null) {
            showStatus("Open OFFGRID before starting the camera role.")
            return
        }
        try {
            LiveService.startStreaming(owner, camera = true, microphone = false)
            streamServiceStarted = true
        } catch (error: SecurityException) {
            showStatus("Camera streaming permission is not available.")
            return
        }
        pairing.loadKey(
            onLoaded = { key, _ ->
                worker.execute {
                    try {
                        val server = CameraRemoteServer(
                            bindAddress = LocalAddressPolicy.siteLocalAddress(),
                            pairingKey = { key },
                            onSession = { session ->
                                val encoder = RemotePreviewEncoder(
                                    onEncodedFrame = { bytes, timestamp, flags ->
                                        session.sendVideo(bytes, timestamp, flags)
                                    },
                                    onFailure = ::showStatus,
                                )
                                remoteEncoder = encoder
                                encoder.start(host) { started ->
                                    if (started) showStatus("Camera phone is ready on local Wi-Fi.")
                                }
                                try {
                                    for ((key, default) in CAMERA_SETTINGS) {
                                        val value = host.readSetting(key) ?: default
                                        session.sendControl(RemoteMessage(key, "setting", value))
                                    }
                                    while (!Thread.currentThread().isInterrupted) {
                                        when (val packet = session.receive()) {
                                            is RemotePacket.Control -> {
                                                val message = packet.message
                                                if (message.type == "fire") host.fire(message.value)
                                                else {
                                                    host.writeSetting(message.key, message.value)
                                                    session.sendControl(message)
                                                }
                                            }
                                            is RemotePacket.Video -> Unit
                                        }
                                    }
                                } finally {
                                    encoder.close()
                                    remoteEncoder = null
                                }
                            },
                            onFailure = ::showStatus,
                        )
                        remoteServer = server
                        server.start()
                        discovery.advertise(server.port)
                        showStatus("Camera phone is ready on local Wi-Fi.")
                    } catch (error: IllegalArgumentException) {
                        showStatus(error.message ?: "Could not start the local camera link.")
                    } catch (error: IllegalStateException) {
                        showStatus(error.message ?: "Could not find a private Wi-Fi address.")
                    } catch (error: java.io.IOException) {
                        showStatus("Could not open the local camera link.")
                    }
                }
            },
            onFailure = ::showStatus,
        )
    }

    private fun stopCameraRole() {
        discovery.stopAdvertising()
        remoteServer?.close()
        val hadServer = remoteServer != null
        remoteServer = null
        remoteEncoder?.close()
        remoteEncoder = null
        if (hadServer || streamServiceStarted) {
            streamServiceStarted = false
            LiveService.stopStreaming(appContext)
        }
    }

    private fun showStatus(message: String) {
        main.post {
            activity?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
        }
    }

    companion object {
        private const val LIVE_OVERLAY_KEY = "fx.live.overlay"
        private val CAMERA_SETTINGS = mapOf(
            "fx.cam.on" to "true",
            "fx.mic.on" to "false",
            "fx.camera.mode" to "live",
        )

        fun install(
            activity: Activity,
            effects: EffectsController,
            processor: () -> CompositorProcessor?,
            controlBar: View,
            panel: TerminalPanel,
        ) {
            LiveIntegration(activity, effects, processor, controlBar, panel)
        }
    }
}
