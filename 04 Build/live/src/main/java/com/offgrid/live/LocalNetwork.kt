package com.offgrid.live

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiP2pConfig
import android.net.wifi.WifiP2pManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.io.DataInputStream
import java.io.DataOutputStream

object LocalAddressPolicy {
    fun isAllowed(address: InetAddress): Boolean =
        address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress

    fun requireAllowed(address: InetAddress) {
        require(isAllowed(address)) { "Only local network addresses are allowed" }
    }
}

class RemoteServiceDiscovery(context: Context) {
    private val manager = context.getSystemService(NsdManager::class.java)
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null

    fun advertise(port: Int) {
        stopAdvertising()
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = listener
        manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopAdvertising() {
        registration?.let { manager.unregisterService(it) }
        registration = null
    }

    fun discover(onFound: (InetSocketAddress) -> Unit) {
        stopDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = stopDiscovery()
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = stopDiscovery()
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val address = info.host ?: return
                        if (LocalAddressPolicy.isAllowed(address)) {
                            onFound(InetSocketAddress(address, info.port))
                        }
                    }
                })
            }
        }
        discovery = listener
        manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopDiscovery() {
        discovery?.let { manager.stopServiceDiscovery(it) }
        discovery = null
    }

    companion object {
        const val SERVICE_TYPE = "_offgrid._tcp."
        const val SERVICE_NAME = "OFFGRID Remote"
    }
}

class WifiDirectDiscovery(context: Context) {
    private val manager = context.getSystemService(WifiP2pManager::class.java)
    private val channel = manager.initialize(context, context.mainLooper, null)

    fun discover(onStarted: (Boolean) -> Unit) {
        manager.discoverPeers(channel, resultListener(onStarted))
    }

    fun connect(deviceAddress: String, onStarted: (Boolean) -> Unit) {
        val config = WifiP2pConfig().apply { this.deviceAddress = deviceAddress }
        manager.connect(channel, config, resultListener(onStarted))
    }

    private fun resultListener(onStarted: (Boolean) -> Unit) = object : WifiP2pManager.ActionListener {
        override fun onSuccess() = onStarted(true)
        override fun onFailure(reason: Int) = onStarted(false)
    }

    private companion object {
    }
}

class LocalSocketServer(
    port: Int = 0,
    bindAddress: InetAddress = localAddress(),
) : AutoCloseable {
    private val server = ServerSocket(port, 1, bindAddress)

    val localPort: Int
        get() = server.localPort

    fun accept(): LocalMessageConnection {
        val socket = server.accept()
        LocalAddressPolicy.requireAllowed(socket.inetAddress)
        return LocalMessageConnection(socket)
    }

    override fun close() = server.close()

    private companion object {
        fun localAddress(): InetAddress =
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?: throw IllegalStateException("No private Wi-Fi address is available")
    }
}

class LocalMessageConnection(private val socket: Socket) : AutoCloseable {
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())

    fun send(channel: EncryptedChannel, message: RemoteMessage) {
        val payload = channel.encrypt(message.encode())
        output.writeInt(payload.size)
        output.write(payload)
        output.flush()
    }

    fun receive(channel: EncryptedChannel): RemoteMessage {
        val size = input.readInt()
        require(size in MIN_FRAME_SIZE..MAX_FRAME_SIZE)
        val encrypted = ByteArray(size)
        input.readFully(encrypted)
        return RemoteMessage.decode(channel.decrypt(encrypted))
    }

    override fun close() = socket.close()

    companion object {
        private const val MIN_FRAME_SIZE = 24
        private const val MAX_FRAME_SIZE = 16 * 1024 * 1024
    }
}
