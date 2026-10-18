package com.offgrid.compositor

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.offgrid.compositor.terminal.TerminalPanel
import com.offgrid.live.CameraRemoteServer
import com.offgrid.live.LiveHostRegistry
import com.offgrid.live.LiveOverlayController
import com.offgrid.live.LiveService
import com.offgrid.live.LocalAddressPolicy
import com.offgrid.live.ManualAddress
import com.offgrid.live.OffgridHost
import com.offgrid.live.PairingActivity
import com.offgrid.live.PairingWorkflow
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.terminal.PanelAction
import com.offgrid.live.LiveActions
import com.offgrid.live.RemoteMessage
import com.offgrid.live.RemotePacket
import com.offgrid.live.RemotePreviewActivity
import com.offgrid.live.RemotePreviewEncoder
import com.offgrid.live.RemoteServiceDiscovery
import com.offgrid.live.RemoteSession
import java.util.concurrent.Executors

internal class LiveIntegration(
    activity: Activity,
    effects: EffectsController,
    processor: () -> CompositorProcessor?,
    private val lock: BroadcastLockStore,
    panel: TerminalPanel,
) : DefaultLifecycleObserver {
    private val appContext = activity.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    /** Phase 1.3: status and lock words for Phone 2 go out on their own thread, never the UI's. */
    private val phone2 = Executors.newSingleThreadExecutor()
    private val pairing = PairingWorkflow(appContext)
    private var effects: EffectsController? = effects
    private var panel: TerminalPanel? = panel
    @Volatile private var remoteSession: RemoteSession? = null
    private var resumed = false
    private var toast: Toast? = null
    private val lockListener: (LockState) -> Unit = { state -> onLockChanged(state) }
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
    private var activity: Activity? = activity
    private var activeRole = "off"
    private val pickArVideo = (activity as? ComponentActivity)?.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        onArClipPicked(uri)
    }

    init {
        host.onPcOutputChanged = { enabled -> if (enabled) pc.startFromSettings() else pc.stop() }
        host.onRestoreControls = ::restoreCleanOutput
        host.onLockCommand = ::onLockCommand
        LiveHostRegistry.host = host
        effects.onLiveAction = ::onAction
        effects.onLiveSettings = ::syncSettings
        lock.addListener(lockListener)
        syncSettings()
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(this)
    }

    private fun onResume() {
        resumed = true
        if (host.readSetting(FxKeys.LIVE_PC_ON) == "true") pc.startFromSettings()
        showOverlayIfWanted()
    }

    private fun onPause() {
        resumed = false
        cancelToast()
        pc.stop()
        if (host.readSetting(LIVE_OVERLAY_KEY) == "true") {
            showOverlayIfWanted()
        } else if (remoteServer == null && remoteEncoder == null && !streamServiceStarted) {
            appContext.stopService(Intent(appContext, LiveService::class.java))
        }
    }

    /** The overlay comes up only while the stage is not locked. Locked, its window stays off. */
    private fun showOverlayIfWanted() {
        if (lock.locked) return
        if (host.readSetting(LIVE_OVERLAY_KEY) == "true") LiveOverlayController.openOverlay(appContext)
    }

    /**
     * Phase 1.3: every lock change reaches the overlay and Phone 2. Locked removes the overlay
     * window (its secure-window flag would black out the broadcast). Unlocked lets it back when wanted.
     */
    private fun onLockChanged(state: LockState) {
        if (state.locked) {
            cancelToast()
            LiveOverlayController.lockOverlay(appContext)
        } else {
            LiveOverlayController.unlockOverlay(appContext)
            showOverlayIfWanted()
        }
        tellPhone2(RemoteMessage.LOCK_STATE_KEY, state.word)
    }

    /** Phone 2 sent a lock command over the paired link (fire lock_on, lock_off, or arm). */
    private fun onLockCommand(action: String) {
        when (action) {
            RemoteMessage.LOCK_ON -> {
                lock.lock(LockSource.REMOTE)
                tellPhone2Status("Locked from Phone 2.")
            }
            RemoteMessage.LOCK_ARM -> {
                lock.arm()
                tellPhone2Status("Armed from Phone 2. The stage stays locked after the share ends.")
            }
            RemoteMessage.LOCK_OFF -> when (lock.unlock()) {
                UnlockResult.UNLOCKED -> tellPhone2Status("Unlocked from Phone 2.")
                UnlockResult.REFUSED_SHARE_VISIBLE -> tellPhone2Status("Unlock refused. A screen share is live on the S25.")
                UnlockResult.NOTHING_TO_UNLOCK -> tellPhone2Status("The S25 was not locked.")
            }
        }
    }

    /** Sends one display-only setting to Phone 2, off the UI thread. Silent when no phone is linked. */
    fun tellPhone2(key: String, value: String) {
        val session = remoteSession ?: return
        val safe = value.take(MAX_PHONE2_TEXT)
        try {
            phone2.execute {
                runCatching { session.sendControl(RemoteMessage(key, "setting", safe)) }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            // close() shut the sender down; a late status line has nowhere to go.
        }
    }

    /** The status line words, for Phone 2's screen. The panel on the S25 types the same words. */
    fun tellPhone2Status(message: String) {
        panel?.announce(message)
        tellPhone2(RemoteMessage.STATUS_LINE_KEY, message)
    }

    fun close() {
        effects?.onLiveAction = null
        effects?.onLiveSettings = null
        effects = null
        host.detach()
        host.onLockCommand = null
        lock.removeListener(lockListener)
        activity = null
        panel = null
        pairing.close()
        pc.close()
        worker.shutdownNow()
        phone2.shutdownNow()
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
            // Phase 1.3: Clean screen share is the Broadcast Lock now (V2 section 7.3, "absorbs CLEAN_OUTPUT").
            LiveActions.CLEAN_OUTPUT -> lock.lock(LockSource.MANUAL)
            LiveActions.LOCK_ARM -> if (lock.state.armed) {
                when (lock.unlock()) {
                    UnlockResult.REFUSED_SHARE_VISIBLE -> tellPhone2Status("A screen share is live. The lock stays on.")
                    else -> tellPhone2Status("The lock is disarmed.")
                }
            } else {
                lock.arm()
            }
            LiveActions.OPEN_PAIRING -> launch(PairingActivity::class.java)
            LiveActions.COPY_PAIRING_KEY -> copyPairingKey()
            LiveActions.OPEN_PREVIEW -> launch(RemotePreviewActivity::class.java)
            LiveActions.SEND_TO_PC -> pc.startFromSettings()
            PanelAction.AR_PICK_CLIP.name -> pickArVideo?.launch(ArDeckCatalog.pickIntent())
                ?: showStatus("Open OFFGRID to pick a video.")
        }
    }

    private fun onArClipPicked(uri: android.net.Uri) {
        val owner = activity ?: return
        runCatching {
            owner.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = runCatching {
            owner.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment ?: "clip"
        effects?.addArClip(name, uri.toString())
        showStatus("AR clip ready.")
    }

    /** The overlay's expand control, kept for the old Clean screen share path: it asks to unlock. */
    private fun restoreCleanOutput() {
        if (lock.unlock() == UnlockResult.REFUSED_SHARE_VISIBLE) {
            tellPhone2Status("Unlock refused. A screen share is live.")
        }
    }

    /** Copies the pairing key and stays on Live and Remote. Works for a PC or an Android phone. */
    private fun copyPairingKey() {
        pairing.loadKey(
            onLoaded = { _, _ ->
                try {
                    pairing.copyCurrentKey()
                    tellPhone2Status("Pairing key copied. Paste it on a PC or an Android phone.")
                } catch (error: IllegalStateException) {
                    tellPhone2Status(error.message ?: "Make a pairing key first.")
                }
            },
            onFailure = {
                pairing.makeKey(
                    onCreated = { _, _ ->
                        try {
                            pairing.copyCurrentKey()
                            tellPhone2Status("Pairing key made and copied. Paste it on a PC or an Android phone.")
                        } catch (error: IllegalStateException) {
                            tellPhone2Status(error.message ?: "Could not copy the pairing key.")
                        }
                    },
                    onFailure = { reason -> tellPhone2Status(reason) },
                )
            },
        )
    }

    /** No OFFGRID screen opens over the stage while it is locked, whoever asked (panel or Phone 2). */
    private fun launch(destination: Class<out Activity>) {
        val owner = activity ?: return
        if (lock.locked) {
            tellPhone2Status("Unlock the S25 first. Nothing opens over the stage while it is locked.")
            return
        }
        owner.startActivity(Intent(owner, destination))
    }

    private fun overlayChanged(enabled: Boolean) {
        if (enabled) {
            if (lock.locked) {
                // Phone 2 can write this setting at any time. The overlay comes back on unlock.
                tellPhone2Status("The overlay comes back when the lock is off.")
                return
            }
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
                        val bindAddress = LocalAddressPolicy.siteLocalAddress()
                        val server = CameraRemoteServer(
                            bindAddress = bindAddress,
                            pairingKey = { key },
                            onSession = { session ->
                                val encoder = RemotePreviewEncoder(
                                    onEncodedFrame = { bytes, timestamp, flags ->
                                        session.sendVideo(bytes, timestamp, flags)
                                    },
                                    onFailure = ::showStatus,
                                )
                                remoteEncoder = encoder
                                remoteSession = session
                                encoder.start(host) { started ->
                                    if (started) showStatus("Camera phone is ready on local Wi-Fi.")
                                }
                                try {
                                    for ((key, default) in CAMERA_SETTINGS) {
                                        val value = host.readSetting(key) ?: default
                                        session.sendControl(RemoteMessage(key, "setting", value))
                                    }
                                    // Phase 1.3: Phone 2 shows the lock word from the first moment.
                                    session.sendControl(RemoteMessage(RemoteMessage.LOCK_STATE_KEY, "setting", lock.state.word))
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
                                    remoteSession = null
                                    encoder.close()
                                    remoteEncoder = null
                                }
                            },
                            onFailure = ::showStatus,
                        )
                        remoteServer = server
                        server.start()
                        discovery.advertise(server.port)
                        val where = ManualAddress.describe(bindAddress, server.port)
                        showStatus("Camera phone is ready on local Wi-Fi at $where. Type it on the remote phone if searching fails.")
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

    /**
     * Link status. The panel types it and Phone 2 shows it. A toast only while OFFGRID is in
     * front and unlocked: a toast is system chrome drawn over whatever app is on screen, Bigo
     * included, and it can carry the LAN address. A lock or a pause takes it down.
     */
    private fun showStatus(message: String) {
        main.post {
            tellPhone2Status(message)
            val owner = activity
            if (owner != null && resumed && !lock.locked) {
                cancelToast()
                toast = Toast.makeText(owner, message, Toast.LENGTH_LONG).also { it.show() }
            }
        }
    }

    private fun cancelToast() {
        toast?.cancel()
        toast = null
    }

    companion object {
        private const val LIVE_OVERLAY_KEY = "fx.live.overlay"
        private const val MAX_PHONE2_TEXT = 200
        private val CAMERA_SETTINGS = mapOf(
            "fx.cam.on" to "true",
            "fx.mic.on" to "false",
            "fx.camera.mode" to "live",
        )

        fun install(
            activity: Activity,
            effects: EffectsController,
            processor: () -> CompositorProcessor?,
            lock: BroadcastLockStore,
            panel: TerminalPanel,
        ): LiveIntegration = LiveIntegration(activity, effects, processor, lock, panel)
    }
}
