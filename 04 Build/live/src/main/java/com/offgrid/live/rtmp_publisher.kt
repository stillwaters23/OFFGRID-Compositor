package com.offgrid.live

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom

class RtmpPublisher(
    private val socket: Socket,
    private val appName: String,
    private val streamName: String,
    private val tcUrl: String,
) : PcTransport {
    private var io: RtmpChunkIo? = null
    private var streamId: Int = 0
    private var videoConfigSent = false
    private var audioConfigSent = false

    fun start() {
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())
        val c1 = ByteArray(HANDSHAKE)
        SecureRandom().nextBytes(c1)
        c1.fill(0, 0, 8)
        output.write(3)
        output.write(c1)
        output.flush()
        val version = input.readUnsignedByte()
        if (version != 3) throw IOException("RTMP version was refused.")
        val s1 = ByteArray(HANDSHAKE)
        input.readFully(s1)
        val s2 = ByteArray(HANDSHAKE)
        input.readFully(s2)
        if (!s2.contentEquals(c1)) throw IOException("RTMP handshake did not echo.")
        output.write(s1)
        output.flush()
        val chunks = RtmpChunkIo(input, output)
        io = chunks
        chunks.write(2, 1, 0, 0, intBytes(OUT_CHUNK))
        chunks.chunkSize = OUT_CHUNK
        chunks.write(2, 5, 0, 0, intBytes(WINDOW))
        chunks.write(3, 20, 0, 0, connectCommand())
        val connected = readCommand(chunks, 1.0)
        if (connected == "_error") throw IOException("RTMP connect was refused.")
        chunks.write(
            3,
            20,
            0,
            0,
            RtmpAmf.command("createStream", 2.0, RtmpAmf.nullValue()),
        )
        readCommand(chunks, 2.0)
        if (streamId == 0) streamId = 1
        chunks.write(8, 20, streamId, 0, publishCommand())
    }

    override fun write(sample: PcSample) {
        val chunks = io ?: throw IOException("RTMP is not connected.")
        when (sample) {
            is PcSample.VideoConfig -> {
                val record = H264Payload.avcDecoderConfiguration(sample.sps, sample.pps)
                chunks.write(6, 9, streamId, 0, H264Payload.flvVideo(true, 0, record))
                videoConfigSent = true
            }
            is PcSample.Video -> {
                if (!videoConfigSent) return
                val stamp = (sample.timeUs / 1000L).toInt().coerceAtLeast(0)
                val payload = H264Payload.flvVideo(sample.keyframe, 1, H264Payload.avcc(sample.nalus))
                chunks.write(6, 9, streamId, stamp, payload)
            }
            is PcSample.AudioConfig -> {
                chunks.write(4, 8, streamId, 0, H264Payload.flvAudio(0, sample.asc))
                audioConfigSent = true
            }
            is PcSample.Audio -> {
                if (!audioConfigSent) return
                val stamp = (sample.timeUs / 1000L).toInt().coerceAtLeast(0)
                chunks.write(4, 8, streamId, stamp, H264Payload.flvAudio(1, sample.frame))
            }
        }
    }

    override fun close() {
        try {
            socket.close()
        } catch (_: IOException) {
            Unit
        }
    }

    private fun connectCommand(): ByteArray = RtmpAmf.command(
        "connect",
        1.0,
        RtmpAmf.properties(
            listOf(
                "app" to appName,
                "type" to "nonprivate",
                "flashVer" to "FMLE/3.0 (compatible; OFFGRID)",
                "tcUrl" to tcUrl,
            ),
        ),
    )

    private fun publishCommand(): ByteArray = RtmpAmf.command(
        "publish",
        0.0,
        RtmpAmf.nullValue(),
        RtmpAmf.string(streamName),
        RtmpAmf.string("live"),
    )

    private fun readCommand(chunks: RtmpChunkIo, transaction: Double): String {
        while (true) {
            val message = try {
                chunks.read()
            } catch (error: EOFException) {
                throw IOException("RTMP connection closed.", error)
            }
            if (message.type != 20) continue
            val values = RtmpAmf.readAll(message.payload)
            val name = (values.getOrNull(0) as? AmfValue.Text)?.value ?: continue
            val id = (values.getOrNull(1) as? AmfValue.Num)?.value ?: continue
            if (id != transaction) continue
            values.filterIsInstance<AmfValue.Num>().getOrNull(1)?.let { streamId = it.value.toInt() }
            return name
        }
    }

    companion object {
        private const val HANDSHAKE = 1536
        private const val OUT_CHUNK = 4096
        private const val WINDOW = 2_500_000

        fun open(config: PcOutputConfig): RtmpPublisher {
            val address = localAddress(config.host)
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = 4_000
            socket.connect(InetSocketAddress(address, config.port), 4_000)
            val publisher = RtmpPublisher(socket, config.rtmpApp, config.rtmpStream, config.url)
            try {
                publisher.start()
            } catch (error: IOException) {
                socket.close()
                throw error
            }
            socket.soTimeout = 0
            return publisher
        }

        fun localAddress(host: String): InetAddress {
            val address = InetAddress.getByName(host)
            LocalAddressPolicy.requireAllowed(address)
            return address
        }

        private fun intBytes(value: Int): ByteArray = byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
    }
}
