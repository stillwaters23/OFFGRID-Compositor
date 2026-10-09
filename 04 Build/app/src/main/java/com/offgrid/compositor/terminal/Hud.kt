package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.offgrid.compositor.R

/**
 * Colors, fonts, and sizes for the terminal panel.
 * Our own look: dark glass, cyan and gold glow lines, laser green text.
 */
internal object Hud {
    const val CYAN = 0xFF46F0FF.toInt()
    const val GOLD = 0xFFFFC34D.toInt()
    const val LASER = 0xFF33FF00.toInt()
    var isLightTheme = false
        private set
    val TEXT: Int get() = if (isLightTheme) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    val TEXT_DIM: Int get() = if (isLightTheme) 0xFF303030.toInt() else 0xFFB8B8C0.toInt()
    const val GLASS_TOP = 0xFF0D1E26.toInt()
    const val GLASS_BOTTOM = 0xFF03090D.toInt()
    const val INK = 0xFF000000.toInt()

    private var monoFace: Typeface? = null
    private var monoBoldFace: Typeface? = null

    /** JetBrains Mono, bundled with the app, per the laser typewriter spec. */
    fun mono(context: Context): Typeface {
        monoFace?.let { return it }
        val loaded = try {
            ResourcesCompat.getFont(context, R.font.jetbrains_mono)
        } catch (error: Resources.NotFoundException) {
            null
        }
        val face = loaded ?: Typeface.MONOSPACE
        monoFace = face
        return face
    }

    fun monoBold(context: Context): Typeface {
        monoBoldFace?.let { return it }
        val face = Typeface.create(mono(context), 700, false)
        monoBoldFace = face
        return face
    }

    val condensed: Typeface by lazy { Typeface.create("sans-serif-condensed", Typeface.NORMAL) }

    val condensedBold: Typeface by lazy { Typeface.create("sans-serif-condensed", Typeface.BOLD) }

    fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density

    fun dpi(context: Context, value: Float): Int = (dp(context, value) + 0.5f).toInt()

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun updateSystemTheme(configuration: Configuration) {
        isLightTheme = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
    }
}
