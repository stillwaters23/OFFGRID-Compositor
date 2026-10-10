package com.offgrid.live

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcTransportTest {
    @Test
    fun pairingKeyIsLongAndGeneratedSecurely() {
        val source = java.io.File("src/main/java/com/offgrid/live/PairingKey.kt").readText()
        assertTrue(source.contains("SecureRandom"))
        assertFalse(source.contains("java.util.Random("))
        assertTrue(PairingKey.LENGTH >= 100)
        assertTrue(PairingKey.isValid(PairingKey.generate()))
    }

    @Test
    fun liveSourcesDoNotLogPairingKeys() {
        val root = java.io.File("src/main/java")
        val logged = root.walkTopDown().filter { it.extension == "kt" }.filter { file ->
            val text = file.readText()
            text.contains("android.util.Log") && text.contains("pairingKey")
        }.map { it.path }.toList()
        assertTrue(logged.joinToString(), logged.isEmpty())
        val key = PairingKey.generate()
        val line = LiveLog.redact("status $key fingerprint", key)
        assertFalse(line.contains(key))
        assertTrue(line.contains("[redacted]"))
    }

    @Test
    fun liveActionsCoverOverlayCleanPairingAndRemote() {
        assertTrue(LiveActions.handles(LiveActions.OPEN_OVERLAY))
        assertTrue(LiveActions.handles(LiveActions.CLEAN_OUTPUT))
        assertTrue(LiveActions.handles(LiveActions.OPEN_PAIRING))
        assertTrue(LiveActions.handles(LiveActions.OPEN_PREVIEW))
        assertTrue(LiveActions.handles(LiveActions.SEND_TO_PC))
        assertFalse(LiveActions.handles("FIRE"))
    }

    @Test
    fun h264TagsAndMpegTsPacketsMatchTheWireLayout() {
        val sps = byteArrayOf(0x67, 0x42, 0x00, 0x1F, 0x01)
        val pps = byteArrayOf(0x68, 0xCE.toByte(), 0x06)
        val record = H264Payload.avcDecoderConfiguration(sps, pps)
        val parsed = H264Payload.codecConfig(record)
        assertTrue(parsed != null && parsed.first.contentEquals(sps) && parsed.second.contentEquals(pps))
        val tag = H264Payload.flvVideo(true, 0, record)
        assertEquals(0x17, tag[0].toInt() and 0xFF)
        val annex = H264Payload.annexB(listOf(sps), true)
        assertEquals(0, annex[0].toInt())
        assertEquals(1, annex[3].toInt())
        val adts = H264Payload.adts(byteArrayOf(0x21, 0x10))
        assertEquals(0xFF, adts[0].toInt() and 0xFF)
        assertEquals(adts.size, ((adts[3].toInt() and 0x03) shl 11) or ((adts[4].toInt() and 0xFF) shl 3) or ((adts[5].toInt() and 0xE0) shr 5))
        val mux = MpegTsMux()
        assertEquals("0376e6e7", mux.crc("123456789".toByteArray()).joinToString("") { "%02x".format(it) })
        val video = mux.video(annex, 9000, true)
        assertEquals(0, video.size % MpegTsMux.PACKET)
        assertEquals(0x47, video[0].toInt() and 0xFF)
        assertTrue(video.any { it == 0x47.toByte() })
    }

    @Test
    fun publicAddressesNeverOpenATransport() {
        val error = runCatching { RtmpPublisher.localAddress("8.8.8.8") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun reconnectReportsTheLostPcAndThenSends() {
        val statuses = ArrayList<String>()
        val sleeps = ArrayList<Long>()
        val opens = AtomicInteger()
        val sent = CountDownLatch(1)
        val outbound = PcOutbound(
            open = {
                if (opens.incrementAndGet() < 3) throw java.io.IOException("down")
                object : PcTransport {
                    override fun write(sample: PcSample) {
                        sent.countDown()
                    }
                    override fun close() = Unit
                }
            },
            onStatus = { statuses += it },
            sendingStatus = "Sending to PC 127.0.0.1 (SRT)",
            sleep = { sleeps += it },
        )
        outbound.start()
        outbound.offer(PcSample.Video(listOf(byteArrayOf(0x65, 1)), 0, true))
        assertTrue(sent.await(3, TimeUnit.SECONDS))
        outbound.close()
        assertTrue(statuses.contains(PcOutputConfig.LOST_PC))
        assertTrue(statuses.contains("Sending to PC 127.0.0.1 (SRT)"))
        assertEquals(listOf(1_000L, 2_000L), sleeps)
    }

    @Test
    fun rtmpPublishCarriesAnH264KeyFrame() {
        val loopback = InetAddress.getByName("127.0.0.1")
        val video = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0, 1, loopback).use { server ->
            val seen = executor.submit<String> {
                server.accept().use { socket ->
                    socket.soTimeout = 4_000
                    val input = DataInputStream(socket.getInputStream())
                    val output = DataOutputStream(socket.getOutputStream())
                    assertEquals(3, input.readUnsignedByte())
                    val c1 = ByteArray(1536)
                    input.readFully(c1)
                    val s1 = ByteArray(1536)
                    output.write(3)
                    output.write(s1)
                    output.write(c1)
                    output.flush()
                    input.readFully(ByteArray(1536))
                    val chunks = RtmpChunkIo(input, output)
                    var published = ""
                    var videoSeen = false
                    while (true) {
                        val message = try {
                            chunks.read()
                        } catch (_: EOFException) {
                            break
                        } catch (_: java.net.SocketException) {
                            break
                        }
                        if (message.type == 9 && message.payload.size > 5 && message.payload[1].toInt() == 1) {
                            videoSeen = true
                            video.countDown()
                            continue
                        }
                        if (message.type != 20) continue
                        val values = RtmpAmf.readAll(message.payload)
                        val name = (values.firstOrNull() as? AmfValue.Text)?.value ?: continue
                        if (name == "connect") {
                            chunks.write(3, 20, 0, 0, RtmpAmf.command("_result", 1.0, RtmpAmf.properties(emptyList())))
                        } else if (name == "createStream") {
                            chunks.write(3, 20, 0, 0, RtmpAmf.command("_result", 2.0, RtmpAmf.nullValue(), RtmpAmf.number(1.0)))
                        } else if (name == "publish") {
                            published = values.filterIsInstance<AmfValue.Text>().joinToString(" ") { it.value }
                        }
                    }
                    check(videoSeen)
                    published
                }
            }
            try {
                val config = PcOutputConfig(PcOutputProtocol.RTMP, "127.0.0.1", server.localPort, 1280, 720, 6_000_000)
                RtmpPublisher.open(config).use { publisher ->
                    publisher.write(PcSample.VideoConfig(byteArrayOf(0x67, 0x42, 0, 0x1F), byteArrayOf(0x68, 0x01)))
                    publisher.write(PcSample.Video(listOf(byteArrayOf(0x65, 0x11)), 33_000, true))
                    assertTrue(video.await(4, TimeUnit.SECONDS))
                }
                assertTrue(seen.get(4, TimeUnit.SECONDS).contains("offgrid"))
            } finally {
                server.close()
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun srtCallerHandshakesAndSendsMpegTs() {
        val loopback = InetAddress.getByName("127.0.0.1")
        val listener = DatagramSocket(0, loopback)
        listener.soTimeout = 4_000
        val executor = Executors.newSingleThreadExecutor()
        val config = PcOutputConfig(PcOutputProtocol.SRT, "127.0.0.1", listener.localPort, 1280, 720, 6_000_000)
        val caller = SrtCaller(InetSocketAddress(loopback, listener.localPort), config.srtStreamId)
        val started = executor.submit<Unit> { caller.start() }
        try {
            val induction = nextDatagram(listener)
            assertEquals(4, intAt(induction, 16))
            assertEquals(2, shortAt(induction, 22))
            assertEquals(1, intAt(induction, 36))
            val callerId = intAt(induction, 40)
            reply(listener, induction, handshake(5, 0x4A17, 1, 0x11112222, 0x00C0FFEE, callerId))
            val conclusion = nextDatagram(listener)
            assertEquals(5, intAt(conclusion, 16))
            assertEquals(0x00C0FFEE, intAt(conclusion, 44))
            assertTrue(shortAt(conclusion, 22) and 0x0001 != 0)
            assertTrue(String(conclusion, Charsets.UTF_8).contains("live/offgrid"))
            reply(listener, conclusion, handshake(5, 0x0001, -1, 0x11112222, 0x00C0FFEE, callerId))
            started.get(4, TimeUnit.SECONDS)
            caller.write(PcSample.Video(listOf(byteArrayOf(0x65, 0x22)), 0, true))
            val data = nextDatagram(listener)
            assertTrue(data.drop(16).any { it == 0x47.toByte() })
        } finally {
            caller.close()
            listener.close()
            executor.shutdownNow()
        }
    }

    private fun nextDatagram(socket: DatagramSocket): ByteArray {
        val packet = DatagramPacket(ByteArray(2048), 2048)
        socket.receive(packet)
        return packet.data.copyOf(packet.length).also { lastPacket = packet }
    }

    private var lastPacket: DatagramPacket? = null

    private fun reply(socket: DatagramSocket, request: ByteArray, response: ByteArray) {
        val from = lastPacket ?: return
        socket.send(DatagramPacket(response, response.size, from.address, from.port))
        request.size
    }

    private fun handshake(version: Int, extension: Int, type: Int, socketId: Int, cookie: Int, destination: Int): ByteArray {
        val packet = ByteArray(64)
        putInt(packet, 0, 0x80000000.toInt())
        putInt(packet, 12, destination)
        putInt(packet, 16, version)
        putShort(packet, 22, extension)
        putInt(packet, 28, 1500)
        putInt(packet, 32, 8192)
        putInt(packet, 36, type)
        putInt(packet, 40, socketId)
        putInt(packet, 44, cookie)
        return packet
    }

    private fun putInt(target: ByteArray, index: Int, value: Int) {
        ByteBuffer.wrap(target).putInt(index, value)
    }

    private fun putShort(target: ByteArray, index: Int, value: Int) {
        target[index] = (value ushr 8).toByte()
        target[index + 1] = value.toByte()
    }

    private fun intAt(buffer: ByteArray, index: Int): Int = ByteBuffer.wrap(buffer).getInt(index)

    private fun shortAt(buffer: ByteArray, index: Int): Int =
        ((buffer[index].toInt() and 0xFF) shl 8) or (buffer[index + 1].toInt() and 0xFF)
}
