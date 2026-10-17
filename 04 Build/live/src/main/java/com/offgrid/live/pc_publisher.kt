package com.offgrid.live

class PcPublisher(
    private val config: PcOutputConfig,
    private val host: OffgridHost,
    private val onStatus: (String) -> Unit,
) : AutoCloseable {
    private val outbound = PcOutbound(open = { PcOutbound.open(config) }, onStatus = onStatus, sendingStatus = config.sendingStatus())
    private var video: PcVideoEncoder? = null
    private var audio: SilentAacEncoder? = null

    fun start() {
        outbound.start()
        video = PcVideoEncoder(config, host, outbound::offer, onStatus).also { it.start() }
        audio = SilentAacEncoder(outbound::offer, onStatus).also { it.start() }
    }

    override fun close() {
        video?.close()
        audio?.close()
        video = null
        audio = null
        outbound.close()
    }
}
