package com.offgrid.compositor

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The OFFGRID to OFFGRID handshake codes (feature 14, final build M4).
 *
 * Each phone has its own 300 character code. Two people swap codes before a call. A code is
 * replaced with a new one after every call, and a blocked person's code never works again.
 * Calls themselves need a server and are not built yet; the codes are.
 *
 * Everything is locked with a key kept in the phone's own secure key store (Android
 * Keystore). Royalty's code is never saved as plain text. For other people only a
 * fingerprint (a one-way hash) is kept, never their code.
 */
internal class Handshake(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Royalty's current code. A new one is made the first time. Null if the key store failed. */
    fun myCode(): String? {
        val sealed = prefs.getString(MY_CODE, null)
        if (sealed != null) open(sealed)?.let { return it }
        return newCode()
    }

    /** Makes a new code. The old one stops working. */
    fun newCode(): String? {
        val code = makeCode()
        val sealed = seal(code) ?: return null
        prefs.edit { putString(MY_CODE, sealed) }
        return code
    }

    enum class Status { TRUSTED, BLOCKED, UNKNOWN, NOT_A_CODE, MINE }

    /** Says whether [code] is trusted, blocked, new, or Royalty's own. */
    fun check(code: String): Status {
        val clean = code.trim()
        if (!looksLikeCode(clean)) return Status.NOT_A_CODE
        if (clean == myCode()) return Status.MINE
        val print = fingerprint(clean)
        return when {
            print in set(BLOCKED) -> Status.BLOCKED
            print in set(TRUSTED) -> Status.TRUSTED
            else -> Status.UNKNOWN
        }
    }

    /** Trusts [code], unless it is blocked. Returns the status after. */
    fun trust(code: String): Status {
        val status = check(code)
        if (status != Status.UNKNOWN && status != Status.TRUSTED) return status
        update(TRUSTED) { it + fingerprint(code.trim()) }
        return Status.TRUSTED
    }

    /** Blocks [code] for good. It is also taken off the trusted list. */
    fun block(code: String): Status {
        val status = check(code)
        if (status == Status.NOT_A_CODE || status == Status.MINE) return status
        val print = fingerprint(code.trim())
        update(TRUSTED) { it - print }
        update(BLOCKED) { it + print }
        return Status.BLOCKED
    }

    private fun set(name: String): Set<String> {
        val sealed = prefs.getString(name, null) ?: return emptySet()
        val plain = open(sealed) ?: return emptySet()
        return plain.split(SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private fun update(name: String, change: (Set<String>) -> Set<String>) {
        val next = change(set(name))
        val sealed = seal(next.joinToString(SEPARATOR)) ?: return
        prefs.edit { putString(name, sealed) }
    }

    private fun key(): SecretKey? = try {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build(),
            )
            generateKey()
        }
    } catch (error: Exception) {
        Log.e(TAG, "The secure key store is not available", error)
        null
    }

    private fun seal(plain: String): String? {
        val key = key() ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(body, Base64.NO_WRAP)
        } catch (error: Exception) {
            Log.e(TAG, "Locking failed", error)
            null
        }
    }

    private fun open(sealed: String): String? {
        val key = key() ?: return null
        val parts = sealed.split(':')
        if (parts.size != 2) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (error: Exception) {
            Log.e(TAG, "Unlocking failed", error)
            null
        }
    }

    companion object {
        const val CODE_LENGTH = 300

        /** Letters and numbers that are easy to tell apart: no 0, O, 1, l, or I. */
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

        private const val FILE_NAME = "handshake_locked"
        private const val MY_CODE = "my_code"
        private const val TRUSTED = "trusted"
        private const val BLOCKED = "blocked"
        private const val SEPARATOR = ","
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "offgrid_handshake"
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val TAG = "OffgridHandshake"

        fun makeCode(random: SecureRandom = SecureRandom()): String =
            String(CharArray(CODE_LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

        fun looksLikeCode(text: String): Boolean = text.length == CODE_LENGTH && text.all { it in ALPHABET }

        /** A one-way fingerprint. The code can't be got back from it. */
        fun fingerprint(code: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(code.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
