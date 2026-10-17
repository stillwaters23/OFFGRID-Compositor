package com.offgrid.live

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.security.GeneralSecurityException
import java.util.concurrent.Executors

class PairingWorkflow(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val clipboard = appContext.getSystemService(ClipboardManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val keyWorker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "OffgridPairingKey").apply { isDaemon = true }
    }
    private var currentKey: String? = null

    fun makeKey(onCreated: (String, String) -> Unit, onFailure: (String) -> Unit) {
        clearCurrentClipboard()
        keyWorker.execute {
            val key = try {
                PairingKey.generate()
            } catch (error: SecurityException) {
                mainHandler.post { onFailure("Could not make a secure pairing key.") }
                return@execute
            }
            try {
                KeyVault(appContext).save(key)
            } catch (error: GeneralSecurityException) {
                mainHandler.post { onFailure("Could not save the pairing key in Android Keystore.") }
                return@execute
            } catch (error: SecurityException) {
                mainHandler.post { onFailure("Could not save the pairing key in Android Keystore.") }
                return@execute
            }
            mainHandler.post {
                currentKey = key
                onCreated(key, PairingKey.fingerprint(key))
            }
        }
    }

    fun acceptPastedKey(
        value: String,
        onAccepted: (String) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        if (!PairingKey.isValid(value)) {
            onFailure("Pairing key must be at least 100 characters and use only letters, numbers, - and _.")
            return
        }
        keyWorker.execute {
            try {
                KeyVault(appContext).save(value)
            } catch (error: GeneralSecurityException) {
                mainHandler.post { onFailure("Could not save the pairing key in Android Keystore.") }
                return@execute
            } catch (error: SecurityException) {
                mainHandler.post { onFailure("Could not save the pairing key in Android Keystore.") }
                return@execute
            }
            mainHandler.post {
                currentKey = value
                onAccepted(PairingKey.fingerprint(value))
            }
        }
    }

    fun loadKey(onLoaded: (String, String) -> Unit, onFailure: (String) -> Unit) {
        keyWorker.execute {
            val key = try {
                KeyVault(appContext).read()
            } catch (error: GeneralSecurityException) {
                mainHandler.post { onFailure("Could not open the saved pairing key.") }
                return@execute
            } catch (error: SecurityException) {
                mainHandler.post { onFailure("Could not open the saved pairing key.") }
                return@execute
            }
            if (key == null) {
                mainHandler.post { onFailure("No saved pairing key is available.") }
                return@execute
            }
            mainHandler.post {
                currentKey = key
                onLoaded(key, PairingKey.fingerprint(key))
            }
        }
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

    fun forget(onForgotten: () -> Unit, onFailure: (String) -> Unit) {
        clearCurrentClipboard()
        currentKey = null
        keyWorker.execute {
            try {
                KeyVault(appContext).forget()
            } catch (error: GeneralSecurityException) {
                mainHandler.post { onFailure("Could not forget the pairing key.") }
                return@execute
            } catch (error: SecurityException) {
                mainHandler.post { onFailure("Could not forget the pairing key.") }
                return@execute
            }
            mainHandler.post { onForgotten() }
        }
    }

    override fun close() {
        keyWorker.shutdownNow()
    }

    private fun clearCurrentClipboard() {
        val key = currentKey ?: return
        val active = clipboard.primaryClip
        if (active?.itemCount == 1 && active.getItemAt(0).text?.toString() == key) {
            clipboard.clearPrimaryClip()
        }
    }

    private companion object {
        const val CLIPBOARD_LIFETIME_MS = 60_000L
    }
}
