package com.alpefe.fujiptp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Generous rounded geometry: cards 24dp, controls pill-shaped.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Spacing scale — generous, editorial.
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

// Corner radius tokens.
object Radius {
    val pill = 999.dp
    val card = 24.dp
    val cardLarge = 28.dp
    val chip = 14.dp
    val control = 16.dp
}
