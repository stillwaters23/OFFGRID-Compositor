package com.offgrid.compositor.terminal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * The panel's glass.
 *
 * The glass sits [margin] inside the drawable's edges. That margin is room for the
 * outer glow. The soft glows are blurred once into small gray masks, at half size,
 * then tinted each frame, so the pulse stays cheap on the phone.
 *
 * Glow strength 50 is the normal look. Above 50 the glow grows: a second layer of
 * outer and edge glow, a wash of light inside the edge, and a brighter, thicker edge
 * line, so 100 is big and bright even when the panel fills the screen.
 *
 * Titanium frame (Step 2.2): a band of dark brushed titanium around the glass, with a
 * light bevel on the outside, a dark bevel on the inside, and a thin line of light.
 * The title bar sits on woven carbon fiber. Everything is drawn on the phone from
 * small made-up textures. No pictures, and no web code.
 *
 * [pane] draws an inner pane with no outer glow, for open folders and the typed guidance.
 * With Sapphire glass on, a pane is thick, deep, frosted glass with an inner shadow,
 * so it looks set into the panel.
 */
internal class PanelGlassDrawable(
    private val density: Float,
    margin: Float,
    private val pane: Boolean = false,
) : Drawable() {
    var margin = margin
        set(value) {
            if (field == value) return
            field = value
            rebuildShape()
            updateFill()
            invalidateSelf()
        }

        private fun drawResizeBar(canvas: Canvas, glow: Masks, current: PanelLook) {
            val top = resizeBarTop(current)
            resizeMaskTarget.set(
                bounds.left.toFloat(),
                bounds.top + top / MASK_SCALE,
                bounds.right.toFloat(),
                bounds.top + top / MASK_SCALE + glow.resizeBar.height / MASK_SCALE,
            )
            maskPaint.blendMode = null
            maskPaint.color = Hud.lighten(current.edge, 0.6f)
            maskPaint.alpha = 255
            canvas.drawBitmap(glow.resizeBar, null, resizeMaskTarget, maskPaint)
            val level = current.glowValue
            if (level <= 0) return
            val pulse = resizeBarPulse
            drawAdditiveMask(
                canvas,
                glow.resizeCore,
                top,
                Hud.lighten(current.edge, 0.6f),
                PanelGeometry.resizeBarLayerAlpha(level, 0) * pulse,
            )
            drawAdditiveMask(
                canvas,
                glow.resizeMid,
                top,
                current.edge,
                PanelGeometry.resizeBarLayerAlpha(level, 1) * pulse,
            )
            drawAdditiveMask(
                canvas,
                glow.resizeOuter,
                top,
                current.outerGlow,
                PanelGeometry.resizeBarLayerAlpha(level, 2) * pulse,
            )
        }

        private fun resizeBarTop(current: PanelLook): Float {
            val radius = dp(PanelGeometry.resizeBarGlowRadiusDp(current.glowValue).coerceAtLeast(1f))
            val halfRange = radius + dp(PanelGeometry.RESIZE_BAR_HEIGHT_DP) / 2f + dp(2f)
            return ((glass.bottom - dp(PanelGeometry.borderInsetDp(current.borderThickness)) -
                halfRange - bounds.top) * MASK_SCALE).coerceAtLeast(0f)
        }

        private fun drawAdditiveMask(canvas: Canvas, mask: Bitmap, top: Float, color: Int, alpha: Float) {
            if (alpha <= 0f) return
            maskPaint.blendMode = BlendMode.PLUS
            maskPaint.color = color
            maskPaint.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            resizeMaskTarget.set(
                bounds.left.toFloat(),
                bounds.top + top / MASK_SCALE,
                bounds.right.toFloat(),
                bounds.top + top / MASK_SCALE + mask.height / MASK_SCALE,
            )
            canvas.drawBitmap(mask, null, resizeMaskTarget, maskPaint)
            maskPaint.blendMode = null
        }

    var fullBleed = false
        set(value) {
            if (field == value) return
            field = value
            rebuildShape()
            updateFill()
            invalidateSelf()
        }

    var seeThroughAtFullSize = false
        set(value) {
            if (field == value) return
            field = value
            updateFill()
            invalidateSelf()
        }


    var look: PanelLook? = null
        set(value) {
            val reshape = field?.roundness != value?.roundness || field?.holo != value?.holo ||
                field?.titanium != value?.titanium || field?.borderThickness != value?.borderThickness
            field = value
            if (reshape) rebuildShape()
            updateFill()
            invalidateSelf()
        }

    /** 0 to 1. How bright the breathing glow is right now. */
    var breath = 0.8f

    var resizeBarPulse = 1f
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    var resizeBarVisible = true
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    /** While the panel is being resized, keep the old glow masks stretched instead of redrawing them. */
    var holdMasks = false
        set(value) {
            field = value
            if (!value) invalidateSelf()
        }

    /** Height of the title bar, in pixels from the top of the glass inside the frame. It sits on carbon fiber. */
    var headerHeight = 0f
        set(value) {
            if (field == value) return
            field = value
            updateFill()
            invalidateSelf()
        }

    /**
     * How far inside the glass edge the panel's content should start, in pixels.
     * The titanium frame is drawn in this band. It never changes with size, so the
     * panel and the small tag keep steady padding.
     */
    val frameInset: Float
        get() = if (titaniumOn()) dp(FRAME_DP) else 0f

    private val glass = RectF()
    private val inside = RectF()
    private val shape = Path()
    private val edgeShape = Path()
    private val innerShape = Path()
    private val frameBand = Path()
    private val brackets = Path()
    private val ticks = Path()
    private val inner = Path()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metal = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brushed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val carbon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bevelLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskTarget = RectF()
    private val resizeMaskTarget = RectF()
    private val masks = LinkedHashMap<String, Masks>()
    private var displayCornerRadii: FloatArray? = null

    // Sapphire pane layers, built when the size or the look changes.
    private var topShadow: LinearGradient? = null
    private var sideShadow: LinearGradient? = null
    private var bottomLight: LinearGradient? = null
    private var glassSheen: LinearGradient? = null
    private var titleShadow: LinearGradient? = null

    private class Masks(
        val outer: Bitmap,
        val bloom: Bitmap,
        val edge: Bitmap,
        val wash: Bitmap,
        val resizeBar: Bitmap,
        val resizeCore: Bitmap,
        val resizeMid: Bitmap,
        val resizeOuter: Bitmap,
    )

    private class ResizeMasks(
        val bar: Bitmap,
        val core: Bitmap,
        val mid: Bitmap,
        val outer: Bitmap,
    )

    private fun dp(value: Float) = value * density

    private fun titaniumOn(): Boolean = !pane && look?.titanium == true

    private fun sapphireOn(): Boolean = pane && look?.sapphire == true

    fun setDisplayCornerRadii(radii: FloatArray) {
        if (displayCornerRadii?.contentEquals(radii) == true) return
        displayCornerRadii = radii.copyOf()
        rebuildShape()
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rebuildShape()
        updateFill()
    }

    private fun corner(box: RectF = glass): Float {
        val roundness = look?.roundness ?: 0.04f
        val most = minOf(box.width(), box.height()) / 2f
        return (roundness * dp(30f)).coerceAtMost(most).coerceAtLeast(0f)
    }

    private fun rebuildShape() {
        val b = bounds
        if (b.isEmpty) return
        glass.set(b.left + margin, b.top + margin, b.right - margin, b.bottom - margin)
        shape.reset()
        edgeShape.reset()
        inner.reset()
        innerShape.reset()
        frameBand.reset()
        brackets.reset()
        ticks.reset()
        if (glass.width() <= 0f || glass.height() <= 0f) return
        val holo = look?.holo == true
        val size = corner()
        if (fullBleed) {
            val radii = displayCornerRadii ?: FloatArray(8) { size }
            shape.addRoundRect(glass, radii, Path.Direction.CW)
            if (holo) {
                val innerBox = RectF(glass)
                innerBox.inset(dp(4.5f), dp(4.5f))
                cutCorners(inner, innerBox, size * 0.75f, dp(4f))
                if (!pane) buildHoloMarks(size)
            }
        } else if (holo) {
            cutCorners(shape, glass, size, dp(6f))
            val innerBox = RectF(glass)
            innerBox.inset(dp(4.5f), dp(4.5f))
            cutCorners(inner, innerBox, size * 0.75f, dp(4f))
            if (!pane) buildHoloMarks(size)
        } else {
            shape.addRoundRect(glass, size, size, Path.Direction.CW)
        }
        inside.set(glass)
        if (titaniumOn()) {
            val band = dp(FRAME_DP)
            inside.inset(band, band)
            if (inside.width() > 0f && inside.height() > 0f) {
                val innerCorner = (size - band).coerceAtLeast(0f)
                if (holo) {
                    cutCorners(innerShape, inside, (size - band * 0.6f).coerceAtLeast(0f), dp(3f))
                } else {
                    innerShape.addRoundRect(inside, innerCorner, innerCorner, Path.Direction.CW)
                }
                frameBand.op(shape, innerShape, Path.Op.DIFFERENCE)
            } else {
                inside.set(glass)
            }
        }
        if (fullBleed) {
            val borderInset = dp(PanelGeometry.borderInsetDp(look?.borderThickness ?: 50))
            val borderBox = RectF(glass).apply { inset(borderInset, borderInset) }
            if (!borderBox.isEmpty) {
                val radii = displayCornerRadii ?: FloatArray(8) { size }
                val innerRadii = FloatArray(radii.size) { index ->
                    PanelGeometry.borderCornerRadius(radii[index], borderInset)
                }
                edgeShape.addRoundRect(borderBox, innerRadii, Path.Direction.CW)
            }
        } else {
            edgeShape.set(shape)
        }
    }

    private fun cutCorners(path: Path, box: RectF, cut: Float, round: Float) {
        path.moveTo(box.left + cut, box.top)
        path.lineTo(box.right - round, box.top)
        path.quadTo(box.right, box.top, box.right, box.top + round)
        path.lineTo(box.right, box.bottom - cut)
        path.lineTo(box.right - cut, box.bottom)
        path.lineTo(box.left + round, box.bottom)
        path.quadTo(box.left, box.bottom, box.left, box.bottom - round)
        path.lineTo(box.left, box.top + cut)
        path.close()
    }

    /** Holo Classic's gold corner brackets and the little ruler ticks. */
    private fun buildHoloMarks(cut: Float) {
        val box = glass
        val arm = minOf(dp(22f), box.width() / 5f, box.height() / 3f)
        brackets.moveTo(box.right - arm, box.top)
        brackets.lineTo(box.right - dp(6f), box.top)
        brackets.quadTo(box.right, box.top, box.right, box.top + dp(6f))
        brackets.lineTo(box.right, box.top + arm)
        brackets.moveTo(box.left, box.bottom - arm)
        brackets.lineTo(box.left, box.bottom - dp(6f))
        brackets.quadTo(box.left, box.bottom, box.left + dp(6f), box.bottom)
        brackets.lineTo(box.left + arm, box.bottom)
        val dashStart = box.left + cut + dp(10f)
        brackets.moveTo(dashStart, box.top)
        brackets.lineTo(minOf(dashStart + dp(38f), box.right - arm - dp(8f)), box.top)
        var y = box.top + box.height() * 0.32f
        var index = 0
        while (y <= box.top + box.height() * 0.68f) {
            val length = if (index % 4 == 0) dp(7f) else dp(4f)
            ticks.moveTo(box.left + dp(2.5f), y)
            ticks.lineTo(box.left + dp(2.5f) + length, y)
            y += dp(7f)
            index++
        }
    }

    private fun updateFill() {
        val current = look ?: return
        if (glass.isEmpty) return
        val opacity = if (fullBleed && !seeThroughAtFullSize) 1f else current.opacity
        fill.shader = null
        when {
            sapphireOn() -> buildSapphire(current)
            pane && current.holo -> fill.shader = LinearGradient(
                0f, glass.top, 0f, glass.bottom,
                Hud.withAlpha(0xFF0B3A44.toInt(), 0.35f + 0.55f * opacity),
                Hud.withAlpha(Hud.HOLO_GLASS_BOTTOM, 0.45f + 0.5f * opacity),
                Shader.TileMode.CLAMP,
            )
            pane -> fill.color = Hud.withAlpha(Hud.blend(current.skin, current.edge, 0.07f), 0.5f + 0.45f * opacity)
            current.holo -> fill.shader = LinearGradient(
                0f, glass.top, 0f, glass.bottom,
                Hud.withAlpha(current.skin, opacity),
                Hud.withAlpha(Hud.blend(current.skin, Hud.INK, 0.75f), opacity),
                Shader.TileMode.CLAMP,
            )
            else -> fill.color = Hud.withAlpha(current.skin, opacity)
        }
        if (current.holo && !pane) {
            grain.shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            grain.alpha = (255 * (0.25f + 0.75f * opacity)).toInt()
            sheen.shader = LinearGradient(
                0f, glass.top, 0f, glass.top + glass.height() * 0.35f,
                Hud.withAlpha(Hud.WHITE, 0.04f + 0.06f * opacity),
                0x00FFFFFF,
                Shader.TileMode.CLAMP,
            )
        }
        if (titaniumOn()) buildTitanium(current)
    }

    /** Dark brushed titanium: a cool metal gradient, fine brushed streaks, and carbon fiber for the title bar. */
    private fun buildTitanium(current: PanelLook) {
        val opacity = if (fullBleed && !seeThroughAtFullSize) 1f else current.opacity
        val frameAlpha = 0.35f + 0.65f * opacity
        metal.shader = LinearGradient(
            0f, glass.top, 0f, glass.bottom,
            intArrayOf(
                Hud.withAlpha(0xFF4A5157.toInt(), frameAlpha),
                Hud.withAlpha(0xFF23282C.toInt(), frameAlpha),
                Hud.withAlpha(0xFF15181B.toInt(), frameAlpha),
                Hud.withAlpha(0xFF2B3035.toInt(), frameAlpha),
            ),
            floatArrayOf(0f, 0.18f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        brushed.shader = BitmapShader(brushedBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        brushed.alpha = (255 * 0.55f * frameAlpha).toInt()
        carbon.shader = BitmapShader(carbonTile(density), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        carbon.alpha = (255 * (0.25f + 0.65f * opacity)).toInt()
        bevelLight.shader = LinearGradient(
            glass.left, glass.top, glass.right, glass.bottom,
            intArrayOf(Hud.withAlpha(Hud.WHITE, 0.45f * frameAlpha), Hud.withAlpha(Hud.WHITE, 0.08f), 0x00FFFFFF),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        val barBottom = minOf(inside.top + headerHeight, inside.bottom)
        titleShadow = LinearGradient(
            0f, barBottom, 0f, barBottom + dp(5f),
            Hud.withAlpha(Hud.INK, 0.55f), 0x00000000, Shader.TileMode.CLAMP,
        )
    }

    /** Thick sapphire glass: deep tinted color, frost, an inner shadow at the top and left, and a soft sheen. */
    private fun buildSapphire(current: PanelLook) {
        val opacity = current.opacity
        val tint = current.glassTint
        fill.shader = LinearGradient(
            0f, glass.top, 0f, glass.bottom,
            intArrayOf(
                Hud.withAlpha(Hud.lighten(tint, 0.06f), 0.8f + 0.18f * opacity),
                Hud.withAlpha(tint, 0.82f + 0.16f * opacity),
                Hud.withAlpha(Hud.blend(tint, Hud.INK, 0.55f), 0.86f + 0.13f * opacity),
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        grain.shader = BitmapShader(grainBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        grain.alpha = 150
        val depth = minOf(dp(14f), glass.height() / 4f)
        topShadow = LinearGradient(
            0f, glass.top, 0f, glass.top + depth,
            Hud.withAlpha(Hud.INK, 0.7f), 0x00000000, Shader.TileMode.CLAMP,
        )
        sideShadow = LinearGradient(
            glass.left, 0f, glass.left + depth * 0.7f, 0f,
            Hud.withAlpha(Hud.INK, 0.5f), 0x00000000, Shader.TileMode.CLAMP,
        )
        bottomLight = LinearGradient(
            0f, glass.bottom - dp(8f), 0f, glass.bottom,
            0x00FFFFFF, Hud.withAlpha(Hud.lighten(tint, 0.6f), 0.12f), Shader.TileMode.CLAMP,
        )
        glassSheen = LinearGradient(
            glass.left, glass.top, glass.left + glass.width() * 0.55f, glass.top + glass.height() * 0.4f,
            intArrayOf(0x00FFFFFF, Hud.withAlpha(Hud.WHITE, 0.07f), 0x00FFFFFF),
            floatArrayOf(0.35f, 0.5f, 0.65f),
            Shader.TileMode.CLAMP,
        )
    }

    /** Blurs the glow shapes once for this size. Kept for the last two sizes, like panel and tag. */
    private fun masksFor(): Masks? {
        val b = bounds
        if (b.isEmpty || glass.isEmpty || pane) return null
        val key = "${b.width()}x${b.height()}:$margin:${look?.roundness}:${look?.holo}:${look?.borderThickness}:${look?.glowValue}"
        masks[key]?.let { return it }
        if (holdMasks) return masks.values.lastOrNull()
        // The masks are soft, so half size looks the same and uses a quarter of the memory.
        val maskWidth = (b.width() * MASK_SCALE).toInt().coerceAtLeast(1)
        val maskHeight = (b.height() * MASK_SCALE).toInt().coerceAtLeast(1)
        val outer = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val bloom = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val edge = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val wash = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val local = Path(shape)
        local.offset(-b.left.toFloat(), -b.top.toFloat())
        local.transform(Matrix().apply { setScale(MASK_SCALE, MASK_SCALE) })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.WHITE }
        paint.maskFilter = BlurMaskFilter((margin * 0.9f * MASK_SCALE).coerceAtLeast(1f), BlurMaskFilter.Blur.OUTER)
        Canvas(outer).drawPath(local, paint)
        paint.maskFilter = BlurMaskFilter((margin * 1.35f * MASK_SCALE).coerceAtLeast(1f), BlurMaskFilter.Blur.OUTER)
        Canvas(bloom).drawPath(local, paint)
        // Light that spreads inward from the edge and fades out toward the middle.
        // An inner blur is soft only at the rim and solid inside, so it is taken away
        // from a solid fill. What stays is a band of light just inside the edge.
        val washCanvas = Canvas(wash)
        paint.maskFilter = null
        washCanvas.drawPath(local, paint)
        paint.maskFilter = BlurMaskFilter(dp(WASH_DP) * MASK_SCALE, BlurMaskFilter.Blur.INNER)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        washCanvas.drawPath(local, paint)
        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2.5f) * MASK_SCALE
        paint.maskFilter = BlurMaskFilter(dp(5f) * MASK_SCALE, BlurMaskFilter.Blur.NORMAL)
        Canvas(edge).drawPath(local, paint)
        val resize = buildResizeBarMasks(b, maskWidth)
        val made = Masks(
            outer,
            bloom,
            edge,
            wash,
            resize.bar,
            resize.core,
            resize.mid,
            resize.outer,
        )
        masks[key] = made
        while (masks.size > 2) {
            val oldest = masks.keys.first()
            masks.remove(oldest)
        }
        return made
    }

    private fun buildResizeBarMasks(bounds: Rect, maskWidth: Int): ResizeMasks {
        val panelLook = look ?: return ResizeMasks(
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
            Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8),
        )
        val path = Path()
        val radiusPx = dp(PanelGeometry.resizeBarGlowRadiusDp(panelLook.glowValue).coerceAtLeast(1f))
        val strokePx = dp(PanelGeometry.RESIZE_BAR_HEIGHT_DP)
        val halfRange = radiusPx + strokePx / 2f + dp(2f)
        val top = ((glass.bottom - dp(PanelGeometry.borderInsetDp(panelLook.borderThickness)) - halfRange - bounds.top) *
            MASK_SCALE).coerceAtLeast(0f)
        val maskHeight = (minOf(halfRange * 2f * MASK_SCALE, bounds.height() * MASK_SCALE - top))
            .roundToInt()
            .coerceAtLeast(1)
        val left = (glass.left + dp(PanelGeometry.RESIZE_BAR_INSET_DP) - bounds.left) * MASK_SCALE
        val right = (glass.right - dp(PanelGeometry.RESIZE_BAR_INSET_DP) - bounds.left) * MASK_SCALE
        val bar = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val core = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val mid = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        val outer = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
        if (right <= left) return ResizeMasks(bar, core, mid, outer)
        val centerY = (glass.bottom - dp(PanelGeometry.borderInsetDp(panelLook.borderThickness)) - bounds.top) *
            MASK_SCALE - top
        val stroke = strokePx * MASK_SCALE
        val radius = dp(PanelGeometry.RESIZE_BAR_CAP_RADIUS_DP) * MASK_SCALE
        path.addRoundRect(left, centerY - stroke / 2f, right, centerY + stroke / 2f, radius, radius, Path.Direction.CW)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Hud.WHITE }
        Canvas(bar).drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.ROUND
        paint.maskFilter = BlurMaskFilter(dp(3f) * MASK_SCALE, BlurMaskFilter.Blur.NORMAL)
        Canvas(core).drawPath(path, paint)
        paint.maskFilter = BlurMaskFilter(dp(12f) * MASK_SCALE, BlurMaskFilter.Blur.NORMAL)
        Canvas(mid).drawPath(path, paint)
        paint.maskFilter = BlurMaskFilter(
            dp(PanelGeometry.resizeBarGlowRadiusDp(panelLook.glowValue).coerceAtLeast(1f)) * MASK_SCALE,
            BlurMaskFilter.Blur.NORMAL,
        )
        Canvas(outer).drawPath(path, paint)
        return ResizeMasks(bar, core, mid, outer)
    }

    private fun drawMask(canvas: Canvas, mask: Bitmap, color: Int, alpha: Float) {
        if (alpha <= 0f) return
        maskPaint.color = color
        maskPaint.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        maskTarget.set(bounds)
        canvas.drawBitmap(mask, null, maskTarget, maskPaint)
    }

    override fun draw(canvas: Canvas) {
        val current = look ?: return
        if (bounds.isEmpty || glass.isEmpty) return
        if (sapphireOn()) {
            drawSapphire(canvas, current)
            if (resizeBarVisible) masksFor()?.let { drawResizeBar(canvas, it, current) }
            return
        }
        val strength = current.glowStrength
        val boost = current.glowBloom
        val breathing = if (current.pulse) breath else 0.8f
        val glow = masksFor()
        if (glow != null && current.outerGlowOn && strength > 0f) {
            drawMask(canvas, glow.bloom, current.outerGlow, minOf(1f, boost * 0.35f) * breathing)
            drawMask(canvas, glow.outer, current.outerGlow, strength * (0.35f + 0.65f * breathing))
            // Above 50, a second layer makes the outer glow brighter and fuller.
            drawMask(canvas, glow.outer, Hud.lighten(current.outerGlow, 0.3f * boost.coerceAtMost(1f)), minOf(1f, boost) * (0.5f + 0.5f * breathing))
        }
        val titanium = titaniumOn() && !frameBand.isEmpty
        if (titanium) {
            canvas.drawPath(innerShape, fill)
            drawCarbonTitleBar(canvas)
            drawFrame(canvas)
        } else {
            canvas.drawPath(shape, fill)
            if (current.holo && !pane) {
                canvas.drawPath(shape, grain)
                canvas.drawPath(shape, sheen)
            }
        }
        if (glow != null && current.edgeGlow && strength > 0f) {
            drawMask(canvas, glow.wash, current.edge, (WASH_NORMAL * strength + WASH_BOOST * boost) * (0.4f + 0.6f * breathing))
            drawMask(canvas, glow.edge, current.edge, strength * (0.4f + 0.6f * breathing))
            drawMask(canvas, glow.edge, Hud.lighten(current.edge, 0.3f * boost), boost * (0.5f + 0.5f * breathing))
        }
        line.strokeWidth = dp(if (pane) 1f else PanelGeometry.borderStrokeDp(current.borderThickness))
        line.color = Hud.withAlpha(Hud.lighten(current.edge, if (pane) 0f else 0.35f * boost), if (pane) 0.5f else 0.92f)
        canvas.drawPath(if (fullBleed) edgeShape else shape, line)
        if (titanium) {
            // A thin line of light where the frame meets the glass, like the edge of an inset screen.
            line.strokeWidth = dp(1f)
            line.color = Hud.withAlpha(current.edge, 0.3f + 0.35f * strength * breathing)
            canvas.drawPath(innerShape, line)
        }
        if (current.holo) {
            line.strokeWidth = dp(1f)
            line.color = Hud.withAlpha(current.edge, 0.16f + 0.1f * strength)
            if (!pane && !titanium) canvas.drawPath(inner, line)
            line.color = Hud.withAlpha(current.edge, 0.4f)
            canvas.drawPath(ticks, line)
            holoStroke(canvas, brackets, current.decor, strength)
        }
        if (resizeBarVisible && glow != null) drawResizeBar(canvas, glow, current)
    }

    /** The titanium band: metal, brushed streaks, a light bevel outside, and a dark bevel inside. */
    private fun drawFrame(canvas: Canvas) {
        canvas.drawPath(frameBand, metal)
        canvas.drawPath(frameBand, brushed)
        bevelLight.strokeWidth = dp(1.2f)
        canvas.save()
        canvas.clipPath(frameBand)
        canvas.drawPath(shape, bevelLight)
        shade.style = Paint.Style.STROKE
        shade.shader = null
        shade.color = Hud.withAlpha(Hud.INK, 0.75f)
        shade.strokeWidth = dp(2.2f)
        canvas.drawPath(innerShape, shade)
        canvas.restore()
    }

    /** The title bar sits on woven carbon fiber, with a soft shadow under it. */
    private fun drawCarbonTitleBar(canvas: Canvas) {
        if (headerHeight <= 0f) return
        val bottom = minOf(inside.top + headerHeight, inside.bottom)
        canvas.save()
        canvas.clipPath(innerShape)
        canvas.clipRect(inside.left, inside.top, inside.right, bottom)
        canvas.drawRect(inside.left, inside.top, inside.right, bottom, carbon)
        canvas.restore()
        shade.style = Paint.Style.FILL
        shade.shader = titleShadow
        canvas.drawRect(inside.left, bottom, inside.right, minOf(bottom + dp(5f), inside.bottom), shade)
        shade.shader = null
    }

    /** A sapphire pane: deep glass, frost, inner shadows, a soft sheen, and a thin rim of light. */
    private fun drawSapphire(canvas: Canvas, current: PanelLook) {
        canvas.drawPath(shape, fill)
        canvas.save()
        canvas.clipPath(shape)
        canvas.drawRect(glass, grain)
        shade.style = Paint.Style.FILL
        shade.shader = topShadow
        canvas.drawRect(glass, shade)
        shade.shader = sideShadow
        canvas.drawRect(glass, shade)
        shade.shader = bottomLight
        canvas.drawRect(glass, shade)
        shade.shader = glassSheen
        canvas.drawRect(glass, shade)
        shade.shader = null
        canvas.restore()
        line.strokeWidth = dp(1f)
        line.color = Hud.withAlpha(current.edge, 0.55f)
        canvas.drawPath(if (fullBleed) edgeShape else shape, line)
        line.strokeWidth = dp(0.8f)
        line.color = Hud.withAlpha(Hud.WHITE, 0.1f)
        canvas.save()
        canvas.translate(dp(1.2f), dp(1.2f))
        canvas.clipRect(glass.left, glass.top, glass.right - dp(2.4f), glass.bottom - dp(2.4f))
        canvas.drawPath(shape, line)
        canvas.restore()
    }

    /** Holo Classic's glow: soft wide strokes under one bright thin stroke. */
    private fun holoStroke(canvas: Canvas, path: Path, color: Int, strength: Float) {
        if (strength > 0f) {
            line.strokeWidth = dp(8f)
            line.color = Hud.withAlpha(color, 0.07f * strength)
            canvas.drawPath(path, line)
            line.strokeWidth = dp(5f)
            line.color = Hud.withAlpha(color, 0.2f * strength)
            canvas.drawPath(path, line)
        }
        line.strokeWidth = dp(2.2f)
        line.color = color
        canvas.drawPath(path, line)
    }

    override fun getOutline(outline: Outline) {
        val r = Rect()
        glass.roundOut(r)
        outline.setRoundRect(r, corner())
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        /** How wide the titanium frame is. */
        const val FRAME_DP = 6f

        /** The glow masks are built at this share of full size. They are soft, so it does not show. */
        const val MASK_SCALE = 0.5f

        /** How far the wash of light reaches in from the edge, and how bright it is at 50 and at 100. */
        const val WASH_DP = 26f
        const val WASH_NORMAL = 0.14f
        const val WASH_BOOST = 0.9f

        val grainBitmap: Bitmap get() = PanelTextures.grain
        val brushedBitmap: Bitmap get() = PanelTextures.brushed

        fun carbonTile(density: Float): Bitmap = PanelTextures.carbon(density)
    }
}
