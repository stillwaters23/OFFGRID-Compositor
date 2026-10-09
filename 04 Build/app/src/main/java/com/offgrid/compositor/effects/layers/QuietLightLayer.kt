package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.EffectTimes
import com.offgrid.compositor.effects.Fire
import com.offgrid.compositor.effects.Fired
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.min

/**
 * Quiet mode's power light (feature 35). The app darkens and holds the picture. This adds a
 * near-black veil, so the dark is sure, and the small light near the bottom middle.
 *
 * The light flickers on like a machine booting up, then stays on as the picture comes back,
 * and fades with the veil. Each flicker takes at least a third of a second, so it never
 * blinks more than 3 times a second. With less motion asked for, it glows up without blinking.
 */
class QuietLightLayer : OverlayLayer {
    private val veil = Paint().apply { style = Paint.Style.FILL }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val flare = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var shaderColor = 0
    private var haloShader: Shader? = null
    private var flareShader: Shader? = null

    override fun isActive(frame: EffectFrame): Boolean {
        val fired = lastQuiet(frame) ?: return false
        val since = frame.time - fired.at
        return since >= 0f && since < EffectTimes.quiet(frame.settings).total
    }

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val fired = lastQuiet(frame) ?: return
        val q = EffectTimes.quiet(frame.settings)
        val t = frame.time - fired.at
        if (t < 0f || t >= q.total) return
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return

        val flickerStart = q.fade + q.dark
        val backStart = flickerStart + q.flicker
        val dark = when {
            t < q.fade -> EffectMath.smooth(t / q.fade)
            t < backStart -> 1f
            else -> 1f - EffectMath.smooth((t - backStart) / q.back)
        }
        veil.color = EffectMath.withAlpha(VEIL, VEIL_ALPHA * dark)
        canvas.drawRect(0f, 0f, w, h, veil)

        val light = when {
            t < flickerStart -> 0f
            t < backStart -> if (frame.reducedMotion) {
                EffectMath.smooth((t - flickerStart) / q.flicker)
            } else {
                flicker(t - flickerStart, q.flickers)
            }
            else -> 1f - EffectMath.smooth((t - backStart) / q.back)
        }
        if (light <= 0f) return
        drawLight(canvas, w, h, light, frame.settings.value(FxKeys.QUIET_LIGHT), frame.panelColor)
    }

    /**
     * Brightness during the flicker. Each flicker gets its own slot of [EffectTimes.MIN_FLASH_SECONDS]:
     * a quick rise, a short hold, then off. Each one is a little stronger, like power coming back.
     */
    private fun flicker(t: Float, count: Int): Float {
        val slotLength = EffectTimes.MIN_FLASH_SECONDS
        val slot = (t / slotLength).toInt()
        if (slot >= count) return 1f
        val f = t / slotLength - slot
        val strength = FIRST_FLICKER + (1f - FIRST_FLICKER) * (slot + 1f) / count
        val wobble = 1f - WOBBLE * EffectMath.hash(slot, SALT_WOBBLE)
        // The last flicker rises and stays on.
        if (slot == count - 1) return EffectMath.smooth(f / RISE) * (strength * wobble + (1f - strength * wobble) * EffectMath.smooth(f))
        val shape = EffectMath.smooth(f / RISE) * (1f - EffectMath.smooth((f - HOLD) / (1f - HOLD)))
        return shape * strength * wobble
    }

    private fun drawLight(canvas: Canvas, w: Float, h: Float, light: Float, size: Float, panel: Int) {
        shaders(panel or OPAQUE)
        val unit = min(w, h)
        val radius = unit * (MIN_SIZE + (MAX_SIZE - MIN_SIZE) * size) / 2f
        val cx = w / 2f
        val cy = h * LIGHT_Y
        val alpha = (255 * EffectMath.clamp01(light)).toInt()

        halo.alpha = alpha
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius * HALO, radius * HALO)
        canvas.drawCircle(0f, 0f, 1f, halo)
        canvas.restore()

        // A thin sideways lens streak, like a real light seen through a camera.
        flare.alpha = (alpha * FLARE_ALPHA).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius * FLARE_LONG, radius * FLARE_THIN)
        canvas.drawOval(-1f, -1f, 1f, 1f, flare)
        canvas.restore()

        core.color = EffectMath.withAlpha(mix(panel, Color.WHITE, CORE_WHITE), light)
        canvas.drawCircle(cx, cy, radius, core)
        core.color = EffectMath.withAlpha(Color.WHITE, light)
        canvas.drawCircle(cx, cy, radius * HOT_SPOT, core)
    }

    private fun shaders(color: Int) {
        if (haloShader != null && color == shaderColor) return
        shaderColor = color
        haloShader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(EffectMath.withAlpha(mix(color, Color.WHITE, 0.5f), 0.9f), EffectMath.withAlpha(color, 0.35f), EffectMath.withAlpha(color, 0.1f), 0),
            floatArrayOf(0f, 0.12f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
        halo.shader = haloShader
        flareShader = LinearGradient(
            -1f,
            0f,
            1f,
            0f,
            intArrayOf(0, EffectMath.withAlpha(color, 0.5f), EffectMath.withAlpha(Color.WHITE, 0.9f), EffectMath.withAlpha(color, 0.5f), 0),
            floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        flare.shader = flareShader
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        val k = EffectMath.clamp01(t)
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
        return Color.rgb(r, g, b)
    }

    private companion object {
        const val VEIL = 0xFF030405.toInt()
        const val VEIL_ALPHA = 0.97f
        const val OPAQUE = 0xFF000000.toInt()

        /** Light size: 1% to 6% of the video width across. */
        const val MIN_SIZE = 0.01f
        const val MAX_SIZE = 0.06f
        const val LIGHT_Y = 0.84f
        const val HALO = 7f
        const val FLARE_LONG = 16f
        const val FLARE_THIN = 0.35f
        const val FLARE_ALPHA = 0.55f
        const val CORE_WHITE = 0.55f
        const val HOT_SPOT = 0.5f

        const val FIRST_FLICKER = 0.35f
        const val WOBBLE = 0.2f
        const val RISE = 0.12f
        const val HOLD = 0.45f
        const val SALT_WOBBLE = 41

        /** The newest quiet firing, found without making a list iterator. */
        fun lastQuiet(frame: EffectFrame): Fired? {
            val fired = frame.fired
            for (i in fired.size - 1 downTo 0) {
                if (fired[i].kind == Fire.QUIET) return fired[i]
            }
            return null
        }
    }
}
