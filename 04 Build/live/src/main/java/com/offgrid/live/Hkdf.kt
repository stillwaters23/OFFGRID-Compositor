package com.offgrid.live

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hkdf {
    fun sha256(inputKeyMaterial: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..(255 * HASH_LENGTH))
        val actualSalt = if (salt.isEmpty()) ByteArray(HASH_LENGTH) else salt
        val extract = hmac(actualSalt, inputKeyMaterial)
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            previous = hmac(extract, previous + info + counter.toByte())
            val count = minOf(previous.size, length - offset)
            previous.copyInto(output, offset, 0, count)
            offset += count
            counter++
        }
        return output
    }

    fun hmac(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }

    private const val HASH_LENGTH = 32
}
