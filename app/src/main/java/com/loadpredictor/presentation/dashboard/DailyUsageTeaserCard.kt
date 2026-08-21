package com.loadpredictor.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.presentation.theme.MintPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily usage bar strip teaser matching preview.webp.
 */
@Composable
fun DailyUsageTeaserCard(
    buckets: List<UsageBucket>,
    onViewHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateRangeText = if (buckets.isNotEmpty()) {
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        val firstDate = sdf.format(Date(buckets.first().startTimestamp))
        val lastDate = sdf.format(Date(buckets.last().startTimestamp))
        "$firstDate – $lastDate"
    } else {
        "Recent 14 Days"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = dateRangeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewHistoryClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141824)),
            border = BorderStroke(1.dp, Color(0xFF222B3D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Mini Bar Strip
                val maxBytes = (buckets.maxOfOrNull { it.totalBytes } ?: 1L).coerceAtLeast(1024L * 1024L).toFloat()
                val visibleBuckets = buckets.takeLast(14)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    if (visibleBuckets.isEmpty()) return@Canvas

                    val count = visibleBuckets.size
                    val spacingPx = 6.dp.toPx()
                    val totalSpacing = spacingPx * (count - 1).coerceAtLeast(0)
                    val barWidth = ((size.width - totalSpacing) / count).coerceIn(6.dp.toPx(), 20.dp.toPx())
                    val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                    visibleBuckets.forEachIndexed { index, bucket ->
                        val x = index * (barWidth + spacingPx)
                        val ratio = (bucket.totalBytes.toFloat() / maxBytes).coerceIn(0.12f, 1f)
                        val barHeight = size.height * ratio
                        val y = size.height - barHeight

                        val isToday = index == visibleBuckets.lastIndex
                        val barColor = if (isToday) {
                            MintPrimary
                        } else {
                            Color(0xFF233544)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }
    }
}

