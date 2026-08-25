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
)

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
                r("Kodachrome 64", "ClassicChrome", "Clásico atemporal, rojos profundos y tonos cálidos", "https://fujixweekly.com/2022/11/28/kodachrome-64-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodachrome 25", "ClassicChrome", "Granos finos, colores suaves y nostálgicos", "https://fujixweekly.com/2023/03/06/kodachrome-25-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 400 v2", "ClassicChrome", "Retrato pastel, pieles suaves", "https://fujixweekly.com/2022/12/16/kodak-portra-400-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 160 v2", "ClassicChrome", "Portra clásico, tonos delicados", "https://fujixweekly.com/2023/11/10/kodak-portra-160-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 800 v3", "ClassicChrome", "Grano visible, colores saturados suaves", "https://fujixweekly.com/2024/02/14/kodak-portra-800-v3-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Kodak Gold 200", "ClassicChrome", "Dorado cálido, contraste suave", "https://fujixweekly.com/2023/10/24/kodak-gold-200-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Ultramax 400", "ClassicChrome", "Saturación viva, sombras suaves", "https://fujixweekly.com/2023/01/17/kodak-ultramax-400-a-film-simulation-recipe-for-the-fujifilm-x-t5-x-trans-v/"),
                r("Kodak Negative", "ClassicChrome", "Negativo Kodak genérico, equilibrado", "https://fujixweekly.com/2022/12/22/kodak-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodacolor VR 200", "ClassicChrome", "Retro americano, tonos crema", "https://fujixweekly.com/2025/04/16/kodacolor-vr-200-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("1976 Kodak", "ClassicChrome", "Estética 70s, colores desvanecidos", "https://fujixweekly.com/2023/08/03/1976-kodak-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("1971 Kodak", "ClassicChrome", "Retro cálido con sombras largas", "https://fujixweekly.com/2026/03/25/1971-kodak-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Kodak Royal Gold 400", "ClassicChrome", "Dorado brillante, contraste medio", "https://fujixweekly.com/2024/03/06/kodak-royal-gold-400-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Kodak Pro 400", "ClassicChrome", "Profesional versátil, neutro", "https://fujixweekly.com/2025/09/06/kodak-pro-400-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Kodak Vericolor VPS", "ClassicChrome", "Vericolor pastel, pieles suaves", "https://fujixweekly.com/2025/07/07/kodak-vericolor-vps-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Vericolor III 160", "ClassicChrome", "Laboratorio clásico, neutro", "https://fujixweekly.com/2026/02/20/kodak-vericolor-iii-160-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Kodak Vericolor Warm", "ClassicChrome", "Cálido de estudio", "https://fujixweekly.com/2022/12/12/kodak-vericolor-warm-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Gold Max 400 Expired", "ClassicChrome", "Caducado dorado, grano extra", "https://fujixweekly.com/2024/12/31/kodak-gold-max-400-expired-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Kodak Farbwelt 200 Expired", "ClassicChrome", "Farbwelt vintage, tonos teñidos", "https://fujixweekly.com/2025/12/09/kodak-farbwelt-200-expired-a-fujifilm-film-simulation-recipe-for-x-trans-v-cameras/"),
                r("Classic Color", "ClassicChrome", "El look Classic Chrome puro", "https://fujixweekly.com/2024/04/22/classic-color-fujifilm-x-t5-x-trans-v-and-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Classic Amber", "ClassicChrome", "Ámbar cálido, toque dorado", "https://fujixweekly.com/2025/11/01/classic-amber-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Classic Retro", "ClassicChrome", "Retro con carácter", "https://fujixweekly.com/2026/07/11/classic-retro-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("1960 Chrome", "ClassicChrome", "Los 60, diapositiva vintage", "https://fujixweekly.com/2024/07/15/1960-chrome-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Vivid Chrome", "ClassicChrome", "Chrome vivo, contraste marcado", "https://fujixweekly.com/2026/04/13/vivid-chrome-a-fujifilm-recipe-for-x-and-gfx-cameras/"),
                r("Vintage Bronze", "ClassicChrome", "Bronce envejecido, tonos tierra", "https://fujixweekly.com/2023/01/13/vintage-bronze-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Summer of 1960", "ClassicChrome", "Verano de los 60, luminoso", "https://fujixweekly.com/2023/03/22/summer-of-1960-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Summer of '59", "ClassicChrome", "Finales de los 50, nostálgico", "https://fujixweekly.com/2026/05/02/summer-of-59-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("Summer Sun", "ClassicChrome", "Sol de verano, cálido y alegre", "https://fujixweekly.com/2025/06/27/summer-sun-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("California Summer", "ClassicChrome", "Costa oeste, luz dorada", "https://fujixweekly.com/2024/06/04/california-summer-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Texas Sun", "ClassicChrome", "Sol de Texas, saturado", "https://fujixweekly.com/2024/12/04/texas-sun-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pacific Blues", "ClassicChrome", "Azules del Pacífico, frescura", "https://fujixweekly.com/2022/12/06/pacific-blues-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Emulsion '86", "ClassicChrome", "Emulsión 86, grano vintage", "https://fujixweekly.com/2023/01/07/creating-your-own-film-simulation-recipe-for-a-unique-look-emulsion-86-a-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Agfa Ultra 100 v2", "ClassicChrome", "Agfa ultra, colores punchy", "https://fujixweekly.com/2024/02/20/agfa-ultra-100-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Thommy's Ektachrome", "ClassicChrome", "Ektachrome de Thommy, diapositiva", "https://fujixweekly.com/2023/03/24/thommys-ektachrome-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Retro Slide", "ClassicChrome", "Diapositiva retro, colores vivos", "https://fujixweekly.com/2026/08/10/retro-slide-a-fujifilm-recipe-for-5th-gen-x-trans-v-cameras/"),
                r("Kodak Vision3 250D v2", "ClassicChrome", "Cine de día, visión 3", "https://fujixweekly.com/2023/12/19/kodak-vision3-250d-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("CineStill 400D v1", "Eterna", "CineStill 400D, halos suaves", "https://fujixweekly.com/2023/01/17/cinestill-400d-v1-a-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("CineStill 400D v2", "Eterna", "CineStill 400D v2, más contraste", "https://fujixweekly.com/2023/01/17/cinestill-400d-v2-a-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("CineStill 800T", "Eterna", "Tungsteno, neones nocturnos", "https://fujixweekly.com/2024/04/16/cinestill-800t-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pushed CineStill 800T", "Eterna", "800T forzado, grano extra", "https://fujixweekly.com/2023/07/22/pushed-cinestill-800t-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Expired ECN-2 100T", "Eterna", "ECN-2 caducado, teñido", "https://fujixweekly.com/2024/02/01/expired-ecn-2-100t-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Fluorescent Night", "Eterna", "Noche fluorescente, verde frío", "https://fujixweekly.com/2023/12/04/fluorescent-night-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vintage Eterna", "Eterna", "Eterna vintage, cinematográfico", "https://fujixweekly.com/2024/02/05/vintage-eterna-fujifilm-x-trans-v-x-trans-iv-x-h1-film-simulation-recipes/"),
                r("Vintage Cinema", "Eterna", "Cine antiguo, tonos apagados", "https://fujixweekly.com/2024/06/07/vintage-cinema-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Eterna Summer", "Eterna", "Verano cinematográfico", "https://fujixweekly.com/2024/04/08/eterna-summer-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Expired Kodak Vision2 250D", "Eterna", "Vision2 caducado, luz de día", "https://fujixweekly.com/2025/12/23/expired-kodak-vision2-250d-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras/"),
                r("Easy Reala Ace", "RealaAce", "Reala Ace fácil, natural", "https://fujixweekly.com/2024/06/20/easy-reala-ace-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Reala Ace", "RealaAce", "Reala Ace original", "https://fujixweekly.com/2023/09/15/reala-ace-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Nostalgia Negative", "NostalgicNegative", "Nostálgico, sombras largas", "https://fujixweekly.com/2022/11/22/nostalgia-negative-my-first-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Nostalgic Air", "NostalgicNegative", "Aire nostálgico, luminoso", "https://fujixweekly.com/2026/04/19/nostalgic-air-a-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Nostalgic Americana", "ClassicNegative", "América nostálgica, retrato", "https://fujixweekly.com/2024/04/29/nostalgic-americana-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Timeless Negative", "ClassicNegative", "Negativo atemporal", "https://fujixweekly.com/2022/11/30/timeless-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Appalachian Negative", "ClassicNegative", "Montañas Apalaches, verde profundo", "https://fujixweekly.com/2024/06/18/appalachian-negative-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujifilm Negative", "ClassicNegative", "Negativo Fuji por excelencia", "https://fujixweekly.com/2024/10/31/fujifilm-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Film", "ClassicNegative", "Fujicolor clásico", "https://fujixweekly.com/2024/09/18/fujicolor-film-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor 100 Industrial", "ClassicNegative", "Industrial, tonos fríos", "https://fujixweekly.com/2024/04/05/fujicolor-100-industrial-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Superia 100", "ClassicNegative", "Superia suave y neutro", "https://fujixweekly.com/2023/07/11/fujicolor-superia-100-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Superia 1600", "ClassicNegative", "Superia rápida, grano visible", "https://fujixweekly.com/2024/02/25/fujicolor-superia-1600-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Natura 1600", "ClassicNegative", "Natura nocturna, cálido", "https://fujixweekly.com/2024/02/29/fujicolor-natura-1600-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Super HG v2", "ClassicNegative", "Super HG, contraste medio", "https://fujixweekly.com/2023/07/20/fujicolor-super-hg-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujicolor Reala 100", "ClassicNegative", "Reala, pieles perfectas", "https://fujixweekly.com/2023/09/06/fujicolor-reala-100-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Superia Xtra 400", "ClassicNegative", "Xtra 400, versátil", "https://fujixweekly.com/2022/12/09/superia-xtra-400-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Reggie's Superia", "ClassicNegative", "Superia de Reggie, vibrante", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Copenhagen Negative", "ClassicNegative", "Copenhague, luz nórdica", "https://fujixweekly.com/2026/05/18/copenhagen-negative-a-fujifilm-recipe-for-fifth-generation-cameras/"),
                r("Pushed Analog", "ClassicNegative", "Analógico forzado, grano", "https://fujixweekly.com/2024/08/29/pushed-analog-fujifilm-x-t50-x-trans-v-film-simulation-recipe/"),
                r("Indoor Angouleme", "ClassicNegative", "Interior de Angulema, suave", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("1970's Summer", "NostalgicNegative", "Verano de los 70, desvanecido", "https://fujixweekly.com/2022/11/27/1970s-summer-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vibrant Arizona", "Velvia", "Arizona vibrante, Wes Anderson", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("Vivid Velvia", "Velvia", "Velvia vivo, naturaleza saturada", "https://fujixweekly.com/2025/10/28/vivid-velvia-fujifilm-x-e5-x-trans-v-film-simulation-recipe/"),
                r("BewareMyVelvia", "Velvia", "Velvia extremo, punch total", "https://fujixweekly.com/2026/01/13/bewaremyvelvia-a-fujifilm-film-simulation-recipe-for-x-trans-iv-v/"),
                r("Provia Positive", "Provia", "Provia positivo, diapositiva", "https://fujixweekly.com/2026/05/27/fujifilm-recipe-provia-positive/"),
                r("RedScale", "ClassicChrome", "Redscale, rojos intensos", "https://fujixweekly.com/2025/02/18/redscale-fujifilm-x-trans-iv-v-film-simulation-recipe/"),
                r("Ilford FP4 Plus 125", "Monochrome", "Ilford FP4, clásico B/N", "https://fujixweekly.com/2022/12/28/ilford-fp4-plus-125-fujifilm-x-t5-x-trans-v-x-trans-iv-film-simulation-recipe/"),
                r("Kodak Plus-X 125", "Monochrome", "Plus-X, B/N fino", "https://fujixweekly.com/2025/09/21/kodak-plus-x-125-fujifilm-x-trans-iv-x-trans-v-film-simulation-recipe/"),
                r("Kodak T-Max 100 Soft Tone", "Monochrome", "T-Max suave, tonos delicados", "https://fujixweekly.com/2024/10/07/kodak-t-max-100-soft-tone-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras-part-2/"),
                r("Kodak T-Max 100 Hard Tone", "Monochrome", "T-Max duro, contraste fuerte", "https://fujixweekly.com/2024/10/04/kodak-t-max-100-hard-tone-a-film-simulation-recipe-for-fujifilm-x-trans-iv-x-trans-v-cameras-part-1/"),
                r("Kodak T-Max P3200", "Monochrome", "P3200, grano grueso", "https://fujixweekly.com/2023/05/18/kodak-t-max-p3200-a-fujifilm-film-simulation-recipe-for-x-trans-iv-v/"),
                r("Agfa Scala", "Monochrome", "Scala, B/N sofisticado", "https://fujixweekly.com/2026/02/27/agfa-scala-fujifilm-recipe-for-x-trans-v-cameras/"),
                r("Classic B&W", "Monochrome", "B/N clásico, versátil", "https://fujixweekly.com/2025/08/03/classic-bw-film-simulation-recipe/"),
                r("FRGMT B&W", "Monochrome", "B/N de Hiroshi Fujiwara", "https://fujixweekly.com/2025/12/04/frgmt-bw-a-fujifilm-film-simulation-recipe-by-hiroshi-fujiwara/"),
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
                r("Classic Cuban Negative", "ClassicNegative", "Negativo cubano clásico", "https://www.osan-bilgi.com/classic-cuban-negative"),
                r("Cubanace", "ClassicNegative", "Cuba con toque Reala Ace", "https://www.osan-bilgi.com/cubanace"),
                r("Summer Chrome", "ClassicChrome", "Chrome veraniego, luminoso", "https://www.osan-bilgi.com/summer-chrome"),
                r("Vibrant Astia Soft", "Astia", "Astia suave y vibrante", "https://www.osan-bilgi.com/vibrant-astia-soft"),
                r("Alpine Negative", "ClassicNegative", "Alpino, verde frío", "https://www.osan-bilgi.com/alpine-negative"),
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
                r("Kodak Portra 800", "ClassicChrome", "Portra 800 con DR400 y grano fuerte", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Kodak Gold 200", "ClassicChrome", "Gold 200, dorado cálido", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Wes Anderson", "ClassicChrome", "Look Wes Anderson, 4350K", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("1970's Summer", "NostalgicNegative", "Verano 70s, nostálgico", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
                r("Gentle Reala Ace", "RealaAce", "Reala Ace suave y natural", "https://www.alexarmitage.com/fuji-x100vi-recipes"),
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
                r("Reggie's Superia", "ClassicNegative", "Superia de Reggie, vibrante y versátil", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Reggie's Portra", "ClassicChrome", "Classic Chrome · DR-Auto · H -1 / S -1 · Color +2 · NR -4 · Sharp -2 · Grain Weak Small · CC Strong · FX Blue Weak · WB Auto +2R/-4B", "https://fujixweekly.com/fujifilm-x-trans-v-recipes/"),
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
                r("Reggie's Portra", "ClassicChrome", "#1 · El más popular de 2026 · retrato versátil", "https://fujixweekly.com/2022/06/11/fujifilm-x-trans-iv-film-simulation-recipe-reggies-portra/"),
                r("Kodachrome 64", "ClassicChrome", "#2 · Favorito de siempre (X-Trans IV)", "https://fujixweekly.com/2020/05/27/my-fujifilm-x100v-kodachrome-64-film-simulation-recipe/"),
                r("Kodak Gold 200", "ClassicChrome", "#3 · Dorado cálido (X-Trans V)", "https://fujixweekly.com/2023/10/24/kodak-gold-200-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vibrant Arizona", "ClassicChrome", "#4 · Inspirada en Wes Anderson", "https://fujixweekly.com/2023/04/25/getting-a-wes-anderson-look-from-your-fujifilm-camera-4-new-film-simulation-recipes/"),
                r("Kodachrome 64 (X-Trans V)", "ClassicChrome", "#5 · La versión X-Trans V del clásico", "https://fujixweekly.com/2022/11/28/kodachrome-64-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 400 v2", "ClassicChrome", "#6 · Retrato pastel (X-Trans V)", "https://fujixweekly.com/2022/12/16/kodak-portra-400-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Tri-X 400", "Acros", "#7 · El B/N más popular · Acros", "https://fujixweekly.com/2020/06/18/fujifilm-x100v-film-simulation-recipe-kodak-tri-x-400/"),
                r("Universal Negative", "ClassicNegative", "#8 · 14 recipes en una · Film Dial", "https://fujixweekly.com/2025/03/28/universal-negative-14-fujifilm-x100vi-x-trans-v-film-simulation-recipes-yes-14/"),
                r("CineStill 800T", "Eterna", "#9 · Tungsteno nocturno · Eterna", "https://fujixweekly.com/2024/04/16/cinestill-800t-fujifilm-x-trans-v-film-simulation-recipe/"),
                r("Pacific Blues", "ClassicNegative", "#10 · Azules del Pacífico (X-Trans V)", "https://fujixweekly.com/2022/12/06/pacific-blues-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Fujifilm Negative", "RealaAce", "#11 · Reala Ace · negativo Fuji", "https://fujixweekly.com/2024/10/31/fujifilm-negative-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Kodak Portra 800 v3", "ClassicChrome", "#12 · Grano visible, colores suaves", "https://fujixweekly.com/2024/02/14/kodak-portra-800-v3-fujifilm-x-t5-x-trans-v-x-e4-x-trans-iv-film-simulation-recipe/"),
                r("Pacific Blues (IV)", "ClassicNegative", "#13 · La versión X-Trans IV", "https://fujixweekly.com/2022/08/04/fujifilm-x-e4-x-trans-iv-film-simulation-recipe-pacific-blues/"),
                r("Reggie's Superia", "ClassicNegative", "#14 · La revelación de 2026", "https://fujixweekly.com/2026/03/12/reggies-superia-a-fujifilm-recipe-for-x-trans-iv-v-cameras/"),
                r("Kodak Portra 400 v2 (IV)", "ClassicChrome", "#15 · La versión X-Trans IV", "https://fujixweekly.com/2020/12/30/fujifilm-x100v-film-simulation-recipe-kodak-portra-400-v2/"),
                r("California Summer", "NostalgicNegative", "#16 · Nostalgic Neg. mejor rankeada", "https://fujixweekly.com/2024/06/04/california-summer-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("14 Film Dial Recipes", "ClassicChrome", "#17 · Para X-T50 con Film Dial", "https://fujixweekly.com/2024/05/16/fujifilm-x-t50-film-dial-settings-14-new-film-simulation-recipes-yes-14/"),
                r("Kodak Portra 400", "ClassicChrome", "#18 · El clásico Portra (X-Trans IV)", "https://fujixweekly.com/2020/06/10/fujifilm-x100v-film-simulation-kodak-portra-400/"),
                r("Reala Ace", "ClassicNegative", "#19 · Reala Ace pura · favorita personal", "https://fujixweekly.com/2023/09/15/reala-ace-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Easy Reala Ace", "RealaAce", "#20 · Creada por Nathalie Boucry", "https://fujixweekly.com/2024/06/20/easy-reala-ace-fujifilm-x100vi-x-trans-v-film-simulation-recipe/"),
                r("Kodak Gold 200 (III)", "ClassicChrome", "#21 · La versión X-Trans III", "https://fujixweekly.com/2020/04/16/my-fujifilm-x-t30-kodak-gold-200-film-simulation-recipe/"),
                r("PRO Negative 160C", "RealaAce", "#22 · Primera recipe Reala Ace de FXW", "https://fujixweekly.com/2024/03/27/pro-negative-160c-fujifilm-x100vi-film-simulation-recipe/"),
                r("1976 Kodak", "NostalgicNegative", "#23 · Nostálgica, estética 70s", "https://fujixweekly.com/2023/08/03/1976-kodak-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("Vintage Kodachrome", "ClassicChrome", "#24 · La quinta recipe publicada en FXW", "https://fujixweekly.com/2017/10/21/my-fujifilm-x100f-vintage-kodachrome-film-simulation-recipe/"),
                r("Fujicolor Super HG v2", "ClassicNegative", "#25 · Co-creada con Thomas Schwab", "https://fujixweekly.com/2023/07/20/fujicolor-super-hg-v2-fujifilm-x-t5-x-trans-v-film-simulation-recipe/"),
                r("McCurry Kodachrome", "ClassicChrome", "#26 · Homenaje a las diapositivas Kodachrome", "https://fujixweekly.com/2024/01/25/mccurry-kodachrome-a-fujifilm-x-trans-iv-film-simulation-recipe/"),
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
        // --- Analog Dreams (placeholder) ------------------------------------
        DiscoverCollection(
            id = "dreams",
            name = "Analog Dreams",
            tagline = "Sueños analógicos",
            logo = "✨",
            colorHex = 0xFF55303C,
            source = "",
            recipes = listOf(
                r("Golden Hour", "NostalgicNegative", "Dorado, nostálgico, suave", ""),
                r("Moonlit Acros", "Acros", "Monocromo etéreo con sombras largas", ""),
            ),
        ),
    )

    fun byId(id: String): DiscoverCollection? = collections.firstOrNull { it.id == id }
}
