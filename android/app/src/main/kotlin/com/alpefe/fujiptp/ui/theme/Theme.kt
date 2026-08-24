package com.alpefe.fujiptp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark, warm, pastel design. The app renders in dark mode with muted pastel
// accents over a warm espresso background.
private val DarkColors = darkColorScheme(
    primary = PeachDeep,
    onPrimary = Color(0xFF3A1D0F),
    primaryContainer = Peach,
    onPrimaryContainer = Ink,
    secondary = LavenderDeep,
    onSecondary = Color(0xFF241B3D),
    secondaryContainer = Lavender,
    onSecondaryContainer = Ink,
    tertiary = SoftBlueDeep,
    tertiaryContainer = SoftBlue,
    onTertiaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = Color(0xFF3A1414),
    errorContainer = DustyPink,
    onErrorContainer = Ink,
)

@Composable
fun FujiRecipesTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Canvas.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
