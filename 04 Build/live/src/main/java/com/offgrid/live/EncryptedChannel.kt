package com.offgrid.live

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedChannel(
    key: ByteArray,
    private val noncePrefix: ByteArray,
) {
    private val secret = SecretKeySpec(key.copyOf(), "AES")
    private var sendCounter = 0L
    private var receiveCounter = 0L

    init {
        require(key.size == 32)
        require(noncePrefix.size == 4)
    }

    @Synchronized
    fun encrypt(plainText: ByteArray): ByteArray {
        check(sendCounter < REKEY_LIMIT) { "Session must be rekeyed" }
        val counter = sendCounter++
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, nonce(counter)))
        val encrypted = cipher.doFinal(plainText)
        return ByteBuffer.allocate(Long.SIZE_BYTES + encrypted.size)
            .putLong(counter)
            .put(encrypted)
            .array()
    }

    @Synchronized
    fun decrypt(frame: ByteArray): ByteArray {
        require(frame.size >= Long.SIZE_BYTES + TAG_LENGTH)
        val buffer = ByteBuffer.wrap(frame)
        val counter = buffer.long
        check(counter == receiveCounter) { "Replay or out-of-order frame" }
        check(receiveCounter < REKEY_LIMIT) { "Session must be rekeyed" }
        val payload = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, nonce(counter)))
        val plainText = cipher.doFinal(payload)
        receiveCounter++
        return plainText
    }

    private fun nonce(counter: Long): ByteArray =
        ByteBuffer.allocate(NONCE_LENGTH).put(noncePrefix).putLong(counter).array()

    private companion object {
        const val NONCE_LENGTH = 12
        const val TAG_LENGTH = 16
        const val REKEY_LIMIT = 1L shl 20
    }
}
