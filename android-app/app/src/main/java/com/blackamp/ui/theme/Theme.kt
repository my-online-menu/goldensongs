package com.blackamp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// ---- BlackAmp palette (matches the web player) ----
val LcdGreen      = Color(0xFF00D563)
val LcdGreenDim   = Color(0xFF0A7A3A)
val Accent        = Color(0xFF00FF7B)
val PanelTop      = Color(0xFF141414)
val PanelBottom   = Color(0xFF0C0C0C)
val BevelHi       = Color(0xFF383838)
val TitleGradA    = Color(0xFF303030)
val TitleGradB    = Color(0xFF0E0E0E)
val ScreenBlack   = Color(0xFF000000)
val SurfaceDark   = Color(0xFF121212)
val SurfaceCard   = Color(0xFF1A1A1A)
val TextDim       = Color(0xFF8A8A8A)

val PanelBrush = Brush.verticalGradient(listOf(PanelTop, PanelBottom))
val TitleBrush = Brush.verticalGradient(listOf(TitleGradA, TitleGradB))

private val BlackAmpColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = LcdGreen,
    onSecondary = Color.Black,
    background = Color(0xFF070707),
    onBackground = Color(0xFFE6E6E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextDim,
    error = Color(0xFFFF5C5C)
)

/** Monospace for the LCD readouts, matching the web skin's Courier feel. */
val LcdTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
)

@Composable
fun BlackAmpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackAmpColors,
        content = content
    )
}
