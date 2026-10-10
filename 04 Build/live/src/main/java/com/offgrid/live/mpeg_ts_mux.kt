package com.offgrid.live

internal class MpegTsMux {
    private val continuity = HashMap<Int, Int>()

    fun video(annexB: ByteArray, pts90k: Long, keyframe: Boolean): ByteArray {
        val packets = ArrayList<ByteArray>()
        if (keyframe) {
            packets += section(PID_PAT, pat())
            packets += section(PID_PMT, pmt())
        }
        packets += pes(PID_VIDEO, 0xE0, annexB, pts90k, keyframe)
        return packets.fold(ByteArray(0)) { all, packet -> all + packet }
    }

    fun audio(adts: ByteArray, pts90k: Long): ByteArray =
        pes(PID_AUDIO, 0xC0, adts, pts90k, pcr = false).fold(ByteArray(0)) { all, packet -> all + packet }

    fun section(pid: Int, table: ByteArray): ByteArray {
        val payload = ByteArray(1 + table.size)
        payload[0] = 0
        table.copyInto(payload, 1)
        return packet(pid, payload, start = true, pcr90k = null)
    }

    private fun pes(pid: Int, streamId: Int, payload: ByteArray, pts90k: Long, pcr: Boolean): List<ByteArray> {
        val header = pesHeader(streamId, payload.size, pts90k)
        val full = header + payload
        val packets = ArrayList<ByteArray>()
        var offset = 0
        var first = true
        while (offset < full.size) {
            val room = if (first && pcr) 176 else 184
            val count = minOf(room, full.size - offset)
            val piece = full.copyOfRange(offset, offset + count)
            packets += packet(pid, piece, start = first, pcr90k = if (first && pcr) pts90k else null)
            offset += count
            first = false
        }
        return packets
    }

    private fun packet(pid: Int, payload: ByteArray, start: Boolean, pcr90k: Long?): ByteArray {
        val packet = ByteArray(PACKET)
        packet[0] = 0x47
        packet[1] = ((if (start) 0x40 else 0) or (pid shr 8)).toByte()
        packet[2] = pid.toByte()
        val counter = continuity.getOrDefault(pid, 0) and 0x0F
        continuity[pid] = counter + 1
        val adapt = pcr90k != null || payload.size < 184
        packet[3] = ((if (adapt) 0x30 else 0x10) or counter).toByte()
        var index = 4
        if (adapt) {
            val adaptLength = 183 - payload.size
            packet[index++] = adaptLength.coerceAtLeast(0).toByte()
            if (adaptLength > 0) {
                if (pcr90k != null) {
                    packet[index++] = 0x10
                    writePcr(packet, index, pcr90k)
                    index += 6
                    for (pad in index until 4 + 1 + adaptLength) packet[pad] = 0xFF.toByte()
                    index = 4 + 1 + adaptLength
                } else {
                    packet[index++] = 0
                    for (pad in index until 4 + 1 + adaptLength) packet[pad] = 0xFF.toByte()
                    index = 4 + 1 + adaptLength
                }
            }
        }
        payload.copyInto(packet, index)
        return packet
    }

    private fun pesHeader(streamId: Int, payloadSize: Int, pts90k: Long): ByteArray {
        val packetLength = (3 + 5 + payloadSize).coerceAtMost(0xFFFF)
        val header = ByteArray(9 + 5)
        header[0] = 0
        header[1] = 0
        header[2] = 1
        header[3] = streamId.toByte()
        header[4] = (packetLength shr 8).toByte()
        header[5] = packetLength.toByte()
        header[6] = 0x80.toByte()
        header[7] = 0x80.toByte()
        header[8] = 5
        writePts(header, 9, pts90k)
        return header
    }

    fun pat(): ByteArray {
        val body = byteArrayOf(
            0x00, 0xB0.toByte(), 0x0D, 0x00, 0x01, 0xC1.toByte(), 0x00, 0x00,
            0x00, 0x01, 0xE1.toByte(), 0x00,
        )
        return body + crc(body)
    }

    fun pmt(): ByteArray {
        val body = byteArrayOf(
            0x02, 0xB0.toByte(), 0x17, 0x00, 0x01, 0xC1.toByte(), 0x00, 0x00,
            0xE1.toByte(), 0x00, 0xF0.toByte(), 0x00,
            0x1B, 0xE1.toByte(), 0x00, 0xF0.toByte(), 0x00,
            0x0F, 0xE1.toByte(), 0x01, 0xF0.toByte(), 0x00,
        )
        return body + crc(body)
    }

    fun crc(data: ByteArray): ByteArray {
        var value = 0xFFFFFFFF.toInt()
        for (byte in data) {
            value = value xor ((byte.toInt() and 0xFF) shl 24)
            repeat(8) {
                value = if (value and 0x80000000.toInt() != 0) (value shl 1) xor 0x04C11DB7 else value shl 1
            }
        }
        return byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )
    }

    private fun writePts(target: ByteArray, index: Int, pts90k: Long) {
        val pts = pts90k and 0x1FFFFFFFFL
        target[index] = ((0x20L or ((pts shr 29) and 0x0EL) or 1L) and 0xFF).toByte()
        target[index + 1] = ((pts shr 22) and 0xFF).toByte()
        target[index + 2] = ((((pts shr 14) and 0xFE) or 1L) and 0xFF).toByte()
        target[index + 3] = ((pts shr 7) and 0xFF).toByte()
        target[index + 4] = ((((pts shl 1) and 0xFE) or 1L) and 0xFF).toByte()
    }

    private fun writePcr(target: ByteArray, index: Int, pcr90k: Long) {
        val base = pcr90k and 0x1FFFFFFFFL
        target[index] = (base shr 25).toByte()
        target[index + 1] = (base shr 17).toByte()
        target[index + 2] = (base shr 9).toByte()
        target[index + 3] = (base shr 1).toByte()
        target[index + 4] = (((base and 1) shl 7) or 0x7E).toByte()
        target[index + 5] = 0
    }

    companion object {
        const val PACKET = 188
        const val PID_PAT = 0
        const val PID_PMT = 0x1000
        const val PID_VIDEO = 0x100
        const val PID_AUDIO = 0x101
    }
}
