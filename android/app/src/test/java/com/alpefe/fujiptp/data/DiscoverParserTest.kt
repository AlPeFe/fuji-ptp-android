package com.alpefe.fujiptp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverParserTest {

    @Test
    fun parsesEddyPointAndShoot() {
        val r = DiscoverRecipe(
            "EDDY'S POINT & SHOOT",
            "ClassicNegative",
            "Film Sim: Classic Negative · Grain: Strong Large · Colour Chrome: Off · FX Blue: Weak · WB: Auto (R +4, B -4) · Dynamic Range: 200 · Tone Curve: H +1 / S 0 · Colour: +3 · Sharpness: -4 · NR: -4 · Clarity: 0",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicNegative, m.filmSimulation)
        assertEquals(DynamicRange.Dr200, m.dynamicRange)
        assertEquals(GrainEffect.StrongLarge, m.grainEffect)
        assertEquals(EffectStrength.Off, m.colorChrome)
        assertEquals(EffectStrength.Weak, m.colorChromeFxBlue)
        assertEquals(WhiteBalanceMode.Auto, m.whiteBalanceMode)
        assertEquals(4, m.whiteBalanceShiftR)
        assertEquals(-4, m.whiteBalanceShiftB)
        assertEquals(1f, m.highlight)
        assertEquals(0f, m.shadow)
        assertEquals(3f, m.color)
        assertEquals(-4f, m.sharpness)
        assertEquals(-4, m.noiseReduction)
        assertEquals(0f, m.clarity)
    }

    @Test
    fun parsesAfterRain() {
        val r = DiscoverRecipe(
            "After Rain",
            "ClassicNegative",
            "WB: 5200K (R +3, B -4) · DR400 · Grain: Strong Large · CC: Strong · FX Blue: Strong · Color: +4 · Sharpness: -2 · H -0.5 / S +2.0 · NR: -4 · Clarity: 0",
            "",
        )
        val m = r.toModel()
        assertEquals(WhiteBalanceMode.ColorTemperature, m.whiteBalanceMode)
        assertEquals(5200, m.whiteBalanceTemperature)
        assertEquals(3, m.whiteBalanceShiftR)
        assertEquals(-4, m.whiteBalanceShiftB)
        assertEquals(DynamicRange.Dr400, m.dynamicRange)
        assertEquals(GrainEffect.StrongLarge, m.grainEffect)
        assertEquals(EffectStrength.Strong, m.colorChrome)
        assertEquals(EffectStrength.Strong, m.colorChromeFxBlue)
        assertEquals(4f, m.color)
        assertEquals(-2f, m.sharpness)
        assertEquals(-0.5f, m.highlight)
        assertEquals(2f, m.shadow)
        assertEquals(-4, m.noiseReduction)
    }

    @Test
    fun parsesReggiesPortra() {
        val r = DiscoverRecipe(
            "Reggie's Portra",
            "ClassicChrome",
            "Classic Chrome · DR-Auto · H -1 / S -1 · Color +2 · NR -4 · Sharp -2 · Grain Weak Small · CC Strong · FX Blue Weak · WB Auto +2R/-4B",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.ClassicChrome, m.filmSimulation)
        assertEquals(DynamicRange.Dr100, m.dynamicRange)
        assertEquals(GrainEffect.WeakSmall, m.grainEffect)
        assertEquals(EffectStrength.Strong, m.colorChrome)
        assertEquals(EffectStrength.Weak, m.colorChromeFxBlue)
        assertEquals(2, m.whiteBalanceShiftR)
        assertEquals(-4, m.whiteBalanceShiftB)
        assertEquals(-1f, m.highlight)
        assertEquals(-1f, m.shadow)
        assertEquals(2f, m.color)
        assertEquals(-2f, m.sharpness)
        assertEquals(-4, m.noiseReduction)
    }

    @Test
    fun parsesNewspaper() {
        val r = DiscoverRecipe(
            "Newspaper",
            "AcrosYellow",
            "Film Simulation: Acros + Ye · Grain Effect: Strong · Grain Size: Small · WB Shift: R -4, B -3 · Highlight Tone: +4 · Shadow Tone: +4 · Sharpness: +2 · Clarity: 0 · High ISO NR: 0",
            "",
        )
        val m = r.toModel()
        assertEquals(FilmSimulation.AcrosYellow, m.filmSimulation)
        assertEquals(GrainEffect.StrongSmall, m.grainEffect)
        assertEquals(-4, m.whiteBalanceShiftR)
        assertEquals(-3, m.whiteBalanceShiftB)
        assertEquals(4f, m.highlight)
        assertEquals(4f, m.shadow)
        assertEquals(2f, m.sharpness)
        assertEquals(0, m.noiseReduction)
    }
}
