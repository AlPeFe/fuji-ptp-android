package com.alpefe.fujiptp.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Design tokens — "playful modern minimalism", pink pastel + sage green.
// Warm off-white (broken white) background, dusty pink as the primary
// accent, and a soft sage/mint green as the complementary tone.
// ---------------------------------------------------------------------------

// Backgrounds / neutrals
val Canvas = Color(0xFFF7F4F0)        // warm broken-white background
val Surface = Color(0xFFFFFCF8)       // cards (near-white)
val SurfaceSoft = Color(0xFFF1ECE6)   // soft neutral wells
val Ink = Color(0xFF3D3834)           // primary text (warm dark gray)
val InkSoft = Color(0xFF8C857B)       // secondary text
val InkFaint = Color(0xFFB8B1A6)      // tertiary / hints
val Hairline = Color(0xFFEDE7E0)      // subtle separators

// Pastel accents — pink family (primary) + sage green (complementary)
val Lavender = Color(0xFFF3E8F5)      // soft lilac-pink
val LavenderDeep = Color(0xFFB48AC9)
val SoftBlue = Color(0xFFE8EEF7)      // cool tint for camera card
val SoftBlueDeep = Color(0xFF8FA8C9)
val PastelGreen = Color(0xFFE4F0E5)   // sage / mint (complementary)
val PastelGreenDeep = Color(0xFF7FAF8B)
val Peach = Color(0xFFF9E0E4)         // dusty pink (primary accent)
val PeachDeep = Color(0xFFD982A0)     // deeper pink
val SoftYellow = Color(0xFFF7EDD9)    // warm cream (WB section)
val SoftYellowDeep = Color(0xFFC9A24B)
val DustyPink = Color(0xFFF7E3E8)     // pink tint for highlights
val DustyPinkDeep = Color(0xFFD98BA0)

// Film simulation accent colors (chips) — soft pastel versions
val FilmVelvia = Color(0xFFFFD9D0)
val FilmClassicChrome = Color(0xFFF7E8C9)
val FilmClassicNeg = Color(0xFFF4DDC8)
val FilmMono = Color(0xFFE8E5E1)
val FilmEterna = Color(0xFFD8E4EE)
val FilmProvia = Color(0xFFE2EFDC)

// Functional
val Success = Color(0xFF7FAF8B)
val Danger = Color(0xFFE58A8A)
val OnPastel = Ink // text drawn on pastel chips is always warm dark
