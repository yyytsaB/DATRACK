package com.loadpredictor.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

// ---------------------------------------------------------------------------
// Compose Material 3 Theme Configuration
// ---------------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = PaceConservative,
    onSecondary = Color(0xFF003548),
    secondaryContainer = PaceConservativeContainer,
    onSecondaryContainer = PaceConservativeText,
    tertiary = PaceOnTrack,
    onTertiary = Color(0xFF003820),
    tertiaryContainer = PaceOnTrackContainer,
    onTertiaryContainer = PaceOnTrackText,
    background = DarkBackground,
    onBackground = TextHighEmphasis,
    surface = DarkSurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMediumEmphasis,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = PaceCritical,
    onError = Color.White,
    errorContainer = PaceCriticalContainer,
    onErrorContainer = PaceCriticalText
)

// App is designed with an immersive dark aesthetic as default
private val LightColorScheme = DarkColorScheme

@Composable
fun LoadPredictorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Retain cohesive high-contrast dark aesthetic
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoadPredictorTypography,
        content = content
    )
}

// ---------------------------------------------------------------------------
// Glance Widget Theme Tokens (Synchronized with App Theme)
// ---------------------------------------------------------------------------
val LoadPredictorGlanceColorScheme = ColorProviders(
    light = DarkColorScheme,
    dark = DarkColorScheme
)
