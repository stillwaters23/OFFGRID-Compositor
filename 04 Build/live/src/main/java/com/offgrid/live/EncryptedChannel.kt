package com.offgrid.live

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedChannel(
    key: ByteArray,
    private val noncePrefix: ByteArray,
    private val rekeyLimit: Long = DEFAULT_REKEY_LIMIT,
    private val lifetimeNanos: Long = DEFAULT_LIFETIME_NANOS,
) {
    private var keyBytes = key.copyOf()
    private var secret = SecretKeySpec(keyBytes, "AES")
    private var sendCounter = 0L
    private var receiveCounter = 0L
    private var sendEpoch = 0
    private var receiveEpoch = 0
    private var sendEpochStartedAt = System.nanoTime()

    init {
        require(key.size == 32)
        require(noncePrefix.size == 4)
        require(rekeyLimit > 0)
        require(lifetimeNanos > 0)
    }

    @Synchronized
    fun encrypt(plainText: ByteArray): ByteArray {
        if (sendCounter >= rekeyLimit || System.nanoTime() - sendEpochStartedAt >= lifetimeNanos) {
            ratchetSendKey()
        }
        val counter = sendCounter++
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, nonce(noncePrefix, counter)))
        val encrypted = cipher.doFinal(plainText)
        return ByteBuffer.allocate(Int.SIZE_BYTES + Long.SIZE_BYTES + encrypted.size)
            .putInt(sendEpoch)
            .putLong(counter)
            .put(encrypted)
            .array()
    }

    @Synchronized
    fun decrypt(frame: ByteArray): ByteArray {
        require(frame.size >= HEADER_LENGTH + TAG_LENGTH)
        val buffer = ByteBuffer.wrap(frame)
        val epoch = buffer.int
        require(epoch == receiveEpoch || epoch == receiveEpoch + 1) { "Invalid encryption epoch" }
        if (epoch == receiveEpoch + 1) ratchetReceiveKey()
        val counter = buffer.long
        check(counter == receiveCounter) { "Replay or out-of-order frame" }
        val payload = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, nonce(noncePrefix, counter)))
        val plainText = cipher.doFinal(payload)
        receiveCounter++
        return plainText
    }

    private fun ratchetSendKey() {
        val nextEpoch = sendEpoch + 1
        secret = ratchet(keyBytes, nextEpoch).also { next ->
            keyBytes.fill(0)
            keyBytes = next
        }.let { SecretKeySpec(it, "AES") }
        sendEpoch = nextEpoch
        sendCounter = 0
        sendEpochStartedAt = System.nanoTime()
    }

    private fun ratchetReceiveKey() {
        val nextEpoch = receiveEpoch + 1
        val next = ratchet(keyBytes, nextEpoch)
        keyBytes.fill(0)
        keyBytes = next
        secret = SecretKeySpec(next, "AES")
        receiveEpoch = nextEpoch
        receiveCounter = 0
    }

    private fun ratchet(key: ByteArray, epoch: Int): ByteArray =
        Hkdf.sha256(
            key,
            ByteBuffer.allocate(Int.SIZE_BYTES).putInt(epoch).array(),
            "OFFGRID channel rekey v1".toByteArray(Charsets.US_ASCII),
            32,
        )

    private fun nonce(prefix: ByteArray, counter: Long): ByteArray =
        ByteBuffer.allocate(NONCE_LENGTH).put(prefix).putLong(counter).array()

    private companion object {
        const val NONCE_LENGTH = 12
        const val TAG_LENGTH = 16
        const val HEADER_LENGTH = Int.SIZE_BYTES + Long.SIZE_BYTES
        const val DEFAULT_REKEY_LIMIT = 1L shl 20
        const val DEFAULT_LIFETIME_NANOS = 30L * 60 * 1_000_000_000
    }
}
