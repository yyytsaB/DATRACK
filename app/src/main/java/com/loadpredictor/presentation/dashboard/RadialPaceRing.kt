package com.loadpredictor.presentation.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.PaceCalibrating
import com.loadpredictor.presentation.theme.PaceConservative
import com.loadpredictor.presentation.theme.PaceCritical
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Canvas-based radial progress ring reflecting remaining data and semantic pace colors.
 * MinSdk 26 compatible.
 */
@Composable
fun RadialPaceRing(
    remainingRatio: Float, // 0.0f to 1.0f (remaining / total)
    remainingFormatted: String, // e.g. "17.5"
    totalAllowanceFormatted: String, // e.g. "24.0 GB"
    pace: BurnPace,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 14.dp
) {
    val targetProgress = when (pace) {
        BurnPace.DEPLETED -> 0f
        else -> remainingRatio.coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "RadialPaceProgress"
    )

    val ringColor = when (pace) {
        BurnPace.BURNING_FAST -> Color(0xFFFF7043) // Glowing Orange/Coral
        BurnPace.ON_TRACK -> MintPrimary           // Electric Mint
        BurnPace.CONSERVATIVE -> PaceConservative  // Sky Blue
        BurnPace.INSUFFICIENT_DATA -> PaceCalibrating // Amber Gold
        BurnPace.DEPLETED -> Color(0xFF7F1D1D)     // Muted Crimson
    }

    val trackColor = SurfaceLayer2

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = diameter / 2f

            // 1. Draw Background Track (full 360 circle)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 2. Draw Progress Arc (starting at top = -90 degrees, clockwise)
            if (animatedProgress > 0.001f) {
                val sweepAngle = 360f * animatedProgress
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // 3. Draw Ambient Glow & Dot at the leading endpoint
                val endAngleRad = Math.toRadians((-90.0 + sweepAngle)).toFloat()
                val dotX = center.x + radius * cos(endAngleRad)
                val dotY = center.y + radius * sin(endAngleRad)
                val glowRadius = strokePx * 1.2f

                // Outer ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.85f),
                            ringColor.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(dotX, dotY),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(dotX, dotY)
                )

                // Inner bright dot
                drawCircle(
                    color = Color.White,
                    radius = strokePx * 0.35f,
                    center = Offset(dotX, dotY)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            val parts = remainingFormatted.trim().split(" ")
            val numberPart = parts.getOrNull(0) ?: remainingFormatted
            val unitPart = parts.getOrNull(1) ?: "GB"

            Text(
                text = numberPart,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = TextHighEmphasis,
                letterSpacing = (-1).sp,
                lineHeight = 46.sp
            )
            Text(
                text = "of $totalAllowanceFormatted",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMediumEmphasis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "remaining",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }
    }
}
