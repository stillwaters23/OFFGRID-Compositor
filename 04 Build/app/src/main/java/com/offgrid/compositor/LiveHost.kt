package com.offgrid.compositor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.terminal.PanelPrefs
import com.offgrid.live.FrameSink
import com.offgrid.live.OffgridHost

internal class LiveHost(
    private val context: Context,
    private val effects: EffectsController,
    private val processor: () -> CompositorProcessor?,
) : OffgridHost {
    private val preferences = PanelPrefs(context)
    private val main = Handler(Looper.getMainLooper())

    override fun readSetting(key: String): String? = when {
        preferences.hasToggle(key) -> preferences.toggle(key, false).toString()
        preferences.hasNumber(key) -> preferences.number(key, 50).toString()
        else -> preferences.text(key) ?: if (key == REMOTE_ROLE) "off" else null
    }

    override fun writeSetting(key: String, value: String) {
        main.post {
            if (value.toBooleanStrictOrNull() != null) {
                preferences.setToggle(key, value.toBoolean())
            } else {
                preferences.write { text(key, value) }
            }
            effects.refresh()
        }
    }

    override fun fire(action: String) {
        val kind = FIRES[action] ?: return
        main.post { effects.fire(kind) }
    }

    override fun openApp() {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    override fun frameSurfaceSink(): FrameSink = object : FrameSink {
        override fun attach(surface: Surface, width: Int, height: Int) {
            processor()?.setLiveSurface(surface, width, height)
        }

        override fun detach(surface: Surface) {
            processor()?.clearLiveSurface(surface)
        }
    }

    private companion object {
        const val REMOTE_ROLE = "fx.remote.role"
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
