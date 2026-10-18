package com.offgrid.live

object LiveActions {
    const val OPEN_OVERLAY = "LIVE_OPEN_OVERLAY_SETTINGS"
    const val CLEAN_OUTPUT = "LIVE_CLEAN_OUTPUT"
    const val OPEN_PAIRING = "REMOTE_OPEN_PAIRING"
    const val OPEN_PREVIEW = "REMOTE_OPEN_PREVIEW"
    const val SEND_TO_PC = "LIVE_SEND_TO_PC"
    const val COPY_PAIRING_KEY = "COPY_PAIRING_KEY"

    /** Phase 1.3: arms or disarms the Broadcast Lock. CLEAN_OUTPUT is "Lock now". */
    const val LOCK_ARM = "LOCK_ARM"

    val HANDLED = setOf(OPEN_OVERLAY, CLEAN_OUTPUT, OPEN_PAIRING, OPEN_PREVIEW, SEND_TO_PC, COPY_PAIRING_KEY, LOCK_ARM)

    fun handles(name: String): Boolean = name in HANDLED
}

object LiveLog {
    fun redact(message: String, secret: String?): String {
        if (secret.isNullOrEmpty() || secret.length < 8) return message
        return message.replace(secret, "[redacted]")
    }
}
