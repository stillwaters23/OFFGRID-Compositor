package com.offgrid.live

import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PcOutbound(
    private val open: () -> PcTransport,
    private val onStatus: (String) -> Unit,
    private val sendingStatus: String,
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
    private val sleep: (Long) -> Unit = Thread::sleep,
) : AutoCloseable {
    private val queue = ArrayBlockingQueue<PcSample>(QUEUE)
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    private var videoConfig: PcSample.VideoConfig? = null
    private var audioConfig: PcSample.AudioConfig? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        thread = Thread(::loop, "OffgridPcOutput").apply {
            isDaemon = true
            start()
        }
    }

    fun offer(sample: PcSample) {
        if (!running.get()) return
        when (sample) {
            is PcSample.VideoConfig -> videoConfig = sample
            is PcSample.AudioConfig -> audioConfig = sample
            else -> Unit
        }
        while (running.get() && !queue.offer(sample)) {
            queue.poll()
        }
    }

    private fun loop() {
        var transport: PcTransport? = null
        while (running.get()) {
            if (transport == null) {
                transport = connect() ?: break
            }
            val sample = try {
                queue.poll(200, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                if (!running.get()) break
                continue
            } ?: continue
            try {
                transport.write(sample)
            } catch (error: IOException) {
                closeQuietly(transport)
                transport = null
                queue.offer(sample)
                if (running.get()) onStatus(PcOutputConfig.LOST_PC)
                sleep(backoff.nextDelayMillis())
            }
        }
        closeQuietly(transport)
    }

    private fun connect(): PcTransport? {
        while (running.get()) {
            try {
                val transport = open()
                backoff.reset()
                onStatus(sendingStatus)
                videoConfig?.let { transport.write(it) }
                audioConfig?.let { transport.write(it) }
                return transport
            } catch (error: IllegalArgumentException) {
                onStatus(error.message ?: "Only a local PC address is allowed.")
                running.set(false)
                return null
            } catch (_: IOException) {
                if (!running.get()) return null
                onStatus(PcOutputConfig.LOST_PC)
                sleep(backoff.nextDelayMillis())
            }
        }
        return null
    }

    override fun close() {
        running.set(false)
        thread?.interrupt()
        thread?.join(500)
        thread = null
    }

    private fun closeQuietly(transport: PcTransport?) {
        try {
            transport?.close()
        } catch (_: Exception) {
            Unit
        }
    }

    companion object {
        private const val QUEUE = 90

        fun open(config: PcOutputConfig): PcTransport = when (config.protocol) {
            PcOutputProtocol.SRT -> SrtCaller.open(config)
            PcOutputProtocol.RTMP -> RtmpPublisher.open(config)
        }
    }
}
