package com.alpefe.fujiptp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverScrapedValuesTest {

    @Test
    fun parsesScrapedKodachrome64() {
        val r = DiscoverRecipe(
            "Kodachrome 64", "ClassicChrome",
            "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -5 Blue · Dynamic Range: DR200 · Highlight: 0 · Shadow: +0.5 · Color: +2 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicChrome, m.filmSimulation)
        assertEquals(GrainEffect.WeakSmall, m.grainEffect)
        assertEquals(EffectStrength.Strong, m.colorChrome)
        assertEquals(EffectStrength.Off, m.colorChromeFxBlue)
        assertEquals(WhiteBalanceMode.Daylight, m.whiteBalanceMode)
        assertEquals(2, m.whiteBalanceShiftR)
        assertEquals(-5, m.whiteBalanceShiftB)
        assertEquals(DynamicRange.Dr200, m.dynamicRange)
        assertEquals(0f, m.highlight)
        assertEquals(0.5f, m.shadow)
        assertEquals(2f, m.color)
        assertEquals(1f, m.sharpness)
        assertEquals(-4, m.noiseReduction)
        assertEquals(3f, m.clarity)
    }

    @Test
    fun parsesScrapedNostalgic() {
        val r = DiscoverRecipe(
            "Kodak Negative", "NostalgicNegative",
            "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Auto, +3 Red & -2 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: +2 · Color: +3 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.NostalgicNegative, m.filmSimulation)
        assertEquals(GrainEffect.WeakLarge, m.grainEffect)
        assertEquals(EffectStrength.Strong, m.colorChrome)
        assertEquals(DynamicRange.Dr400, m.dynamicRange)
        assertEquals(3, m.whiteBalanceShiftR)
        assertEquals(-2, m.whiteBalanceShiftB)
        assertEquals(-1f, m.highlight)
        assertEquals(2f, m.shadow)
        assertEquals(-2f, m.clarity)
    }

    @Test
    fun parsesMonochromeScraped() {
        val r = DiscoverRecipe(
            "Kodak T-Max 100 Hard Tone", "MonochromeGreen",
            "Film Simulation: Monochrome+G · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Grain Effect: Weak, Large · Color Chrome Effect: Off · White Balance: 5500K, +1 Red & -1 Blue · Dynamic Range: DR400 · Highlight: +3 · Shadow: -1 · Sharpness: +2 · High ISO NR: -4 · Clarity: 0",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.MonochromeGreen, m.filmSimulation)
        assertEquals(GrainEffect.WeakLarge, m.grainEffect)
        assertEquals(WhiteBalanceMode.ColorTemperature, m.whiteBalanceMode)
        assertEquals(5500, m.whiteBalanceTemperature)
        assertEquals(3f, m.highlight)
        assertEquals(-1f, m.shadow)
        assertEquals(2f, m.sharpness)
    }
}
