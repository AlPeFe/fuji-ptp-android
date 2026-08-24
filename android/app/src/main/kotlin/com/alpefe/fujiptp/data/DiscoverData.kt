package com.alpefe.fujiptp.data

/**
 * Predefined public collections shown in Discover. These are curated by the
 * app owner (e.g. Fuji X Weekly recipes, community collections), are
 * READ-ONLY: the user can only import recipes from them into their own
 * collections. Hardcoded for now; will be served remotely later.
 */
data class DiscoverRecipe(
    val name: String,
    val filmSimulation: String,
    val description: String,
)

data class DiscoverCollection(
    val id: String,
    val name: String,
    val tagline: String,
    /** Simple logo glyph (emoji placeholder until real logos are provided). */
    val logo: String,
    val colorHex: Long,
    val recipes: List<DiscoverRecipe>,
)

object DiscoverData {

    private fun r(name: String, film: String, desc: String) =
        DiscoverRecipe(name, film, desc)

    val collections: List<DiscoverCollection> = listOf(
        DiscoverCollection(
            id = "fxw",
            name = "Fuji X Weekly",
            tagline = "Recipes de la comunidad",
            logo = "📷",
            colorHex = 0xFF35445C,
            recipes = listOf(
                r("Kodachrome 64", "ClassicChrome", "Clásico, tonos cálidos y rojos profundos"),
                r("Portra 400 v2", "ClassicNegative", "Suave y pastel, ideal para retrato"),
                r("Tri-X 400", "Monochrome", "Blanco y negro granulado, alto contraste"),
            ),
        ),
        DiscoverCollection(
            id = "reddit",
            name = "r/fujix",
            tagline = "Lo mejor de Reddit",
            logo = "🎞️",
            colorHex = 0xFF463A66,
            recipes = listOf(
                r("Cinestill 800T", "Eterna", "Tungsteno, neones y noches urbanas"),
                r("Agfa Vista 200", "Astia", "Verdes frescos, piel suave"),
            ),
        ),
        DiscoverCollection(
            id = "lab",
            name = "Film Recipes Lab",
            tagline = "Experimentos de laboratorio",
            logo = "🧪",
            colorHex = 0xFF35453A,
            recipes = listOf(
                r("Fuji 160NS", "ProNegHigh", "Saturado con sombras suaves"),
                r("Velvia Punch", "Velvia", "Naturaleza saturada y vibrante"),
                r("Bleach Bypass", "EternaBleachBypass", "Contraste alto, color desaturado"),
            ),
        ),
        DiscoverCollection(
            id = "dreams",
            name = "Analog Dreams",
            tagline = "Sueños analógicos",
            logo = "🌙",
            colorHex = 0xFF5A3826,
            recipes = listOf(
                r("Golden Hour", "NostalgicNegative", "Dorado, nostálgico, suave"),
                r("Moonlit Acros", "Acros", "Monocromo etéreo con sombras largas"),
            ),
        ),
    )

    fun byId(id: String): DiscoverCollection? = collections.firstOrNull { it.id == id }
}
