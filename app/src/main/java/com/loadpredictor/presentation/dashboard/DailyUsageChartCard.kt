package com.loadpredictor.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.UsageBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Jetpack Compose Canvas / Column chart displaying day-by-day mobile data consumption
 * within the active promo period.
 *
 * Implements smooth animated bars, peak indicator, interactive daily inspect, and average daily line.
 * Has zero third-party chart dependencies.
 */
@Composable
fun DailyUsageChartCard(
    buckets: List<UsageBucket>,
    modifier: Modifier = Modifier
) {
    var selectedBucketIndex by remember(buckets) {
        mutableStateOf(if (buckets.isNotEmpty()) buckets.size - 1 else -1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title + Selected Day Header
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Device-measured mobile usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (selectedBucketIndex in buckets.indices) {
                    val selected = buckets[selectedBucketIndex]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${formatDayLabel(selected.startTimestamp)}: ${com.loadpredictor.util.DataFormatter.formatBytes(selected.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (buckets.isEmpty() || buckets.all { it.totalBytes == 0L }) {
                // Empty / initial tracking state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊 Initializing daily history...\nUsage will be plotted as device traffic occurs.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                val maxBytes = buckets.maxOf { it.totalBytes }.coerceAtLeast(1024L * 1024L) // at least 1 MB
                val averageBytes = buckets.map { it.totalBytes }.average()

                // Chart Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    // Average Line Canvas
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val outlineColor = MaterialTheme.colorScheme.outlineVariant
                    Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                        val avgY = size.height * (1f - (averageBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f))
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Bar Columns
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val recentBuckets = buckets.takeLast(14) // Display last 14 days max for clean width
                        recentBuckets.forEachIndexed { index, bucket ->
                            val isSelected = (selectedBucketIndex == buckets.indexOf(bucket))
                            val targetFraction = (bucket.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)
                            val animatedHeightFraction by animateFloatAsState(
                                targetValue = targetFraction,
                                animationSpec = tween(durationMillis = 600),
                                label = "barHeight"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedBucketIndex = buckets.indexOf(bucket) },
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
                                            .fillMaxWidth(0.65f)
                                            .fillMaxHeight(animatedHeightFraction)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = formatShortDay(bucket.startTimestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Explanatory note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Daily bars reflect actual on-device data measured by Android since promo registration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

