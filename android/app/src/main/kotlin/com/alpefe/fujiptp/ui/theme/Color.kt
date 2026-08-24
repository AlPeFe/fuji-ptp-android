package com.alpefe.fujiptp.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Design tokens — "playful modern minimalism", dark warm palette.
// The app renders in dark mode with muted pastel accents over a warm
// espresso background, keeping the same playful card language.
// ---------------------------------------------------------------------------

// Backgrounds / neutrals
val Canvas = Color(0xFF171310)        // warm espresso background
val Surface = Color(0xFF241E19)       // cards
val SurfaceSoft = Color(0xFF2E2721)   // soft neutral wells
val Ink = Color(0xFFF2EBE2)           // primary text (warm off-white)
val InkSoft = Color(0xFFB3A89A)       // secondary text
val InkFaint = Color(0xFF7D7368)      // tertiary / hints
val Hairline = Color(0xFF362F28)      // subtle separators

// Pastel accents — muted for dark backgrounds, one hue per section
val Lavender = Color(0xFF463A66)
val LavenderDeep = Color(0xFFA99BE8)
val SoftBlue = Color(0xFF35445C)
val SoftBlueDeep = Color(0xFF8FB4E8)
val PastelGreen = Color(0xFF35453A)
val PastelGreenDeep = Color(0xFF8FC49C)
val Peach = Color(0xFF5A3826)
val PeachDeep = Color(0xFFF0A878)
val SoftYellow = Color(0xFF4E442C)
val SoftYellowDeep = Color(0xFFE0BC6E)
val DustyPink = Color(0xFF55303C)
val DustyPinkDeep = Color(0xFFE89BB1)

// Film simulation accent colors (chips) — muted dark versions
val FilmVelvia = Color(0xFF5E3A33)
val FilmClassicChrome = Color(0xFF57492E)
val FilmClassicNeg = Color(0xFF553F30)
val FilmMono = Color(0xFF3A3835)
val FilmEterna = Color(0xFF33424E)
val FilmProvia = Color(0xFF3C4A38)

// Functional
val Success = Color(0xFF8FC49C)
val Danger = Color(0xFFE58A8A)
val OnPastel = Ink // text drawn on pastel chips is always warm off-white
