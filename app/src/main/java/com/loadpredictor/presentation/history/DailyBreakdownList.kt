package com.loadpredictor.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.MintPrimary
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
 * Grouped breakdown list displaying day-by-day consumption in reverse chronological order.
 */
@Composable
fun DailyBreakdownList(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
                Text(
                    text = "${buckets.size} ${if (buckets.size == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumEmphasis
                )
            }

            if (buckets.isEmpty()) {
                Text(
                    text = "No usage records available for this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumEmphasis,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val maxBytes = buckets.maxOf { it.totalBytes }.coerceAtLeast(1024L * 1024L)
                val reversedBuckets = buckets.reversed() // Reverse chronological order (newest first)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    reversedBuckets.forEach { bucket ->
                        val isSelected = (bucket.startTimestamp == (selectedBucketTimestamp ?: buckets.lastOrNull()?.startTimestamp))
                        DailyBreakdownRow(
                            bucket = bucket,
                            maxBytes = maxBytes,
                            isSelected = isSelected,
                            onClick = { onSelectBucketTimestamp(bucket.startTimestamp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBreakdownRow(
    bucket: UsageBucket,
    maxBytes: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fraction = (bucket.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SurfaceRecessed else SurfaceLayer1,
        border = if (isSelected) BorderStroke(1.dp, MintPrimary.copy(alpha = 0.5f)) else BorderStroke(1.dp, Color(0xFF222B3D)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Date Badge
            Column(modifier = Modifier.width(80.dp)) {
                Text(
                    text = formatDateMonthDay(bucket.startTimestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MintPrimary else TextHighEmphasis
                )
                Text(
                    text = formatDayOfWeek(bucket.startTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLowEmphasis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Mini Horizontal Proportion Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF252E3E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) MintPrimary else Color(0xFF4B5563))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Formatted Bytes Amount
            Text(
                text = DataFormatter.formatBytes(bucket.totalBytes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MintPrimary else TextHighEmphasis
            )
        }
    }
}

private fun formatDateMonthDay(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.US)
    return sdf.format(Date(timestamp))
}

private fun formatDayOfWeek(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE", Locale.US)
    return sdf.format(Date(timestamp))
}
