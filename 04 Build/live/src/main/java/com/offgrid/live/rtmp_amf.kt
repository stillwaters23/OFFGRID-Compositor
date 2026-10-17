package com.offgrid.live

internal sealed interface AmfValue {
    data class Text(val value: String) : AmfValue
    data class Num(val value: Double) : AmfValue
    data object NullValue : AmfValue
    data class Bool(val value: Boolean) : AmfValue
    data object Other : AmfValue
}

internal object RtmpAmf {
    fun command(name: String, transaction: Double, vararg rest: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(string(name))
        out.write(number(transaction))
        rest.forEach(out::write)
        return out.toByteArray()
    }

    fun string(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 0xFFFF)
        val out = ByteArray(3 + bytes.size)
        out[0] = 0x02
        out[1] = (bytes.size shr 8).toByte()
        out[2] = bytes.size.toByte()
        bytes.copyInto(out, 3)
        return out
    }

    fun number(value: Double): ByteArray {
        val bits = java.lang.Double.doubleToLongBits(value)
        val out = ByteArray(9)
        out[0] = 0x00
        for (shift in 0 until 8) {
            out[1 + shift] = (bits ushr (56 - 8 * shift)).toByte()
        }
        return out
    }

    fun nullValue(): ByteArray = byteArrayOf(0x05)

    fun properties(values: List<Pair<String, String>>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(0x03)
        for ((name, value) in values) {
            val key = name.toByteArray(Charsets.UTF_8)
            out.write(key.size shr 8)
            out.write(key.size and 0xFF)
            out.write(key)
            out.write(string(value))
        }
        out.write(0)
        out.write(0)
        out.write(0x09)
        return out.toByteArray()
    }

    fun readAll(payload: ByteArray): List<AmfValue> {
        val values = ArrayList<AmfValue>()
        var index = 0
        while (index < payload.size) {
            val next = read(payload, index) ?: break
            values += next.first
            index = next.second
        }
        return values
    }

    private fun read(payload: ByteArray, start: Int): Pair<AmfValue, Int>? {
        if (start >= payload.size) return null
        return when (payload[start].toInt() and 0xFF) {
            0x00 -> {
                if (start + 9 > payload.size) return null
                val bits = (0 until 8).fold(0L) { acc, shift ->
                    acc or ((payload[start + 1 + shift].toLong() and 0xFF) shl (56 - 8 * shift))
                }
                AmfValue.Num(java.lang.Double.longBitsToDouble(bits)) to start + 9
            }
            0x01 -> AmfValue.Bool(payload.getOrNull(start + 1)?.toInt() == 1) to start + 2
            0x02 -> {
                if (start + 3 > payload.size) return null
                val length = ((payload[start + 1].toInt() and 0xFF) shl 8) or (payload[start + 2].toInt() and 0xFF)
                if (start + 3 + length > payload.size) return null
                AmfValue.Text(payload.copyOfRange(start + 3, start + 3 + length).toString(Charsets.UTF_8)) to start + 3 + length
            }
            0x05 -> AmfValue.NullValue to start + 1
            0x03 -> skipObject(payload, start + 1)?.let { AmfValue.Other to it }
            0x08 -> {
                if (start + 5 > payload.size) return null
                skipObject(payload, start + 5)?.let { AmfValue.Other to it }
            }
            else -> AmfValue.Other to payload.size
        }
    }

    private fun skipObject(payload: ByteArray, start: Int): Int? {
        var index = start
        while (index + 2 < payload.size) {
            if (payload[index] == 0.toByte() && payload[index + 1] == 0.toByte() && payload[index + 2] == 0x09.toByte()) {
                return index + 3
            }
            val keyLength = ((payload[index].toInt() and 0xFF) shl 8) or (payload[index + 1].toInt() and 0xFF)
            index += 2 + keyLength
            val value = read(payload, index) ?: return null
            index = value.second
        }
        return null
    }
}
