package com.alpefe.fujiptp.data

/**
 * Predefined public collections shown in Discover. These are curated by the
 * app owner (Fuji X Weekly, community collections...), are READ-ONLY: the
 * user can only import recipes from them into their own collections.
 *
 * Every collection and every recipe carries its [source] link so the user
 * always knows where it comes from. Hardcoded for now; will be served
 * remotely later.
 */
data class DiscoverRecipe(
    val name: String,
    val filmSimulation: String,
    val description: String,
    /** Source URL of this specific recipe. */
    val source: String,
) {
    /**
     * Parses the recipe values embedded in [description] into a full
     * RecipeModel (film sim + tone, grain, WB, DR, etc.), so importing from
     * Discover preserves the real settings instead of defaults.
     */
    fun toModel(): RecipeModel {
        val d = description.lowercase()
        val film = FilmSimulation.entries.firstOrNull { it.name == filmSimulation }
            ?: FilmSimulation.ClassicChrome

        /** Value of the segment right after [key], up to the next "·" or end. */
        fun segment(key: String): String {
            val idx = d.indexOf(key)
            if (idx < 0) return ""
            var end = d.indexOf('·', idx + key.length)
            if (end < 0) end = d.length
            return d.substring(idx + key.length, end)
        }
        fun intAfter(vararg keys: String): Int? {
            for (key in keys) {
                val m = Regex("""([+-]?\d+)""").find(segment(key))
                if (m != null) return m.groupValues[1].toInt()
            }
            return null
        }
        fun floatAfter(vararg keys: String): Float? {
            for (key in keys) {
                val m = Regex("""([+-]?\d+(?:\.\d+)?)""").find(segment(key))
                if (m != null) return m.groupValues[1].toFloat()
            }
            return null
        }

        // Grain: "grain: strong large" / "grain effect: strong, small" /
        // "grain weak small" / "grain: off" / "grain effect: strong" +
        // "grain size: small"
        val grainSection = segment("grain")
        val grainSizeSection = segment("grain size")
        val grain = when {
            grainSection.contains("strong") && (grainSection.contains("large") || grainSizeSection.contains("large")) -> GrainEffect.StrongLarge
            grainSection.contains("weak") && (grainSection.contains("large") || grainSizeSection.contains("large")) -> GrainEffect.WeakLarge
            grainSection.contains("strong") && (grainSection.contains("small") || grainSizeSection.contains("small")) -> GrainEffect.StrongSmall
            grainSection.contains("weak") && (grainSection.contains("small") || grainSizeSection.contains("small")) -> GrainEffect.WeakSmall
            else -> GrainEffect.Off
        }

        // Color chrome / FX blue (segment-scoped, so one value can't bleed
        // into the next)
        fun effectOf(section: String): EffectStrength = when {
            section.contains("strong") -> EffectStrength.Strong
            section.contains("weak") -> EffectStrength.Weak
            else -> EffectStrength.Off
        }
        val cc = effectOf(
            segment("cc:").ifEmpty { segment("color chrome:").ifEmpty { segment("colour chrome:").ifEmpty { segment("color chrome effect:").ifEmpty { segment("cc ") } } } }
        )
        val fx = effectOf(
            segment("fx blue:").ifEmpty { segment("color chrome fx blue:").ifEmpty { segment("fx blue ") } }
        )

        // White balance: mode + R/B shifts + temperature
        val wbTemp = Regex("""(\d{4})k""").find(d)?.groupValues?.get(1)?.toInt()
        val wbLower = d
        val wbMode = when {
            wbLower.contains("white balance: daylight") || wbLower.contains("wb: daylight") || wbLower.contains("white balance: cloudy") -> WhiteBalanceMode.Daylight
            wbLower.contains("white balance: shade") || wbLower.contains("wb: shade") -> WhiteBalanceMode.Shade
            wbLower.contains("white balance: fluorescent") || wbLower.contains("wb: fluorescent") -> WhiteBalanceMode.Fluorescent1
            wbLower.contains("white balance: incandescent") || wbLower.contains("wb: incandescent") -> WhiteBalanceMode.Incandescent
            wbLower.contains("white balance: underwater") || wbLower.contains("wb: underwater") -> WhiteBalanceMode.Underwater
            wbTemp != null -> WhiteBalanceMode.ColorTemperature
            else -> WhiteBalanceMode.Auto
        }
        // Shifts. Handles: "+2 Red & -5 Blue", "+2R/-4B", "(R +1, B -3)",
        // "R -4, B -3", "+1 Red & +1 Blue".
        fun shiftOf(word: String, single: String): Int {
            // "N word" (e.g. "+2 Red")
            Regex("""([+-]?\d+)\s*$word""").find(d)?.let { return it.groupValues[1].toInt() }
            // "word N" (e.g. "Red -3")
            Regex("""$word\s*([+-]?\d+)""").find(d)?.let { return it.groupValues[1].toInt() }
            // "N single" (e.g. "+2R") or "single N" (e.g. "R +1")
            Regex("""([+-]?\d+)\s*$single\b""").find(d)?.let { return it.groupValues[1].toInt() }
            Regex("""\b$single\s*([+-]?\d+)""").find(d)?.let { return it.groupValues[1].toInt() }
            return 0
        }
        val wbR = shiftOf("red", "r")
        val wbB = shiftOf("blue", "b")

        // Dynamic range
        val dr = when {
            d.contains("dr400") || d.contains("dr 400") || d.contains("dynamic range: 400") -> DynamicRange.Dr400
            d.contains("dr200") || d.contains("dr 200") || d.contains("dynamic range: 200") -> DynamicRange.Dr200
            else -> DynamicRange.Dr100
        }

        // Tone curve: "h +1 / s 0", "h -1.5", "highlight: +4", "shadow: +4"
        fun toneValue(label: String): Float? {
            val patterns = listOf(
                Regex("""\b$label\b\s*([+-]?\d+(?:\.\d+)?)"""),
                Regex("""(?:^|[^a-z])$label:\s*([+-]?\d+(?:\.\d+)?)"""),
            )
            for (p in patterns) {
                val m = p.find(d)
                if (m != null) return m.groupValues[1].toFloat()
            }
            return null
        }
        val highlight = toneValue("h") ?: toneValue("highlight") ?: toneValue("highlight tone")
        val shadow = toneValue("s") ?: toneValue("shadow") ?: toneValue("shadow tone")

        return RecipeModel(
            name = name,
            filmSimulation = film,
            dynamicRange = dr,
            grainEffect = grain,
            colorChrome = cc,
            colorChromeFxBlue = fx,
            whiteBalanceMode = wbMode,
            whiteBalanceShiftR = wbR,
            whiteBalanceShiftB = wbB,
            whiteBalanceTemperature = wbTemp,
            highlight = highlight ?: 0f,
            shadow = shadow ?: 0f,
            color = floatAfter("color:") ?: floatAfter("colour:") ?: run {
                // "color +2" / "colour +2" (no colon) — skip "color chrome"
                val m = Regex("""(?:color|colour)\s+([+-]?\d+(?:\.\d+)?)""").find(d)
                m?.groupValues?.get(1)?.toFloat() ?: 0f
            },
            sharpness = floatAfter("sharpness:") ?: floatAfter("sharp:") ?: floatAfter("sharp ") ?: 0f,
            clarity = floatAfter("clarity:") ?: 0f,
            noiseReduction = intAfter("nr:") ?: intAfter("noise reduction:") ?: run {
                val m = Regex("""\bnr\s*([+-]?\d+)""").find(d)
                m?.groupValues?.get(1)?.toInt() ?: 0
            },
        )
    }
}

data class DiscoverCollection(
    val id: String,
    val name: String,
    val tagline: String,
    /** Simple logo glyph (emoji placeholder until real logos are provided). */
    val logo: String,
    val colorHex: Long,
    /** Source URL of the collection (the website it was curated from). */
    val source: String,
    val recipes: List<DiscoverRecipe>,
)

object DiscoverData {

    private fun r(name: String, film: String, desc: String, source: String) =
        DiscoverRecipe(name, film, desc, source)

    val collections: List<DiscoverCollection> = listOf(
        // --- Fuji X Weekly (real, X-Trans V) --------------------------------
        DiscoverCollection(
            id = "fxw",
            name = "Fuji X Weekly",
            tagline = "Recipes de la comunidad",
            logo = "📷",
            colorHex = 0xFF35445C,
            source = "https://fujixweekly.com/fujifilm-x-trans-v-recipes/",
            recipes = listOf(
                r("Kodachrome 64", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -5 Blue · Dynamic Range: DR200 · Highlight: 0 · Shadow: +0.5 · Color: +2 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2022/11/28/kodachrome-64-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodachrome 25", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Off · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Daylight, +2 Red & -4 Blue · Dynamic Range: DR400 · Highlight: +0.5 · Shadow: -0.5 · Color: +1 · Sharpness: +3 · High ISO NR: -4 · Clarity: +3 · ISO: Auto, up to ISO 1600 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2023/03/06/kodachrome-25-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 400 v2", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 5200K, +1 Red & -6 Blue · Dynamic Range: DR400 · Highlight: 0 · Shadow: -2 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2022/12/16/kodak-portra-400-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 160 v2", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Weak · White Balance: Daylight, +4 Red & -5 Blue · Dynamic Range: D-Range Priority (DR-P) Auto · Color: 0 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2023/11/10/kodak-portra-160-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 800 v3", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off (X-Trans V); Weak (X-Trans IV) · White Balance: 6600K, -1 Red & -3 Blue · Highlight: -2 · Shadow: -0.5 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 1/3 (typically)", "https://fujixweekly.com/2024/02/14/kodak-portra-800-v3-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Kodak Gold 200", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Off · White Balance: Daylight, +4 Red & -5 Blue · Dynamic Range: DR400 · Highlight: -1.5 · Shadow: +0.5 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2023/10/24/kodak-gold-200-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Ultramax 400", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Large · Color Chrome Effect: Weak · Color Chrome FX Blue: Off · White Balance: Auto, +1 Red & -5 Blue · Dynamic Range: DR-Auto · Highlight: +1 · Shadow: +1 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: +3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2023/01/17/kodak-ultramax-400-a-film-simulation-recipe-for-the-fujifilm-x-t5-x-trans-v/"),
                r("Kodak Negative", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Auto, +1 Red & -4 Blue · Dynamic Range: DR400 · Highlight: -0.5 · Shadow: +2.5 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +2/3 (typically)", "https://fujixweekly.com/2022/12/22/kodak-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodacolor VR 200", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR200 · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 3000K, +8 Red & -8 Blue · Highlight: +1.5 · Shadow: +2.5 · Color: -4 · Sharpness: +1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2025/04/16/kodacolor-vr-200-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("1976 Kodak", "ClassicChrome", "Film Simulation: Nostalgic Neg. · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Auto, -2 Red & -4 Blue · Dynamic Range: DR200 · Highlight: +1.5 · Shadow: +3 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2023/08/03/1976-kodak-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("1971 Kodak", "ClassicChrome", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 5900K, -1 Red & -6 Blue · Dynamic Range: DR100 · Highlight: +2 · Shadow: -2 · Color: -2 · Sharpness: -4 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +1/3 (typically)", "https://fujixweekly.com/2026/03/25/1971-kodak-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Kodak Royal Gold 400", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong (X-Trans IV), Weak (X-Trans V) · White Balance: Shade, +3 Red & +5 Blue · Highlight:-1 · Shadow: +1 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/03/06/kodak-royal-gold-400-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Kodak Pro 400", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Strong · White Balance: 5200K, +2 Red & -3 Blue · Highlight: -2 · Shadow: 0 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2025/09/06/kodak-pro-400-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Kodak Vericolor VPS", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 4500K, +2 Red & -5 Blue · Highlight: -1 · Shadow: +2 · Color: +3 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2025/07/07/kodak-vericolor-vps-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Vericolor III 160", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 3200K, +7 Red & -8 Blue · Dynamic Range: DR-Auto · Highlight: -2 · Shadow: -1 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2026/02/20/kodak-vericolor-iii-160-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Kodak Vericolor Warm", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 3000K, +8 Red & -9 Blue · Dynamic Range: DR100 · Highlight: -2 · Shadow: -1 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2022/12/12/kodak-vericolor-warm-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Gold Max 400 Expired", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 6700K, +1 Red & +4 Blue · Highlight: -1.5 · Shadow: +1.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 1/3 (typically)", "https://fujixweekly.com/2024/12/31/kodak-gold-max-400-expired-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Kodak Farbwelt 200 Expired", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR100 · Grain Effect: Strong, Large · Color Chrome Effect: Weak · Color Chrome FX Blue: Strong · White Balance: 7500K, +1 Red & -4 Blue · Highlight: -2 · Shadow: -2 · Color: -3 · Sharpness: -4 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to -1 1/3 (typically)", "https://fujixweekly.com/2025/12/09/kodak-farbwelt-200-expired-a-fujifilm-film-simulation-recipe-for-x-trans-v-cameras/"),
                r("Classic Color", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · Weak · White Balance: 5300K, 0 Red & -6 Blue · Highlight: -0.5 · Shadow: -2 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +1 (typically)", "https://fujixweekly.com/2024/04/22/classic-color-fujifilm-x-t5-x-trans-v-and-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Classic Amber", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Fluorescent 1, +1 Red & -6 Blue · Highlight: -1.5 · Shadow: +2.5 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2025/11/01/classic-amber-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Classic Retro", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Daylight, 0 Red & -3 Blue · Dynamic Range: DR400 · Highlight: +4 · Shadow: -2 · Color: -1 · Sharpness: -2 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -2/3 to -1 (typically)", "https://fujixweekly.com/2026/07/11/classic-retro-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("1960 Chrome", "EternaBleachBypass", "Film Simulation: Eterna Bleach Bypass · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off (X-Trans V); Weak (X-Trans IV) · White Balance: Fluorescent 1, -2 Red & -4 Blue · Highlight: -2 · Shadow: -1 · Color: +2 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2024/07/15/1960-chrome-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Vivid Chrome", "Velvia", "Film Simulation: Velvia · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -2 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: -0.5 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: +3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2026/04/13/vivid-chrome-a-fujifilm-recipe-for-x-and-gfx-cameras/"),
                r("Vintage Bronze", "EternaBleachBypass", "Film Simulation: Eterna Bleach Bypass · Grain Effect: Weak, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Daylight, +6 Red & -8 Blue · Dynamic Range: DR200 · Highlight: 0 · Shadow: -1 · Color: 0 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -2/3 to 0 (typically)", "https://fujixweekly.com/2023/01/13/vintage-bronze-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Summer of 1960", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain Effect: Strong, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Strong · White Balance: 5250K, -3 Red & -5 Blue · Dynamic Range: DR400 · Highlight: +4 · Shadow: +2 · Color: +3 · Sharpness: -4 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2023/03/22/summer-of-1960-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Summer of '59", "Eterna", "Film Simulation: Eterna · Grain Effect: Strong, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Strong · White Balance: Auto, +4 Red & -8 Blue · Dynamic Range: DR400 · Highlight: +3 · Shadow: +0.5 · Color: 0 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2026/05/02/summer-of-59-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("Summer Sun", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 7200K, -2 Red & +2 Blue · Highlight: -1 · Shadow: -2 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2025/06/27/summer-sun-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("California Summer", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 6700K, -1 Red & -6 Blue · Highlight: -2 · Shadow: -1 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 1/3 (typically)", "https://fujixweekly.com/2024/06/04/california-summer-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Texas Sun", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 6500K, +2 Red & -6 Blue · Highlight: -2 · Shadow: -0.5 · Color:+4 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2024/12/04/texas-sun-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pacific Blues", "ClassicNegative", "Film Simulation: Classic Negative · Grain: Strong Large · Color Chrome: Strong · FX Blue: Weak · WB: 5800K (R +1, B -3) · DR400 · Highlight: -2 · Shadow: +3 · Color: +4 · Sharpness: -2 · NR: -4 · Clarity: -3", "https://fujixweekly.com/2022/12/06/pacific-blues-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Emulsion '86", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -1 Blue · Dynamic Range: DR400 · Highlight: -2 · Shadow: +2 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1-1/3 (typically)", "https://fujixweekly.com/2023/01/07/creating-your-own-film-simulation-recipe-for-a-unique-look-emulsion-86-a-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Agfa Ultra 100 v2", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Weak, Large · Color Chrome Effect: Weak · Color Chrome FX Blue: Weak · White Balance: 5800K, -3 Red & -3 Blue · Highlight:+1.5 · Shadow: +1 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +1 (typically)", "https://fujixweekly.com/2024/02/20/agfa-ultra-100-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Thommy's Ektachrome", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain Effect: Weak, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Off · White Balance: 5000K, -1 Red & +3 Blue · Dynamic Range: DR100 · Highlight: +1.5 · Shadow: +1.5 · Color: +1 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +0 to +1/3 (typically)", "https://fujixweekly.com/2023/03/24/thommys-ektachrome-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Retro Slide", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 5000K, -1 Red & +3 Blue · Dynamic Range: DR400 · Highlight: 0 · Shadow: +2 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2026/08/10/retro-slide-a-fujifilm-recipe-for-5th-gen-x-trans-v-cameras/"),
                r("Kodak Vision3 250D v2", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Fluorescent 1, -5 Red & 0 Blue · Highlight: +4 · Shadow: +3 · Color: -1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2023/12/19/kodak-vision3-250d-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("CineStill 400D v1", "Astia", "Film Simulation: Astia · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Fluorescent 1, -6 Red & -3 Blue · Dynamic Range: DR200 · Highlight: +3 · Shadow: +1 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2023/01/17/cinestill-400d-v1-a-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("CineStill 400D v2", "Astia", "Film Simulation: Astia · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Fluorescent 1, -2 Red & +4 Blue · Dynamic Range: DR200 · Highlight: -2 · Shadow: 0 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2023/01/17/cinestill-400d-v2-a-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("CineStill 800T", "Eterna", "Film Simulation: Eterna · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Fluorescent 3, -6 Red & -4 Blue · Highlight: 0 · Shadow: +2 · Color: +4 · Sharpness: -3 · High ISO NR: -4 · Clarity: -5 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/04/16/cinestill-800t-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pushed CineStill 800T", "EternaBleachBypass", "Film Simulation: Eterna Bleach Bypass · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 7700K, -9 Red & +5 Blue · Dynamic Range: DR400 · Highlight: -0.5 · Shadow: +1.5 · Color: +3 · Sharpness: 0 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2023/07/22/pushed-cinestill-800t-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Expired ECN-2 100T", "EternaBleachBypass", "Film Simulation: Eterna Bleach Bypass · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong— Weak for X-Trans V · White Balance: 6000K, -9 Red & -6 Blue · Highlight: +0.5 · Shadow: -1 · Color: +1 · Sharpness: -3 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2024/02/01/expired-ecn-2-100t-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Fluorescent Night", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Fluorescent 2, -8 Red & -1 Blue · Highlight: -1 · Shadow: -1.5 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -2/3 to +2/3 (typically)", "https://fujixweekly.com/2023/12/04/fluorescent-night-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vintage Eterna", "Eterna", "Film Simulation: Eterna · Dynamic Range: DR100 · Grain Effect: Strong · Color Chrome Effect: Off or N/A · White Balance: Auto, +2 Red & -5 Blue · Highlight: +3 · Shadow: -1 · Color: +2 · Sharpness: -1 · High ISO NR: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -2/3 to 0 (typically)", "https://fujixweekly.com/2024/02/05/vintage-eterna-fujifilm-x-trans-v-x-trans-iv-x-h1-film-simulation-recipes/"),
                r("Vintage Cinema", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 4900K, +3 Red & +3 Blue · Highlight: +3 · Shadow: -2 · Color: -1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to -2 (typically)", "https://fujixweekly.com/2024/06/07/vintage-cinema-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Eterna Summer", "Eterna", "Film Simulation: Eterna · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong(X-Trans IV); Weak (X-Trans V) · White Balance: Daylight, +3 Red & -7 Blue · Highlight: +2.5 · Shadow: 0 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/04/08/eterna-summer-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Expired Kodak Vision2 250D", "EternaBleachBypass", "Film Simulation: Eterna Bleach Bypass · D-Range Priority: DR-P Auto · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off(X-Trans V); Weak (X-Trans IV) · White Balance: 8700K, -4 Red & -3 Blue · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2025/12/23/expired-kodak-vision2-250d-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras/"),
                r("Easy Reala Ace", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, 0 Red & 0 Blue · Highlight: -1 · Shadow: 0 · Color: 0 · Sharpness: 0 · High ISO NR: -4 · Clarity: 0 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2024/06/20/easy-reala-ace-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Reala Ace", "ClassicNegative", "Film Simulation: Classic Negative · Grain: Weak Small · Color Chrome: Strong · FX Blue: Strong · WB: Auto (R -1, B +1) · DR400 · Highlight: -1.5 · Shadow: -2 · Color: +2 · Sharpness: 0 · NR: -4 · Clarity: -2", "https://fujixweekly.com/2023/09/15/reala-ace-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Nostalgia Negative", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain: Strong Small · Color Chrome: Strong · FX Blue: Weak · WB: Daylight (R +3, B -3) · DR400 · Highlight: -1 · Shadow: +1 · Color: +4 · Sharpness: -1 · NR: -4 · Clarity: -3", "https://fujixweekly.com/2022/11/22/nostalgia-negative-my-first-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Nostalgic Air", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain Effect: Strong, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Auto, +5 Red & -1 Blue · Dynamic Range: DR-Auto · Highlight: -1 · Shadow: 0 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2026/04/19/nostalgic-air-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Nostalgic Americana", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR200 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 5800K, -2 Red & -4 Blue · Highlight: -1 · Shadow: -1.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/04/29/nostalgic-americana-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Timeless Negative", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Auto, +2 Red & -3 Blue · Dynamic Range: DR400 · Highlight: +2 · Shadow: -2 · Color: -3 · Sharpness: 0 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -2/3 to +1/3 (typically)", "https://fujixweekly.com/2022/11/30/timeless-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Appalachian Negative", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 5200K, +2 Red & -2 Blue · Highlight: -1.5 · Shadow: 0 · Color: +4 · Sharpness: +2 · High ISO NR: -4 · Clarity: 0 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 1/3 (typically)", "https://fujixweekly.com/2024/06/18/appalachian-negative-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujifilm Negative", "RealaAce", "Film Simulation: Reala Ace · Grain: Weak Small · Color Chrome: Strong · FX Blue: Off · WB: 5000K (R 0, B -2) · DR400 · Highlight: -1 · Shadow: -0.5 · Color: +2 · Sharpness: -1 · NR: -4 · Clarity: -2", "https://fujixweekly.com/2024/10/31/fujifilm-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Film", "ClassicNegative", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Weak · White Balance: 5500K, -1 Red & -1 Blue · Highlight: -1 · Shadow: -0.5 · Color: -2 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2024/09/18/fujicolor-film-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor 100 Industrial", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Off · White Balance: 3100K, +8 Red & -8 Blue · Highlight: +0.5 · Shadow: +1.5 · Color: -1 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/04/05/fujicolor-100-industrial-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Superia 100", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, 0 Red & -1 Blue · Dynamic Range: DR-Auto · Highlight: -1 · Shadow: -2 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +2/3 (typically)", "https://fujixweekly.com/2023/07/11/fujicolor-superia-100-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Superia 1600", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Daylight, +3 Red & +1 Blue · Highlight:0 · Shadow: +2 · Color: -3 · Sharpness: -1 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 — for best results: 1600 to 6400 · Exposure Compensation: 0 to +1 (typically)", "https://fujixweekly.com/2024/02/25/fujicolor-superia-1600-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Natura 1600", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 5500K, -1 Red & -2 Blue · Highlight:-1.5 · Shadow: +1.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2024/02/29/fujicolor-natura-1600-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Super HG v2", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Weak, Large · Color Chrome Effect: Weak · Color Chrome FX Blue: Weak · White Balance: Auto White Priority, -3 Red & -1 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: +1 · Color: +2 · Sharpness: 0 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to +1 (typically)", "https://fujixweekly.com/2023/07/20/fujicolor-super-hg-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Reala 100", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Daylight, 0 Red & 0 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: -1 · Color: 0 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +1 (typically)", "https://fujixweekly.com/2023/09/06/fujicolor-reala-100-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Superia Xtra 400", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Strong, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Weak · White Balance: Auto, +3 Red & -5 Blue · Dynamic Range: DR400 · Highlight: 0 · Shadow: -1 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +1 (typically)", "https://fujixweekly.com/2022/12/09/superia-xtra-400-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Reggie's Superia", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR-Auto · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, +1 Red & -3 Blue · Highlight: -2 · Shadow: -1 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0 · ISO: Auto, up to ISO 6400", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Copenhagen Negative", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Strong, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Strong · White Balance: 5700K, +1 Red & +1 Blue · Dynamic Range: DR400 · Highlight: +2.5 · Shadow: -2 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to -2/3 (typically)", "https://fujixweekly.com/2026/05/18/copenhagen-negative-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("Pushed Analog", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Fluorescent 1, -3 Red & -2 Blue · Highlight: +1.5 · Shadow: +2 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +2/3 (typically)", "https://fujixweekly.com/2024/08/29/pushed-analog-fujifilm-x-t50-x-trans-v-film-simulation-recipe/"),
                r("Indoor Angouleme", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto Ambience Priority, -2 Red & -6 Blue · Dynamic Range: DR400 · Highlight: -2 · Shadow: -2 · Color: -1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("1970's Summer", "NostalgicNegative", "Film Simulation: Nostalgic Neg · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 6500K, -1 Red & -4 Blue · Dynamic Range: DR400 · Highlight: -2 · Shadow: -0.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2022/11/27/1970s-summer-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vibrant Arizona", "Velvia", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Weak · White Balance: 4350K, +6 Red & -8 Blue · Dynamic Range: DR-P Strong · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +2/3 to + 1 1/3 (typically)", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("Vivid Velvia", "Velvia", "Film Simulation: Velvia · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto Ambience Priority, +1 Red & -3 Blue · Highlight: -1 · Shadow: -1 · Color: +4 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2025/10/28/vivid-velvia-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("BewareMyVelvia", "Velvia", "Film Simulation: Velvia · Dynamic Range: DR200 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 6590K, -9 Red & 0 Blue · Highlight: +2 · Shadow: +2 · Color: +4 · Sharpness: +1 · High ISO NR: -4 · Clarity: 0 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically) ·", "https://fujixweekly.com/2026/01/13/bewaremyvelvia-a-fujifilm-film-simulation-recipe-for-x-trans-iv-v/"),
                r("Provia Positive", "Provia", "Film Simulation: Provia/STD · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto White Priority, +2 Red & -3 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: +1 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: +1/3 to +1 (typically)", "https://fujixweekly.com/2026/05/27/fujifilm-recipe-provia-positive/"),
                r("RedScale", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR200 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 10000K, +9 Red & -9 Blue · Highlight: 0 · Shadow: +2 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3 · ISO: Auto, up to ISO 6400 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2025/02/18/redscale-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("Ilford FP4 Plus 125", "Monochrome", "Film Simulation: Monochrome · Grain Effect: Weak, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Daylight, +6 Red & -8 Blue · Dynamic Range: DR200 · Highlight: -0.5 · Shadow: -1.5 · Monochromatic Color: 0 WC & 0 MG · Sharpness: 0 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1 to -1/3 (typically)", "https://fujixweekly.com/2022/12/28/ilford-fp4-plus-125-fujifilm-x-t5-x-trans-v-x-trans-iv-film-simulation-recipe/"),
                r("Kodak Plus-X 125", "Acros", "Film Simulation: Acros(incl. +Ye, +R, or +G) · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Dynamic Range: DR200 · Grain Effect: Weak, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +9 Red & +9 Blue · Highlight: -1 · Shadow: +1 · Sharpness: -1 · High ISO NR: -4 · Clarity: +2 · ISO: Auto, up to ISO 6400 · Exposure Compensation: -1/3 to -2/3 (typically)", "https://fujixweekly.com/2025/09/21/kodak-plus-x-125-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Kodak T-Max 100 Soft Tone", "MonochromeGreen", "Film Simulation: Monochrome+G · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Grain Effect: Weak, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Daylight, -6 Red & -3 Blue · Dynamic Range: DR200 · Highlight: -0.5 · Shadow: +1.5 · Sharpness: -1 · High ISO NR: -4 · Clarity: -1 · ISO: up to ISO 6400 · Exposure Compensation: -2/3 to +2/3 (typically)", "https://fujixweekly.com/2024/10/07/kodak-t-max-100-soft-tone-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras-part-2/"),
                r("Kodak T-Max 100 Hard Tone", "MonochromeGreen", "Film Simulation: Monochrome+G · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Grain Effect: Weak, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Daylight, -6 Red & -3 Blue · Dynamic Range: DR200 · Highlight: +0.5 · Shadow: +2.5 · Sharpness: -1 · High ISO NR: -4 · Clarity: -1 · ISO: up to ISO 6400 · Exposure Compensation: -2/3 to +2/3 (typically)", "https://fujixweekly.com/2024/10/04/kodak-t-max-100-hard-tone-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras-part-1/"),
                r("Kodak T-Max P3200", "Monochrome", "Film Simulation: Acros · Monochromatic Color(Toning) : WC -1 & MG -1 · Grain Effect: Strong, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: 5500K, +4 Red & +7 Blue · Dynamic Range: DR400 · Highlight: +1 · Shadow: +3 · Sharpness: +2 · High ISO NR: -4 · Clarity: +1 · ISO: up to ISO 12800 · Exposure Compensation: 0 to +2/3 (typically)", "https://fujixweekly.com/2023/05/18/kodak-t-max-p3200-a-fujifilm-film-simulation-recipe-for-x-trans-iv-v/"),
                r("Agfa Scala", "Acros", "Film Simulation: Acros(incl. +Ye, +R, or +G) · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Dynamic Range: DR100 · Grain Effect: Weak, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Auto, 0 Red & 0 Blue · Highlight: +4 · Shadow: 0 · Sharpness: +1 · High ISO NR: -4 · Clarity: +2", "https://fujixweekly.com/2026/02/27/agfa-scala-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Classic B&W", "AcrosGreen", "Film Simulation: Acros+G · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Dynamic Range: DR200 · Grain Effect: Strong, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Incandescent, -9 Red & +9 Blue · Highlight: +3 · Shadow: +4 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3", "https://fujixweekly.com/2025/08/03/classic-bw-film-simulation-recipe/"),
                r("FRGMT B&W", "Acros", "Film Simulation: Acros(incl. +Ye, +R, or +G) · Monochromatic Color(Toning) : WC 0 & MG 0 (Off) · Dynamic Range: DR-Auto · Grain Effect: Strong, Large · Color Chrome Effect: Off · Color Chrome FX Blue: Off · White Balance: Auto, 0 Red & 0 Blue · Highlight: +4 · Shadow: +2 · Sharpness: -4 · High ISO NR: -4 · Clarity: +5", "https://fujixweekly.com/2025/12/04/frgmt-bw-a-fujifilm-film-simulation-recipe-by-hiroshi-fujiwara/"),
            ),
        ),
        // --- Osan Bilgi (real) ----------------------------------------------
        DiscoverCollection(
            id = "osan",
            name = "Osan-Bilgi",
            tagline = "Recipes de Oguzhan Bilgi",
            logo = "🎞️",
            colorHex = 0xFF463A66,
            source = "https://www.osan-bilgi.com/fujifilm-recipes",
            recipes = listOf(
                r("Classic Cuban Negative", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -5 Blue & +4 Red · Highlight: -2 · Shadow: +1 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4", "https://www.osan-bilgi.com/classic-cuban-negative"),
                r("Cubanace", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -5 Blue & +4 Red · Highlight: -2 · Shadow: +1.5 · Color: +1 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4", "https://www.osan-bilgi.com/cubanace"),
                r("Summer Chrome", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, -6 Blue & +5 Red · Highlight: -2 · Shadow: -2 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -4", "https://www.osan-bilgi.com/summer-chrome"),
                r("Vibrant Astia Soft", "Astia", "Film Simulation: Astia Soft · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, -2 Blue & +2 Red · Highlight: -2 · Shadow: +1 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -4", "https://www.osan-bilgi.com/vibrant-astia-soft"),
                r("Alpine Negative", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Off · Color Chrome Effect: Off · Color Chrome FX Blue: Strong · White Balance: Auto, -4 Blue & +2 Red · Highlight: -2 · Shadow: +1 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: -3", "https://www.osan-bilgi.com/alpine-negative"),
            ),
        ),
        // --- Alex Armitage (real) -------------------------------------------
        DiscoverCollection(
            id = "armitage",
            name = "Alex Armitage",
            tagline = "Lo que usa en su X100VI",
            logo = "🌙",
            colorHex = 0xFF5A3826,
            source = "https://www.alexarmitage.com/fuji-x100vi-recipes",
            recipes = listOf(
                r("Kodak Portra 800", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 6600K, -1 Red & -3 Blue · Highlight: -2 · Shadow: -0.5 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Kodak Gold 200", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Small · Color Chrome Effect: Weak · Color Chrome FX Blue: Off · White Balance: Daylight, +4 Red & -5 Blue · Dynamic Range: DR400 · Highlight: -1.5 · Shadow: +0.5 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Wes Anderson", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Weak · White Balance: 4350K, +6 Red & -8 Blue · Dynamic Range: DR-P Strong · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("1970's Summer", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain Effect: Off, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 6300K, -1 Red & -4 Blue · Dynamic Range: DR400 · Highlight: -2 · Shadow: -0.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Gentle Reala Ace", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, 0 Red & 0 Blue · Highlight: -1 · Shadow: 0 · Color: 0 · Sharpness: 0 · High ISO NR: -4 · Clarity: 0", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
            ),
        ),
        // --- Reggie's (real) -------------------------------------------------
        DiscoverCollection(
            id = "reggies",
            name = "Reggie's",
            tagline = "Recipes de Reggie",
            logo = "🍜",
            colorHex = 0xFF4E442C,
            source = "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/",
            recipes = listOf(
                r("Reggie's Superia", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR-Auto · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, +1 Red & -3 Blue · Highlight: -2 · Shadow: -1 · Color: +1 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Reggie's Portra", "ClassicChrome", "Classic Chrome · DR-Auto · H -1 / S -1 · Color +2 · NR -4 · Sharp -2 · Grain Weak Small · CC Strong · FX Blue Weak · WB Auto +2R/-4B", "https://fujixweekly.com/fujifilm-x-trans-v-recipes/"),
                r("Reggie's B&W", "AcrosRed", "Film Simulation: Acros + R Filter · Grain Effect: Weak, Small · WB: Auto · Dynamic Range: Auto · DR Priority: Off · Tone Curve: H +2, S +2 · Sharpness: -1 · Noise Reduction: -3 · Clarity: 0", "https://fujixweekly.com/fujifilm-x-trans-v-recipes/"),
            ),
        ),
        // --- Black and White (Kevin Mullins, real) --------------------------
        DiscoverCollection(
            id = "bw",
            name = "Black and White",
            tagline = "B/N de Kevin Mullins",
            logo = "⚫",
            colorHex = 0xFF3E3E3E,
            source = "https://www.kevinmullinsphotography.co.uk/blog/fujifilm-recipe-black-and-white",
            recipes = listOf(
                r(
                    "Newspaper",
                    "AcrosYellow",
                    "Film Simulation: Acros + Ye · Grain Effect: Strong · Grain Size: Small · WB Shift: R -4, B -3 · Highlight Tone: +4 · Shadow Tone: +4 · Sharpness: +2 · Clarity: 0 · High ISO NR: 0 · Look de prensa B/N de los 70-80",
                    "https://www.kevinmullinsphotography.co.uk/blog/fujifilm-recipe-black-and-white",
                ),
            ),
        ),
        // --- EDDY's Point & Shoot (real, from image) ------------------------
        DiscoverCollection(
            id = "eddy",
            name = "EDDY",
            tagline = "Point & Shoot",
            logo = "📸",
            colorHex = 0xFF55303C,
            source = "",
            recipes = listOf(
                r(
                    "EDDY'S POINT & SHOOT",
                    "ClassicNegative",
                    "Film Sim: Classic Negative · Grain: Strong Large · Colour Chrome: Off · FX Blue: Weak · WB: Auto (R +4, B -4) · Dynamic Range: 200 · Tone Curve: H +1 / S 0 · Colour: +3 · Sharpness: -4 · NR: -4 · Clarity: 0 · Usa Tiffen Black Pro Mist 1/8 (si no, Clarity -3) · Suele sobreexponer +1",
                    "",
                ),
                r(
                    "EDDY'S POINT & SHOOT 2",
                    "ClassicNegative",
                    "Para sunset/golden hour · Film Sim: Classic Negative · Grain: Strong Large · Colour Chrome: Strong · FX Blue: Strong · WB: Auto (R +5, B -5) · Dynamic Range: 200 · Tone Curve: H -1 / S +2 · Colour: +4 · Sharpness: -4 · NR: -4 · Clarity: 0 · Usa Tiffen Black Pro Mist 1/8 (si no, Clarity -3)",
                    "",
                ),
            ),
        ),
        // --- 2026 Popular (Top 26 most-viewed, Fuji X Weekly) ---------------
        DiscoverCollection(
            id = "popular2026",
            name = "2026 Popular",
            tagline = "Top 26 recipes más vistas de 2026",
            logo = "🏆",
            colorHex = 0xFF35445C,
            source = "https://fujixweekly.com/2026/08/03/top-26-most-popular-fujifilm-recipes-of-2026-so-far-summer-edition/",
            recipes = listOf(
                r("Reggie's Portra", "ClassicChrome", "Classic Chrome · DR-Auto · H -1 / S -1 · Color +2 · NR -4 · Sharp -2 · Grain Weak Small · CC Strong · FX Blue Weak · WB Auto +2R/-4B", "https://fujixweekly.com/2022/06/11/fujifilm-x-trans-iv-film-simulation-recipe-reggies-portra/"),
                r("Kodachrome 64", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -5 Blue · Dynamic Range: DR200 · Highlight: 0 · Shadow: +0.5 · Color: +2 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3", "https://fujixweekly.com/2020/05/27/my-fujifilm-x100v-kodachrome-64-film-simulation-recipe/"),
                r("Kodak Gold 200", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Daylight, +4 Red & -4 Blue · Dynamic Range: DR400 · Highlight: 0 · Shadow: +1 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: +2", "https://fujixweekly.com/2023/10/24/kodak-gold-200-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vibrant Arizona", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Off · Color Chrome FX Blue: Weak · White Balance: 4350K, +6 Red & -8 Blue · Dynamic Range: DR-P Strong · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("Kodachrome 64 (X-Trans V)", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: Daylight, +2 Red & -5 Blue · Dynamic Range: DR200 · Highlight: 0 · Shadow: +0.5 · Color: +2 · Sharpness: +1 · High ISO NR: -4 · Clarity: +3", "https://fujixweekly.com/2022/11/28/kodachrome-64-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 400 v2", "ClassicChrome", "Film Simulation: Classic Chrome · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 5200K, +1 Red & -6 Blue · Dynamic Range: DR400 · Highlight: 0 · Shadow: -2 · Color: +2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -2", "https://fujixweekly.com/2022/12/16/kodak-portra-400-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Tri-X 400", "Acros", "Film Simulation: Acros (Y/R/G) · Dynamic Range: DR200 · Highlight: 0 · Shadow: +3 · Noise Reduction: -4 · Sharpening: +1 · Clarity: +4 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome Effect Blue: Off · White Balance: Daylight, +9 Red & -9 Blue", "https://fujixweekly.com/2020/06/18/fujifilm-x100v-film-simulation-recipe-kodak-tri-x-400/"),
                r("Universal Negative", "ClassicNegative", "Film Simulation: Any (see below) · Dynamic Range: DR400 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 4000K, 0 Red & -5 Blue · Highlight: 0 · Shadow: -2 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0", "https://fujixweekly.com/2025/03/28/universal-negative-14-fujifilm-x100vi-x-trans-v-film-simulation-recipes-yes-14/"),
                r("CineStill 800T", "Eterna", "Film Simulation: Eterna · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 4000K, -5 Red & +2 Blue · Highlight: -1 · Shadow: +2 · Color: -1 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2024/04/16/cinestill-800t-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pacific Blues", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Strong Large · Color Chrome: Strong · FX Blue: Weak · WB: 5800K (R +1, B -3) · DR400 · Highlight: -2 · Shadow: +3 · Color: +4 · Sharpness: -2 · NR: -4 · Clarity: -3", "https://fujixweekly.com/2022/12/06/pacific-blues-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujifilm Negative", "RealaAce", "Film Simulation: Reala Ace · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · WB: 5000K (R 0, B -2) · DR400 · Highlight: -1 · Shadow: -0.5 · Color: +2 · Sharpness: -1 · NR: -4 · Clarity: -2", "https://fujixweekly.com/2024/10/31/fujifilm-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 800 v3", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 6600K, -1 Red & -3 Blue · Highlight: -2 · Shadow: -0.5 · Color: +3 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2024/02/14/kodak-portra-800-v3-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Pacific Blues (IV)", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR400 · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 5800K, +1 Red & -3 Blue · Highlight: -2 · Shadow: +3 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2022/08/04/fujifilm-x-e4-x-trans-iv-film-simulation-recipe-pacific-blues/"),
                r("Reggie's Superia", "ClassicNegative", "Film Simulation: Classic Negative · Dynamic Range: DR-Auto · Grain Effect: Strong, Large · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: Auto, +2 Red & -3 Blue · Highlight: -2 · Shadow: +2 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Kodak Portra 400 v2 (IV)", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR400 · Highlight: 0 · Shadow: -2 · Color: +2 · Noise Reduction: -4 · Sharpening: -2 · Clarity: -2 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome Effect Blue: Weak · White Balance: 5200K, +1 Red & -6 Blue", "https://fujixweekly.com/2020/12/30/fujifilm-x100v-film-simulation-recipe-kodak-portra-400-v2/"),
                r("California Summer", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Strong · White Balance: 6300K, -1 Red & -4 Blue · Highlight: -2 · Shadow: -0.5 · Color: -2 · Sharpness: -2 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2024/06/04/california-summer-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("14 Film Dial Recipes", "ClassicChrome", "Film Simulation: Any (see below) · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto White Priority, +2 Red & -4 Blue · Highlight: -1.5 · Shadow: -1 · Color: +3 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2", "https://fujixweekly.com/2024/05/16/fujifilm-x-t50-film-dial-settings-14-new-film-simulation-recipes-yes-14/"),
                r("Kodak Portra 400", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR-Auto · Highlight: -1 · Shadow: -2 · Color: +2 · Noise Reduction: -4 · Sharpening: -2 · Clarity: +2 · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome Effect Blue: Weak · White Balance: Daylight, +3 Red & -5 Blue", "https://fujixweekly.com/2020/06/10/fujifilm-x100v-film-simulation-kodak-portra-400/"),
                r("Reala Ace", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Weak Small · Color Chrome: Strong · FX Blue: Strong · WB: Auto (R -1, B +1) · DR400 · Highlight: -1.5 · Shadow: -2 · Color: +2 · Sharpness: 0 · NR: -4 · Clarity: -2", "https://fujixweekly.com/2023/09/15/reala-ace-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Easy Reala Ace", "RealaAce", "Film Simulation: Reala Ace · Dynamic Range: DR400 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, 0 Red & 0 Blue · Highlight: -1 · Shadow: 0 · Color: 0 · Sharpness: 0 · High ISO NR: -4 · Clarity: 0", "https://fujixweekly.com/2024/06/20/easy-reala-ace-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Kodak Gold 200 (III)", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR-Auto · Highlight: -2 · Shadow: +1 · Color: +3 · Noise Reduction: -4 · Sharpening: -2 · Grain Effect: Strong · Color Chrome Effect: Off · White Balance: Daylight, +4 Red & -5 Blue", "https://fujixweekly.com/2020/04/16/my-fujifilm-x-t30-kodak-gold-200-film-simulation-recipe/"),
                r("PRO Negative 160C", "ProNegHigh", "Film Simulation: Pro Neg. Hi · Dynamic Range: DR200 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: Auto, +1 Red & -2 Blue · Highlight: -0.5 · Shadow: -0.5 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -2", "https://fujixweekly.com/2024/03/27/pro-negative-160c-fujifilm-x100vi-film-simulation-recipe/"),
                r("1976 Kodak", "NostalgicNegative", "Film Simulation: Nostalgic Neg. · Grain Effect: Strong, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Weak · White Balance: 5800K, +3 Red & -3 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: +1 · Color: +4 · Sharpness: -1 · High ISO NR: -4 · Clarity: -3", "https://fujixweekly.com/2023/08/03/1976-kodak-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vintage Kodachrome", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR200 · Highlight: +4 · Shadow: -2 · Color: +4 · Sharpening: +1 · Noise Reduction: -3 · Grain: Strong · White Balance: Auto, +2 Red, -4 Blue", "https://fujixweekly.com/2017/10/21/my-fujifilm-x100f-vintage-kodachrome-film-simulation-recipe/"),
                r("Fujicolor Super HG v2", "ClassicNegative", "Film Simulation: Classic Negative · Grain Effect: Weak, Large · Color Chrome Effect: Weak · Color Chrome FX Blue: Weak · White Balance: Auto, +3 Red & -3 Blue · Dynamic Range: DR400 · Highlight: -1 · Shadow: +2 · Color: +4 · Sharpness: -2 · High ISO NR: -4 · Clarity: 0", "https://fujixweekly.com/2023/07/20/fujicolor-super-hg-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("McCurry Kodachrome", "ClassicChrome", "Film Simulation: Classic Chrome · Dynamic Range: DR100 · Grain Effect: Weak, Small · Color Chrome Effect: Strong · Color Chrome FX Blue: Off · White Balance: 5900K, -1 Red & +4 Blue · Highlight: 0 · Shadow: 0 · Color: +2 · Sharpness: -2 · High ISO NR: -4", "https://fujixweekly.com/2024/01/25/mccurry-kodachrome-a-fujifilm-x-trans-iv-film-simulation-recipe/"),
            ),
        ),
        // --- REDDIT (real) ---------------------------------------------------
        DiscoverCollection(
            id = "reddit",
            name = "REDDIT",
            tagline = "Compartida por la comunidad",
            logo = "💬",
            colorHex = 0xFF35453A,
            source = "https://www.reddit.com/r/fujix/",
            recipes = listOf(
                r(
                    "LEICA X",
                    "ClassicChrome",
                    "Film Sim: Classic Chrome · Grain: Off · Chrome Effect: Strong · FX Blue: Strong · WB: Auto (R +1, B -2) · DRange: Auto · DRange Prio: Off · Curve: H -1, S 0 · Color: +4 · Sharpness: 0 · High ISO NR: -4 · Clarity: 0",
                    "https://www.reddit.com/r/fujix/",
                ),
            ),
        ),
        // --- Instagram (real, from reel) -------------------------------------
        DiscoverCollection(
            id = "instagram",
            name = "Instagram",
            tagline = "Recipes virales de IG",
            logo = "📱",
            colorHex = 0xFF55303C,
            source = "https://www.instagram.com/reel/Da1zXNRtm1U/?utm_source=ig_web_button_share_sheet",
            recipes = listOf(
                r(
                    "Early Summer",
                    "ClassicNegative",
                    "WB: Shade (R -3, B +3) · DR400 · Grain: Strong Large · CC: Strong · FX Blue: Weak · Color: +2 · Sharpness: 0 · H -1.5 / S -1.0 · NR: -4 · Clarity: 0",
                    "https://www.instagram.com/reel/Da1zXNRtm1U/?utm_source=ig_web_button_share_sheet",
                ),
                r(
                    "Clockwise Negative",
                    "ClassicNegative",
                    "WB: Auto (R +1, B -3) · DR400 · Grain: Weak/Strong Small · CC: Strong · FX Blue: Weak · Color: +4 · Sharpness: -2 · H -1.0 / S -1.5 · NR: -4 · Clarity: -3",
                    "https://www.instagram.com/reel/Da1zXNRtm1U/?utm_source=ig_web_button_share_sheet",
                ),
                r(
                    "Portra 400",
                    "ClassicChrome",
                    "WB: Auto (R +2, B -4) · DR400 · Grain: Strong Small/Large · CC: Strong · FX Blue: Off · Color: +2 · Sharpness: -2 · H -1.0 / S -1.0 · NR: -4 · Clarity: -2",
                    "https://www.instagram.com/reel/Da1zXNRtm1U/?utm_source=ig_web_button_share_sheet",
                ),
                r(
                    "After Rain",
                    "ClassicNegative",
                    "WB: 5200K (R +3, B -4) · DR400 · Grain: Strong Large · CC: Strong · FX Blue: Strong · Color: +4 · Sharpness: -2 · H -0.5 / S +2.0 · NR: -4 · Clarity: 0",
                    "https://www.instagram.com/reel/Da1zXNRtm1U/?utm_source=ig_web_button_share_sheet",
                ),
            ),
        ),
    )

    fun byId(id: String): DiscoverCollection? = collections.firstOrNull { it.id == id }
}
