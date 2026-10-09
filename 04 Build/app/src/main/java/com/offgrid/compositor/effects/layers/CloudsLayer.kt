package com.offgrid.compositor.effects.layers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Flying through clouds (feature 23).
 *
 * Full: the whole box is the sky seen from a plane. A soft floor of clouds rolls by below,
 * and clouds rush toward the viewer and slide past the edges. Around: clouds drift along the
 * bottom and up the sides, and the middle stays clear, so Royalty is still seen.
 *
 * Speed is how fast they fly. Thickness is how many clouds and how full they are.
 * Height is how high up: high means thinner clouds, a deeper blue sky, and the far line lower.
 *
 * The cloud shapes are painted once into small pictures and reused, so each frame is quick.
 * Clouds fade in far away and fade out before they fill the box, so it never flashes white.
 */
class CloudsLayer : OverlayLayer {
    private val sprites = arrayOfNulls<Bitmap>(SPRITES)
    private val art = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val plain = Paint().apply { style = Paint.Style.FILL }
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shade = LightingColorFilter(SHADE_TINT, 0)
    private val spot = RectF()
    private val order = IntArray(MAX_FLYERS)
    private val depth = FloatArray(MAX_FLYERS)

    // Cached look. It is only made again when the size or height changes.
    private var lookWidth = 0
    private var lookHeight = 0
    private var lookAltitude = -1
    private var skyShader: Shader? = null
    private var floorShader: Shader? = null
    private var sunShader: Shader? = null
    private var farShader: Shader? = null
    private var hazeShader: Shader? = null
    private var horizon = 0f

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.CLOUDS_ON)

    override fun coversAll(frame: EffectFrame): Boolean =
        frame.settings.on(FxKeys.CLOUDS_ON) && !frame.settings.chose(FxKeys.CLOUDS_PLACE, FxKeys.CLOUDS_AROUND)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        makeSprites()
        val s = frame.settings
        val altitude = s.value(FxKeys.CLOUDS_HEIGHT)
        val thick = s.value(FxKeys.CLOUDS_THICKNESS)
        val calm = if (frame.reducedMotion) CALM else 1f
        val speed = EffectMath.lerp(MIN_SPEED, MAX_SPEED, s.value(FxKeys.CLOUDS_SPEED)) * calm
        look(frame.width, frame.height, s.raw(FxKeys.CLOUDS_HEIGHT))
        // How far we have flown. Kept as a Double so clouds stay smooth after hours of streaming.
        val travel = frame.time.toDouble() * speed
        if (s.chose(FxKeys.CLOUDS_PLACE, FxKeys.CLOUDS_AROUND)) {
            drawAround(canvas, w, h, travel, thick, altitude)
        } else {
            drawFull(canvas, w, h, travel, thick, altitude)
        }
    }

    private fun drawFull(canvas: Canvas, w: Float, h: Float, travel: Double, thick: Float, altitude: Float) {
        plain.shader = skyShader
        canvas.drawRect(0f, 0f, w, horizon + 1f, plain)
        plain.shader = sunShader
        canvas.drawRect(0f, 0f, w, horizon, plain)
        plain.shader = floorShader
        canvas.drawRect(0f, horizon, w, h, plain)
        plain.shader = null

        val focal = w * FOCAL
        val cx = w / 2f
        val thin = 1f - THIN_HIGH * altitude

        // The floor of clouds below, rolling toward the viewer in rows.
        val below = EffectMath.lerp(LOW_FLOOR, HIGH_FLOOR, altitude)
        val range = FLOOR_ROWS * FLOOR_GAP
        // Far rows first, so near rows cover them. The farthest row is the one that started last.
        val lead = frac(travel / range)
        val first = (ceil((1f - lead) * FLOOR_ROWS).toInt()) % FLOOR_ROWS
        for (m in 0 until FLOOR_ROWS) {
            val k = (first + m) % FLOOR_ROWS
            val u = (k * FLOOR_GAP + travel) / range
            val pass = floor(u).toInt()
            val z = FLOOR_NEAR + range * (1f - (u - pass).toFloat())
            val fade = EffectMath.smooth((FLOOR_NEAR + range - z) / (range * 0.25f)) * EffectMath.smooth((z - FLOOR_NEAR) / 0.4f)
            if (fade <= 0.01f) continue
            val y = horizon + below * focal / z
            val wide = FLOOR_PUFF * focal / z
            val reach = (cx / (focal / z)) + FLOOR_PUFF
            val lanes = (reach / FLOOR_LANE).toInt() + 1
            for (j in -lanes..lanes) {
                val seed = j * 131 + (k + pass * FLOOR_ROWS) * 17
                val x = cx + (j + EffectMath.hash(seed, SALT_FLOOR_X) * 0.7f) * FLOOR_LANE * focal / z
                val size = wide * (0.8f + 0.5f * EffectMath.hash(seed, SALT_FLOOR_S))
                val tall = size * FLOOR_FLAT
                spot.set(x - size / 2f, y - tall * 0.75f, x + size / 2f, y + tall * 0.25f)
                if (spot.right < 0f || spot.left > w || spot.top > h) continue
                art.alpha = (255 * fade * (0.55f + 0.4f * thick) * thin).toInt()
                drawSprite(canvas, floorSprite(seed), spot, EffectMath.hash(seed, SALT_FLIP) < 0.5f)
            }
        }

        // A soft glow where the clouds come from.
        soft.shader = farShader
        canvas.drawRect(0f, horizon - h * 0.2f, w, horizon + h * 0.2f, soft)
        soft.shader = null

        // Clouds rushing toward the viewer, past the edges of the box.
        val count = MIN_FLYERS + ((MAX_FLYERS - MIN_FLYERS) * thick + 0.5f).toInt()
        val span = FAR - NEAR
        for (i in 0 until count) {
            depth[i] = FAR - span * frac((EffectMath.hash(i, SALT_PHASE) * span + travel) / span)
            order[i] = i
        }
        // Far to near, sorted in place without making anything new.
        for (a in 1 until count) {
            val moving = order[a]
            var b = a - 1
            while (b >= 0 && depth[order[b]] < depth[moving]) {
                order[b + 1] = order[b]
                b--
            }
            order[b + 1] = moving
        }
        for (n in 0 until count) {
            val i = order[n]
            val z = depth[i]
            val pass = floor((EffectMath.hash(i, SALT_PHASE) * span + travel) / span).toInt()
            val seed = i + pass * MAX_FLYERS
            val fadeIn = EffectMath.smooth((FAR - z) / (span * 0.2f))
            val fadeOut = EffectMath.smooth((z - NEAR) / (FADE_NEAR - NEAR))
            val alpha = fadeIn * fadeOut * (0.7f + 0.3f * thick) * thin
            if (alpha <= 0.01f) continue
            val angle = EffectMath.hash(seed, SALT_ANGLE) * TWO_PI
            val off = EffectMath.lerp(MIN_OFF, MAX_OFF, EffectMath.hash(seed, SALT_OFF))
            val x = cx + cos(angle) * off * focal / z
            val y = horizon - h * 0.03f + sin(angle) * off * FLYER_FLAT * focal / z
            val size = EffectMath.lerp(MIN_PUFF, MAX_PUFF, EffectMath.hash(seed, SALT_SIZE)) * (1f - 0.3f * altitude) * focal / z
            val tall = size * SPRITE_H / SPRITE_W
            spot.set(x - size / 2f, y - tall / 2f, x + size / 2f, y + tall / 2f)
            if (spot.right < 0f || spot.left > w || spot.bottom < 0f || spot.top > h) continue
            art.alpha = (255 * alpha).toInt()
            drawSprite(canvas, sprite(seed), spot, EffectMath.hash(seed, SALT_FLIP) < 0.5f)
        }
    }

    private fun drawAround(canvas: Canvas, w: Float, h: Float, travel: Double, thick: Float, altitude: Float) {
        val thin = 1f - THIN_HIGH * altitude
        val bank = h * EffectMath.lerp(BANK_LOW, BANK_HIGH, altitude) * (0.7f + 0.6f * thick)
        val drift = travel * w * AROUND_DRIFT
        val bobTime = frac(travel / BOB_LOOP) * BOB_LOOP.toFloat()

        // A soft white mist low down ties the clouds together.
        plain.shader = hazeShader
        canvas.save()
        canvas.translate(0f, h - bank * 1.6f)
        canvas.scale(1f, bank * 1.6f)
        canvas.drawRect(0f, 0f, w, 1f, plain)
        canvas.restore()
        plain.shader = null

        // Up both sides, slowly rising.
        val sides = 2 + (3f * thick + 0.5f).toInt()
        for (side in 0..1) {
            for (i in 0 until sides) {
                val seed = side * 50 + i
                val run = h * 1.35f
                val rise = (EffectMath.hash(seed, SALT_PHASE) * run + drift * 0.6f * (0.7f + 0.6f * EffectMath.hash(seed, SALT_SPEED))) % run
                val y = h - rise.toFloat() + h * 0.15f
                val size = w * (0.42f + 0.2f * EffectMath.hash(seed, SALT_SIZE)) * (0.8f + 0.4f * thick)
                val tall = size * SPRITE_H / SPRITE_W
                val inset = size * (0.12f + 0.12f * EffectMath.hash(seed, SALT_OFF))
                val x = if (side == 0) inset - size * 0.2f else w - inset + size * 0.2f
                // Fade near the top so they melt away instead of sliding off.
                val fade = EffectMath.smooth(y / (h * 0.25f)) * (0.55f + 0.35f * thick) * thin
                spot.set(x - size / 2f, y - tall / 2f, x + size / 2f, y + tall / 2f)
                art.alpha = (255 * fade).toInt()
                drawSprite(canvas, sprite(seed + 3), spot, side == 1)
            }
        }

        // A few thin wisps across the very top.
        for (i in 0 until TOP_WISPS) {
            val seed = 200 + i
            val size = w * (0.55f + 0.25f * EffectMath.hash(seed, SALT_SIZE))
            val run = w + size
            val x = ((EffectMath.hash(seed, SALT_PHASE) * run + drift * (0.5f + 0.3f * i)) % run).toFloat() - size / 2f
            val tall = size * SPRITE_H / SPRITE_W
            val y = h * (0.01f + 0.025f * EffectMath.hash(seed, SALT_OFF))
            spot.set(x - size / 2f, y - tall / 2f, x + size / 2f, y + tall / 2f)
            art.alpha = (255 * (0.5f + 0.3f * thick) * thin).toInt()
            drawSprite(canvas, sprites[STRATUS], spot, i % 2 == 1)
        }

        // The bank along the bottom: three rows, the back ones slower and a little shaded.
        for (row in 0 until BANK_ROWS) {
            val back = row == 0
            val each = 3 + (2f * thick + 0.5f).toInt()
            val size = w * (0.5f + 0.12f * row) * (0.85f + 0.3f * thick)
            val gap = (w + size) / each
            val run = gap * each
            val speed = (0.5f + 0.35f * row)
            val y = h - bank * (0.95f - 0.33f * row)
            for (i in 0 until each) {
                val seed = 100 + row * 20 + i
                val x = ((i * gap + EffectMath.hash(seed, SALT_PHASE) * gap * 0.6f + drift * speed) % run).toFloat() - size / 2f
                val tall = size * SPRITE_H / SPRITE_W
                val bob = h * 0.006f * sin(bobTime * 0.4f + i * 1.7f + row)
                spot.set(x - size / 2f, y - tall / 2f + bob, x + size / 2f, y + tall / 2f + bob)
                art.alpha = (255 * (0.8f + 0.2f * thick) * (0.75f + 0.25f * thin)).toInt()
                art.colorFilter = if (back) shade else null
                drawSprite(canvas, sprite(seed), spot, (i + row) % 2 == 0)
            }
        }
        art.colorFilter = null
    }

    private fun floorSprite(seed: Int): Bitmap? =
        sprites[FLOOR_FIRST + (EffectMath.hash(seed, SALT_KIND) * FLOOR_KINDS).toInt().coerceIn(0, FLOOR_KINDS - 1)]

    private fun sprite(seed: Int): Bitmap? = sprites[(EffectMath.hash(seed, SALT_KIND) * CUMULUS).toInt().coerceIn(0, CUMULUS - 1)]

    private fun drawSprite(canvas: Canvas, picture: Bitmap?, where: RectF, flip: Boolean) {
        val bitmap = picture ?: return
        if (flip) {
            canvas.save()
            canvas.scale(-1f, 1f, where.centerX(), where.centerY())
            canvas.drawBitmap(bitmap, null, where, art)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, null, where, art)
        }
    }

    /** Makes the sky colors for this size and height. Only runs when they change. */
    private fun look(width: Int, height: Int, altitude: Int) {
        if (width == lookWidth && height == lookHeight && altitude == lookAltitude) return
        lookWidth = width
        lookHeight = height
        lookAltitude = altitude
        val w = width.toFloat()
        val h = height.toFloat()
        val a = altitude / 100f
        horizon = h * EffectMath.lerp(HORIZON_LOW, HORIZON_HIGH, a)
        val top = mix(SKY_TOP_LOW, SKY_TOP_HIGH, a)
        val mid = mix(SKY_MID_LOW, SKY_MID_HIGH, a)
        val low = mix(SKY_LOW_LOW, SKY_LOW_HIGH, a)
        skyShader = LinearGradient(
            0f, 0f, 0f, horizon,
            intArrayOf(top, mid, low), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP,
        )
        sunShader = RadialGradient(
            w * 0.84f, h * 0.05f, w * 0.8f,
            intArrayOf(SUN, EffectMath.withAlpha(SUN, 0.75f), EffectMath.withAlpha(SUN, 0.22f), EffectMath.withAlpha(SUN, 0.06f), 0),
            floatArrayOf(0f, 0.045f, 0.16f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        floorShader = LinearGradient(
            0f, horizon, 0f, h,
            intArrayOf(mix(low, Color.WHITE, 0.5f), mix(FLOOR_MID, low, 0.2f), mix(FLOOR_LOW, top, 0.25f * a)),
            floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP,
        )
        farShader = LinearGradient(
            0f, horizon - h * 0.2f, 0f, horizon + h * 0.2f,
            intArrayOf(0, EffectMath.withAlpha(Color.WHITE, 0.4f - 0.15f * a), 0),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        hazeShader = LinearGradient(
            0f, 0f, 0f, 1f,
            intArrayOf(0, EffectMath.withAlpha(MIST, 0.35f), EffectMath.withAlpha(MIST, 0.7f)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
    }

    /** Paints the cloud pictures once. They are reused every frame after that. */
    private fun makeSprites() {
        if (sprites[0] != null) return
        for (k in 0 until SPRITES) sprites[k] = paintCloud(k)
    }

    private companion object {
        const val TWO_PI = (PI * 2).toFloat()
        const val CALM = 0.5f

        /** Flying speed in cloud lengths a second, at speed 0 and 100. */
        const val MIN_SPEED = 0.7f
        const val MAX_SPEED = 6f

        const val FOCAL = 0.6f
        const val HORIZON_LOW = 0.42f
        const val HORIZON_HIGH = 0.6f
        const val THIN_HIGH = 0.4f

        const val FLOOR_ROWS = 9
        const val FLOOR_GAP = 1.3f
        const val FLOOR_NEAR = 0.7f
        const val FLOOR_PUFF = 2.6f
        const val FLOOR_LANE = 1.9f
        const val FLOOR_FLAT = 0.36f
        const val LOW_FLOOR = 1.7f
        const val HIGH_FLOOR = 1.25f

        const val MIN_FLYERS = 6
        const val MAX_FLYERS = 26
        const val FAR = 12f
        const val NEAR = 0.75f
        const val FADE_NEAR = 2.2f
        const val MIN_OFF = 0.9f
        const val MAX_OFF = 3f
        const val FLYER_FLAT = 0.75f
        const val MIN_PUFF = 1.4f
        const val MAX_PUFF = 2.8f

        /** Around: how tall the bottom bank is, as a share of the height, low and high up. */
        const val BANK_LOW = 0.2f
        const val BANK_HIGH = 0.13f
        const val BANK_ROWS = 3
        const val TOP_WISPS = 2
        const val AROUND_DRIFT = 0.04f

        /** The bank's gentle bob repeats after this much flying, so it stays smooth. */
        const val BOB_LOOP = 2.0 * PI * 5.0

        const val SALT_FLOOR_X = 301
        const val SALT_FLOOR_S = 302
        const val SALT_FLIP = 303
        const val SALT_PHASE = 304
        const val SALT_ANGLE = 305
        const val SALT_OFF = 306
        const val SALT_SIZE = 307
        const val SALT_SPEED = 308
        const val SALT_KIND = 309
        const val SALT_LOBE = 320

        const val SKY_TOP_LOW = 0xFF3F8DDB.toInt()
        const val SKY_TOP_HIGH = 0xFF0A2F7A.toInt()
        const val SKY_MID_LOW = 0xFF8CC0EE.toInt()
        const val SKY_MID_HIGH = 0xFF3C7CD0.toInt()
        const val SKY_LOW_LOW = 0xFFDCEDFA.toInt()
        const val SKY_LOW_HIGH = 0xFFB4D8F4.toInt()
        const val FLOOR_MID = 0xFFE6EEF7.toInt()
        const val FLOOR_LOW = 0xFFB5C6DC.toInt()
        const val SUN = 0xFFFFF6DE.toInt()
        const val MIST = 0xFFF2F6FB.toInt()
        const val SHADE_TINT = 0xD9E2EE

        /** The cloud pictures: three heaped clouds, one long flat one, and two tops for the floor. */
        const val SPRITES = 6
        const val CUMULUS = 3
        const val STRATUS = 3
        const val FLOOR_FIRST = 4
        const val FLOOR_KINDS = 2
        const val SPRITE_W = 448
        const val SPRITE_H = 280

        const val LIT = 0xFFFFFDF8.toInt()
        const val SHADOW = 0xFFBDCADD.toInt()
        const val BASE = 0xFF8C9CBA.toInt()

        fun frac(x: Double): Float = (x - floor(x)).toFloat()

        fun mix(from: Int, to: Int, t: Float): Int {
            val k = EffectMath.clamp01(t)
            val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * k).toInt()
            val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * k).toInt()
            val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * k).toInt()
            return Color.rgb(r, g, b)
        }

        /**
         * Paints one cloud. Soft puffs are added up into a thickness map, its edges are made
         * uneven with soft noise, and each spot is lit by how much cloud lies between it and
         * the sun above. Tops come out bright white, insides and the flat base a soft blue-gray.
         * The same [kind] always paints the same cloud.
         */
        fun paintCloud(kind: Int): Bitmap {
            val bw = SPRITE_W
            val bh = SPRITE_H
            val thick = FloatArray(bw * bh)
            val flat = kind == STRATUS
            // Floor clouds are seen from above: mostly sunlit tops, with a long soft fade below.
            val above = kind >= FLOOR_FIRST
            val salt = SALT_LOBE + kind * 10
            val baseY = bh * if (flat) 0.62f else 0.72f
            fun puff(x: Float, y: Float, r: Float, squash: Float) {
                val ry = r * squash
                val x0 = (x - r).toInt().coerceAtLeast(0)
                val x1 = (x + r).toInt().coerceAtMost(bw - 1)
                val y0 = (y - ry).toInt().coerceAtLeast(0)
                val y1 = (y + ry).toInt().coerceAtMost(bh - 1)
                for (py in y0..y1) {
                    val dy = (py - y) / ry
                    for (px in x0..x1) {
                        val dx = (px - x) / r
                        val d = 1f - (dx * dx + dy * dy)
                        if (d > 0f) thick[py * bw + px] += d * d
                    }
                }
            }
            val squash = if (flat) 0.42f else 1f
            // A row of puffs along the bottom.
            val bases = if (flat) 9 else 7
            for (j in 0 until bases) {
                val r = bw * (if (flat) 0.1f else 0.085f) * (1f + 0.45f * EffectMath.hash(j, salt))
                val x = bw * (0.14f + 0.72f * j / (bases - 1f)) + bw * 0.04f * (EffectMath.hash(j, salt + 1) - 0.5f)
                puff(x, baseY - r * 0.25f * squash, r, squash)
            }
            // Bigger heaps on top, tallest near the middle.
            val domes = if (flat) 4 else 6
            for (j in 0 until domes) {
                val x = bw * (0.22f + 0.56f * EffectMath.hash(j, salt + 2))
                val middle = 1f - 1.2f * abs(x / bw - 0.5f)
                val r = bw * (if (flat) 0.09f else 0.11f + 0.07f * EffectMath.hash(j, salt + 3)) * middle
                val lift = if (flat) 0.3f else 0.85f + 0.5f * EffectMath.hash(j, salt + 4)
                val y = (baseY - r * lift * squash).coerceAtLeast(r * squash + 6f)
                puff(x, y, r, squash)
            }
            // Small puffs on the upper edge for a bumpy outline.
            for (j in 0 until if (flat) 6 else 12) {
                val x = bw * (0.2f + 0.6f * EffectMath.hash(j, salt + 5))
                val r = bw * (0.035f + 0.035f * EffectMath.hash(j, salt + 6))
                // Put each one on the current top of the cloud at that spot.
                var top = baseY.toInt()
                val col = x.toInt().coerceIn(0, bw - 1)
                for (py in 0 until bh) {
                    if (thick[py * bw + col] > 0.35f) {
                        top = py
                        break
                    }
                }
                puff(x, (top + r * 0.35f * squash).coerceAtLeast(r * squash + 3f), r, squash)
            }

            // How solid each spot is, with uneven, wispy edges.
            val solid = FloatArray(bw * bh)
            for (py in 0 until bh) {
                val base = if (above) {
                    1f - EffectMath.smooth((py - (baseY - bh * 0.3f)) / (bh * 0.42f))
                } else {
                    1f - EffectMath.smooth((py - (baseY - bh * 0.03f)) / (bh * 0.16f))
                }
                for (px in 0 until bw) {
                    val i = py * bw + px
                    val grain = fbm(px / NOISE_SCALE, py / NOISE_SCALE, kind)
                    val d = thick[i] * (0.6f + 0.8f * grain)
                    solid[i] = EffectMath.smooth((d - 0.18f) / 0.42f) * base
                }
            }
            // Light from the sun above: the more cloud above a spot, the more shaded it is.
            // It is added up down each column, so the shading is smooth.
            val pixels = IntArray(bw * bh)
            for (px in 0 until bw) {
                var shadow = 0f
                for (py in 0 until bh) {
                    val i = py * bw + px
                    val a = solid[i]
                    shadow = shadow * LIGHT_KEEP + a
                    if (a <= 0.004f) continue
                    val light = 1f / (1f + shadow * if (above) LIGHT_DIM * 0.45f else LIGHT_DIM)
                    val low = EffectMath.smooth((py - (baseY - bh * 0.2f)) / (bh * 0.24f))
                    val grain = 0.94f + 0.12f * fbm(px / 18f + 40f, py / 18f, kind + 7)
                    val k = EffectMath.clamp01(light * grain)
                    var r = Color.red(SHADOW) + (Color.red(LIT) - Color.red(SHADOW)) * k
                    var g = Color.green(SHADOW) + (Color.green(LIT) - Color.green(SHADOW)) * k
                    var b = Color.blue(SHADOW) + (Color.blue(LIT) - Color.blue(SHADOW)) * k
                    val under = low * (1f - k) * if (above) 0.3f else 0.8f
                    r += (Color.red(BASE) - r) * under
                    g += (Color.green(BASE) - g) * under
                    b += (Color.blue(BASE) - b) * under
                    pixels[i] = Color.argb((a * 255f).toInt(), r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
                }
            }
            return Bitmap.createBitmap(pixels, bw, bh, Bitmap.Config.ARGB_8888)
        }

        /** Soft value noise from 0 to 1, a few layers deep, the same every time. */
        fun fbm(x: Float, y: Float, seed: Int): Float {
            var sum = 0f
            var amp = 0.5f
            var fx = x
            var fy = y
            var total = 0f
            for (o in 0 until 4) {
                sum += amp * noise(fx, fy, seed * 13 + o)
                total += amp
                amp *= 0.5f
                fx *= 2.03f
                fy *= 2.03f
            }
            return sum / total
        }

        fun noise(x: Float, y: Float, seed: Int): Float {
            val ix = floor(x).toInt()
            val iy = floor(y).toInt()
            val fx = EffectMath.smooth(x - ix)
            val fy = EffectMath.smooth(y - iy)
            val a = EffectMath.hash(ix * 73856 + iy * 19349, seed)
            val b = EffectMath.hash((ix + 1) * 73856 + iy * 19349, seed)
            val c = EffectMath.hash(ix * 73856 + (iy + 1) * 19349, seed)
            val d = EffectMath.hash((ix + 1) * 73856 + (iy + 1) * 19349, seed)
            return EffectMath.lerp(EffectMath.lerp(a, b, fx), EffectMath.lerp(c, d, fx), fy)
        }

        const val NOISE_SCALE = 34f
        const val LIGHT_KEEP = 0.975f
        const val LIGHT_DIM = 0.038f
            }
}
