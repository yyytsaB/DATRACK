package com.loadpredictor.presentation.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.PurpleBadgeBackground
import com.loadpredictor.presentation.theme.PurpleBadgeText
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interactive Canvas / Column chart displaying day-by-day mobile data consumption.
 *
 * Supports tap-to-inspect bar selection with tooltips, smooth bar transitions,
 * and a dashed average baseline.
 */
@Composable
fun InteractiveUsageChart(
    buckets: List<UsageBucket>,
    selectedBucketTimestamp: Long?,
    onSelectBucketTimestamp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Selected Day Tooltip Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Consumption",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )
                    Text(
                        text = "Tap a bar to inspect daily usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumEmphasis
                    )
                }

                val selectedBucket = if (selectedBucketTimestamp != null) {
                    buckets.find { it.startTimestamp == selectedBucketTimestamp } ?: buckets.lastOrNull()
                } else {
                    buckets.lastOrNull()
                }

                if (selectedBucket != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleBadgeBackground,
                        border = BorderStroke(1.dp, Color(0xFF3B2D6B))
                    ) {
                        Text(
                            text = "${formatDayLabel(selectedBucket.startTimestamp)}: ${DataFormatter.formatBytes(selectedBucket.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurpleBadgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (buckets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(SurfaceRecessed, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Initializing daily history...\nUsage will be plotted as device traffic occurs.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextMediumEmphasis,
                        lineHeight = 20.sp
                    )
                }
            } else {
                val maxBytes = buckets.maxOf { it.totalBytes }.coerceAtLeast(1024L * 1024L) // at least 1 MB
                val averageBytes = buckets.map { it.totalBytes }.average()

                // Recessed Chart Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(SurfaceRecessed, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    // Average Line Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        val avgY = size.height * (1f - (averageBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f))
                        drawLine(
                            color = DarkOutline,
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Bar Columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Display up to 14 bars max in the viewport for clean readability
                        val displayBuckets = if (buckets.size > 14) buckets.takeLast(14) else buckets
                        displayBuckets.forEach { bucket ->
                            val isSelected = (bucket.startTimestamp == (selectedBucketTimestamp ?: buckets.lastOrNull()?.startTimestamp))
                            val targetFraction = (bucket.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)
                            val animatedHeightFraction by animateFloatAsState(
                                targetValue = targetFraction,
                                animationSpec = tween(durationMillis = 500),
                                label = "barHeight"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onSelectBucketTimestamp(bucket.startTimestamp) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (displayBuckets.size <= 7) 0.55f else 0.70f)
                                            .fillMaxHeight(animatedHeightFraction)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (isSelected) MintPrimary
                                                else Color(0xFF2D3442)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = formatShortDay(bucket.startTimestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MintPrimary else TextMediumEmphasis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Explanatory baseline note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextLowEmphasis,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dashed line shows the average daily burn rate across this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLowEmphasis
                )
            }
        }
    }
}

private fun formatDayLabel(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d", Locale.US)
    return sdf.format(Date(timestamp))
}

private fun formatShortDay(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE", Locale.US)
    return sdf.format(Date(timestamp)).take(2)
}
