package com.alpefe.fujiptp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DAD2),
    onPrimaryContainer = TerracottaDark,
    secondary = Olive,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3CF),
    onSecondaryContainer = Color(0xFF2E3A22),
    tertiary = Color(0xFF7A5C3E),
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SlotCard,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFB8AE9D),
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF4A1708),
    primaryContainer = Color(0xFF6E2F1E),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Sage,
    onSecondary = Color(0xFF27331C),
    secondaryContainer = Color(0xFF3E4A31),
    onSecondaryContainer = Color(0xFFDDE6CE),
    tertiary = Color(0xFFE0C29F),
    background = Espresso,
    onBackground = NightInk,
    surface = EspressoElevated,
    onSurface = NightInk,
    surfaceVariant = SlotCardDark,
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF55493D),
    error = Color(0xFFE07A66),
)

@Composable
fun FujiRecipesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
