package com.offgrid.compositor.terminal

internal object NeonPalette {
    const val UV_VIOLET = 0xFF7B02B3.toInt()
    const val ELECTRIC_PURPLE = 0xFF8B00FF.toInt()
    const val HOT_MAGENTA = 0xFFFF00A6.toInt()
    const val NEON_PINK = 0xFFF82D97.toInt()
    const val CYAN_GLOW = 0xFF00E5FC.toInt()
    const val DEEP_BLUE = 0xFF0032FF.toInt()
    const val TOXIC_LIME = 0xFFB6FF00.toInt()
    const val BLACKLIGHT_WHITE = 0xFFFFFFFF.toInt()

    val swatches = listOf(
        NeonSwatch("Pink", NEON_PINK, "Compassion and love."),
        NeonSwatch("Magenta", HOT_MAGENTA, "Creativity and confidence."),
        NeonSwatch("Fuchsia", 0xFFC501E2.toInt(), "Creativity and luxury."),
        NeonSwatch("Ruby", 0xFFDB0F27.toInt(), "Passion and energy."),
        NeonSwatch("Crimson", 0xFFBA110C.toInt(), "Passion and strength."),
        NeonSwatch("Scarlet", 0xFFFF2500.toInt(), "Energy and courage."),
        NeonSwatch("Candy", 0xFFFF0800.toInt(), "Energy and focus."),
        NeonSwatch("Ferrari", 0xFFE12901.toInt(), "Speed and action."),
        NeonSwatch("Imperial", 0xFFED2939.toInt(), "Power and presence."),
        NeonSwatch("Raspberry", 0xFFD31A38.toInt(), "Warmth and care."),
        NeonSwatch("Chilli", 0xFFC21A09.toInt(), "Energy and heat."),
        NeonSwatch("Vermilion", 0xFFE34234.toInt(), "Energy and expression."),
        NeonSwatch("Orange", 0xFFFF6A00.toInt(), "Creativity and warmth."),
        NeonSwatch("Yellow", 0xFFFFFF00.toInt(), "Happiness and clarity."),
        NeonSwatch("Lime", TOXIC_LIME, "Growth and freshness."),
        NeonSwatch("Green", 0xFF2EF8A0.toInt(), "Growth and harmony."),
        NeonSwatch("Mint", 0xFF00FFD1.toInt(), "Calm and renewal."),
        NeonSwatch("Cyan", CYAN_GLOW, "Clarity and calm."),
        NeonSwatch("Sky", 0xFF01C4E7.toInt(), "Trust and openness."),
        NeonSwatch("Blue", DEEP_BLUE, "Trust and calm."),
        NeonSwatch("Indigo", 0xFF3512C5.toInt(), "Focus and depth."),
        NeonSwatch("Violet", ELECTRIC_PURPLE, "Luxury and creativity."),
        NeonSwatch("Purple", UV_VIOLET, "Luxury and imagination."),
        NeonSwatch("White", BLACKLIGHT_WHITE, "Purity and simplicity."),
        NeonSwatch("Black", 0xFF000000.toInt(), "Power and mystery."),
        NeonSwatch("Silver", 0xFFC4D1E8.toInt(), "Modern and balanced."),
        NeonSwatch("Gold", 0xFFFFC400.toInt(), "Prestige and success."),
        NeonSwatch("Cyber Navy", 0xFF07152F.toInt(), "Power and mystery.", cyber = true),
        NeonSwatch("Cyber Blue", 0xFF102A62.toInt(), "Trust and focus.", cyber = true),
        NeonSwatch("Cyber Indigo", 0xFF24205B.toInt(), "Depth and focus.", cyber = true),
        NeonSwatch("Cyber Violet", 0xFF321A52.toInt(), "Luxury and mystery.", cyber = true),
    )
}

internal data class NeonSwatch(
    val name: String,
    val color: Int,
    val meaning: String,
    val cyber: Boolean = false,
)
