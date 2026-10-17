package com.offgrid.live

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.GeneralSecurityException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemoteLink(
    private val address: InetSocketAddress,
    private val pairingKey: String,
    private val onState: (String) -> Unit,
    private val onPacket: (RemotePacket) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val reader = Executors.newSingleThreadExecutor { task ->
        Thread(task, "OffgridRemoteLink").apply { isDaemon = true }
    }
    private val writer = ThreadPoolExecutor(
        1,
        1,
        0,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(SEND_QUEUE_SIZE),
        { task -> Thread(task, "OffgridRemoteSend").apply { isDaemon = true } },
    )
    @Volatile
    private var activeSession: RemoteSession? = null

    fun start() {
        check(!closed.get()) { "Remote link is closed." }
        require(PairingKey.isValid(pairingKey))
        require(!address.isUnresolved) { "The camera phone address is not available." }
        LocalAddressPolicy.requireAllowed(checkNotNull(address.address))
        if (!running.compareAndSet(false, true)) return
        reader.execute(::connectUntilClosed)
    }

    fun sendControl(message: RemoteMessage) {
        try {
            writer.execute {
                val session = activeSession
                if (session == null) {
                    onState("The camera phone is not connected.")
                } else {
                    try {
                        session.sendControl(message)
                    } catch (error: IOException) {
                        onState(LOST_MESSAGE)
                        activeSession?.close()
                    } catch (error: IllegalStateException) {
                        onState(LOST_MESSAGE)
                        activeSession?.close()
                    }
                }
            }
        } catch (error: RejectedExecutionException) {
            onState("The remote link is busy.")
        }
    }

    private fun connectUntilClosed() {
        var delayMillis = RETRY_START_MS
        while (running.get()) {
            var connectingSocket: Socket? = null
            try {
                val socket = Socket()
                connectingSocket = socket
                socket.connect(address, CONNECT_TIMEOUT_MS)
                val session = RemoteSession.establish(socket, pairingKey, true)
                activeSession = session
                delayMillis = RETRY_START_MS
                onState("Connected to the camera phone.")
                while (running.get()) onPacket(session.receive())
            } catch (error: IllegalArgumentException) {
                if (error.message == "Keys don't match") {
                    onState("Keys don't match")
                    running.set(false)
                    break
                }
                onState(LOST_MESSAGE)
            } catch (error: IOException) {
                if (running.get()) onState(LOST_MESSAGE)
            } catch (error: IllegalStateException) {
                if (running.get()) onState(LOST_MESSAGE)
            } catch (error: GeneralSecurityException) {
                onState("Secure link could not start.")
                running.set(false)
            } finally {
                activeSession?.close()
                activeSession = null
                try {
                    connectingSocket?.close()
                } catch (_: IOException) {
                }
            }
            if (running.get()) {
                try {
                    Thread.sleep(delayMillis)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
                delayMillis = (delayMillis * 2).coerceAtMost(RETRY_MAX_MS)
            }
        }
    }

    override fun close() {
        closed.set(true)
        running.set(false)
        activeSession?.close()
        reader.shutdownNow()
        writer.shutdownNow()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val RETRY_START_MS = 1_000L
        const val RETRY_MAX_MS = 15_000L
        const val SEND_QUEUE_SIZE = 32
        const val LOST_MESSAGE = "Lost the camera phone. Trying again."
    }
}
