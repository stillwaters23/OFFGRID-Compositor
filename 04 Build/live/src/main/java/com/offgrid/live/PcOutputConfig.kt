package com.offgrid.live

import java.net.Inet6Address
import java.net.InetAddress

enum class PcOutputProtocol(val value: String) {
    SRT("srt"),
    RTMP("rtmp"),
}

data class PcOutputConfig(
    val protocol: PcOutputProtocol,
    val host: String,
    val port: Int,
    val width: Int,
    val height: Int,
    val bitrate: Int,
) {
    val url: String
        get() {
            val authorityHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
            return when (protocol) {
                PcOutputProtocol.SRT ->
                    "srt://$authorityHost:$port?mode=caller&streamid=live/offgrid"
                PcOutputProtocol.RTMP ->
                    "rtmp://$authorityHost:$port/live/offgrid"
            }
        }

    companion object {
        const val DEFAULT_PORT = 9000
        const val DEFAULT_BITRATE = 6_000_000
        const val FRAME_RATE = 30
        const val KEYFRAME_INTERVAL_SECONDS = 1

        fun create(
            protocol: String,
            host: String,
            port: String,
            resolution: String,
            bitrate: String,
            resolve: (String) -> Array<InetAddress> = InetAddress::getAllByName,
        ): PcOutputConfig {
            val selectedProtocol = PcOutputProtocol.entries.firstOrNull { it.value == protocol }
                ?: throw IllegalArgumentException("Choose SRT or RTMP.")
            val cleanHost = host.trim().removeSurrounding("[", "]")
            require(cleanHost.isNotEmpty()) { "Type the PC's local IP address." }
            require(cleanHost.none(Char::isWhitespace)) { "Enter a valid PC address." }
            val addresses = try {
                resolve(cleanHost)
            } catch (error: java.net.UnknownHostException) {
                throw IllegalArgumentException("The PC address could not be found.", error)
            }
            require(addresses.isNotEmpty() && addresses.all(LocalAddressPolicy::isAllowed)) {
                "Only a loopback, private, or link-local PC address is allowed."
            }
            val selectedPort = port.toIntOrNull()
                ?: throw IllegalArgumentException("Enter a port from 1 to 65535.")
            require(selectedPort in 1..65535) { "Enter a port from 1 to 65535." }
            val (width, height) = when (resolution) {
                "720p" -> 1280 to 720
                "1080p" -> 1920 to 1080
                else -> throw IllegalArgumentException("Choose 720p or 1080p.")
            }
            val selectedBitrate = bitrate.toIntOrNull()
                ?: throw IllegalArgumentException("Enter a video rate from 1,000,000 to 12,000,000.")
            require(selectedBitrate in MIN_BITRATE..MAX_BITRATE) {
                "Enter a video rate from 1,000,000 to 12,000,000."
            }
            val normalizedHost = when {
                addresses.first() is Inet6Address -> addresses.first().hostAddress.substringBefore('%')
                else -> addresses.first().hostAddress
            }
            return PcOutputConfig(
                selectedProtocol,
                normalizedHost,
                selectedPort,
                width,
                height,
                selectedBitrate,
            )
        }

        private const val MIN_BITRATE = 1_000_000
        private const val MAX_BITRATE = 12_000_000
    }
}

class ReconnectBackoff(
    private val initialMillis: Long = 1_000,
    private val maxMillis: Long = 15_000,
) {
    private var nextMillis = initialMillis

    fun nextDelayMillis(): Long = nextMillis.also {
        nextMillis = (nextMillis * 2).coerceAtMost(maxMillis)
    }

    fun reset() {
        nextMillis = initialMillis
    }
}
