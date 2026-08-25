package com.alpefe.fujiptp.data

import org.json.JSONObject

/**
 * Enums mirror the Rust DTO contract in `fuji-ptp-android/rust`:
 * [wire] must equal the Rust enum name string exactly.
 */
enum class FilmSimulation(val wire: String, val label: String, val hex: Long) {
    Provia("Provia", "PROVIA", 0xFF7BA05B),
    Velvia("Velvia", "VELVIA", 0xFFC0392B),
    Astia("Astia", "ASTIA", 0xFF5D8AA8),
    ProNegHigh("ProNegHigh", "PRO Neg. Hi", 0xFF8E44AD),
    ProNegStandard("ProNegStandard", "PRO Neg. Std", 0xFF6C7A89),
    Monochrome("Monochrome", "MONOCHROME", 0xFF3E3E3E),
    MonochromeYellow("MonochromeYellow", "MONO + Ye", 0xFF8A6D3B),
    MonochromeRed("MonochromeRed", "MONO + R", 0xFF7F2D2D),
    MonochromeGreen("MonochromeGreen", "MONO + G", 0xFF2E5E3A),
    Sepia("Sepia", "SEPIA", 0xFF8B6F47),
    ClassicChrome("ClassicChrome", "CLASSIC CHROME", 0xFFB8860B),
    Acros("Acros", "ACROS", 0xFF4A4A4A),
    AcrosYellow("AcrosYellow", "ACROS + Ye", 0xFF9A7B4F),
    AcrosRed("AcrosRed", "ACROS + R", 0xFF8F3535),
    AcrosGreen("AcrosGreen", "ACROS + G", 0xFF3A6B4A),
    Eterna("Eterna", "ETERNA", 0xFF2F4F6F),
    ClassicNegative("ClassicNegative", "CLASSIC Neg.", 0xFFA0522D),
    EternaBleachBypass("EternaBleachBypass", "ETERNA BLEACH BYPASS", 0xFF556B7A),
    NostalgicNegative("NostalgicNegative", "NOSTALGIC Neg.", 0xFFC97B4A),
    RealaAce("RealaAce", "REALA ACE", 0xFF3A8F8F);

    val color: Long get() = hex
}

enum class DynamicRange(val wire: String, val label: String) {
    Dr100("Dr100", "DR100"),
    Dr200("Dr200", "DR200"),
    Dr400("Dr400", "DR400");
}

enum class GrainEffect(val wire: String, val label: String) {
    Off("Off", "Off"),
    WeakSmall("WeakSmall", "Weak · Small"),
    StrongSmall("StrongSmall", "Strong · Small"),
    WeakLarge("WeakLarge", "Weak · Large"),
    StrongLarge("StrongLarge", "Strong · Large");
}

enum class EffectStrength(val wire: String, val label: String) {
    Off("Off", "Off"),
    Weak("Weak", "Weak"),
    Strong("Strong", "Strong");
}

enum class WhiteBalanceMode(val wire: String, val label: String) {
    Auto("Auto", "Auto"),
    Daylight("Daylight", "Daylight"),
    Shade("Shade", "Shade"),
    Fluorescent1("Fluorescent1", "Fluorescent 1"),
    Fluorescent2("Fluorescent2", "Fluorescent 2"),
    Fluorescent3("Fluorescent3", "Fluorescent 3"),
    Incandescent("Incandescent", "Incandescent"),
    Underwater("Underwater", "Underwater"),
    ColorTemperature("ColorTemperature", "K · Temperatura"),
    AmbiencePriority("AmbiencePriority", "Ambience Priority");
}

/** Recipe domain model. Maps 1:1 to the Rust [com.alpefe.fujiptp.data] DTO. */
data class RecipeModel(
    val id: Long = 0L,
    val name: String = "",
    val filmSimulation: FilmSimulation = FilmSimulation.ClassicChrome,
    val dynamicRange: DynamicRange = DynamicRange.Dr100,
    val grainEffect: GrainEffect = GrainEffect.Off,
    val smoothSkin: EffectStrength = EffectStrength.Off,
    val colorChrome: EffectStrength = EffectStrength.Off,
    val colorChromeFxBlue: EffectStrength = EffectStrength.Off,
    val whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.Auto,
    val whiteBalanceShiftR: Int = 0,
    val whiteBalanceShiftB: Int = 0,
    val whiteBalanceTemperature: Int? = null,
    val highlight: Float = 0f,
    val shadow: Float = 0f,
    val color: Float = 0f,
    val sharpness: Float = 0f,
    val clarity: Float = 0f,
    val noiseReduction: Int = 0,
    val exposure: Float = 0f,
    val dynamicRangePriority: Int = 0,
    val monochromeWc: Float = 0f,
    val monochromeMg: Float = 0f,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isMonochrome: Boolean
        get() = filmSimulation == FilmSimulation.Monochrome ||
            filmSimulation == FilmSimulation.MonochromeYellow ||
            filmSimulation == FilmSimulation.MonochromeRed ||
            filmSimulation == FilmSimulation.MonochromeGreen ||
            filmSimulation == FilmSimulation.Sepia ||
            filmSimulation == FilmSimulation.Acros ||
            filmSimulation == FilmSimulation.AcrosYellow ||
            filmSimulation == FilmSimulation.AcrosRed ||
            filmSimulation == FilmSimulation.AcrosGreen

    fun sameValuesAs(other: RecipeModel): Boolean =
        name == other.name &&
            filmSimulation == other.filmSimulation &&
            dynamicRange == other.dynamicRange &&
            grainEffect == other.grainEffect &&
            smoothSkin == other.smoothSkin &&
            colorChrome == other.colorChrome &&
            colorChromeFxBlue == other.colorChromeFxBlue &&
            whiteBalanceMode == other.whiteBalanceMode &&
            whiteBalanceShiftR == other.whiteBalanceShiftR &&
            whiteBalanceShiftB == other.whiteBalanceShiftB &&
            whiteBalanceTemperature == other.whiteBalanceTemperature &&
            highlight == other.highlight &&
            shadow == other.shadow &&
            color == other.color &&
            sharpness == other.sharpness &&
            clarity == other.clarity &&
            noiseReduction == other.noiseReduction &&
            exposure == other.exposure &&
            dynamicRangePriority == other.dynamicRangePriority &&
            monochromeWc == other.monochromeWc &&
            monochromeMg == other.monochromeMg

    /** Serialize to the exact JSON shape the Rust core (serde) expects. */
    fun toNativeJson(): String = JSONObject()
        .put("name", name)
        .put("film_simulation", filmSimulation.name)
        .put("dynamic_range", dynamicRange.name)
        .put("grain_effect", grainEffect.name)
        .put("smooth_skin", smoothSkin.name)
        .put("color_chrome", colorChrome.name)
        .put("color_chrome_fx_blue", colorChromeFxBlue.name)
        .put(
            "white_balance",
            JSONObject()
                .put("mode", whiteBalanceMode.name)
                .put("shift_r", whiteBalanceShiftR)
                .put("shift_b", whiteBalanceShiftB)
                .put("color_temperature", whiteBalanceTemperature),
        )
        .put("highlight", highlight)
        .put("shadow", shadow)
        .put("color", color)
        .put("sharpness", sharpness)
        .put("clarity", clarity)
        .put("noise_reduction", noiseReduction)
        .put("exposure", exposure)
        .put("dynamic_range_priority", dynamicRangePriority)
        .put("monochrome_wc", monochromeWc)
        .put("monochrome_mg", monochromeMg)
        .toString()

    companion object {
        private inline fun <reified T : Enum<T>> enumByWire(name: String, fallback: T): T =
            enumValues<T>().firstOrNull { (it as? Any?) != null && it.name == name } ?: fallback

        fun fromNativeJson(json: JSONObject): RecipeModel = RecipeModel(
            name = json.optString("name", ""),
            filmSimulation = enumByWire(json.optString("film_simulation"), FilmSimulation.ClassicChrome),
            dynamicRange = enumByWire(json.optString("dynamic_range"), DynamicRange.Dr100),
            grainEffect = enumByWire(json.optString("grain_effect"), GrainEffect.Off),
            smoothSkin = enumByWire(json.optString("smooth_skin"), EffectStrength.Off),
            colorChrome = enumByWire(json.optString("color_chrome"), EffectStrength.Off),
            colorChromeFxBlue = enumByWire(json.optString("color_chrome_fx_blue"), EffectStrength.Off),
            whiteBalanceMode = enumByWire(json.optString("white_balance_mode"), WhiteBalanceMode.Auto),
            whiteBalanceShiftR = json.optInt("white_balance_shift_r", 0),
            whiteBalanceShiftB = json.optInt("white_balance_shift_b", 0),
            whiteBalanceTemperature = if (json.isNull("white_balance_temperature")) null
            else json.optInt("white_balance_temperature", 0).takeIf { it > 0 },
            highlight = json.optDouble("highlight", 0.0).toFloat(),
            shadow = json.optDouble("shadow", 0.0).toFloat(),
            color = json.optDouble("color", 0.0).toFloat(),
            sharpness = json.optDouble("sharpness", 0.0).toFloat(),
            clarity = json.optDouble("clarity", 0.0).toFloat(),
            noiseReduction = json.optInt("noise_reduction", 0),
            exposure = json.optDouble("exposure", 0.0).toFloat(),
            dynamicRangePriority = json.optInt("dynamic_range_priority", 0),
            monochromeWc = json.optDouble("monochrome_wc", 0.0).toFloat(),
            monochromeMg = json.optDouble("monochrome_mg", 0.0).toFloat(),
        )

        fun newDraft(): RecipeModel = RecipeModel(
            name = "New Recipe",
            filmSimulation = FilmSimulation.ClassicChrome,
        )
    }
}
