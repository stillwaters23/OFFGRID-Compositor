package com.offgrid.compositor.effects

import com.offgrid.compositor.effects.layers.BeachLayer
import com.offgrid.compositor.effects.layers.CloudsLayer
import com.offgrid.compositor.effects.layers.EmojiRainLayer
import com.offgrid.compositor.effects.layers.FaceDiamondLayer
import com.offgrid.compositor.effects.layers.FireworksLayer
import com.offgrid.compositor.effects.layers.FlyingHeroesLayer
import com.offgrid.compositor.effects.layers.GlowBorderLayer
import com.offgrid.compositor.effects.layers.IllusionLayer
import com.offgrid.compositor.effects.layers.InkLayer
import com.offgrid.compositor.effects.layers.LaserWriteLayer
import com.offgrid.compositor.effects.layers.PortalLayer
import com.offgrid.compositor.effects.layers.QuietLightLayer
import com.offgrid.compositor.effects.layers.RadarLayer
import com.offgrid.compositor.effects.layers.RedSweepLayer
import com.offgrid.compositor.effects.layers.RollerCoasterLayer
import com.offgrid.compositor.effects.layers.StatusLabelLayer
import com.offgrid.compositor.effects.layers.TestCodeLayer

/**
 * Every drawn effect, bottom to top (final build, M4). The face diamond is lowest, so a
 * scene that fills the box (beach, clouds, status label) hides it. Quiet mode's dark veil
 * covers the rest, and the test code is always on top so viewers can read it.
 */
object EffectLayers {
    fun all(): List<OverlayLayer> = listOf(
        FaceDiamondLayer(),
        // Scenes that can fill the whole box. The status label is last of them, so it wins.
        BeachLayer(),
        CloudsLayer(),
        RollerCoasterLayer(),
        StatusLabelLayer(),
        IllusionLayer(),
        PortalLayer(),
        FlyingHeroesLayer(),
        InkLayer(),
        EmojiRainLayer(),
        FireworksLayer(),
        GlowBorderLayer(),
        RedSweepLayer(),
        RadarLayer(),
        LaserWriteLayer(),
        QuietLightLayer(),
        TestCodeLayer(),
    )
}
