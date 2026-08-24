package com.alpefe.fujiptp.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Design tokens — "playful modern minimalism"
// Neutral warm background + pastel accent palette (one hue per section).
// ---------------------------------------------------------------------------

// Backgrounds / neutrals
val Canvas = Color(0xFFFAF8F5)        // warm off-white app background
val Surface = Color(0xFFFFFFFF)       // cards
val SurfaceSoft = Color(0xFFF4F1EC)   // soft neutral wells
val Ink = Color(0xFF3A3630)           // primary text (warm dark gray)
val InkSoft = Color(0xFF8A8378)       // secondary text
val InkFaint = Color(0xFFB5AFA3)      // tertiary / hints
val Hairline = Color(0xFFEFEBE4)      // subtle separators

// Pastel accents — one per section
val Lavender = Color(0xFFEDE6FB)      // active slots
val LavenderDeep = Color(0xFF8B7BD8)
val SoftBlue = Color(0xFFDFEEFB)      // camera / connection
val SoftBlueDeep = Color(0xFF6E9EDB)
val PastelGreen = Color(0xFFE2F3E4)   // success / saved
val PastelGreenDeep = Color(0xFF6FAF7E)
val Peach = Color(0xFFFFE7D6)         // new recipe / FAB
val PeachDeep = Color(0xFFE89B6E)
val SoftYellow = Color(0xFFFBF0D3)    // library accents
val SoftYellowDeep = Color(0xFFC9A24B)
val DustyPink = Color(0xFFF9E3E8)     // editor highlights
val DustyPinkDeep = Color(0xFFD4839A)

// Film simulation accent colors (chips) — soft pastel versions
val FilmVelvia = Color(0xFFFFD9D0)
val FilmClassicChrome = Color(0xFFF7E8C9)
val FilmClassicNeg = Color(0xFFF4DDC8)
val FilmMono = Color(0xFFE4E2DE)
val FilmEterna = Color(0xFFD8E4EE)
val FilmProvia = Color(0xFFE2EFDC)

// Functional
val Success = Color(0xFF6FAF7E)
val Danger = Color(0xFFE58A8A)
val OnPastel = Ink // text drawn on pastel chips is always warm dark
