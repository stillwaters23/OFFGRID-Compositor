package com.offgrid.compositor.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

internal class NeonColorRow(
    context: Context,
    selectedColor: Int,
    private val lockReason: (Int) -> String?,
    onColorChanged: (Int) -> Unit,
    onLocked: (String) -> Unit,
) : LinearLayout(context) {
    private val swatchViews = ArrayList<NeonSwatchView>(NeonPalette.swatches.size)
    private val selected = selectedColor
    private val chooseColor = onColorChanged
    private val lockedColor = onLocked

    init {
        orientation = VERTICAL
        addSwatches(NeonPalette.swatches.filterNot { it.cyber })
        addView(TextView(context).apply {
            text = "CYBER"
            typeface = Hud.monoBold(context)
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(NeonPalette.CYAN_GLOW)
            gravity = Gravity.CENTER_VERTICAL
        }, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, 32f)).apply {
            topMargin = Hud.dpi(context, GAP_DP)
        })
        addSwatches(NeonPalette.swatches.filter { it.cyber })
    }

    private fun addSwatches(swatches: List<NeonSwatch>) {
        swatches.chunked(SWATCHES_PER_ROW).forEachIndexed { rowIndex, group ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            group.forEachIndexed { index, swatch ->
                val reason = lockReason(swatch.color)
                val swatchView = NeonSwatchView(context, swatch, swatch.color == selected, reason)
                swatchView.setOnClickListener {
                    val locked = lockReason(swatch.color)
                    if (locked != null) {
                        lockedColor(locked)
                        return@setOnClickListener
                    }
                    swatchViews.forEach { it.isSelectedSwatch = it === swatchView }
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    chooseColor(swatch.color)
                }
                swatchView.setOnLongClickListener {
                    showMeaning(swatchView, swatch)
                    true
                }
                swatchViews += swatchView
                row.addView(swatchView, LayoutParams(Hud.dpi(context, TILE_DP), Hud.dpi(context, TILE_DP)).apply {
                    if (index > 0) marginStart = Hud.dpi(context, GAP_DP)
                })
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, Hud.dpi(context, TILE_DP)).apply {
                if (rowIndex > 0) topMargin = Hud.dpi(context, GAP_DP)
            })
        }
    }

    fun setSelected(color: Int) {
        swatchViews.forEach { it.isSelectedSwatch = it.swatch.color == color }
    }

    private fun showMeaning(anchor: View, swatch: NeonSwatch) {
        val card = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Hud.dpi(context, 8f), Hud.dpi(context, 8f), Hud.dpi(context, 12f), Hud.dpi(context, 8f))
            background = GradientDrawable().apply {
                setColor(0xFF080A10.toInt())
                cornerRadius = Hud.dp(context, 8f)
                setStroke(Hud.dpi(context, 1f), swatch.color)
            }
        }
        card.addView(View(context).apply { setBackgroundColor(swatch.color) },
            LayoutParams(Hud.dpi(context, 4f), LayoutParams.MATCH_PARENT))
        card.addView(TextView(context).apply {
            text = "${swatch.name}: ${swatch.meaning}"
            typeface = Hud.mono(context)
            textSize = 10f
            setTextColor(NeonPalette.BLACKLIGHT_WHITE)
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = Hud.dpi(context, 8f)
        })
        PopupWindow(card, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true).apply {
            elevation = Hud.dp(context, 8f)
            PanelPopups.track(this)
            showAsDropDown(anchor, 0, Hud.dpi(context, 4f))
        }
    }

    private class NeonSwatchView(
        context: Context,
        val swatch: NeonSwatch,
        selected: Boolean,
        lockReason: String?,
    ) : View(context) {
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = swatch.color
            alpha = 90
            setShadowLayer(Hud.dp(context, 7f), 0f, 0f, swatch.color)
        }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = swatch.color }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = Hud.dp(context, 2f)
            color = NeonPalette.BLACKLIGHT_WHITE
        }
        private val contrastRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = Hud.dp(context, 1f)
            color = 0xFFA8B0C2.toInt()
        }
        private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = Hud.dp(context, 1.6f)
            strokeCap = Paint.Cap.ROUND
            color = 0xFFF4F4F4.toInt()
        }
        private val locked = lockReason != null
        private val lockMessage = lockReason
        var isSelectedSwatch = selected
            set(value) {
                if (field == value) return
                field = value
                invalidate()
            }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            isClickable = true
            isFocusable = true
            contentDescription = if (locked) {
                "Locked ${swatch.name}. $lockMessage"
            } else {
                "${swatch.name}, ${RainbowMix.hex(swatch.color)}. ${swatch.meaning}"
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val center = width / 2f
            val radius = Hud.dp(context, 12f)
            canvas.drawCircle(center, height / 2f, radius, halo)
            canvas.drawCircle(center, height / 2f, radius, fill)
            if (swatch.color == 0xFF000000.toInt() || swatch.color == 0xFFFFFFFF.toInt()) {
                canvas.drawCircle(center, height / 2f, radius, contrastRing)
            }
            if (isSelectedSwatch) canvas.drawCircle(center, height / 2f, Hud.dp(context, 17f), ring)
            if (locked) {
                val midY = height / 2f
                val body = android.graphics.RectF(
                    center - Hud.dp(context, 4f),
                    midY - Hud.dp(context, 1f),
                    center + Hud.dp(context, 4f),
                    midY + Hud.dp(context, 6f),
                )
                canvas.drawRoundRect(body, Hud.dp(context, 1.2f), Hud.dp(context, 1.2f), lockPaint)
                canvas.drawArc(
                    center - Hud.dp(context, 2.6f),
                    midY - Hud.dp(context, 7f),
                    center + Hud.dp(context, 2.6f),
                    midY + Hud.dp(context, 1f),
                    180f,
                    180f,
                    false,
                    lockPaint,
                )
            }
        }
    }

    private companion object {
        const val SWATCHES_PER_ROW = 6
        const val TILE_DP = 40f
        const val GAP_DP = 8f
    }
}
