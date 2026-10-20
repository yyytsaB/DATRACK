package com.loadpredictor.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Core Surface & Elevation Hierarchy (Near-Black -> Layer 1 -> Layer 2 -> Recessed)
// ---------------------------------------------------------------------------
val DarkBackground = Color(0xFF0D1117)       // Deepest near-black canvas
val SurfaceBase = DarkBackground

val SurfaceLayer1 = Color(0xFF171B22)        // Main cards & dialog surface
val DarkSurfaceVariant = SurfaceLayer1

val SurfaceLayer2 = Color(0xFF1E2536)        // Elevated tiles, input containers, preset chips
val DarkSurfaceHighlight = SurfaceLayer2

val SurfaceRecessed = Color(0xFF11161F)      // Recessed advisory boxes & chart stage
val DarkSurface = SurfaceRecessed

val DarkOutline = Color(0xFF2C3340)          // Structural card & element border
val BorderHighlight = Color(0xFF2C3340)      // Subtle top highlight border
val DarkOutlineVariant = Color(0xFF232936)   // Ultra subtle separators

// ---------------------------------------------------------------------------
// Typography High & Low Emphasis
// ---------------------------------------------------------------------------
val TextHighEmphasis = Color(0xFFFFFFFF)     // Pure crisp white for headers & hero numbers
val TextMediumEmphasis = Color(0xFF9CA6B5)   // Slate gray for labels & body (contrast > 5.9:1)
val TextLowEmphasis = Color(0xFF8994A5)      // Muted slate for captions & timestamps

// ---------------------------------------------------------------------------
// Light-Card Surface & Typography Tokens (For Widget Light Preview Cards)
// ---------------------------------------------------------------------------
val LightCardSurface = Color(0xFFFFFFFF)     // Crisp white surface for light widget cards
val LightCardOutline = Color(0xFFE5EBF2)     // Light gray border
val TextLightHigh = Color(0xFF10141E)        // Near-black text for light cards
val TextLightMedium = Color(0xFF6B7280)      // Slate gray secondary text for light cards
val TextLightLow = Color(0xFF9CA3AF)         // Muted gray captions for light cards
val LightRingTrack = Color(0xFFE5EBF2)       // Light ring background track
val LightLinearTrack = Color(0xFFE9EDF5)     // Light linear progress bar track
val DarkRingTrack = Color(0xFF222938)        // Dark ring background track
val DarkLinearTrack = Color(0xFF252D3D)      // Dark linear progress bar track

// ---------------------------------------------------------------------------
// Primary Brand & Interactive Accent (Teal / Electric Mint)
// ---------------------------------------------------------------------------
val MintPrimary = Color(0xFF3DD6C8)          // Electric Mint accent for buttons, FAB, focus rings, sliders
val MintOnPrimary = Color(0xFF0A1513)        // Deep dark text on mint (contrast > 14:1)
val MintPrimaryContainer = Color(0xFF163431) // Deep mint container for active chips/badges
val MintOnPrimaryContainer = Color(0xFF9FF0E8)// Bright mint text on deep container (contrast > 9:1)
val MintGlow = Color(0x6600F5D4)             // Subtle ambient mint glow

// ---------------------------------------------------------------------------
// Midnight Purple Alert & Context Palette
// ---------------------------------------------------------------------------
val SurfacePurpleTop = Color(0xFF241B44)     // Purple gradient top
val SurfacePurpleBottom = Color(0xFF16122C)  // Purple gradient bottom
val AlertCardBackground = SurfacePurpleTop   // Deep midnight purple
val AlertCardBorder = Color(0xFF3D2D6E)      // Soft violet border
val AlertCardText = Color(0xFFDDD6FE)        // Soft lavender body text
val AlertCardIcon = Color(0xFFA78BFA)        // Vibrant violet icon
val PurpleBadgeBackground = Color(0xFF2D2350)// Lavender/purple pill container
val PurpleBadgeText = Color(0xFFDDD6FE)      // Lavender pill text

val PurpleGradientBrush = Brush.verticalGradient(
    listOf(SurfacePurpleTop, SurfacePurpleBottom)
)

// ---------------------------------------------------------------------------
// Semantic Pace Status Colors (Distinct from Brand Buttons)
// ---------------------------------------------------------------------------
val PaceOnTrack = Color(0xFF4ADE80)          // Crisp Emerald Green
val PaceOnTrackContainer = Color(0xFF063D28) // Deep emerald background
val PaceOnTrackText = Color(0xFF6EE7B7)      // Light emerald text

val PaceConservative = Color(0xFF4ADE80)     // Sky Cyan Blue
val PaceConservativeContainer = Color(0xFF0C3B52)// Deep cyan background
val PaceConservativeText = Color(0xFF7DD3FC) // Light cyan text

val PaceCalibrating = Color(0xFF60A5FA)      // Warm Amber Gold
val PaceCalibratingContainer = Color(0xFF192C47)// Deep amber background
val PaceCalibratingText = Color(0xFF93C5FD)  // Light amber text

val PaceCritical = Color(0xFFF87171)         // Coral Red
val PaceCriticalContainer = Color(0xFF4A1515)// Deep red background
val PaceCriticalText = Color(0xFFFFB4B4)     // Light red text

// ---------------------------------------------------------------------------
// Dynamic Allowance Progression Color Mapping (Green -> Yellow -> Orange -> Red)
// ---------------------------------------------------------------------------
fun getDataProgressColor(remainingRatio: Float): Color {
    val ratio = remainingRatio.coerceIn(0f, 1f)
    return when {
        ratio > 0.50f -> MintPrimary          // Electric Mint / Green (#00F5D4)
        ratio > 0.25f -> Color(0xFFFACC15)   // Vibrant Yellow (#FACC15)
        ratio > 0.10f -> Color(0xFFFF9F43)   // Warning Orange (#FF9F43)
        else -> Color(0xFFFF4D4D)            // Critical Red (#FF4D4D)
    }
}

fun getDataProgressIntColor(remainingRatio: Float): Int {
    val ratio = remainingRatio.coerceIn(0f, 1f)
    return when {
        ratio > 0.50f -> android.graphics.Color.parseColor("#00F5D4")
        ratio > 0.25f -> android.graphics.Color.parseColor("#FACC15")
        ratio > 0.10f -> android.graphics.Color.parseColor("#FF9F43")
        else -> android.graphics.Color.parseColor("#FF4D4D")
    }
}

