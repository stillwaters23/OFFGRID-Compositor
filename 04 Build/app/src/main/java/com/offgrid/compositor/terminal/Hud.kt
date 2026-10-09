package com.offgrid.compositor.terminal

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.offgrid.compositor.R

/**
 * Fonts, sizes, and small color helpers for the terminal panel.
 * The colors themselves come from the theme. See PanelLook.kt.
 */
internal object Hud {
    /** Step 2 colors. Holo Classic still uses them. */
    const val HOLO_CYAN = 0xFF46F0FF.toInt()
    const val HOLO_GOLD = 0xFFFFC34D.toInt()
    const val HOLO_LASER = 0xFF33FF00.toInt()
    const val HOLO_LABEL = 0xFFCCFF99.toInt()
    const val HOLO_TEXT = 0xFFDDF8FF.toInt()
    const val HOLO_GLASS_TOP = 0xFF0D1E26.toInt()
    const val HOLO_GLASS_BOTTOM = 0xFF03090D.toInt()
    const val INK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

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

    /** Mixes [color] toward [other]. 0 keeps [color], 1 gives [other]. Alpha comes from [color]. */
    fun blend(color: Int, other: Int, share: Float): Int {
        val t = share.coerceIn(0f, 1f)
        val r = Color.red(color) + (Color.red(other) - Color.red(color)) * t
        val g = Color.green(color) + (Color.green(other) - Color.green(color)) * t
        val b = Color.blue(color) + (Color.blue(other) - Color.blue(color)) * t
        return Color.argb(Color.alpha(color), r.toInt(), g.toInt(), b.toInt())
    }

    fun lighten(color: Int, share: Float): Int = blend(color, WHITE, share)

    /** 0 for grays, 1 for the most colorful colors. */
    fun saturation(color: Int): Float {
        val high = maxOf(Color.red(color), Color.green(color), Color.blue(color))
        val low = minOf(Color.red(color), Color.green(color), Color.blue(color))
        return if (high == 0) 0f else (high - low) / high.toFloat()
    }
}
