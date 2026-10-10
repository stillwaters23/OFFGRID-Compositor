package com.offgrid.live

object H264Payload {
    fun nalUnits(buffer: ByteArray): List<ByteArray> {
        if (buffer.size >= 4 && buffer[0] == 0.toByte() && buffer[1] == 0.toByte() &&
            (buffer[2] == 1.toByte() || (buffer[2] == 0.toByte() && buffer[3] == 1.toByte()))
        ) {
            return annexBUnits(buffer)
        }
        return avccUnits(buffer)
    }

    fun codecConfig(buffer: ByteArray): Pair<ByteArray, ByteArray>? {
        if (buffer.size >= 7 && buffer[0] == 1.toByte()) {
            return avcDecoderRecord(buffer)
        }
        val units = nalUnits(buffer)
        val sps = units.firstOrNull { nalType(it) == 7 } ?: return null
        val pps = units.firstOrNull { nalType(it) == 8 } ?: return null
        return sps to pps
    }

    fun avcDecoderConfiguration(sps: ByteArray, pps: ByteArray): ByteArray {
        require(sps.isNotEmpty() && pps.isNotEmpty())
        val profile = sps.getOrElse(1) { 0x42 }
        val compat = sps.getOrElse(2) { 0 }
        val level = sps.getOrElse(3) { 0x1F }
        val out = java.io.ByteArrayOutputStream()
        out.write(1)
        out.write(profile.toInt())
        out.write(compat.toInt())
        out.write(level.toInt())
        out.write(0xFF)
        out.write(0xE1)
        out.write(sps.size shr 8)
        out.write(sps.size and 0xFF)
        out.write(sps)
        out.write(1)
        out.write(pps.size shr 8)
        out.write(pps.size and 0xFF)
        out.write(pps)
        return out.toByteArray()
    }

    fun annexB(units: List<ByteArray>, keyframe: Boolean): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(START)
        out.write(byteArrayOf(0x09, if (keyframe) 0x10 else 0x30))
        for (unit in units) {
            if (unit.isEmpty()) continue
            out.write(START)
            out.write(unit)
        }
        return out.toByteArray()
    }

    fun avcc(units: List<ByteArray>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (unit in units) {
            out.write(unit.size ushr 24)
            out.write(unit.size ushr 16)
            out.write(unit.size ushr 8)
            out.write(unit.size)
            out.write(unit)
        }
        return out.toByteArray()
    }

    fun flvVideo(keyframe: Boolean, packetType: Int, payload: ByteArray): ByteArray {
        val out = ByteArray(5 + payload.size)
        val frame = if (keyframe || packetType == 0) 0x10 else 0x20
        out[0] = (frame or 0x07).toByte()
        out[1] = packetType.toByte()
        payload.copyInto(out, 5)
        return out
    }

    fun flvAudio(packetType: Int, payload: ByteArray): ByteArray {
        val out = ByteArray(2 + payload.size)
        out[0] = 0xAF.toByte()
        out[1] = packetType.toByte()
        payload.copyInto(out, 2)
        return out
    }

    fun audioSpecificConfig(): ByteArray = byteArrayOf(0x11, 0x88.toByte())

    fun adts(raw: ByteArray, sampleRateIndex: Int = 3, channels: Int = 1): ByteArray {
        val full = raw.size + 7
        val header = ByteArray(7 + raw.size)
        header[0] = 0xFF.toByte()
        header[1] = 0xF1.toByte()
        header[2] = ((1 shl 6) or (sampleRateIndex shl 2) or (channels shr 2)).toByte()
        header[3] = (((channels and 3) shl 6) or (full shr 11)).toByte()
        header[4] = ((full and 0x7FF) shr 3).toByte()
        header[5] = (((full and 7) shl 5) or 0x1F).toByte()
        header[6] = 0xFC.toByte()
        raw.copyInto(header, 7)
        return header
    }

    fun nalType(unit: ByteArray): Int = if (unit.isEmpty()) -1 else unit[0].toInt() and 0x1F

    private fun avcDecoderRecord(record: ByteArray): Pair<ByteArray, ByteArray>? {
        if (record.size < 8) return null
        var index = 5
        val spsCount = record[index].toInt() and 0x1F
        index++
        if (spsCount < 1 || index + 2 > record.size) return null
        val spsLength = readShort(record, index)
        index += 2
        if (spsLength <= 0 || index + spsLength > record.size) return null
        val sps = record.copyOfRange(index, index + spsLength)
        index += spsLength
        if (index >= record.size) return null
        index++
        if (index + 2 > record.size) return null
        val ppsLength = readShort(record, index)
        index += 2
        if (ppsLength <= 0 || index + ppsLength > record.size) return null
        return sps to record.copyOfRange(index, index + ppsLength)
    }

    private fun avccUnits(buffer: ByteArray): List<ByteArray> {
        val units = ArrayList<ByteArray>()
        var index = 0
        while (index + 4 <= buffer.size) {
            val length = ((buffer[index].toInt() and 0xFF) shl 24) or
                ((buffer[index + 1].toInt() and 0xFF) shl 16) or
                ((buffer[index + 2].toInt() and 0xFF) shl 8) or
                (buffer[index + 3].toInt() and 0xFF)
            index += 4
            if (length <= 0 || index + length > buffer.size) break
            units += buffer.copyOfRange(index, index + length)
            index += length
        }
        return units
    }

    private fun annexBUnits(buffer: ByteArray): List<ByteArray> {
        val starts = ArrayList<Int>()
        var index = 0
        while (index + 3 < buffer.size) {
            if (buffer[index] == 0.toByte() && buffer[index + 1] == 0.toByte()) {
                if (buffer[index + 2] == 1.toByte()) {
                    starts += index + 3
                    index += 3
                    continue
                }
                if (index + 3 < buffer.size && buffer[index + 2] == 0.toByte() && buffer[index + 3] == 1.toByte()) {
                    starts += index + 4
                    index += 4
                    continue
                }
            }
            index++
        }
        return starts.mapIndexed { position, start ->
            val end = if (position + 1 < starts.size) {
                var mark = starts[position + 1] - 3
                if (mark > start && buffer[mark - 1] == 0.toByte()) mark--
                mark
            } else {
                buffer.size
            }
            buffer.copyOfRange(start, end.coerceAtLeast(start))
        }.filter { it.isNotEmpty() }
    }

    private fun readShort(buffer: ByteArray, index: Int): Int =
        ((buffer[index].toInt() and 0xFF) shl 8) or (buffer[index + 1].toInt() and 0xFF)

    private val START = byteArrayOf(0, 0, 0, 1)
}
