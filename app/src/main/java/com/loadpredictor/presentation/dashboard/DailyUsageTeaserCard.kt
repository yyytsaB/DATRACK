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
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceLayer2
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Compact mini daily bar strip teaser on Dashboard with tap-through to History tab.
 */
@Composable
fun DailyUsageTeaserCard(
    buckets: List<UsageBucket>,
    onViewHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewHistoryClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
                Text(
                    text = "View History →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
            }

            // Mini Bar Strip
            val maxBytes = (buckets.maxOfOrNull { it.totalBytes } ?: 1L).coerceAtLeast(1024L * 1024L).toFloat()
            val visibleBuckets = buckets.takeLast(14)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                if (visibleBuckets.isEmpty()) return@Canvas

                val count = visibleBuckets.size
                val spacingPx = 6.dp.toPx()
                val totalSpacing = spacingPx * (count - 1).coerceAtLeast(0)
                val barWidth = ((size.width - totalSpacing) / count).coerceIn(6.dp.toPx(), 20.dp.toPx())
                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                visibleBuckets.forEachIndexed { index, bucket ->
                    val x = index * (barWidth + spacingPx)
                    val ratio = (bucket.totalBytes.toFloat() / maxBytes).coerceIn(0.08f, 1f)
                    val barHeight = size.height * ratio
                    val y = size.height - barHeight

                    val isToday = index == visibleBuckets.lastIndex
                    val barColor = if (isToday) MintPrimary else SurfaceLayer2

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
