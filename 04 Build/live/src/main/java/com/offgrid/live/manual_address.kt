package com.offgrid.live

import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * The always-works fallback for the two-phone link: the remote phone types the camera phone's
 * address, like 192.168.1.5:9000, as shown in the camera phone's Pairing screen.
 *
 * Only a dotted IPv4 number on the private Wi-Fi is accepted. Names are never looked up, so no
 * request leaves the phone, and public addresses are refused the same way the socket code does.
 */
object ManualAddress {
    /** Turns typed text into a local socket address, or throws [IllegalArgumentException] with plain words. */
    fun parse(text: String, defaultPort: Int = CameraRemoteServer.DEFAULT_PORT): InetSocketAddress {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Type the camera phone's address, like 192.168.1.5:$defaultPort." }
        val colon = trimmed.lastIndexOf(':')
        val host = if (colon >= 0) trimmed.substring(0, colon) else trimmed
        val portText = if (colon >= 0) trimmed.substring(colon + 1) else defaultPort.toString()
        val port = portText.toIntOrNull()
        require(port != null && port in 1..65535) { "The port must be a number from 1 to 65535." }
        val parts = host.split('.')
        require(parts.size == 4 && parts.all { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) }) {
            "Type the address as four numbers, like 192.168.1.5."
        }
        val bytes = ByteArray(4)
        for (index in 0 until 4) {
            val value = parts[index].toInt()
            require(value in 0..255) { "Each part of the address is 0 to 255." }
            bytes[index] = value.toByte()
        }
        val address = InetAddress.getByAddress(bytes)
        require(LocalAddressPolicy.isAllowed(address)) { "Only private Wi-Fi addresses are allowed, like 192.168.x.x or 10.x.x.x." }
        return InetSocketAddress(address, port)
    }

    /** The line the camera phone shows next to its pairing key, like "192.168.1.5:9000". */
    fun describe(address: InetAddress, port: Int): String = "${address.hostAddress}:$port"
}
