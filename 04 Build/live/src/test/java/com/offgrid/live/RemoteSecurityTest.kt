package com.offgrid.live

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
            assertEquals(6, PairingKey.fingerprint(key).length)
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

        val orderedSender = EncryptedChannel(key, byteArrayOf(5, 6, 7, 8))
        val orderedReceiver = EncryptedChannel(key, byteArrayOf(5, 6, 7, 8))
        val first = orderedSender.encrypt(byteArrayOf(1))
        val second = orderedSender.encrypt(byteArrayOf(2))
        assertFalse(runCatching { orderedReceiver.decrypt(second) }.isSuccess)
        assertArrayEquals(byteArrayOf(1), orderedReceiver.decrypt(first))
    }

    @Test
    fun channelRekeysBeforeItsMessageLimitWithoutDroppingTheLink() {
        val key = ByteArray(32) { (it + 3).toByte() }
        val prefix = byteArrayOf(1, 3, 5, 7)
        val sender = EncryptedChannel(key, prefix, rekeyLimit = 2)
        val receiver = EncryptedChannel(key, prefix, rekeyLimit = 2)
        repeat(6) { index ->
            val plain = byteArrayOf(index.toByte())
            assertArrayEquals(plain, receiver.decrypt(sender.encrypt(plain)))
        }
    }

    @Test
    fun channelRekeysAfterItsLifetime() {
        val key = ByteArray(32) { (it + 7).toByte() }
        val prefix = byteArrayOf(2, 4, 6, 8)
        val sender = EncryptedChannel(key, prefix, lifetimeNanos = 1)
        val receiver = EncryptedChannel(key, prefix, lifetimeNanos = Long.MAX_VALUE)
        val plain = byteArrayOf(7, 8, 9)
        assertArrayEquals(plain, receiver.decrypt(sender.encrypt(plain)))
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
        val initiatorTag = RemoteHandshake.confirm(key, transcript, RemoteHandshake.ROLE_INITIATOR)
        val responderTag = RemoteHandshake.confirm(key, transcript, RemoteHandshake.ROLE_RESPONDER)
        assertFalse(initiatorTag.contentEquals(responderTag))
        assertTrue(RemoteHandshake.verifyConfirmation(key, transcript, RemoteHandshake.ROLE_INITIATOR, initiatorTag))
        assertTrue(RemoteHandshake.verifyConfirmation(key, transcript, RemoteHandshake.ROLE_RESPONDER, responderTag))
        assertFalse(RemoteHandshake.verifyConfirmation(key, transcript, RemoteHandshake.ROLE_RESPONDER, initiatorTag))
        assertFalse(RemoteHandshake.verifyConfirmation(PairingKey.generate(), transcript, RemoteHandshake.ROLE_INITIATOR, initiatorTag))
        assertFalse(initiatorTag.contentEquals(Hkdf.hmac(key.toByteArray(Charsets.US_ASCII), transcript)))
        assertArrayEquals(firstKeys.transcriptMac, RemoteHandshake.transcriptMac(key, transcript))
        assertNotEquals(firstKeys.initiatorKey.toList(), firstKeys.responderKey.toList())
    }

    @Test
    fun echoedConfirmationDoesNotProveThePairingKey() {
        val loopback = InetAddress.getLoopbackAddress()
        val pairingKey = PairingKey.generate()
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            try {
                val echoed = executor.submit<Boolean> {
                    val socket = server.accept()
                    socket.soTimeout = 2_000
                    val input = DataInputStream(socket.getInputStream())
                    val output = DataOutputStream(socket.getOutputStream())
                    val peer = RemoteHandshake.create()
                    val remoteKey = readFrame(input)
                    val remoteNonce = readFrame(input)
                    writeFrame(output, peer.publicKey)
                    writeFrame(output, peer.nonce)
                    output.flush()
                    val proof = ByteArray(32)
                    input.readFully(proof)
                    output.write(proof)
                    output.flush()
                    socket.close()
                    remoteKey.isNotEmpty() && remoteNonce.size == 32
                }
                val failure = runCatching {
                    RemoteSession.establish(Socket(loopback, server.localPort), pairingKey, true)
                }.exceptionOrNull()
                assertEquals("Keys don't match", failure?.message)
                assertTrue(echoed.get(2, TimeUnit.SECONDS))
            } finally {
                executor.shutdownNow()
            }
        }
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
    fun authenticatedSessionCarriesControlsAndFinishedVideoFrames() {
        val loopback = InetAddress.getLoopbackAddress()
        val pairingKey = PairingKey.generate()
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            try {
                val remote = executor.submit<RemoteSession> {
                    RemoteSession.establish(server.accept(), pairingKey, false)
                }
                RemoteSession.establish(Socket(loopback, server.localPort), pairingKey, true).use { camera ->
                    remote.get().use { controller ->
                        controller.sendControl(RemoteMessage("fx.cam.on", "setting", "false"))
                        val setting = camera.receive() as RemotePacket.Control
                        assertEquals("fx.cam.on", setting.message.key)
                        assertEquals("false", setting.message.value)

                        camera.sendVideo(byteArrayOf(1, 2, 3), 24_000L, 1)
                        val frame = controller.receive() as RemotePacket.Video
                        assertArrayEquals(byteArrayOf(1, 2, 3), frame.sample)
                        assertEquals(24_000L, frame.presentationTimeUs)
                    }
                }
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun authenticatedSessionRejectsDifferentPairingKeys() {
        val loopback = InetAddress.getLoopbackAddress()
        val pairingKey = PairingKey.generate()
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            try {
                val remote = executor.submit<Boolean> {
                    val socket = server.accept()
                    try {
                        RemoteSession.establish(socket, PairingKey.generate(), false).close()
                        false
                    } catch (error: IllegalArgumentException) {
                        error.message == "Keys don't match"
                    }
                }
                val failure = runCatching {
                    RemoteSession.establish(
                        Socket(loopback, server.localPort),
                        pairingKey,
                        true,
                    )
                }.exceptionOrNull()
                assertEquals("Keys don't match", failure?.message)
                assertTrue(remote.get())
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun incompleteHandshakeTimesOut() {
        val loopback = InetAddress.getLoopbackAddress()
        val pairingKey = PairingKey.generate()
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            try {
                val timedOut = executor.submit<Boolean> {
                    try {
                        RemoteSession.establish(server.accept(), pairingKey, false, handshakeTimeoutMs = 100)
                        false
                    } catch (_: java.net.SocketTimeoutException) {
                        true
                    }
                }
                Socket(loopback, server.localPort).use {
                    assertTrue(timedOut.get(2, TimeUnit.SECONDS))
                }
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun closingOnePhoneUnblocksTheOther() {
        val loopback = InetAddress.getLoopbackAddress()
        val pairingKey = PairingKey.generate()
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            try {
                val remote = executor.submit<RemoteSession> {
                    RemoteSession.establish(server.accept(), pairingKey, false)
                }
                RemoteSession.establish(Socket(loopback, server.localPort), pairingKey, true).use { camera ->
                    val controller = remote.get(2, TimeUnit.SECONDS)
                    camera.close()
                    val error = runCatching { controller.receive() }.exceptionOrNull()
                    assertTrue(error is java.io.IOException)
                    controller.close()
                }
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

    private fun readFrame(input: DataInputStream): ByteArray {
        val size = input.readInt()
        require(size in 1..256)
        return ByteArray(size).also(input::readFully)
    }

    private fun writeFrame(output: DataOutputStream, value: ByteArray) {
        output.writeInt(value.size)
        output.write(value)
    }

    private fun String.hexBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
}
