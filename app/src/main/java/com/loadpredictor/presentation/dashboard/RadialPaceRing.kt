package com.loadpredictor.presentation.dashboard

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.theme.getDataProgressColor

/**
 * Animated Canvas-based radial progress ring reflecting remaining data and progression colors.
 * Features a hardware-accelerated Gaussian blurred full-arc ambient glow and rich center shine matching preview.webp.
 */
@Composable
fun RadialPaceRing(
    remainingRatio: Float, // 0.0f to 1.0f (remaining / total)
    remainingFormatted: String, // e.g. "2.8" or "17.5 GB"
    totalAllowanceFormatted: String, // e.g. "8 GB" or "24 GB"
    pace: BurnPace,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 15.dp
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

    val ringColor = getDataProgressColor(remainingRatio)
    val trackColor = Color(0xFF1E2532)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - (strokePx * 2.4f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = diameter / 2f
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(diameter, diameter)
            val rectF = RectF(topLeft.x, topLeft.y, topLeft.x + arcSize.width, topLeft.y + arcSize.height)

            // 1. Center Ambient Radial Shine (Rich luminous aura in the middle of the circle)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.32f),
                        ringColor.copy(alpha = 0.16f),
                        ringColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.05f,
                center = center
            )

            // 2. Draw Background Track (full 360 circle)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 3. Draw Progress Arc with Smooth Gaussian Blurred Glow along the whole arc
            if (animatedProgress > 0.001f) {
                val sweepAngle = 360f * animatedProgress

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas

                    // Outer wide Gaussian blur glow
                    val wideBlurPaint = AndroidPaint().apply {
                        isAntiAlias = true
                        style = AndroidPaint.Style.STROKE
                        this.strokeWidth = strokePx + 28.dp.toPx()
                        strokeCap = AndroidPaint.Cap.ROUND
                        color = ringColor.copy(alpha = 0.38f).toArgb()
                        maskFilter = BlurMaskFilter(22.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    nativeCanvas.drawArc(rectF, -90f, sweepAngle, false, wideBlurPaint)

                    // Inner intense Gaussian blur glow
                    val intenseBlurPaint = AndroidPaint().apply {
                        isAntiAlias = true
                        style = AndroidPaint.Style.STROKE
                        this.strokeWidth = strokePx + 14.dp.toPx()
                        strokeCap = AndroidPaint.Cap.ROUND
                        color = ringColor.copy(alpha = 0.68f).toArgb()
                        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    nativeCanvas.drawArc(rectF, -90f, sweepAngle, false, intenseBlurPaint)
                }

                // Core Crisp Progress Arc on top
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
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

            Text(
                text = numberPart,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor,
                letterSpacing = (-1).sp,
                lineHeight = 48.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "of $totalAllowanceFormatted",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E9AA8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "remaining",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF5A6678)
            )
        }
    }
}
