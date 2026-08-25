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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Optional Inspection Tooltip if user tapped a bar
            val selectedBucket = if (selectedBucketTimestamp != null) {
                buckets.find { it.startTimestamp == selectedBucketTimestamp } ?: buckets.lastOrNull()
            } else {
                null
            }

            if (selectedBucket != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleBadgeBackground,
                        border = BorderStroke(1.dp, Color(0xFF3B2D6B))
                    ) {
                        Text(
                            text = "${formatDayLabel(selectedBucket.startTimestamp)}: ${DataFormatter.formatBytes(selectedBucket.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PurpleBadgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (buckets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No usage records available to plot.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextMediumEmphasis
                    )
                }
            } else {
                val maxBytes = buckets.maxOf { it.totalBytes }.coerceAtLeast(1024L * 1024L) // at least 1 MB
                val averageBytes = buckets.map { it.totalBytes }.average()

                // Direct Chart Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    // Average Line Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        val avgY = size.height * (1f - (averageBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f))
                        drawLine(
                            color = Color(0xFF263244),
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
                        // Display up to 14 bars max
                        val displayBuckets = if (buckets.size > 14) buckets.takeLast(14) else buckets
                        displayBuckets.forEachIndexed { index, bucket ->
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
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (isSelected) MintPrimary
                                                else Color(0xFF14B8A6).copy(alpha = 0.75f)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Show date label for every ~3 bars or endpoints
                                val showLabel = displayBuckets.size <= 7 || index % 3 == 0 || index == displayBuckets.lastIndex
                                Text(
                                    text = if (showLabel) formatShortDay(bucket.startTimestamp) else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MintPrimary else TextLowEmphasis,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
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
                    text = "Dashed line shows the average daily burn rate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLowEmphasis
                )
            }
        }
    }
}

private fun formatDayLabel(timestamp: Long): String {
    return DataFormatter.formatDayLabel(timestamp)
}

private fun formatShortDay(timestamp: Long): String {
    return DataFormatter.formatShortDay(timestamp)
}
