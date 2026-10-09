package com.offgrid.live

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

class PairingWorkflow(context: Context) {
    private val appContext = context.applicationContext
    private val clipboard = appContext.getSystemService(ClipboardManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentKey: String? = null

    fun makeKey(): Pair<String, String> {
        val key = PairingKey.generate()
        currentKey = key
        KeyVault(appContext).save(key)
        return key to PairingKey.fingerprint(key)
    }

    fun acceptPastedKey(value: String): String {
        require(PairingKey.isValid(value)) { "Pairing key must be at least 100 characters and use only letters, numbers, - and _." }
        currentKey = value
        KeyVault(appContext).save(value)
        return PairingKey.fingerprint(value)
    }

    fun copyCurrentKey() {
        val key = checkNotNull(currentKey) { "Make a pairing key first." }
        val clip = ClipData.newPlainText("OFFGRID pairing key", key)
        clip.description.extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        clipboard.setPrimaryClip(clip)
        mainHandler.postDelayed({
            val active = clipboard.primaryClip
            if (active?.itemCount == 1 && active.getItemAt(0).text?.toString() == key) {
                clipboard.clearPrimaryClip()
            }
        }, CLIPBOARD_LIFETIME_MS)
    }

    fun forget() {
        KeyVault(appContext).forget()
        currentKey = null
    }

    private companion object {
        const val CLIPBOARD_LIFETIME_MS = 60_000L
    }
}
