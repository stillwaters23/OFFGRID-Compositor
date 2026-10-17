package com.offgrid.live

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PairingKey {
    const val LENGTH = 128
    const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val random = SecureRandom()

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    fun isValid(value: String): Boolean =
        value.length >= 100 && value.all { it in ALPHABET }

    fun fingerprint(value: String): String {
        require(isValid(value))
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).take(6)
    }
}
