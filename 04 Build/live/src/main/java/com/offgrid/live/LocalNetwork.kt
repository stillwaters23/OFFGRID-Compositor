package com.offgrid.live

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiP2pConfig
import android.net.wifi.WifiP2pManager
import android.net.wifi.WifiManager
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

class RemoteServiceDiscovery(
    context: Context,
    private val onFailure: (String) -> Unit,
) {
    private val manager = context.getSystemService(NsdManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun advertise(port: Int) {
        require(port in 1..65535)
        stopAdvertising()
        val info = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registration = null
                onFailure("Could not advertise the OFFGRID remote on this network.")
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                onFailure("Could not stop local remote discovery.")
            }
        }
        registration = listener
        try {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: SecurityException) {
            registration = null
            onFailure("Local network permission is missing.")
        }
    }

    fun stopAdvertising() {
        registration?.let {
            try {
                manager.unregisterService(it)
            } catch (error: IllegalArgumentException) {
                onFailure("Could not stop local remote discovery.")
            }
        }
        registration = null
    }

    fun discover(onFound: (InetSocketAddress) -> Unit) {
        stopDiscovery()
        multicastLock = wifiManager.createMulticastLock("offgrid-remote-discovery").apply {
            setReferenceCounted(false)
            acquire()
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onFailure("Could not search this Wi-Fi network.")
                discovery = null
                releaseMulticastLock()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                onFailure("Could not stop local remote discovery.")
                discovery = null
                releaseMulticastLock()
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        onFailure("Could not find the remote phone on this network.")
                    }
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
        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: SecurityException) {
            discovery = null
            releaseMulticastLock()
            onFailure("Local network permission is missing.")
        }
    }

    fun stopDiscovery() {
        discovery?.let {
            try {
                manager.stopServiceDiscovery(it)
            } catch (error: IllegalArgumentException) {
                onFailure("Could not stop local remote discovery.")
            }
        }
        discovery = null
        releaseMulticastLock()
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
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

    fun groupOwnerAddress(onResolved: (InetAddress?) -> Unit) {
        manager.requestConnectionInfo(channel) { info ->
            val address = info.groupOwnerAddress
            onResolved(address?.takeIf(LocalAddressPolicy::isAllowed))
        }
    }

    private fun resultListener(onStarted: (Boolean) -> Unit) = object : WifiP2pManager.ActionListener {
        override fun onSuccess() = onStarted(true)
        override fun onFailure(reason: Int) = onStarted(false)
    }

}

class LocalSocketServer(
    port: Int = 0,
    bindAddress: InetAddress = localAddress(),
) : AutoCloseable {
    private val server = bind(port, bindAddress)

    val localPort: Int
        get() = server.localPort

    fun accept(): LocalMessageConnection {
        return LocalMessageConnection(acceptSocket())
    }

    fun acceptSession(pairingKey: String): RemoteSession =
        RemoteSession.establish(acceptSocket(), pairingKey, false)

    private fun acceptSocket(): Socket {
        val socket = server.accept()
        try {
            LocalAddressPolicy.requireAllowed(socket.inetAddress)
        } catch (error: IllegalArgumentException) {
            socket.close()
            throw error
        }
        return socket
    }

    override fun close() = server.close()

    private companion object {
        fun localAddress(): InetAddress =
            (NetworkInterface.getNetworkInterfaces()
                ?: throw IllegalStateException("No private Wi-Fi address is available"))
                .toList()
                .asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?: throw IllegalStateException("No private Wi-Fi address is available")

        fun bind(port: Int, address: InetAddress): ServerSocket {
            LocalAddressPolicy.requireAllowed(address)
            return ServerSocket(port, 1, address)
        }
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
