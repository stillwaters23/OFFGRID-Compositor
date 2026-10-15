package com.offgrid.live

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class RemotePreviewActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var pairing: PairingWorkflow
    private lateinit var discovery: RemoteServiceDiscovery
    private lateinit var status: TextView
    private lateinit var surfaceView: SurfaceView
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
        for (action in FIRE_ACTIONS) {
            root.addView(button("Fire $action") {
                link?.sendControl(RemoteMessage("", "fire", action))
            }, marginTop(8))
        }
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
        status.text = "Searching this Wi-Fi network."
        discovery.discover { address -> connect(address, key) }
    }

    private fun connect(address: InetSocketAddress, key: String) {
        discovery.stopDiscovery()
        runOnUiThread { status.text = "Found camera phone. Connecting securely." }
        val remote = RemoteLink(
            address = address,
            pairingKey = key,
            onState = { message -> runOnUiThread { status.text = message } },
            onPacket = { packet ->
                when (packet) {
                    is RemotePacket.Video -> decoder?.queue(packet)
                    is RemotePacket.Control -> if (packet.message.type == "setting") {
                        cameraSettings[packet.message.key] = packet.message.value
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

    private fun marginTop(value: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(value) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val WHITE = 0xfff4f4f4.toInt()
        const val UV = 0xff9b5cff.toInt()
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
        val FIRE_ACTIONS = listOf("shatter", "freeze", "quiet", "fireworks", "red_sweep", "laser_writing")
    }
}
