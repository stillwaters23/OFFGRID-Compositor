package com.offgrid.live

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

class SrtCaller(
    private val remote: InetSocketAddress,
    private val streamId: String,
) : PcTransport {
    private val socket = DatagramSocket()
    private val random = SecureRandom()
    private val mux = MpegTsMux()
    private val running = AtomicBoolean(false)
    private var reader: Thread? = null
    private var peerSocketId = 0
    private var sequence = 0
    private var messageNumber = 1
    private var startedAt = System.nanoTime()
    private var failed: IOException? = null

    fun start() {
        val address = remote.address ?: throw IOException("The PC address is not available.")
        LocalAddressPolicy.requireAllowed(address)
        socket.connect(remote)
        socket.soTimeout = 300
        val socketId = random.nextInt(0x7FFFFFFE) + 1
        sequence = random.nextInt(0x3FFFFFFF) + 1
        val induction = handshake(
            version = 4,
            encryption = 0,
            extension = UDT_DGRAM,
            type = INDUCTION,
            socketId = socketId,
            cookie = 0,
            destination = 0,
            extensions = ByteArray(0),
        )
        val invited = await(induction) { response ->
            response.version == 5 && response.extension == SRT_MAGIC && response.type == INDUCTION
        } ?: throw IOException("The PC did not answer SRT.")
        val extensions = hsreq() + streamIdExtension(streamId)
        val conclusion = handshake(
            version = 5,
            encryption = 0,
            extension = HSREQ or CONFIG,
            type = CONCLUSION,
            socketId = socketId,
            cookie = invited.cookie,
            destination = 0,
            extensions = extensions,
        )
        val confirmed = await(conclusion) { response ->
            response.type != INDUCTION && response.type == CONCLUSION && response.version == 5
        } ?: throw IOException("The PC did not confirm SRT.")
        peerSocketId = confirmed.socketId
        startedAt = System.nanoTime()
        running.set(true)
        reader = Thread(::readLoop, "OffgridSrtRead").apply {
            isDaemon = true
            start()
        }
    }

    override fun write(sample: PcSample) {
        failed?.let { throw it }
        if (!running.get()) throw IOException("SRT is not connected.")
        val bytes = when (sample) {
            is PcSample.VideoConfig -> {
                val annex = H264Payload.annexB(listOf(sample.sps, sample.pps), true)
                mux.video(annex, 0, true)
            }
            is PcSample.Video -> {
                val annex = H264Payload.annexB(sample.nalus, sample.keyframe)
                mux.video(annex, sample.timeUs * 9 / 100, sample.keyframe)
            }
            is PcSample.AudioConfig -> ByteArray(0)
            is PcSample.Audio -> mux.audio(H264Payload.adts(sample.frame), sample.timeUs * 9 / 100)
        }
        var offset = 0
        while (offset < bytes.size) {
            val count = minOf(PAYLOAD, bytes.size - offset)
            sendData(bytes.copyOfRange(offset, offset + count))
            offset += count
        }
    }

    override fun close() {
        running.set(false)
        if (peerSocketId != 0) {
            try {
                send(control(SHUTDOWN, 0, peerSocketId, ByteArray(0)))
            } catch (_: IOException) {
                Unit
            }
        }
        socket.close()
        reader?.join(200)
    }

    private fun readLoop() {
        val buffer = ByteArray(2048)
        while (running.get()) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.soTimeout = 1_000
                socket.receive(packet)
            } catch (_: IOException) {
                if (running.get()) sendKeepalive()
                continue
            }
            if (packet.length < 16) continue
            val word = intAt(buffer, 0)
            if (word >= 0) continue
            val type = (word ushr 16) and 0x7FFF
            when (type) {
                ACK -> send(control(ACKACK, intAt(buffer, 4), peerSocketId, ByteArray(0)))
                SHUTDOWN -> {
                    failed = IOException("The PC closed SRT.")
                    running.set(false)
                }
            }
        }
    }

    private fun sendData(payload: ByteArray) {
        val header = ByteArray(16 + payload.size)
        putInt(header, 0, sequence and 0x7FFFFFFF)
        putInt(header, 4, (0b11 shl 30) or (1 shl 29) or (messageNumber and 0x03FFFFFF))
        putInt(header, 8, timestamp())
        putInt(header, 12, peerSocketId)
        payload.copyInto(header, 16)
        send(header)
        sequence = (sequence + 1) and 0x7FFFFFFF
        messageNumber = (messageNumber + 1) and 0x03FFFFFF
    }

    private fun sendKeepalive() {
        if (peerSocketId == 0) return
        try {
            send(control(KEEPALIVE, 0, peerSocketId, ByteArray(0)))
        } catch (_: IOException) {
            failed = IOException("Lost the PC.")
        }
    }

    private fun await(packet: ByteArray, matches: (Handshake) -> Boolean): Handshake? {
        var tries = 0
        while (tries < 6) {
            tries++
            send(packet)
            val response = receiveHandshake() ?: continue
            if (matches(response)) return response
            if (response.type != INDUCTION) throw IOException("The PC refused the SRT handshake.")
        }
        return null
    }

    private fun receiveHandshake(): Handshake? {
        val buffer = ByteArray(1500)
        val packet = DatagramPacket(buffer, buffer.size)
        return try {
            socket.receive(packet)
            if (packet.length < 64) null else parseHandshake(buffer, packet.length)
        } catch (_: IOException) {
            null
        }
    }

    private fun send(bytes: ByteArray) {
        socket.send(DatagramPacket(bytes, bytes.size))
    }

    private fun timestamp(): Int = ((System.nanoTime() - startedAt) / 1_000L).toInt()

    private data class Handshake(
        val version: Int,
        val extension: Int,
        val type: Int,
        val socketId: Int,
        val cookie: Int,
    )

    private fun parseHandshake(buffer: ByteArray, length: Int): Handshake? {
        if (length < 64 || intAt(buffer, 0) >= 0) return null
        return Handshake(
            version = intAt(buffer, 16),
            extension = shortAt(buffer, 22),
            type = intAt(buffer, 36),
            socketId = intAt(buffer, 40),
            cookie = intAt(buffer, 44),
        )
    }

    private fun handshake(
        version: Int,
        encryption: Int,
        extension: Int,
        type: Int,
        socketId: Int,
        cookie: Int,
        destination: Int,
        extensions: ByteArray,
    ): ByteArray {
        val packet = ByteArray(64 + extensions.size)
        controlHeader(packet, HANDSHAKE, 0, destination)
        putInt(packet, 16, version)
        putShort(packet, 20, encryption)
        putShort(packet, 22, extension)
        putInt(packet, 24, sequence)
        putInt(packet, 28, 1500)
        putInt(packet, 32, 8192)
        putInt(packet, 36, type)
        putInt(packet, 40, socketId)
        putInt(packet, 44, cookie)
        extensions.copyInto(packet, 64)
        return packet
    }

    private fun hsreq(): ByteArray {
        val block = ByteArray(4 + 16)
        putShort(block, 0, 1)
        putShort(block, 2, 4)
        putInt(block, 4, 0x01050000)
        putInt(block, 8, 0x1B)
        putInt(block, 12, 120)
        putInt(block, 16, 120)
        return block
    }

    private fun streamIdExtension(value: String): ByteArray {
        val text = value.toByteArray(Charsets.UTF_8)
        val words = (text.size + 3) / 4
        val block = ByteArray(4 + words * 4)
        putShort(block, 0, 5)
        putShort(block, 2, words)
        text.copyInto(block, 4)
        return block
    }

    private fun control(type: Int, extra: Int, destination: Int, body: ByteArray): ByteArray {
        val packet = ByteArray(16 + body.size)
        controlHeader(packet, type, extra, destination)
        body.copyInto(packet, 16)
        return packet
    }

    private fun controlHeader(packet: ByteArray, type: Int, extra: Int, destination: Int) {
        putInt(packet, 0, 0x80000000.toInt() or (type shl 16))
        putInt(packet, 4, extra)
        putInt(packet, 8, timestamp())
        putInt(packet, 12, destination)
    }

    private fun putInt(target: ByteArray, index: Int, value: Int) {
        target[index] = (value ushr 24).toByte()
        target[index + 1] = (value ushr 16).toByte()
        target[index + 2] = (value ushr 8).toByte()
        target[index + 3] = value.toByte()
    }

    private fun putShort(target: ByteArray, index: Int, value: Int) {
        target[index] = (value ushr 8).toByte()
        target[index + 1] = value.toByte()
    }

    private fun intAt(buffer: ByteArray, index: Int): Int =
        ((buffer[index].toInt() and 0xFF) shl 24) or
            ((buffer[index + 1].toInt() and 0xFF) shl 16) or
            ((buffer[index + 2].toInt() and 0xFF) shl 8) or
            (buffer[index + 3].toInt() and 0xFF)

    private fun shortAt(buffer: ByteArray, index: Int): Int =
        ((buffer[index].toInt() and 0xFF) shl 8) or (buffer[index + 1].toInt() and 0xFF)

    companion object {
        const val PAYLOAD = 1316
        private const val UDT_DGRAM = 2
        private const val SRT_MAGIC = 0x4A17
        private const val INDUCTION = 1
        private const val CONCLUSION = -1
        private const val HANDSHAKE = 0
        private const val KEEPALIVE = 1
        private const val ACK = 2
        private const val SHUTDOWN = 5
        private const val ACKACK = 6
        private const val HSREQ = 0x0001
        private const val CONFIG = 0x0004

        fun open(config: PcOutputConfig): SrtCaller {
            val address = RtmpPublisher.localAddress(config.host)
            val caller = SrtCaller(InetSocketAddress(address, config.port), config.srtStreamId)
            try {
                caller.start()
            } catch (error: IOException) {
                caller.close()
                throw error
            }
            return caller
        }
    }
}
