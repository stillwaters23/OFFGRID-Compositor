package com.offgrid.live

import java.io.IOException
import java.net.InetAddress
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean

class CameraRemoteServer(
    bindAddress: InetAddress,
    private val pairingKey: () -> String?,
    private val onSession: (RemoteSession) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val server = LocalSocketServer(bindAddress = bindAddress)
    private var acceptThread: Thread? = null

    val port: Int
        get() = server.localPort

    fun start() {
        if (!running.compareAndSet(false, true)) return
        acceptThread = Thread(::acceptConnections, "OffgridRemoteAccept").apply {
            isDaemon = true
            start()
        }
    }

    private fun acceptConnections() {
        while (running.get()) {
            val key = pairingKey()
            if (!PairingKey.isValid(key ?: "")) {
                onFailure("Make a pairing key before accepting a remote phone.")
                running.set(false)
                break
            }
            try {
                server.acceptSession(key).use(onSession)
            } catch (error: IllegalArgumentException) {
                if (running.get()) onFailure(error.message ?: "Keys don't match")
            } catch (error: IllegalStateException) {
                if (running.get()) onFailure("Remote session expired. Reconnect the remote phone.")
            } catch (error: IOException) {
                if (running.get()) onFailure("Remote phone disconnected.")
            } catch (error: GeneralSecurityException) {
                if (running.get()) onFailure("Secure link could not be verified.")
            }
        }
    }

    override fun close() {
        running.set(false)
        server.close()
        acceptThread?.interrupt()
        acceptThread?.join(500)
        acceptThread = null
    }
}
