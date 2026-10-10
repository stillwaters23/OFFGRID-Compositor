package com.offgrid.live

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

internal data class RtmpMessage(
    val type: Int,
    val streamId: Int,
    val timestamp: Int,
    val payload: ByteArray,
)

internal class RtmpChunkIo(
    private val input: InputStream,
    private val output: OutputStream,
) {
    var chunkSize: Int = 128
    private val headers = HashMap<Int, RtmpMessage>()

    fun write(chunkStreamId: Int, type: Int, streamId: Int, timestamp: Int, payload: ByteArray) {
        var offset = 0
        var first = true
        val extended = timestamp >= 0xFFFFFF
        while (offset < payload.size || first) {
            val format = if (first) 0 else 3
            output.write((format shl 6) or chunkStreamId)
            if (first) {
                write3(if (extended) 0xFFFFFF else timestamp)
                write3(payload.size)
                output.write(type)
                writeLittleInt(streamId)
            }
            if (extended) writeInt(timestamp)
            val count = minOf(chunkSize, payload.size - offset).coerceAtLeast(0)
            if (count > 0) output.write(payload, offset, count)
            offset += count
            first = false
            if (payload.isEmpty()) break
        }
        output.flush()
    }

    fun read(): RtmpMessage {
        while (true) {
            val basic = input.read()
            if (basic < 0) throw EOFException("RTMP connection closed")
            val format = basic shr 6
            val chunkStreamId = basic and 0x3F
            val previous = headers[chunkStreamId]
            var timestamp = previous?.timestamp ?: 0
            var size = previous?.payload?.size ?: 0
            var type = previous?.type ?: 0
            var streamId = previous?.streamId ?: 0
            when (format) {
                0 -> {
                    val stampField = read3()
                    size = read3()
                    type = input.read()
                    streamId = readLittleInt()
                    timestamp = if (stampField == 0xFFFFFF) readInt() else stampField
                }
                1 -> {
                    val deltaField = read3()
                    size = read3()
                    type = input.read()
                    val delta = if (deltaField == 0xFFFFFF) readInt() else deltaField
                    timestamp = (previous?.timestamp ?: 0) + delta
                }
                2 -> {
                    val deltaField = read3()
                    val delta = if (deltaField == 0xFFFFFF) readInt() else deltaField
                    timestamp = (previous?.timestamp ?: 0) + delta
                }
                else -> if ((previous?.timestamp ?: 0) >= 0xFFFFFF) readInt()
            }
            val body = ByteArrayOutputStream()
            var filled = 0
            while (filled < size) {
                if (filled > 0) {
                    val next = input.read()
                    if (next < 0) throw EOFException("RTMP connection closed")
                    if (timestamp >= 0xFFFFFF) readInt()
                }
                val count = minOf(chunkSize, size - filled)
                val piece = ByteArray(count)
                readFully(piece)
                body.write(piece)
                filled += count
            }
            val message = RtmpMessage(type, streamId, timestamp, body.toByteArray())
            headers[chunkStreamId] = message
            if (type == 1 && message.payload.size >= 4) {
                chunkSize = readInt(message.payload).coerceAtLeast(1)
            }
            return message
        }
    }

    private fun read3(): Int {
        val one = input.read()
        val two = input.read()
        val three = input.read()
        if (one < 0 || two < 0 || three < 0) throw EOFException("RTMP connection closed")
        return (one shl 16) or (two shl 8) or three
    }

    private fun readInt(): Int {
        val bytes = ByteArray(4)
        readFully(bytes)
        return readInt(bytes)
    }

    private fun readLittleInt(): Int {
        val bytes = ByteArray(4)
        readFully(bytes)
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }

    private fun readFully(bytes: ByteArray) {
        var offset = 0
        while (offset < bytes.size) {
            val count = input.read(bytes, offset, bytes.size - offset)
            if (count < 0) throw EOFException("RTMP connection closed")
            offset += count
        }
    }

    private fun write3(value: Int) {
        output.write(value ushr 16)
        output.write(value ushr 8)
        output.write(value)
    }

    private fun writeInt(value: Int) {
        output.write(value ushr 24)
        output.write(value ushr 16)
        output.write(value ushr 8)
        output.write(value)
    }

    private fun writeLittleInt(value: Int) {
        output.write(value)
        output.write(value ushr 8)
        output.write(value ushr 16)
        output.write(value ushr 24)
    }

    companion object {
        fun readInt(bytes: ByteArray): Int =
            ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
    }
}
