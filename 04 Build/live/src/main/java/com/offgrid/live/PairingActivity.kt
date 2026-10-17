package com.offgrid.live

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.function.Consumer

class PairingActivity : Activity() {
    private lateinit var workflow: PairingWorkflow
    private lateinit var keyText: TextView
    private lateinit var status: TextView
    private lateinit var addressText: TextView
    private lateinit var pastedKey: EditText

    /**
     * The live consumer only. The platform can still deliver a state that was queued
     * before removeScreenRecordingCallback, and that stale report must not close a
     * later visit to this screen.
     */
    private var shareGuard: Consumer<Int>? = null
    private var closedForShare = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workflow = PairingWorkflow(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
        content.addView(label("OFFGRID / PAIRING", 20, true))
        content.addView(label("Use a private key only on phones you control.", 14))
        keyText = label("No key is shown.", 13)
        keyText.setTextIsSelectable(true)
        content.addView(keyText, marginTop(20))
        status = label("Pairing key stays sealed in Android Keystore.", 13)
        content.addView(status, marginTop(8))
        addressText = label("Looking up this phone's Wi-Fi address.", 13)
        addressText.setTextIsSelectable(true)
        content.addView(addressText, marginTop(16))
        content.addView(label("On the remote phone, type this address if searching does not find the camera phone.", 12), marginTop(4))
        showLocalAddress()
        pastedKey = EditText(this).apply {
            hint = "Paste the 100+ character key"
            setTextColor(WHITE)
            setHintTextColor(DIM)
            setBackgroundColor(GLASS)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            maxLines = 3
            minHeight = dp(80)
        }
        content.addView(pastedKey, marginTop(16))
        content.addView(button("Make pairing key") { makeKey() }, marginTop(12))
        content.addView(button("Copy key") {
            if (closedForShare) return@button
            try {
                workflow.copyCurrentKey()
                status.text = "Copied. Android clears this key after 60 seconds."
            } catch (error: IllegalStateException) {
                status.text = error.message ?: "Make a key first."
            }
        }, marginTop(8))
        content.addView(button("Paste pairing key") { acceptKey() }, marginTop(8))
        content.addView(button("Forget pairing") {
            workflow.forget(
                onForgotten = {
                    if (closedForShare) return@forget
                    keyText.text = "No key is shown."
                    status.text = "Pairing key forgotten."
                },
                onFailure = { if (!closedForShare) status.text = it },
            )
        }, marginTop(8))
        content.addView(button("New key") { makeKey() }, marginTop(8))
        workflow.loadKey(
            onLoaded = { _, fingerprint ->
                if (!closedForShare) status.text = "Saved key fingerprint: $fingerprint"
            },
            onFailure = { if (!closedForShare) status.text = it },
        )
    }

    override fun onStart() {
        super.onStart()
        registerShareGuard()
    }

    override fun onStop() {
        unregisterShareGuard()
        super.onStop()
    }

    /**
     * Android 15 reports whether this window is inside a screen recording or a share.
     * The permission is the one the app module already declares (`DETECT_SCREEN_RECORDING`);
     * it is normal level, so there is no prompt. Older phones have no callback: the guard
     * does nothing. A [SecurityException] is logged and Pairing stays up, the same way the
     * camera screen treats a refused callback.
     */
    private fun registerShareGuard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        unregisterShareGuard()
        val manager = windowManager
        val consumer = object : Consumer<Int> {
            override fun accept(state: Int) {
                if (shareGuard !== this) return
                if (PairingSharePolicy.shouldClose(Build.VERSION.SDK_INT, state)) closeForShare()
            }
        }
        try {
            val initial = manager.addScreenRecordingCallback(ContextCompat.getMainExecutor(this), consumer)
            shareGuard = consumer
            if (PairingSharePolicy.shouldClose(Build.VERSION.SDK_INT, initial)) closeForShare()
        } catch (error: SecurityException) {
            Log.w(TAG, "The screen recording callback was refused. The auto lock is off.", error)
        }
    }

    private fun unregisterShareGuard() {
        val consumer = shareGuard ?: return
        shareGuard = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching { windowManager.removeScreenRecordingCallback(consumer) }
        }
    }

    /** The key and the address must not be the last frame a share captures. Then this screen leaves. */
    private fun closeForShare() {
        if (closedForShare) return
        closedForShare = true
        if (::keyText.isInitialized) {
            keyText.text = ""
            keyText.setTextIsSelectable(false)
        }
        if (::addressText.isInitialized) {
            addressText.text = ""
            addressText.setTextIsSelectable(false)
        }
        if (::status.isInitialized) status.text = ""
        if (::pastedKey.isInitialized) pastedKey.setText("")
        window.decorView.visibility = View.INVISIBLE
        if (!isFinishing) finish()
    }

    private fun makeKey() {
        if (closedForShare) return
        workflow.makeKey(
            onCreated = { key, fingerprint ->
                if (closedForShare) return@makeKey
                keyText.text = key
                status.text = "Show this key once. Fingerprint: $fingerprint"
            },
            onFailure = { if (!closedForShare) status.text = it },
        )
    }

    private fun acceptKey() {
        if (closedForShare) return
        val typed = pastedKey.text?.toString().orEmpty().trim()
        workflow.acceptPastedKey(
            typed,
            onAccepted = { fingerprint ->
                if (closedForShare) return@acceptPastedKey
                pastedKey.text?.clear()
                keyText.text = "Key saved. It is not shown again."
                status.text = "Pairing key fingerprint: $fingerprint"
            },
            onFailure = { if (!closedForShare) status.text = it },
        )
    }

    /** Phase 0.4: the manual fallback for the two-phone link. Read off the main thread; it walks the network interfaces. */
    private fun showLocalAddress() {
        Thread({
            val line = try {
                val address = LocalAddressPolicy.siteLocalAddress()
                "Camera phone address: " + ManualAddress.describe(address, CameraRemoteServer.DEFAULT_PORT)
            } catch (error: IllegalStateException) {
                "No private Wi-Fi address yet. Join the same Wi-Fi as the remote phone, then reopen Pairing."
            }
            runOnUiThread { if (!isDestroyed && !closedForShare) addressText.text = line }
        }, "offgrid-pairing-address").start()
    }

    override fun onDestroy() {
        if (::workflow.isInitialized) workflow.close()
        super.onDestroy()
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
            setColor(GLASS)
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
        const val TAG = "OffgridPairing"
        const val WHITE = 0xfff4f4f4.toInt()
        const val DIM = 0xff777777.toInt()
        const val GLASS = 0xff080808.toInt()
        const val UV = 0xff9b5cff.toInt()
    }
}
