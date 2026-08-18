package com.loadpredictor.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.MintGlow
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * High-craft Permission Required screen matching Panel 2 of reference design:
 * - Glowing concentric mint progress ring hero graphic
 * - High-contrast bold typography
 * - Visual safeguard checklist cards with rich icons (Shield, Lock, Device Memory)
 * - Mint CTA button with ambient teal shadow
 */
@Composable
fun UsagePermissionRequiredCard(
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceLayer1
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing concentric ring graphic
                    GlowingConcentricRing(
                        modifier = Modifier.size(90.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Usage Access Required",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "To accurately forecast when your Philippine prepaid promo data will run out, Load Predictor needs system permission to measure device-level mobile data consumption.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMediumEmphasis,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Rich safeguard checklist with real icons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SafeguardItem(
                            icon = Icons.Outlined.Shield,
                            text = "Excludes WiFi traffic automatically"
                        )
                        SafeguardItem(
                            icon = Icons.Outlined.Lock,
                            text = "Zero background scraping or packet interception"
                        )
                        SafeguardItem(
                            icon = Icons.Outlined.Memory,
                            text = "100% on-device calculations without cloud sync"
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Mint CTA Button with subtle ambient glow shadow
                    Button(
                        onClick = onGrantPermissionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = MintGlow,
                                ambientColor = MintGlow
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintPrimary,
                            contentColor = MintOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MintOnPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grant Usage Access in Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeguardItem(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceRecessed,
        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = SurfaceLayer2,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MintPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 100% minSdk 26 compatible Canvas glowing ring graphic.
 */
@Composable
private fun GlowingConcentricRing(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f - 6.dp.toPx()
        val innerRadius = outerRadius * 0.7f

        // Outer faint background track
        drawCircle(
            color = SurfaceLayer2,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )

        // Outer glowing sweep arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    MintPrimary.copy(alpha = 0.1f),
                    MintPrimary.copy(alpha = 0.5f),
                    MintPrimary
                ),
                center = center
            ),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner glowing soft radial fill
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MintPrimary.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = innerRadius
            ),
            radius = innerRadius,
            center = center
        )

        // Center dot
        drawCircle(
            color = MintPrimary,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
