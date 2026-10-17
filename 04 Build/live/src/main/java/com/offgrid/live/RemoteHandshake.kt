package com.offgrid.live

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyAgreement

class RemoteHandshake private constructor(
    private val keyPair: KeyPair,
    val nonce: ByteArray,
) {
    val publicKey: ByteArray
        get() = keyPair.public.encoded.copyOf()

    fun derive(
        peerPublicKey: ByteArray,
        peerNonce: ByteArray,
        pairingKey: String,
        isInitiator: Boolean,
    ): SessionKeys {
        require(PairingKey.isValid(pairingKey))
        require(nonce.size == NONCE_SIZE && peerNonce.size == NONCE_SIZE)
        val nonceA = if (isInitiator) nonce else peerNonce
        val nonceB = if (isInitiator) peerNonce else nonce
        val peer = java.security.KeyFactory.getInstance("XDH")
            .generatePublic(java.security.spec.X509EncodedKeySpec(peerPublicKey))
        val agreement = KeyAgreement.getInstance("XDH")
        agreement.init(keyPair.private)
        agreement.doPhase(peer, true)
        val sharedSecret = agreement.generateSecret()
        val pairingBytes = pairingKey.toByteArray(Charsets.US_ASCII)
        val salt = nonceA + nonceB
        val info = SESSION_INFO + pairingBytes
        val keyMaterial = Hkdf.sha256(sharedSecret, salt, info, 72)
        val transcript = (if (isInitiator) publicKey + peerPublicKey else peerPublicKey + publicKey) + nonceA + nonceB
        return SessionKeys(
            initiatorKey = keyMaterial.copyOfRange(0, 32),
            responderKey = keyMaterial.copyOfRange(32, 64),
            initiatorNoncePrefix = keyMaterial.copyOfRange(64, 68),
            responderNoncePrefix = keyMaterial.copyOfRange(68, 72),
            transcriptMac = Hkdf.hmac(confirmationKey(pairingBytes), transcript),
        )
    }

    companion object {
        private const val NONCE_SIZE = 32
        private val SESSION_INFO = "OFFGRID remote v1".toByteArray(Charsets.US_ASCII)
        private val secureRandom = SecureRandom()

        fun create(): RemoteHandshake {
            val pair = KeyPairGenerator.getInstance("XDH").apply { initialize(java.security.spec.NamedParameterSpec("X25519")) }.generateKeyPair()
            val nonce = ByteArray(NONCE_SIZE).also(secureRandom::nextBytes)
            return RemoteHandshake(pair, nonce)
        }

        const val ROLE_INITIATOR = "initiator"
        const val ROLE_RESPONDER = "responder"

        fun confirm(pairingKey: String, transcript: ByteArray, role: String): ByteArray {
            require(PairingKey.isValid(pairingKey))
            require(role == ROLE_INITIATOR || role == ROLE_RESPONDER)
            val pairingBytes = pairingKey.toByteArray(Charsets.US_ASCII)
            val tagged = transcript + role.toByteArray(Charsets.US_ASCII)
            return Hkdf.hmac(confirmationKey(pairingBytes), tagged)
        }

        fun verifyConfirmation(
            pairingKey: String,
            transcript: ByteArray,
            role: String,
            confirmation: ByteArray,
        ): Boolean = MessageDigest.isEqual(confirm(pairingKey, transcript, role), confirmation)

        fun transcriptMac(pairingKey: String, transcript: ByteArray): ByteArray {
            require(PairingKey.isValid(pairingKey))
            val pairingBytes = pairingKey.toByteArray(Charsets.US_ASCII)
            return Hkdf.hmac(confirmationKey(pairingBytes), transcript)
        }

        private fun confirmationKey(pairingKey: ByteArray): ByteArray =
            Hkdf.sha256(
                pairingKey,
                ByteArray(32),
                "OFFGRID transcript confirmation v1".toByteArray(Charsets.US_ASCII),
                32,
            )
    }
}

data class SessionKeys(
    val initiatorKey: ByteArray,
    val responderKey: ByteArray,
    val initiatorNoncePrefix: ByteArray,
    val responderNoncePrefix: ByteArray,
    val transcriptMac: ByteArray,
) {
    fun confirmWith(pairingKey: String, transcript: ByteArray): Boolean =
        runCatching { MessageDigest.isEqual(RemoteHandshake.transcriptMac(pairingKey, transcript), transcriptMac) }
            .getOrDefault(false)
}
