package com.offgrid.compositor.effects.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.offgrid.compositor.effects.EffectFrame
import com.offgrid.compositor.effects.EffectMath
import com.offgrid.compositor.effects.FxKeys
import com.offgrid.compositor.effects.OverlayLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Flying heroes (feature 21): OFFGRID's own heroes fly across the video on soft curves.
 * They come in from off the edge and leave off the other edge, one after another.
 * Some fly close and look big. Some fly far off and look small.
 *
 * Nova wears teal with a white cape and a star on the chest. Sparkles trail behind.
 * Volt wears black with a yellow bolt, and leaves a crackling trail of light.
 * Comet has a round glowing helmet and a long fiery tail.
 * Pixel is a blocky 8-bit hero in magenta and cyan, with a trail of square sparks.
 *
 * Every hero is drawn from shapes made once, so nothing is made while drawing.
 * Hero count is 1 to 6. Hero size is 8% to 35% of the width. Hero speed sets the time to
 * cross the video: 7 seconds at 0, 1.5 seconds at 100.
 */
class FlyingHeroesLayer : OverlayLayer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val glowFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val block = Paint().apply { style = Paint.Style.FILL }

    // The body, in hero units. One unit is a little less than the hero's length.
    private val torso = Path()
    private val farSide = Path()
    private val farTrim = Path()
    private val nearSide = Path()
    private val nearTrim = Path()
    private val silhouette = Path()
    private val belt = Path()
    private val star = Path()
    private val bolt = Path()
    private val legStripe = Path()
    private val backShine = Path()
    private val visor = Path()
    private val crest = Path()
    private val face = Path()
    private val spark = Path()
    private val headBox = RectF(HEAD_X - HEAD_RX, HEAD_Y - HEAD_RY, HEAD_X + HEAD_RX, HEAD_Y + HEAD_RY)
    private val shineBox = RectF()

    // Paths filled again each frame, and reused.
    private val cape = Path()
    private val folds = Path()
    private val ribbon = Path()
    private val crackle = Path()

    // The pixel hero: one path of squares per color, for both cape frames.
    private val pixelPaths = Array(2) { Array(PIXEL_COLORS.size) { Path() } }

    private val looks = arrayOf(novaLook(), voltLook(), cometLook(), pixelLook())

    // The heroes in the air right now, sorted far to near.
    private val order = IntArray(MAX_HEROES)
    private val heroSeed = IntArray(MAX_HEROES)
    private val heroU = FloatArray(MAX_HEROES)
    private val heroSize = FloatArray(MAX_HEROES)
    private val heroIndex = IntArray(MAX_HEROES)
    private val heroLaneShare = FloatArray(MAX_HEROES)

    // The trail, in hero units, nearest point first.
    private val trailX = FloatArray(TRAIL_POINTS + 1)
    private val trailY = FloatArray(TRAIL_POINTS + 1)

    // Where a hero is on its path, filled by place().
    private var atX = 0f
    private var atY = 0f
    private var atVX = 0f
    private var atVY = 0f
    private var atDir = 1f
    private var laneShare = 0.5f
    private var startRoom = 0f
    private var endRoom = 0f

    init {
        buildBody()
        buildPixelHero()
    }

    override fun isActive(frame: EffectFrame): Boolean = frame.settings.on(FxKeys.HEROES_ON)

    override fun draw(canvas: Canvas, frame: EffectFrame) {
        val s = frame.settings
        val w = frame.width.toFloat()
        val h = frame.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val look = looks[styleIndex(s.choice(FxKeys.HEROES_HERO))]
        val count = 1 + (s.value(FxKeys.HEROES_COUNT) * (MAX_HEROES - 1) + 0.5f).toInt()
        val size = w * EffectMath.lerp(SIZE_MIN, SIZE_MAX, s.value(FxKeys.HEROES_SIZE))
        var crossing = EffectMath.lerp(CROSS_SLOW, CROSS_FAST, s.value(FxKeys.HEROES_SPEED))
        if (frame.reducedMotion) crossing *= CALM_SLOWER

        // Which heroes are in the air. Each one has its own lane time and a short wait between trips.
        var shown = 0
        for (i in 0 until count) {
            val lane = crossing * (LANE_BASE + LANE_SPREAD * EffectMath.hash(i, 21))
            val period = lane * (1f + GAP_MIN + GAP_EXTRA * EffectMath.hash(i, 22))
            val shifted = frame.time + (i.toFloat() / count + STAGGER_JITTER * EffectMath.hash(i, 23)) * period
            val trip = floor(shifted / period)
            val u = (shifted - trip * period) / lane
            if (u > 1f + LINGER) continue
            val seed = (i + 1) * SEED_STEP + trip.toInt()
            val depth = 0.5f * EffectMath.hash(i, 24) + 0.5f * EffectMath.hash(seed, 7)
            heroLaneShare[shown] = (i + LANE_EDGE + (1f - 2f * LANE_EDGE) * EffectMath.hash(seed, 2)) / count
            heroSeed[shown] = seed
            heroU[shown] = u
            heroIndex[shown] = i
            heroSize[shown] = size * (DEPTH_MIN + (1f - DEPTH_MIN) * depth)
            order[shown] = shown
            shown++
        }
        // Far (small) heroes first, so near ones fly in front.
        for (a in 1 until shown) {
            val key = order[a]
            var b = a - 1
            while (b >= 0 && heroSize[order[b]] > heroSize[key]) {
                order[b + 1] = order[b]
                b--
            }
            order[b + 1] = key
        }
        for (n in 0 until shown) drawHero(canvas, frame, look, order[n], w, h)
    }

    private fun drawHero(canvas: Canvas, frame: EffectFrame, look: Look, slot: Int, w: Float, h: Float) {
        val seed = heroSeed[slot]
        val u = heroU[slot]
        val px = heroSize[slot]
        val unit = px / HERO_LENGTH
        // Room off each edge: enough for the whole hero, and at the far edge for its trail too.
        laneShare = heroLaneShare[slot]
        startRoom = px * START_ROOM
        endRoom = px * (START_ROOM + look.trailLength * TRAIL_SEEN)
        val span = w + startRoom + endRoom
        val calm = if (frame.reducedMotion) CALM_FLUTTER else 1f
        val wiggle = EffectMath.hash(heroIndex[slot], 25) * TWO_PI

        place(seed, u, w, h)
        val cx = atX
        val cy = atY
        val dir = atDir
        var tilt = atan2(atVY, abs(atVX)).coerceIn(-MAX_TILT, MAX_TILT)
        tilt += BANK * sin(frame.time * BANK_SPEED * calm + wiggle)
        val tc = cos(tilt)
        val ts = sin(tilt)

        // How far back the trail reaches, as a share of the trip.
        val trailShare = px * look.trailLength / span
        val step = trailShare / TRAIL_POINTS

        // Bits left behind in the air stay where they were dropped.
        when (look.kind) {
            NOVA -> drawSparkles(canvas, frame, seed, u, step, w, h, px)
            COMET -> drawEmbers(canvas, frame, seed, u, step, w, h, px)
            PIXEL -> drawSquares(canvas, seed, u, step, w, h, unit)
            else -> drawSparks(canvas, frame, seed, u, step, w, h, px)
        }

        // The trail in hero units, so its fade can use one gradient made up front.
        for (j in 0..TRAIL_POINTS) {
            place(seed, u - j * step, w, h)
            val dx = (atX - cx) * dir
            val dy = atY - cy
            trailX[j] = (dx * tc + dy * ts) / unit
            trailY[j] = (-dx * ts + dy * tc) / unit
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(dir, 1f)
        canvas.rotate(tilt * DEGREES)
        canvas.scale(unit, unit)
        val flutter = frame.time * FLUTTER_SPEED * calm + wiggle
        drawTrail(canvas, frame, look, flutter)

        glowFill.shader = look.halo
        canvas.drawCircle(HALO_X, 0f, HALO_R, glowFill)

        if (look.kind == PIXEL) {
            drawPixelHero(canvas, flutter)
        } else {
            drawBody(canvas, look, flutter)
        }
        canvas.restore()
    }

    /** Fills where the hero is at [u] (0 is off one edge, 1 is off the other) and which way it flies. */
    private fun place(seed: Int, u: Float, w: Float, h: Float) {
        val dir = if (EffectMath.hash(seed, 1) < 0.5f) 1f else -1f
        val y0 = EffectMath.lerp(LANE_TOP, LANE_BOTTOM, laneShare)
        val y1 = (y0 + (EffectMath.hash(seed, 3) - 0.5f) * LANE_DRIFT).coerceIn(LANE_TOP, LANE_BOTTOM)
        val bulge = (EffectMath.hash(seed, 4) - 0.5f) * BULGE
        val bobTurns = BOB_TURNS_MIN + BOB_TURNS_EXTRA * EffectMath.hash(seed, 5)
        val bobPhase = EffectMath.hash(seed, 6) * TWO_PI
        val span = w + startRoom + endRoom
        val bobAngle = u * bobTurns * TWO_PI + bobPhase
        atDir = dir
        atX = if (dir > 0f) -startRoom + span * u else w + startRoom - span * u
        atY = h * (y0 + (y1 - y0) * u + bulge * sin(PI_F * u) + BOB * sin(bobAngle))
        atVX = dir * span
        atVY = h * ((y1 - y0) + bulge * PI_F * cos(PI_F * u) + BOB * bobTurns * TWO_PI * cos(bobAngle))
    }

    // ------------------------------------------------------------------ Trails

    private fun drawTrail(canvas: Canvas, frame: EffectFrame, look: Look, flutter: Float) {
        when (look.kind) {
            NOVA -> {
                ribbonPath(NOVA_RIBBON_WIDE, 0f, flutter)
                shade(look.trailOuter)
                canvas.drawPath(ribbon, fill)
                ribbonPath(NOVA_RIBBON_CORE, 0f, flutter)
                shade(look.trailInner)
                canvas.drawPath(ribbon, fill)
            }
            VOLT -> {
                ribbonPath(VOLT_RIBBON, 0f, flutter)
                shade(look.trailOuter)
                canvas.drawPath(ribbon, fill)
                val tick = floor(frame.time * if (frame.reducedMotion) CRACKLE_CALM else CRACKLE_RATE).toInt()
                for (b in 0 until BOLTS) drawCrackle(canvas, look, tick, b)
            }
            COMET -> {
                ribbonPath(FLAME_WIDE, FLAME_WAVE, flutter)
                shade(look.trailOuter)
                canvas.drawPath(ribbon, fill)
                ribbonPath(FLAME_CORE, FLAME_WAVE * 0.6f, flutter + 1.7f)
                shade(look.trailInner)
                canvas.drawPath(ribbon, fill)
            }
            else -> {
                ribbonPath(PIXEL_RIBBON, 0f, flutter)
                shade(look.trailOuter)
                canvas.drawPath(ribbon, fill)
            }
        }
        fill.shader = null
    }

    /** A soft band along the trail, [half] wide at the hero and thin at the end. [wave] makes the edge ripple. */
    private fun ribbonPath(half: Float, wave: Float, flutter: Float) {
        ribbon.reset()
        val last = TRAIL_POINTS
        for (j in 0..last) {
            val f = j.toFloat() / last
            val a = if (j < last) j else last - 1
            var nx = -(trailY[a + 1] - trailY[a])
            var ny = trailX[a + 1] - trailX[a]
            val len = sqrt(nx * nx + ny * ny).coerceAtLeast(EPS)
            nx /= len
            ny /= len
            val ripple = 1f + wave * sin(f * RIPPLE_TURNS - flutter * RIPPLE_SPEED)
            val hw = half * min(1f, TRAIL_START + f * TRAIL_GROW) * (1f - f).pow(TAPER) * ripple + half * TAIL_MIN
            if (j == 0) ribbon.moveTo(trailX[j] + nx * hw, trailY[j] + ny * hw) else ribbon.lineTo(trailX[j] + nx * hw, trailY[j] + ny * hw)
        }
        for (j in last downTo 0) {
            val f = j.toFloat() / last
            val a = if (j < last) j else last - 1
            var nx = -(trailY[a + 1] - trailY[a])
            var ny = trailX[a + 1] - trailX[a]
            val len = sqrt(nx * nx + ny * ny).coerceAtLeast(EPS)
            nx /= len
            ny /= len
            val ripple = 1f + wave * sin(f * RIPPLE_TURNS + PI_F - flutter * RIPPLE_SPEED)
            val hw = half * min(1f, TRAIL_START + f * TRAIL_GROW) * (1f - f).pow(TAPER) * ripple + half * TAIL_MIN
            ribbon.lineTo(trailX[j] - nx * hw, trailY[j] - ny * hw)
        }
        ribbon.close()
    }

    /** One crackling line of light along Volt's trail. Its shape changes, its brightness does not. */
    private fun drawCrackle(canvas: Canvas, look: Look, tick: Int, b: Int) {
        crackle.reset()
        val last = TRAIL_POINTS
        val side = (b - (BOLTS - 1) / 2f) * BOLT_SPREAD
        val length = last - b * 3
        for (j in 0..length) {
            val f = j.toFloat() / last
            val a = if (j < last) j else last - 1
            var nx = -(trailY[a + 1] - trailY[a])
            var ny = trailX[a + 1] - trailX[a]
            val len = sqrt(nx * nx + ny * ny).coerceAtLeast(EPS)
            nx /= len
            ny /= len
            val jag = (EffectMath.hash(j + b * 97 + tick * 1013, 31) - 0.5f) * BOLT_JAG * (0.25f + f)
            val off = side * (0.3f + f) + jag
            val x = trailX[j] + nx * off
            val y = trailY[j] + ny * off
            if (j == 0) crackle.moveTo(x, y) else crackle.lineTo(x, y)
        }
        pen.shader = null
        pen.color = EffectMath.withAlpha(look.glow, BOLT_GLOW_ALPHA)
        pen.strokeWidth = BOLT_GLOW_WIDTH
        canvas.drawPath(crackle, pen)
        pen.color = EffectMath.withAlpha(look.accent, BOLT_MID_ALPHA)
        pen.strokeWidth = BOLT_MID_WIDTH
        canvas.drawPath(crackle, pen)
        pen.color = BOLT_CORE
        pen.strokeWidth = BOLT_CORE_WIDTH
        canvas.drawPath(crackle, pen)
    }

    /** Nova's sparkles: small four-point stars dropped along the path that twinkle and fade. */
    private fun drawSparkles(
        canvas: Canvas,
        frame: EffectFrame,
        seed: Int,
        u: Float,
        step: Float,
        w: Float,
        h: Float,
        px: Float,
    ) {
        val every = step * DROP_EVERY
        val life = step * TRAIL_POINTS * SPARKLE_LIFE
        val first = floor(u / every).toInt()
        for (q in 0 until DROPS) {
            val n = first - q
            val dropped = n * every
            val age = (u - dropped) / life
            if (age < 0f || age >= 1f) continue
            place(seed, dropped, w, h)
            val r1 = EffectMath.hash(n, seed + 41)
            val r2 = EffectMath.hash(n, seed + 42)
            val x = atX + (r1 - 0.5f) * px * SPARKLE_SCATTER
            val y = atY + (r2 - 0.5f) * px * SPARKLE_SCATTER + age * px * SPARKLE_FALL
            val twinkle = 0.75f + 0.25f * sin(frame.time * TWINKLE_SPEED + r1 * TWO_PI)
            val r = px * SPARKLE_SIZE * (0.5f + r2) * (1f - age * 0.6f) * twinkle
            val alpha = (1f - age) * (1f - age)
            glowFill.shader = looks[NOVA].dot
            glowFill.alpha = (alpha * DOT_ALPHA * 255f).toInt()
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(r, r)
            canvas.drawCircle(0f, 0f, SPARKLE_GLOW, glowFill)
            canvas.rotate(r1 * 90f)
            fill.shader = null
            fill.color = EffectMath.withAlpha(if (r1 < 0.5f) Color.WHITE else NOVA_LIGHT, alpha)
            canvas.drawPath(spark, fill)
            canvas.restore()
        }
        glowFill.alpha = 255
    }

    /** Comet's embers: hot specks dropped along the path that float up, cool and fade. */
    private fun drawEmbers(
        canvas: Canvas,
        frame: EffectFrame,
        seed: Int,
        u: Float,
        step: Float,
        w: Float,
        h: Float,
        px: Float,
    ) {
        val every = step * DROP_EVERY
        val life = step * TRAIL_POINTS * EMBER_LIFE
        val first = floor(u / every).toInt()
        for (q in 0 until DROPS) {
            val n = first - q
            val dropped = n * every
            val age = (u - dropped) / life
            if (age < 0f || age >= 1f) continue
            place(seed, dropped, w, h)
            val r1 = EffectMath.hash(n, seed + 51)
            val r2 = EffectMath.hash(n, seed + 52)
            val sway = sin(frame.time * EMBER_SWAY + r1 * TWO_PI) * px * 0.03f
            val x = atX + (r1 - 0.5f) * px * EMBER_SCATTER + sway
            val y = atY + (r2 - 0.5f) * px * EMBER_SCATTER - age * px * EMBER_RISE
            val r = px * EMBER_SIZE * (0.5f + r2) * (1f - age * 0.5f)
            val alpha = 1f - age
            glowFill.shader = looks[COMET].dot
            glowFill.alpha = (alpha * DOT_ALPHA * 255f).toInt()
            canvas.save()
            canvas.translate(x, y)
            canvas.scale(r, r)
            canvas.drawCircle(0f, 0f, EMBER_GLOW, glowFill)
            canvas.restore()
            fill.shader = null
            fill.color = EffectMath.withAlpha(if (age < 0.4f) EMBER_HOT else EMBER_COOL, alpha)
            canvas.drawCircle(x, y, r * 0.5f, fill)
        }
        glowFill.alpha = 255
    }

    /** Volt's sparks: tiny bright specks that jump off the trail. */
    private fun drawSparks(
        canvas: Canvas,
        frame: EffectFrame,
        seed: Int,
        u: Float,
        step: Float,
        w: Float,
        h: Float,
        px: Float,
    ) {
        val every = step * DROP_EVERY
        val life = step * TRAIL_POINTS * SPARK_LIFE
        val first = floor(u / every).toInt()
        pen.shader = null
        pen.strokeWidth = px * SPARK_WIDTH
        for (q in 0 until DROPS) {
            val n = first - q
            val dropped = n * every
            val age = (u - dropped) / life
            if (age < 0f || age >= 1f) continue
            place(seed, dropped, w, h)
            val r1 = EffectMath.hash(n, seed + 61)
            val r2 = EffectMath.hash(n, seed + 62)
            val angle = r1 * TWO_PI
            val reach = px * SPARK_REACH * (0.4f + r2) * EffectMath.easeOut(age)
            val x = atX + cos(angle) * reach
            val y = atY + sin(angle) * reach
            val tail = px * SPARK_TAIL * (1f - age)
            pen.color = EffectMath.withAlpha(if (r2 < 0.5f) Color.WHITE else VOLT_YELLOW, 1f - age)
            canvas.drawLine(x, y, x - cos(angle) * tail, y - sin(angle) * tail, pen)
        }
    }

    /** Pixel's trail: squares on a grid, in steps of fading, like an old game. */
    private fun drawSquares(canvas: Canvas, seed: Int, u: Float, step: Float, w: Float, h: Float, unit: Float) {
        val cell = unit * PIXEL_CELL
        val every = step * DROP_EVERY * PIXEL_DROP
        val life = step * TRAIL_POINTS * PIXEL_LIFE
        val first = floor(u / every).toInt()
        for (q in 0 until DROPS) {
            val n = first - q
            val dropped = n * every
            val age = (u - dropped) / life
            if (age < 0f || age >= 1f) continue
            place(seed, dropped, w, h)
            val r1 = EffectMath.hash(n, seed + 71)
            val r2 = EffectMath.hash(n, seed + 72)
            val big = if (r2 < 0.3f) 2f else 1f
            val gx = floor((atX + (r1 - 0.5f) * cell * 10f) / cell) * cell
            val gy = floor((atY + (r2 - 0.5f) * cell * 8f) / cell) * cell
            // Stepped fade, four steps, like a retro screen.
            val stepped = floor((1f - age) * 4f) / 4f + 0.25f
            block.color = EffectMath.withAlpha(if (n % 2 == 0) PIXEL_MAGENTA else PIXEL_CYAN, stepped.coerceAtMost(1f))
            canvas.drawRect(gx, gy, gx + cell * big, gy + cell * big, block)
        }
    }

    // ------------------------------------------------------------------ Bodies

    /** Fills with [shader] at full strength, whatever color was set before. */
    private fun shade(shader: Shader?) {
        fill.shader = shader
        fill.color = Color.BLACK
    }

    private fun drawBody(canvas: Canvas, look: Look, flutter: Float) {
        if (look.kind == NOVA) drawCape(canvas, look, flutter)

        // A dark edge all around, so the hero reads on any background.
        pen.shader = null
        pen.color = look.outline
        pen.strokeWidth = OUTLINE
        canvas.drawPath(silhouette, pen)
        if (look.kind == COMET) canvas.drawCircle(HEAD_X, HEAD_Y, DOME_R, pen) else canvas.drawOval(headBox, pen)

        shade(look.suitFar)
        canvas.drawPath(farSide, fill)
        shade(look.trimFar)
        canvas.drawPath(farTrim, fill)
        shade(look.suit)
        canvas.drawPath(torso, fill)
        canvas.drawPath(nearSide, fill)
        // A soft line where the near arm and leg cross the body.
        pen.color = EffectMath.withAlpha(look.outline, SEAM_ALPHA)
        pen.strokeWidth = SEAM
        canvas.drawPath(nearSide, pen)
        shade(look.trim)
        canvas.drawPath(nearTrim, fill)
        canvas.drawPath(belt, fill)

        when (look.kind) {
            NOVA -> {
                fill.shader = null
                glowFill.shader = look.dot
                canvas.save()
                canvas.translate(STAR_X, STAR_Y)
                canvas.scale(STAR_GLOW, STAR_GLOW)
                canvas.drawCircle(0f, 0f, 1f, glowFill)
                canvas.restore()
                fill.color = Color.WHITE
                canvas.drawPath(star, fill)
            }
            VOLT -> {
                fill.shader = null
                fill.color = VOLT_YELLOW
                canvas.drawPath(bolt, fill)
                canvas.drawPath(legStripe, fill)
            }
            else -> {
                fill.shader = null
                fill.color = COMET_GOLD
                canvas.drawPath(star, fill)
            }
        }

        // The back catches the light.
        pen.color = EffectMath.withAlpha(look.shine, SHINE_ALPHA)
        pen.strokeWidth = SHINE
        canvas.drawPath(backShine, pen)

        drawHead(canvas, look)
        fill.shader = null
        pen.shader = null
    }

    private fun drawHead(canvas: Canvas, look: Look) {
        if (look.kind == COMET) {
            glowFill.shader = look.dot
            canvas.save()
            canvas.translate(HEAD_X, HEAD_Y)
            canvas.scale(DOME_GLOW, DOME_GLOW)
            canvas.drawCircle(0f, 0f, 1f, glowFill)
            canvas.restore()
            shade(look.helmet)
            canvas.drawCircle(HEAD_X, HEAD_Y, DOME_R, fill)
            fill.shader = null
            fill.color = COMET_FACE
            canvas.drawPath(face, fill)
            pen.color = COMET_RIM
            pen.strokeWidth = DOME_RIM
            canvas.drawCircle(HEAD_X, HEAD_Y, DOME_R, pen)
            pen.color = EffectMath.withAlpha(Color.WHITE, 0.85f)
            pen.strokeWidth = DOME_SHINE
            shineBox.set(HEAD_X - DOME_R * 0.72f, HEAD_Y - DOME_R * 0.72f, HEAD_X + DOME_R * 0.72f, HEAD_Y + DOME_R * 0.72f)
            canvas.drawArc(shineBox, 200f, 70f, false, pen)
            return
        }
        shade(look.helmet)
        canvas.drawOval(headBox, fill)
        if (look.kind == NOVA) {
            shade(look.suit)
            canvas.drawPath(crest, fill)
        } else {
            fill.shader = null
            fill.color = VOLT_YELLOW
            canvas.drawPath(crest, fill)
        }
        glowFill.shader = look.dot
        canvas.save()
        canvas.translate(VISOR_GLOW_X, VISOR_GLOW_Y)
        canvas.scale(VISOR_GLOW, VISOR_GLOW)
        canvas.drawCircle(0f, 0f, 1f, glowFill)
        canvas.restore()
        shade(look.visor)
        canvas.drawPath(visor, fill)
        pen.color = EffectMath.withAlpha(Color.WHITE, HELMET_SHINE_ALPHA)
        pen.strokeWidth = HELMET_SHINE
        shineBox.set(HEAD_X - HEAD_RX * 0.7f, HEAD_Y - HEAD_RY * 0.7f, HEAD_X + HEAD_RX * 0.7f, HEAD_Y + HEAD_RY * 0.7f)
        canvas.drawArc(shineBox, 205f, 60f, false, pen)
    }

    /** Nova's cape: it flows back from the shoulders and ripples in the wind. */
    private fun drawCape(canvas: Canvas, look: Look, flutter: Float) {
        cape.reset()
        folds.reset()
        for (j in 0..CAPE_POINTS) {
            val f = j.toFloat() / CAPE_POINTS
            val x = CAPE_X - CAPE_LENGTH * f
            val y = capeMiddle(f, flutter) - (CAPE_TOP_MIN + CAPE_TOP_GROW * f)
            if (j == 0) cape.moveTo(x, y) else cape.lineTo(x, y)
        }
        // The loose end, with two soft scallops.
        val endX = CAPE_X - CAPE_LENGTH
        val endMid = capeMiddle(1f, flutter)
        val top = endMid - (CAPE_TOP_MIN + CAPE_TOP_GROW)
        val bottom = endMid + (CAPE_LOW_MIN + CAPE_LOW_GROW)
        val sway = CAPE_HEM * sin(flutter * 1.3f)
        cape.quadTo(endX - CAPE_HEM + sway, top + (bottom - top) * 0.25f, endX + sway * 0.5f, top + (bottom - top) * 0.5f)
        cape.quadTo(endX - CAPE_HEM - sway, top + (bottom - top) * 0.75f, endX, bottom)
        for (j in CAPE_POINTS downTo 0) {
            val f = j.toFloat() / CAPE_POINTS
            val x = CAPE_X - CAPE_LENGTH * f
            val y = capeMiddle(f, flutter) + (CAPE_LOW_MIN + CAPE_LOW_GROW * f)
            cape.lineTo(x, y)
        }
        cape.close()
        for (j in 2..CAPE_POINTS) {
            val f = j.toFloat() / CAPE_POINTS
            val x = CAPE_X - CAPE_LENGTH * f
            val y = capeMiddle(f, flutter) - CAPE_TOP_GROW * f * 0.35f
            if (j == 2) folds.moveTo(x, y) else folds.lineTo(x, y)
        }
        pen.shader = null
        pen.color = look.outline
        pen.strokeWidth = OUTLINE
        canvas.drawPath(cape, pen)
        shade(look.cape)
        canvas.drawPath(cape, fill)
        pen.color = EffectMath.withAlpha(NOVA_DEEP, FOLD_ALPHA)
        pen.strokeWidth = FOLD
        canvas.drawPath(folds, pen)
    }

    private fun capeMiddle(f: Float, flutter: Float): Float =
        CAPE_Y - CAPE_RISE * f + CAPE_WAVE * f.pow(1.2f) * sin(flutter - f * CAPE_WAVES)

    /** Pixel, drawn as squares. Its cape has two frames that swap a few times a second. */
    private fun drawPixelHero(canvas: Canvas, flutter: Float) {
        val frameIndex = (floor(flutter / PI_F).toInt() and 1)
        val paths = pixelPaths[frameIndex]
        canvas.save()
        canvas.translate(PIXEL_LEFT, PIXEL_TOP)
        canvas.scale(PIXEL_CELL, PIXEL_CELL)
        for (c in paths.indices) {
            block.color = PIXEL_COLORS[c]
            canvas.drawPath(paths[c], block)
        }
        canvas.restore()
    }

    // ------------------------------------------------------------------ Shapes made once

    private fun buildBody() {
        torso.moveTo(0.32f, -0.086f)
        torso.cubicTo(0.22f, -0.11f, 0.08f, -0.086f, 0f, -0.066f)
        torso.cubicTo(-0.06f, -0.058f, -0.095f, -0.01f, -0.07f, 0.035f)
        torso.cubicTo(-0.045f, 0.072f, 0.05f, 0.068f, 0.12f, 0.074f)
        torso.cubicTo(0.2f, 0.096f, 0.29f, 0.09f, 0.345f, 0.05f)
        torso.cubicTo(0.39f, 0.015f, 0.385f, -0.06f, 0.32f, -0.086f)
        torso.close()
        limb(torso, 0.31f, -0.045f, 0.044f, 0.37f, -0.055f, 0.04f)

        // The far arm reaches forward past the head. The far leg bends a little at the knee.
        limb(farSide, 0.28f, -0.06f, 0.053f, 0.44f, -0.1f, 0.041f)
        limb(farSide, 0.44f, -0.1f, 0.041f, 0.56f, -0.118f, 0.036f)
        limb(farSide, 0f, -0.016f, 0.067f, -0.24f, 0.028f, 0.052f)
        limb(farSide, -0.24f, 0.028f, 0.052f, -0.4f, -0.016f, 0.039f)
        limb(farTrim, 0.545f, -0.115f, 0.041f, 0.6f, -0.124f, 0.046f)
        ball(farTrim, 0.632f, -0.128f, 0.048f)
        limb(farTrim, -0.36f, -0.006f, 0.046f, -0.44f, -0.03f, 0.041f)
        limb(farTrim, -0.44f, -0.03f, 0.039f, -0.53f, -0.058f, 0.022f)

        // The near arm rests along the body. The near leg is straight.
        limb(nearSide, 0.27f, -0.016f, 0.055f, 0.15f, 0.066f, 0.041f)
        limb(nearSide, 0.15f, 0.066f, 0.041f, 0.04f, 0.074f, 0.036f)
        limb(nearSide, 0f, 0.012f, 0.075f, -0.25f, 0.03f, 0.057f)
        limb(nearSide, -0.25f, 0.03f, 0.057f, -0.42f, 0.028f, 0.044f)
        limb(nearTrim, 0.06f, 0.074f, 0.039f, -0.015f, 0.076f, 0.044f)
        limb(nearTrim, -0.37f, 0.029f, 0.05f, -0.5f, 0.031f, 0.046f)
        limb(nearTrim, -0.5f, 0.031f, 0.043f, -0.61f, 0.044f, 0.025f)

        silhouette.set(torso)
        silhouette.op(farSide, Path.Op.UNION)
        silhouette.op(farTrim, Path.Op.UNION)
        silhouette.op(nearSide, Path.Op.UNION)
        silhouette.op(nearTrim, Path.Op.UNION)

        val band = Path()
        band.addRect(0.02f, -0.13f, 0.075f, 0.13f, Path.Direction.CW)
        belt.set(torso)
        belt.op(band, Path.Op.INTERSECT)

        starPath(star, STAR_X, STAR_Y, STAR_R, STAR_R * 0.42f)

        bolt.moveTo(0.345f, -0.04f)
        bolt.lineTo(0.18f, 0.004f)
        bolt.lineTo(0.225f, 0.016f)
        bolt.lineTo(0.02f, 0.058f)
        bolt.lineTo(0.14f, 0.004f)
        bolt.lineTo(0.095f, -0.008f)
        bolt.lineTo(0.27f, -0.062f)
        bolt.close()
        val clip = Path(torso)
        bolt.op(clip, Path.Op.INTERSECT)
        limb(legStripe, -0.03f, 0.012f, 0.015f, -0.37f, 0.02f, 0.01f)

        backShine.moveTo(0.3f, -0.09f)
        backShine.cubicTo(0.2f, -0.104f, 0.09f, -0.086f, 0.01f, -0.07f)

        val head = Path()
        head.addOval(headBox, Path.Direction.CW)
        visor.addRect(HEAD_X + 0.005f, HEAD_Y - 0.036f, HEAD_X + 0.14f, HEAD_Y + 0.014f, Path.Direction.CW)
        visor.op(head, Path.Op.INTERSECT)
        crest.moveTo(HEAD_X - 0.09f, HEAD_Y - 0.04f)
        crest.quadTo(HEAD_X - 0.025f, HEAD_Y - 0.135f, HEAD_X + 0.07f, HEAD_Y - 0.088f)
        crest.quadTo(HEAD_X, HEAD_Y - 0.088f, HEAD_X - 0.09f, HEAD_Y - 0.04f)
        crest.close()

        // Comet's face inside the glass: a dark side view, chin forward.
        face.moveTo(HEAD_X - 0.045f, HEAD_Y - 0.06f)
        face.cubicTo(HEAD_X + 0.02f, HEAD_Y - 0.085f, HEAD_X + 0.07f, HEAD_Y - 0.05f, HEAD_X + 0.065f, HEAD_Y - 0.01f)
        face.lineTo(HEAD_X + 0.078f, HEAD_Y + 0.002f)
        face.lineTo(HEAD_X + 0.062f, HEAD_Y + 0.012f)
        face.cubicTo(HEAD_X + 0.06f, HEAD_Y + 0.045f, HEAD_X + 0.02f, HEAD_Y + 0.06f, HEAD_X - 0.02f, HEAD_Y + 0.06f)
        face.cubicTo(HEAD_X - 0.07f, HEAD_Y + 0.05f, HEAD_X - 0.08f, HEAD_Y - 0.04f, HEAD_X - 0.045f, HEAD_Y - 0.06f)
        face.close()

        // A four-point sparkle, one unit across.
        spark.moveTo(0f, -1f)
        spark.quadTo(0.12f, -0.12f, 1f, 0f)
        spark.quadTo(0.12f, 0.12f, 0f, 1f)
        spark.quadTo(-0.12f, 0.12f, -1f, 0f)
        spark.quadTo(-0.12f, -0.12f, 0f, -1f)
        spark.close()
    }

    private fun buildPixelHero() {
        for (f in 0..1) {
            val rows = if (f == 0) PIXEL_FRAME_A else PIXEL_FRAME_B
            for (y in rows.indices) {
                val row = rows[y]
                for (x in row.indices) {
                    val c = PIXEL_KEYS.indexOf(row[x])
                    if (c < 0) continue
                    pixelPaths[f][c].addRect(x.toFloat(), y.toFloat(), x + 1.02f, y + 1.02f, Path.Direction.CW)
                }
            }
        }
    }

    /** The colors and fades for one hero, made once. */
    private class Look(
        val kind: Int,
        val suit: Shader,
        val suitFar: Shader,
        val trim: Shader,
        val trimFar: Shader,
        val helmet: Shader,
        val visor: Shader,
        val cape: Shader?,
        val halo: Shader,
        val dot: Shader,
        val trailOuter: Shader,
        val trailInner: Shader,
        val outline: Int,
        val shine: Int,
        val glow: Int,
        val accent: Int,
        val trailLength: Float,
    )

    private companion object {
        const val NOVA = 0
        const val VOLT = 1
        const val COMET = 2
        const val PIXEL = 3

        const val MAX_HEROES = 6
        const val SIZE_MIN = 0.08f
        const val SIZE_MAX = 0.35f
        const val CROSS_SLOW = 7f
        const val CROSS_FAST = 1.5f
        const val CALM_SLOWER = 2f
        const val CALM_FLUTTER = 0.5f

        /** One hero's length (fist to toe) in hero units. */
        const val HERO_LENGTH = 1.3f

        /** How far off the edge a trip starts, in hero sizes. The trip ends further off, past the trail. */
        const val START_ROOM = 0.7f

        /** A hero stays a moment after its trip, off screen, so its last sparkles fade out. */
        const val LINGER = 0.1f

        /** The far end of a trail is faint, so a trip can end when most of the trail is off screen. */
        const val TRAIL_SEEN = 0.75f
        const val LANE_BASE = 0.85f
        const val LANE_SPREAD = 0.35f
        const val GAP_MIN = 0.12f
        const val GAP_EXTRA = 0.2f
        const val STAGGER_JITTER = 0.18f
        const val SEED_STEP = 7919
        const val DEPTH_MIN = 0.7f
        const val LANE_EDGE = 0.1f
        const val LANE_TOP = 0.08f
        const val LANE_BOTTOM = 0.86f
        const val LANE_DRIFT = 0.3f
        const val BULGE = 0.16f
        const val BOB = 0.012f
        const val BOB_TURNS_MIN = 1f
        const val BOB_TURNS_EXTRA = 1.5f
        const val MAX_TILT = 0.6f
        const val BANK = 0.05f
        const val BANK_SPEED = 1.3f
        const val FLUTTER_SPEED = 9f

        const val TWO_PI = (2.0 * PI).toFloat()
        const val PI_F = PI.toFloat()
        const val DEGREES = (180.0 / PI).toFloat()
        const val EPS = 1e-4f

        // The trail.
        const val TRAIL_POINTS = 18
        const val TAPER = 0.8f
        const val TRAIL_START = 0.3f
        const val TRAIL_GROW = 5f
        const val TAIL_MIN = 0.04f
        const val RIPPLE_TURNS = 9f
        const val RIPPLE_SPEED = 1.6f
        const val NOVA_RIBBON_WIDE = 0.15f
        const val NOVA_RIBBON_CORE = 0.045f
        const val VOLT_RIBBON = 0.09f
        const val FLAME_WIDE = 0.2f
        const val FLAME_CORE = 0.09f
        const val FLAME_WAVE = 0.2f
        const val PIXEL_RIBBON = 0.07f
        const val BOLTS = 3
        const val BOLT_SPREAD = 0.05f
        const val BOLT_JAG = 0.16f
        const val CRACKLE_RATE = 12f
        const val CRACKLE_CALM = 4f
        const val BOLT_GLOW_ALPHA = 0.28f
        const val BOLT_MID_ALPHA = 0.85f
        const val BOLT_GLOW_WIDTH = 0.06f
        const val BOLT_MID_WIDTH = 0.02f
        const val BOLT_CORE_WIDTH = 0.008f
        const val BOLT_CORE = 0xFFFFFFF4.toInt()

        // Bits dropped along the path.
        const val DROPS = 16
        const val DROP_EVERY = 1.1f
        const val DOT_ALPHA = 0.9f
        const val SPARKLE_LIFE = 1.5f
        const val SPARKLE_SCATTER = 0.35f
        const val SPARKLE_FALL = 0.12f
        const val SPARKLE_SIZE = 0.035f
        const val SPARKLE_GLOW = 2.6f
        const val TWINKLE_SPEED = 5f
        const val EMBER_LIFE = 1.3f
        const val EMBER_SCATTER = 0.3f
        const val EMBER_RISE = 0.35f
        const val EMBER_SIZE = 0.02f
        const val EMBER_GLOW = 3f
        const val EMBER_SWAY = 3f
        const val SPARK_LIFE = 0.9f
        const val SPARK_REACH = 0.35f
        const val SPARK_TAIL = 0.07f
        const val SPARK_WIDTH = 0.008f
        const val PIXEL_DROP = 0.8f
        const val PIXEL_LIFE = 1.4f

        // The body, in hero units.
        const val OUTLINE = 0.024f
        const val SEAM = 0.008f
        const val SEAM_ALPHA = 0.55f
        const val SHINE = 0.012f
        const val SHINE_ALPHA = 0.55f
        const val HEAD_X = 0.405f
        const val HEAD_Y = -0.056f
        const val HEAD_RX = 0.1f
        const val HEAD_RY = 0.094f
        const val DOME_R = 0.125f
        const val DOME_GLOW = 0.34f
        const val DOME_RIM = 0.01f
        const val DOME_SHINE = 0.012f
        const val HELMET_SHINE = 0.01f
        const val HELMET_SHINE_ALPHA = 0.6f
        const val VISOR_GLOW_X = HEAD_X + 0.06f
        const val VISOR_GLOW_Y = HEAD_Y - 0.01f
        const val VISOR_GLOW = 0.12f
        const val STAR_X = 0.2f
        const val STAR_Y = 0.01f
        const val STAR_R = 0.05f
        const val STAR_GLOW = 0.12f
        const val HALO_X = 0.05f
        const val HALO_R = 0.75f

        // Nova's cape.
        const val CAPE_POINTS = 12
        const val CAPE_X = 0.31f
        const val CAPE_Y = -0.09f
        const val CAPE_LENGTH = 1.02f
        const val CAPE_RISE = 0.05f
        const val CAPE_WAVE = 0.055f
        const val CAPE_WAVES = 7f
        const val CAPE_TOP_MIN = 0.012f
        const val CAPE_TOP_GROW = 0.075f
        const val CAPE_LOW_MIN = 0.012f
        const val CAPE_LOW_GROW = 0.04f
        const val CAPE_HEM = 0.035f
        const val FOLD = 0.01f
        const val FOLD_ALPHA = 0.35f

        // Pixel, drawn on a grid of squares.
        const val PIXEL_CELL = 0.042f
        const val PIXEL_LEFT = -0.69f
        const val PIXEL_TOP = -0.3f
        const val PIXEL_KEYS = "kMmCcWYpq"
        val PIXEL_COLORS = intArrayOf(
            0xFF1A0B2E.toInt(), 0xFFFF2BD6.toInt(), 0xFFA3128A.toInt(), 0xFF2BF5F0.toInt(),
            0xFF0E8C9E.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFE14D.toInt(), 0xFF781CA0.toInt(), 0xFF500E6E.toInt(),
        )
        const val PIXEL_MAGENTA = 0xFFFF2BD6.toInt()
        const val PIXEL_CYAN = 0xFF2BF5F0.toInt()
        val PIXEL_FRAME_A = arrayOf(
            ".kkkkk..........................",
            "kpppppkkkk..........kkk.........",
            "kpppppppppkkkkk....kCCCk.....kk.",
            ".kqqqppppppppppkkkkCCCCCk.kkkCCk",
            "..kkkkqqqppppppppppCCCCCkkMMMCCk",
            "..kcckkkkkqqqqpppppCCCWWMMkkkkk.",
            "..kccmmkkkkCMMMMMMMCCCWWkk......",
            "kkkkkkmmmmmCMMYYMMMcCCCCk.......",
            "CCCMMMMMMMMCMMYYMMMcccCCk.......",
            "CCCMMMMMMMMCMMMMMMMkkkkk........",
            "kkkkkkkkkkkkCmmmmmk.............",
            "............kkkkkk..............",
        )
        val PIXEL_FRAME_B = arrayOf(
            "................................",
            "....................kkk.........",
            "..........kkkk.....kCCCk.....kk.",
            ".....kkkkkppppkkkkkCCCCCk.kkkCCk",
            ".kkkkppppppppppppppCCCCCkkMMMCCk",
            "kppccppppppppppppppCCCWWMMkkkkk.",
            "kppccmmqkqqCMMMMMMMCCCWWkk......",
            "kkqqkkmmmmmCMMYYMMMcCCCCk.......",
            "CCCMMMMMMMMCMMYYMMMcccCCk.......",
            "CCCMMMMMMMMCMMMMMMMkkkkk........",
            "kkkkkkkkkkkkCmmmmmk.............",
            "............kkkkkk..............",
        )

        // Colors.
        const val NOVA_LIGHT = 0xFF8FFFF6.toInt()
        const val NOVA_TEAL = 0xFF2BF5E6.toInt()
        const val NOVA_DEEP = 0xFF06505A.toInt()
        const val VOLT_YELLOW = 0xFFFFE01A.toInt()
        const val COMET_GOLD = 0xFFFFD36B.toInt()
        const val COMET_FACE = 0x4D5A1A06
        const val COMET_RIM = 0xFFFFE2A0.toInt()
        const val EMBER_HOT = 0xFFFFF0B0.toInt()
        const val EMBER_COOL = 0xFFFF6A1A.toInt()

        fun styleIndex(choice: String): Int = when (choice) {
            FxKeys.HERO_VOLT -> VOLT
            FxKeys.HERO_COMET -> COMET
            FxKeys.HERO_PIXEL -> PIXEL
            else -> NOVA
        }

        /** Adds a rounded limb from one point to another, thick at the start and thinner at the end. */
        fun limb(into: Path, x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) {
            val dx = x1 - x0
            val dy = y1 - y0
            val len = sqrt(dx * dx + dy * dy)
            val nx = -dy / len
            val ny = dx / len
            val piece = Path()
            piece.moveTo(x0 + nx * r0, y0 + ny * r0)
            piece.lineTo(x1 + nx * r1, y1 + ny * r1)
            piece.lineTo(x1 - nx * r1, y1 - ny * r1)
            piece.lineTo(x0 - nx * r0, y0 - ny * r0)
            piece.close()
            val end = Path()
            end.addCircle(x0, y0, r0, Path.Direction.CW)
            piece.op(end, Path.Op.UNION)
            end.reset()
            end.addCircle(x1, y1, r1, Path.Direction.CW)
            piece.op(end, Path.Op.UNION)
            into.op(piece, Path.Op.UNION)
        }

        /** Adds a round ball, like a fist, joined cleanly to what is already in [into]. */
        fun ball(into: Path, x: Float, y: Float, r: Float) {
            val piece = Path()
            piece.addCircle(x, y, r, Path.Direction.CW)
            into.op(piece, Path.Op.UNION)
        }

        /** A five-point star around [cx], [cy], one point forward. */
        fun starPath(into: Path, cx: Float, cy: Float, outer: Float, inner: Float) {
            for (k in 0 until 10) {
                val r = if (k % 2 == 0) outer else inner
                val a = k * PI_F / 5f
                val x = cx + cos(a) * r
                val y = cy + sin(a) * r
                if (k == 0) into.moveTo(x, y) else into.lineTo(x, y)
            }
            into.close()
        }

        fun down(top: Float, bottom: Float, colors: IntArray, stops: FloatArray? = null): Shader =
            LinearGradient(0f, top, 0f, bottom, colors, stops, Shader.TileMode.CLAMP)

        fun glowSpot(color: Int, strength: Float): Shader = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(EffectMath.withAlpha(color, strength), EffectMath.withAlpha(color, strength * 0.35f), 0),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )

        fun halo(color: Int, strength: Float): Shader = RadialGradient(
            HALO_X,
            0f,
            HALO_R,
            intArrayOf(EffectMath.withAlpha(color, strength), EffectMath.withAlpha(color, strength * 0.3f), 0),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )

        /** A trail fade from the hero (bright) to the end of its tail (gone), along the hero's back. */
        fun trailFade(length: Float, colors: IntArray, stops: FloatArray?): Shader =
            LinearGradient(0f, 0f, -length * HERO_LENGTH, 0f, colors, stops, Shader.TileMode.CLAMP)

        fun novaLook(): Look = Look(
            kind = NOVA,
            suit = down(-0.1f, 0.08f, intArrayOf(0xFF9BFFF7.toInt(), 0xFF22D3C8.toInt(), 0xFF0A7C86.toInt(), 0xFF064450.toInt()), floatArrayOf(0f, 0.3f, 0.7f, 1f)),
            suitFar = down(-0.1f, 0.08f, intArrayOf(0xFF2FB5B0.toInt(), 0xFF0B6F78.toInt(), 0xFF043640.toInt())),
            trim = down(-0.1f, 0.09f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFE8FBFA.toInt(), 0xFF9CC9CC.toInt())),
            trimFar = down(-0.13f, 0.06f, intArrayOf(0xFFDDF4F3.toInt(), 0xFF8FB4B8.toInt())),
            helmet = RadialGradient(HEAD_X + 0.02f, HEAD_Y - 0.04f, 0.12f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFE2F2F3.toInt(), 0xFF8DAAB0.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP),
            visor = down(HEAD_Y - 0.03f, HEAD_Y + 0.012f, intArrayOf(0xFFE8FFFD.toInt(), NOVA_TEAL, 0xFF0B8C92.toInt())),
            cape = LinearGradient(CAPE_X, 0f, CAPE_X - CAPE_LENGTH, 0f, intArrayOf(0xFFF6FFFE.toInt(), 0xFFD8F6F4.toInt(), 0xFF8FDCDC.toInt()), null, Shader.TileMode.CLAMP),
            halo = halo(NOVA_TEAL, 0.22f),
            dot = glowSpot(NOVA_TEAL, 0.8f),
            trailOuter = trailFade(1.5f, intArrayOf(0x662BF5E6, 0x332BF5E6, 0x00000000), floatArrayOf(0f, 0.45f, 1f)),
            trailInner = trailFade(1.5f, intArrayOf(0xEEFFFFFF.toInt(), 0x998FFFF6.toInt(), 0x00000000), floatArrayOf(0f, 0.35f, 1f)),
            outline = 0xE0061A22.toInt(),
            shine = 0xFFFFFFFF.toInt(),
            glow = NOVA_TEAL,
            accent = NOVA_LIGHT,
            trailLength = 1.5f,
        )

        fun voltLook(): Look = Look(
            kind = VOLT,
            suit = down(-0.1f, 0.08f, intArrayOf(0xFF4A4D57.toInt(), 0xFF22242B.toInt(), 0xFF0E0F13.toInt(), 0xFF050506.toInt()), floatArrayOf(0f, 0.3f, 0.7f, 1f)),
            suitFar = down(-0.1f, 0.08f, intArrayOf(0xFF2A2C33.toInt(), 0xFF101115.toInt(), 0xFF040405.toInt())),
            trim = down(-0.1f, 0.09f, intArrayOf(0xFFFFF6B0.toInt(), VOLT_YELLOW, 0xFFC99A00.toInt())),
            trimFar = down(-0.13f, 0.06f, intArrayOf(0xFFE9C400.toInt(), 0xFF9C7600.toInt())),
            helmet = RadialGradient(HEAD_X + 0.02f, HEAD_Y - 0.04f, 0.12f, intArrayOf(0xFF6A6E78.toInt(), 0xFF24262D.toInt(), 0xFF08080A.toInt()), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP),
            visor = down(HEAD_Y - 0.03f, HEAD_Y + 0.012f, intArrayOf(0xFFFFFFE0.toInt(), VOLT_YELLOW, 0xFFD08A00.toInt())),
            cape = null,
            halo = halo(VOLT_YELLOW, 0.2f),
            dot = glowSpot(VOLT_YELLOW, 0.8f),
            trailOuter = trailFade(1.6f, intArrayOf(0x88FFE01A.toInt(), 0x3384D8FF, 0x00000000), floatArrayOf(0f, 0.5f, 1f)),
            trailInner = trailFade(1.6f, intArrayOf(0xEEFFFFFF.toInt(), 0x00000000), null),
            outline = 0xCCFFE680.toInt(),
            shine = 0xFFB8C4FF.toInt(),
            glow = 0xFF7FD4FF.toInt(),
            accent = VOLT_YELLOW,
            trailLength = 1.6f,
        )

        fun cometLook(): Look = Look(
            kind = COMET,
            suit = down(-0.1f, 0.08f, intArrayOf(0xFFFFB066.toInt(), 0xFFF0602A.toInt(), 0xFFB52A12.toInt(), 0xFF5E1206.toInt()), floatArrayOf(0f, 0.3f, 0.7f, 1f)),
            suitFar = down(-0.1f, 0.08f, intArrayOf(0xFFC24A1E.toInt(), 0xFF7E1E0C.toInt(), 0xFF3E0A04.toInt())),
            trim = down(-0.1f, 0.09f, intArrayOf(0xFFFFF1C2.toInt(), COMET_GOLD, 0xFFC27A1E.toInt())),
            trimFar = down(-0.13f, 0.06f, intArrayOf(0xFFE0A84A.toInt(), 0xFF8C5414.toInt())),
            helmet = RadialGradient(HEAD_X + 0.025f, HEAD_Y - 0.02f, DOME_R * 1.1f, intArrayOf(0xFFFFFDF0.toInt(), 0xFFFFE08A.toInt(), 0xEEFF9A2E.toInt(), 0xDDFF5A0A.toInt()), floatArrayOf(0f, 0.3f, 0.75f, 1f), Shader.TileMode.CLAMP),
            visor = down(0f, 1f, intArrayOf(0, 0)),
            cape = null,
            halo = halo(0xFFFF8A1A.toInt(), 0.28f),
            dot = glowSpot(0xFFFF9A2E.toInt(), 0.85f),
            trailOuter = trailFade(2.1f, intArrayOf(0xDDFF8A1A.toInt(), 0xAAF0441A.toInt(), 0x55B3160A, 0x00000000), floatArrayOf(0f, 0.35f, 0.7f, 1f)),
            trailInner = trailFade(2.1f, intArrayOf(0xFFFFFDF0.toInt(), 0xEEFFD45A.toInt(), 0x66FF8A1A, 0x00000000), floatArrayOf(0f, 0.25f, 0.6f, 1f)),
            outline = 0xE02A0A04.toInt(),
            shine = 0xFFFFE6B0.toInt(),
            glow = 0xFFFF8A1A.toInt(),
            accent = COMET_GOLD,
            trailLength = 2.1f,
        )

        fun pixelLook(): Look {
            val none = down(0f, 1f, intArrayOf(0, 0))
            return Look(
                kind = PIXEL,
                suit = none,
                suitFar = none,
                trim = none,
                trimFar = none,
                helmet = none,
                visor = none,
                cape = null,
                halo = halo(PIXEL_MAGENTA, 0.16f),
                dot = glowSpot(PIXEL_CYAN, 0.6f),
                trailOuter = trailFade(1.4f, intArrayOf(0x66FF2BD6, 0x332BF5F0, 0x00000000), floatArrayOf(0f, 0.5f, 1f)),
                trailInner = none,
                outline = 0xFF1A0B2E.toInt(),
                shine = Color.WHITE,
                glow = PIXEL_CYAN,
                accent = PIXEL_MAGENTA,
                trailLength = 1.4f,
            )
        }
    }
}
