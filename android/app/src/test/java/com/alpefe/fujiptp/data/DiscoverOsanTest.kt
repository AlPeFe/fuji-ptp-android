package com.alpefe.fujiptp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverOsanTest {

    @Test
    fun parsesClassicCubanNegative() {
        val r = DiscoverRecipe(
            "Classic Cuban Negative", "ClassicNegative",
            "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -5 Blue & +4 Red · Highlight: -2 · Shadow: +1 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicNegative, m.filmSimulation)
        assertEquals(DynamicRange.Dr400, m.dynamicRange)
        assertEquals(GrainEffect.StrongLarge, m.grainEffect)
        assertEquals(EffectStrength.Strong, m.colorChrome)
        assertEquals(EffectStrength.Strong, m.colorChromeFxBlue)
        assertEquals(WhiteBalanceMode.Auto, m.whiteBalanceMode)
        assertEquals(-5, m.whiteBalanceShiftB)
        assertEquals(4, m.whiteBalanceShiftR)
        assertEquals(-2f, m.highlight)
        assertEquals(1f, m.shadow)
        assertEquals(4f, m.color)
        assertEquals(0f, m.sharpness)
        assertEquals(-4, m.noiseReduction)
        assertEquals(-4f, m.clarity)
    }

    @Test
    fun parsesCubanAce() {
        val r = DiscoverRecipe(
            "Cubanace", "RealaAce",
            "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -5 Blue & +4 Red · Highlight: -2 · Shadow: +1.5 · Color: +1 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.RealaAce, m.filmSimulation)
        assertEquals(1.5f, m.shadow)
        assertEquals(1f, m.color)
        assertEquals(-4, m.noiseReduction)
    }

    @Test
    fun parsesSummerChrome() {
        val r = DiscoverRecipe(
            "Summer Chrome", "ClassicChrome",
            "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -6 Blue & +5 Red · Highlight: -2 · Shadow: -2 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicChrome, m.filmSimulation)
        assertEquals(-6, m.whiteBalanceShiftB)
        assertEquals(5, m.whiteBalanceShiftR)
        assertEquals(-2f, m.shadow)
        assertEquals(4f, m.color)
    }

    @Test
    fun parsesVibrantAstiaSoft() {
        val r = DiscoverRecipe(
            "Vibrant Astia Soft", "Astia",
            "Film Simulation: Astia Soft · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, -2 Blue & +2 Red · Highlight: -2 · Shadow: +1 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.Astia, m.filmSimulation)
        assertEquals(EffectStrength.Weak, m.colorChromeFxBlue)
        assertEquals(-2, m.whiteBalanceShiftB)
        assertEquals(2, m.whiteBalanceShiftR)
        assertEquals(-2f, m.sharpness)
    }

    @Test
    fun parsesAlpineNegative() {
        val r = DiscoverRecipe(
            "Alpine Negative", "ClassicNegative",
            "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Off · Color Chrome Effect: Off · Color Chrome FX Blue: Strong · White Balance: Auto, -4 Blue & +2 Red · Highlight: -2 · Shadow: +1 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -3",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicNegative, m.filmSimulation)
        assertEquals(GrainEffect.Off, m.grainEffect)
        assertEquals(EffectStrength.Off, m.colorChrome)
        assertEquals(EffectStrength.Strong, m.colorChromeFxBlue)
        assertEquals(-4, m.whiteBalanceShiftB)
        assertEquals(2, m.whiteBalanceShiftR)
        assertEquals(-3f, m.clarity)
    }

    @Test
    fun parsesGentleRealaAce() {
        val r = DiscoverRecipe(
            "Gentle Reala Ace", "RealaAce",
            "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, 0 Red & 0 Blue · Highlight: -1 · Shadow: 0 · Color: 0 · Sharpness: 0 · High ISO NR: -4 · Clarity: 0",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.RealaAce, m.filmSimulation)
        assertEquals(GrainEffect.WeakSmall, m.grainEffect)
        assertEquals(0, m.whiteBalanceShiftR)
        assertEquals(0, m.whiteBalanceShiftB)
        assertEquals(-1f, m.highlight)
        assertEquals(0f, m.shadow)
    }
}
