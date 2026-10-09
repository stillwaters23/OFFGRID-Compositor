package com.offgrid.live

import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.Executors
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSecurityTest {
    @Test
    fun generatedKeysHaveExpectedLengthAlphabetAndNoDuplicates() {
        val keys = HashSet<String>()
        repeat(10_000) {
            val key = PairingKey.generate()
            assertEquals(128, key.length)
            assertTrue(PairingKey.isValid(key))
            assertTrue(keys.add(key))
        }
    }

    @Test
    fun rejectsShortOrInvalidKeys() {
        assertFalse(PairingKey.isValid("A".repeat(99)))
        assertFalse(PairingKey.isValid("A".repeat(127) + "!"))
        assertTrue(PairingKey.isValid("A".repeat(100)))
    }

    @Test
    fun hkdfMatchesRfc5869CaseOne() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = "000102030405060708090a0b0c".hexBytes()
        val info = "f0f1f2f3f4f5f6f7f8f9".hexBytes()
        val actual = Hkdf.sha256(ikm, salt, info, 42)
        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a" +
                "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                "34007208d5b887185865",
            actual.hex(),
        )
    }

    @Test
    fun channelAuthenticatesAndRejectsReplayAndWrongKey() {
        val key = ByteArray(32) { it.toByte() }
        val sender = EncryptedChannel(key, byteArrayOf(1, 2, 3, 4))
        val receiver = EncryptedChannel(key, byteArrayOf(1, 2, 3, 4))
        val frame = sender.encrypt("hello".toByteArray())
        assertArrayEquals("hello".toByteArray(), receiver.decrypt(frame))
        assertFalse(runCatching { receiver.decrypt(frame) }.isSuccess)

        val wrongReceiver = EncryptedChannel(ByteArray(32) { (it + 1).toByte() }, byteArrayOf(1, 2, 3, 4))
        assertFalse(runCatching { wrongReceiver.decrypt(frame) }.isSuccess)
    }

    @Test
    fun x25519HandshakeUsesKeyAndRejectsWrongKeyConfirmation() {
        val first = RemoteHandshake.create()
        val second = RemoteHandshake.create()
        val firstNonce = first.nonce
        val secondNonce = second.nonce
        val key = PairingKey.generate()
        val firstKeys = first.derive(second.publicKey, secondNonce, key, true)
        val secondKeys = second.derive(first.publicKey, firstNonce, key, false)
        assertArrayEquals(firstKeys.initiatorKey, secondKeys.initiatorKey)
        assertArrayEquals(firstKeys.responderKey, secondKeys.responderKey)
        assertArrayEquals(firstKeys.transcriptMac, secondKeys.transcriptMac)

        val transcript = first.publicKey + second.publicKey + firstNonce + secondNonce
        val confirmation = RemoteHandshake.confirm(key, transcript)
        assertTrue(RemoteHandshake.verifyConfirmation(key, transcript, confirmation))
        assertFalse(RemoteHandshake.verifyConfirmation(PairingKey.generate(), transcript, confirmation))
        assertNotEquals(firstKeys.initiatorKey.toList(), firstKeys.responderKey.toList())
    }

    @Test
    fun loopbackConnectionCarriesEncryptedControlMessage() {
        val loopback = InetAddress.getLoopbackAddress()
        val key = ByteArray(32) { 9 }
        val prefix = byteArrayOf(4, 3, 2, 1)
        LocalSocketServer(bindAddress = loopback).use { server ->
            val executor = Executors.newSingleThreadExecutor()
            try {
                val received = executor.submit<RemoteMessage> {
                    server.accept().use { connection ->
                        connection.receive(EncryptedChannel(key, prefix))
                    }
                }
                Socket(loopback, server.localPort).use { socket ->
                    LocalMessageConnection(socket).use { connection ->
                        connection.send(
                            EncryptedChannel(key, prefix),
                            RemoteMessage("fx.cam.on", "setting", "true"),
                        )
                    }
                }
                assertEquals(RemoteMessage("fx.cam.on", "setting", "true"), received.get())
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun publicAddressesAreRejected() {
        assertFalse(LocalAddressPolicy.isAllowed(InetAddress.getByName("8.8.8.8")))
        assertTrue(LocalAddressPolicy.isAllowed(InetAddress.getByName("192.168.1.5")))
    }

    private fun String.hexBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
}
