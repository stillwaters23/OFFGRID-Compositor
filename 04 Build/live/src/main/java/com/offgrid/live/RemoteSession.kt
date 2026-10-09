package com.offgrid.live

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Socket
import java.security.MessageDigest

class RemoteSession private constructor(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
    private val sendChannel: EncryptedChannel,
    private val receiveChannel: EncryptedChannel,
) : AutoCloseable {
    @Synchronized
    fun sendControl(message: RemoteMessage) {
        writeEncrypted(byteArrayOf(CONTROL.toByte()) + message.encode())
    }

    @Synchronized
    fun sendVideo(sample: ByteArray, presentationTimeUs: Long, flags: Int) {
        require(sample.size <= MAX_VIDEO_SIZE)
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { packet ->
            packet.writeByte(VIDEO)
            packet.writeLong(presentationTimeUs)
            packet.writeInt(flags)
            packet.writeInt(sample.size)
            packet.write(sample)
        }
        writeEncrypted(output.toByteArray())
    }

    @Synchronized
    fun receive(): RemotePacket {
        val frameSize = input.readInt()
        require(frameSize in MIN_ENCRYPTED_SIZE..MAX_ENCRYPTED_SIZE)
        val encrypted = ByteArray(frameSize)
        input.readFully(encrypted)
        val payload = DataInputStream(receiveChannel.decrypt(encrypted).inputStream())
        return when (payload.readUnsignedByte()) {
            CONTROL -> {
                val messageBytes = ByteArray(payload.available()).also(payload::readFully)
                RemotePacket.Control(RemoteMessage.decode(messageBytes))
            }
            VIDEO -> {
                val timestamp = payload.readLong()
                val flags = payload.readInt()
                val size = payload.readInt()
                require(size in 0..MAX_VIDEO_SIZE && size == payload.available())
                RemotePacket.Video(ByteArray(size).also(payload::readFully), timestamp, flags)
            }
            else -> throw IllegalArgumentException("Unknown remote message")
        }
    }

    private fun writeEncrypted(payload: ByteArray) {
        val encrypted = sendChannel.encrypt(payload)
        output.writeInt(encrypted.size)
        output.write(encrypted)
        output.flush()
    }

    override fun close() = socket.close()

    companion object {
        private const val CONTROL = 1
        private const val VIDEO = 2
        private const val MAX_VIDEO_SIZE = 16 * 1024 * 1024
        private const val MAX_ENCRYPTED_SIZE = MAX_VIDEO_SIZE + 64
        private const val MIN_ENCRYPTED_SIZE = 24

        fun establish(
            socket: Socket,
            pairingKey: String,
            isInitiator: Boolean,
            handshakeTimeoutMs: Int = HANDSHAKE_TIMEOUT_MS,
        ): RemoteSession {
            require(handshakeTimeoutMs > 0)
            socket.soTimeout = handshakeTimeoutMs
            try {
                return establishConnected(socket, pairingKey, isInitiator)
            } catch (error: Exception) {
                try {
                    socket.close()
                } catch (closeError: IOException) {
                    error.addSuppressed(closeError)
                }
                throw error
            } finally {
                if (!socket.isClosed) socket.soTimeout = 0
            }
        }

        private fun establishConnected(
            socket: Socket,
            pairingKey: String,
            isInitiator: Boolean,
        ): RemoteSession {
            require(PairingKey.isValid(pairingKey))
            LocalAddressPolicy.requireAllowed(socket.inetAddress)
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val local = RemoteHandshake.create()
            writeBytes(output, local.publicKey)
            writeBytes(output, local.nonce)
            output.flush()
            val peerPublicKey = readBytes(input, MAX_PUBLIC_KEY_SIZE)
            val peerNonce = readBytes(input, NONCE_SIZE)
            require(peerNonce.size == NONCE_SIZE)

            val firstPublicKey = if (isInitiator) local.publicKey else peerPublicKey
            val secondPublicKey = if (isInitiator) peerPublicKey else local.publicKey
            val firstNonce = if (isInitiator) local.nonce else peerNonce
            val secondNonce = if (isInitiator) peerNonce else local.nonce
            val transcript = firstPublicKey + secondPublicKey + firstNonce + secondNonce
            val proof = RemoteHandshake.confirm(pairingKey, transcript)
            output.write(proof)
            output.flush()
            val peerProof = ByteArray(PROOF_SIZE).also(input::readFully)
            require(MessageDigest.isEqual(proof, peerProof)) { "Keys don't match" }

            val keys = local.derive(peerPublicKey, peerNonce, pairingKey, isInitiator)
            val sendKey = if (isInitiator) keys.initiatorKey else keys.responderKey
            val receiveKey = if (isInitiator) keys.responderKey else keys.initiatorKey
            val sendPrefix = if (isInitiator) keys.initiatorNoncePrefix else keys.responderNoncePrefix
            val receivePrefix = if (isInitiator) keys.responderNoncePrefix else keys.initiatorNoncePrefix
            return RemoteSession(
                socket,
                input,
                output,
                EncryptedChannel(sendKey, sendPrefix),
                EncryptedChannel(receiveKey, receivePrefix),
            )
        }

        private fun writeBytes(output: DataOutputStream, value: ByteArray) {
            output.writeInt(value.size)
            output.write(value)
        }

        private fun readBytes(input: DataInputStream, maxSize: Int): ByteArray {
            val size = input.readInt()
            require(size in 1..maxSize)
            return ByteArray(size).also(input::readFully)
        }

        private const val NONCE_SIZE = 32
        private const val PROOF_SIZE = 32
        private const val MAX_PUBLIC_KEY_SIZE = 128
        private const val HANDSHAKE_TIMEOUT_MS = 5_000
    }
}

sealed interface RemotePacket {
    data class Control(val message: RemoteMessage) : RemotePacket
    data class Video(val sample: ByteArray, val presentationTimeUs: Long, val flags: Int) : RemotePacket
}
