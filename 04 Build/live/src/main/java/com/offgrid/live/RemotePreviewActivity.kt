package com.offgrid.live

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class RemotePreviewActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var pairing: PairingWorkflow
    private lateinit var discovery: RemoteServiceDiscovery
    private lateinit var status: TextView
    private lateinit var lockState: TextView
    private lateinit var cameraStatus: TextView
    private lateinit var surfaceView: SurfaceView
    private lateinit var typedAddress: EditText
    private var pairingKey: String? = null
    private var previewSurface: Surface? = null
    private var link: RemoteLink? = null
    @Volatile
    private var decoder: RemotePreviewDecoder? = null
    private val cameraSettings = ConcurrentHashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pairing = PairingWorkflow(this)
        discovery = RemoteServiceDiscovery(this) { message -> runOnUiThread { status.text = message } }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        root.addView(label("OFFGRID / REMOTE", 20, true))
        status = label("Loading the sealed pairing key.", 13)
        root.addView(status, marginTop(8))
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        root.addView(surfaceView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(240),
        ).apply { topMargin = dp(16) })
        root.addView(button("Find camera phone") { findCamera() }, marginTop(8))
        root.addView(label("If searching fails, type the address shown on the camera phone's Pairing screen.", 12), marginTop(12))
        typedAddress = EditText(this).apply {
            hint = "Camera phone address, like 192.168.1.5:${CameraRemoteServer.DEFAULT_PORT}"
            setTextColor(WHITE)
            setHintTextColor(DIM)
            setBackgroundColor(GLASS)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            maxLines = 1
            minHeight = dp(48)
            contentDescription = "Camera phone address"
        }
        root.addView(typedAddress, marginTop(8))
        root.addView(button("Connect to typed address") { connectTyped() }, marginTop(8))
        addBroadcastLockStrip(root)
        root.addView(button("Auto Blackout Gate on / off") { toggle("fx.live.curtain.on", true) }, marginTop(8))
        root.addView(button("Idle Color: Black") { sendSetting("fx.live.curtain.mode", "black") }, marginTop(8))
        root.addView(button("Idle Color: White") { sendSetting("fx.live.curtain.mode", "white") }, marginTop(8))
        root.addView(button("Camera on / off") { toggle("fx.cam.on", true) }, marginTop(8))
        root.addView(button("Mic on / off") { toggle("fx.mic.on", false) }, marginTop(8))
        for ((key, title) in EFFECT_TOGGLES) {
            root.addView(button("Effect: $title") { toggle(key, false) }, marginTop(8))
        }
        for (mode in CAMERA_MODES) {
            root.addView(button("Camera mode: $mode") {
                sendSetting("fx.camera.mode", mode)
            }, marginTop(8))
        }
        for (action in RemoteMessage.FIRE_ACTIONS - RemoteMessage.LOCK_ACTIONS) {
            root.addView(button("Fire $action") {
                link?.sendControl(RemoteMessage("", "fire", action))
            }, marginTop(8))
        }
        addArDeckStrip(root)
        pairing.loadKey(
            onLoaded = { key, fingerprint ->
                pairingKey = key
                status.text = "Pairing fingerprint: $fingerprint. Find the camera phone."
            },
            onFailure = { status.text = it },
        )
    }

    private fun findCamera() {
        val key = pairingKey
        if (key == null) {
            status.text = "Make or paste a pairing key first."
            return
        }
        closeLink()
        status.text = "Searching this Wi-Fi network. If nothing is found, type the camera phone's address below."
        discovery.discover { address -> connect(address, key) }
    }

    /** Phase 0.4: the always-works path when Wi-Fi discovery is blocked or slow. */
    private fun connectTyped() {
        val key = pairingKey
        if (key == null) {
            status.text = "Make or paste a pairing key first."
            return
        }
        val address = try {
            ManualAddress.parse(typedAddress.text?.toString().orEmpty())
        } catch (error: IllegalArgumentException) {
            status.text = error.message ?: "Type the camera phone's address."
            return
        }
        closeLink()
        connect(address, key, found = false)
    }

    private fun connect(address: InetSocketAddress, key: String, found: Boolean = true) {
        discovery.stopDiscovery()
        val where = ManualAddress.describe(address.address, address.port)
        runOnUiThread {
            status.text = if (found) "Found camera phone at $where. Connecting securely."
            else "Connecting securely to $where."
        }
        val remote = RemoteLink(
            address = address,
            pairingKey = key,
            onState = { message -> runOnUiThread { status.text = message } },
            onPacket = { packet ->
                when (packet) {
                    is RemotePacket.Video -> decoder?.queue(packet)
                    is RemotePacket.Control -> if (packet.message.type == "setting") {
                        cameraSettings[packet.message.key] = packet.message.value
                        when (packet.message.key) {
                            RemoteMessage.LOCK_STATE_KEY -> runOnUiThread { showLockState(packet.message.value) }
                            RemoteMessage.STATUS_LINE_KEY -> runOnUiThread { cameraStatus.text = "S25: ${packet.message.value}" }
                        }
                    }
                }
            },
        )
        link = remote
        remote.start()
        startDecoderIfReady()
    }

    private fun startDecoderIfReady() {
        val surface = previewSurface ?: return
        if (decoder != null) return
        decoder = RemotePreviewDecoder(surface) { message ->
            runOnUiThread { status.text = message }
        }.also(RemotePreviewDecoder::start)
    }

    private fun toggle(key: String, default: Boolean) {
        val current = cameraSettings[key]?.toBooleanStrictOrNull() ?: default
        val next = (!current).toString()
        sendSetting(key, next)
        status.text = if (key == "fx.cam.on") "Camera request sent: $next" else "Mic request sent: $next"
    }

    private fun sendSetting(key: String, value: String) {
        cameraSettings[key] = value
        link?.sendControl(RemoteMessage(key, "setting", value))
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        previewSurface = holder.surface
        startDecoderIfReady()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        previewSurface = null
        decoder?.close()
        decoder = null
    }

    override fun onDestroy() {
        closeLink()
        discovery.stopDiscovery()
        pairing.close()
        super.onDestroy()
    }

    private fun closeLink() {
        decoder?.close()
        decoder = null
        link?.close()
        link = null
    }

    private fun label(text: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(WHITE)
        typeface = android.graphics.Typeface.create(
            "monospace",
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
        )
        gravity = Gravity.START
    }

    private fun button(text: String, action: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(WHITE)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), 0, dp(16), 0)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xff080808.toInt())
            setStroke(dp(1), UV)
        }
        minHeight = dp(48)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    /**
     * Phase 1.3: the Broadcast Lock from Phone 2. Lock and Arm are one tap. Unlock is a one
     * second hold, so a brush of the thumb cannot put the S25's panel on air. The camera phone
     * refuses lock_off while a share is live (A4) and says so on this screen.
     */
    private fun addBroadcastLockStrip(root: LinearLayout) {
        root.addView(label("Broadcast Lock", 16, true), marginTop(16))
        lockState = label("S25 lock: not connected", 13)
        root.addView(lockState, marginTop(4))
        cameraStatus = label("S25: waiting", 12)
        root.addView(cameraStatus, marginTop(4))
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val lockNow = button("Lock now") { fireLock(RemoteMessage.LOCK_ON) }
        val arm = button("Arm") { fireLock(RemoteMessage.LOCK_ARM) }
        val unlock = holdButton("Hold to unlock", HOLD_TO_UNLOCK_MS) { fireLock(RemoteMessage.LOCK_OFF) }
        for (view in listOf(lockNow, arm, unlock)) {
            strip.addView(view, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        }
        root.addView(strip, marginTop(8))
    }

    private fun fireLock(action: String) {
        val remote = link
        if (remote == null) {
            status.text = "Connect to the camera phone first."
            return
        }
        remote.sendControl(RemoteMessage("", "fire", action))
        status.text = when (action) {
            RemoteMessage.LOCK_OFF -> "Unlock sent. The S25 refuses it while a share is live."
            RemoteMessage.LOCK_ARM -> "Arm sent."
            else -> "Lock sent."
        }
    }

    private fun showLockState(word: String) {
        lockState.text = when (word) {
            "live" -> "S25 lock: LIVE. A share is on. Unlock is refused."
            "locked" -> "S25 lock: locked"
            "armed" -> "S25 lock: armed"
            else -> "S25 lock: unlocked"
        }
    }

    /** A button that fires only after [holdMs] of steady pressure. Lifting early does nothing. */
    private fun holdButton(text: String, holdMs: Long, action: () -> Unit) = button(text) {}.apply {
        var fired = false
        val fire = Runnable {
            fired = true
            action()
        }
        setOnClickListener(null)
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    fired = false
                    view.isPressed = true
                    view.postDelayed(fire, holdMs)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    view.removeCallbacks(fire)
                    if (!fired && event.actionMasked == android.view.MotionEvent.ACTION_UP) {
                        status.text = "Hold Unlock for one second."
                    }
                }
            }
            true
        }
    }

    /** Minimal Phone 2 clip strip. The camera phone owns the files; these only send commands. */
    private fun addArDeckStrip(root: LinearLayout) {
        root.addView(label("AR Video Deck", 16, true), marginTop(16))
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((title, action) in AR_THUMBS) {
            val thumb = button(title) {
                link?.sendControl(RemoteMessage("", "fire", action))
            }
            thumb.contentDescription = "AR thumbnail $title"
            strip.addView(thumb, LinearLayout.LayoutParams(dp(88), dp(48)).apply { marginEnd = dp(8) })
        }
        root.addView(strip, marginTop(8))
    }

    private fun marginTop(value: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(value) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val WHITE = 0xfff4f4f4.toInt()
        const val DIM = 0xff777777.toInt()
        const val GLASS = 0xff080808.toInt()
        const val UV = 0xff9b5cff.toInt()
        const val HOLD_TO_UNLOCK_MS = 1_000L
        val EFFECT_TOGGLES = listOf(
            "fx.coaster.on" to "Roller Coaster",
            "fx.heroes.on" to "Flying Heroes",
            "fx.portal.on" to "Portal",
            "fx.clouds.on" to "Clouds",
            "fx.beach.on" to "Beach",
            "fx.emoji.on" to "Emoji Rain",
            "fx.radar.on" to "Radar Sweep",
            "fx.illusion.on" to "Optical Illusion",
            "fx.border.on" to "Glow Border",
        )
        val CAMERA_MODES = listOf("live", "blackout", "cloak", "status", "ghost")
        val AR_THUMBS = listOf(
            "Play" to "ar_play",
            "Pause" to "ar_pause",
            "Next" to "ar_next",
        )
    }
}
