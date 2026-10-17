package com.offgrid.live

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class RemoteMessage(val key: String, val type: String, val value: String) {
    init {
        require(
            type == "fire" && value in FIRE_ACTIONS ||
                key.startsWith("fx.") && type == "setting",
        )
        require(key.length <= MAX_FIELD_LENGTH && type.length <= MAX_FIELD_LENGTH)
        require(value.length <= MAX_FIELD_LENGTH)
    }

    fun encode(): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeUTF(key)
            output.writeUTF(type)
            output.writeUTF(value)
        }
    }.toByteArray()

    companion object {
        private const val MAX_FIELD_LENGTH = 256
        val FIRE_ACTIONS =
            setOf(
                "shatter",
                "freeze",
                "quiet",
                "fireworks",
                "red_sweep",
                "laser_writing",
                "ar_play",
                "ar_pause",
                "ar_next",
                // Phase 1.3 Broadcast Lock from Phone 2. lock_off is refused while a share is live (A4).
                LOCK_ON,
                LOCK_OFF,
                LOCK_ARM,
            )

        const val LOCK_ON = "lock_on"
        const val LOCK_OFF = "lock_off"
        const val LOCK_ARM = "arm"

        /** The lock commands, kept out of the generic effect buttons on Phone 2. */
        val LOCK_ACTIONS = setOf(LOCK_ON, LOCK_OFF, LOCK_ARM)

        /** Settings the camera phone sends to Phone 2 for display only. */
        const val LOCK_STATE_KEY = "fx.lock.state"
        const val STATUS_LINE_KEY = "fx.status.line"

        fun decode(encoded: ByteArray): RemoteMessage {
            require(encoded.size <= MAX_FRAME_SIZE)
            DataInputStream(ByteArrayInputStream(encoded)).use { input ->
                val message = RemoteMessage(input.readUTF(), input.readUTF(), input.readUTF())
                require(input.available() == 0)
                return message
            }
        }

        fun RemoteMessage.applyTo(host: OffgridHost) {
            if (type == "fire") host.fire(value) else host.writeSetting(key, value)
        }

        private const val MAX_FRAME_SIZE = 1024
    }
}
