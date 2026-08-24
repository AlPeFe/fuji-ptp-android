package com.alpefe.fujiptp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light, warm, pastel design. The app is intentionally light-only:
// pastel minimalism reads best on warm white.
private val LightColors = lightColorScheme(
    primary = PeachDeep,
    onPrimary = Color.White,
    primaryContainer = Peach,
    onPrimaryContainer = Ink,
    secondary = LavenderDeep,
    onSecondary = Color.White,
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
    onError = Color.White,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
