package com.offgrid.compositor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.terminal.PanelPrefs
import com.offgrid.live.FrameSink
import com.offgrid.live.OffgridHost
import com.offgrid.live.RemoteMessage

internal class LiveHost(
    private val context: Context,
) : OffgridHost {
    fun applicationContext(): Context = context
    private val preferences = PanelPrefs(context)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var effects: EffectsController? = null
    @Volatile private var processor: (() -> CompositorProcessor?)? = null
    var onOverlayChanged: ((Boolean) -> Unit)? = null
    var onRoleChanged: ((String) -> Unit)? = null
    var onPcOutputChanged: ((Boolean) -> Unit)? = null
    var onRestoreControls: (() -> Unit)? = null

    /** Phase 1.3: Phone 2 sent lock_on, lock_off, or arm. Runs on the main thread. */
    var onLockCommand: ((String) -> Unit)? = null

    fun attach(effects: EffectsController, processor: () -> CompositorProcessor?) {
        this.effects = effects
        this.processor = processor
    }

    fun detach() {
        effects = null
        processor = null
    }

    override fun readSetting(key: String): String? = when {
        preferences.hasToggle(key) -> preferences.toggle(key, false).toString()
        preferences.hasNumber(key) -> preferences.number(key, 50).toString()
        else -> preferences.text(key) ?: if (key == REMOTE_ROLE) "off" else null
    }

    override fun writeSetting(key: String, value: String) {
        main.post {
            val opacity = if (key == FxKeys.AR_OPACITY) value.toIntOrNull()?.coerceIn(0, 100) else null
            when {
                opacity != null -> preferences.setNumber(key, opacity)
                value.toBooleanStrictOrNull() != null -> preferences.setToggle(key, value.toBoolean())
                else -> preferences.write { text(key, value) }
            }
            effects?.refresh()
            when (key) {
                LIVE_OVERLAY -> onOverlayChanged?.invoke(value.toBoolean())
                REMOTE_ROLE -> onRoleChanged?.invoke(value)
                LIVE_PC_ON -> onPcOutputChanged?.invoke(value.toBoolean())
            }
        }
    }

    override fun restoreControls() {
        main.post { onRestoreControls?.invoke() }
    }

    override fun fire(action: String) {
        if (action == ArDeckCatalog.PLAY || action == ArDeckCatalog.PAUSE || action == ArDeckCatalog.NEXT) {
            main.post { effects?.arRemote(action) }
            return
        }
        if (action in RemoteMessage.LOCK_ACTIONS) {
            main.post { onLockCommand?.invoke(action) }
            return
        }
        val kind = FIRES[action] ?: return
        main.post { effects?.fire(kind) }
    }

    override fun openApp() {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    override fun frameSurfaceSink(): FrameSink = object : FrameSink {
        override fun attach(id: String, surface: Surface, width: Int, height: Int) {
            processor?.invoke()?.setExtraOutput(id, surface, width, height)
        }

        override fun detach(id: String) {
            processor?.invoke()?.setExtraOutput(id, null, 0, 0)
        }
    }

    private companion object {
        const val REMOTE_ROLE = "fx.remote.role"
        const val LIVE_OVERLAY = "fx.live.overlay"
        const val LIVE_PC_ON = "fx.live.pc.on"
        val FIRES = mapOf(
            "shatter" to Fire.SHATTER,
            "freeze" to Fire.TIME_FREEZE,
            "quiet" to Fire.QUIET,
            "fireworks" to Fire.FIREWORKS,
            "red_sweep" to Fire.RED_SWEEP,
            "laser_writing" to Fire.LASER_WRITE,
        )
    }
}
