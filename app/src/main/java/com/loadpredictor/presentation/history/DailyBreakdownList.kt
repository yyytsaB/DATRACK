package com.loadpredictor.presentation.history

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Frameless daily consumption list displaying day-by-day records in reverse chronological order.
 * No outer card container, no per-row borders. Separated by subtle hairline dividers.
 */
@Composable
fun DailyBreakdownList(
    buckets: List<UsageBucket>,
    selectedBucketTimestamp: Long?,
    onSelectBucketTimestamp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Plain text section header (no card/frame)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )
            Text(
                text = "${buckets.size} ${if (buckets.size == 1) "day" else "days"}",
                fontSize = 12.sp,
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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                reversedBuckets.forEachIndexed { index, bucket ->
                    val isSelected = (bucket.startTimestamp == (selectedBucketTimestamp ?: buckets.lastOrNull()?.startTimestamp))
                    DailyBreakdownRow(
                        bucket = bucket,
                        maxBytes = maxBytes,
                        isSelected = isSelected,
                        onClick = { onSelectBucketTimestamp(bucket.startTimestamp) }
                    )

                    if (index < reversedBuckets.lastIndex) {
                        HorizontalDivider(
                            color = Color(0xFF181F2C),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
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
    val fraction = (bucket.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.04f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Date (Left)
        Column(modifier = Modifier.width(68.dp)) {
            Text(
                text = formatDateMonthDay(bucket.startTimestamp),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MintPrimary else TextHighEmphasis
            )
            Text(
                text = formatDayOfWeek(bucket.startTimestamp),
                fontSize = 11.sp,
                color = TextLowEmphasis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Mini Horizontal Proportion Bar (Middle)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E2636))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) MintPrimary else Color(0xFF2DD4BF))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Formatted Bytes Amount (Right)
        Text(
            text = DataFormatter.formatBytes(bucket.totalBytes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MintPrimary else TextHighEmphasis
        )
    }
}

private fun formatDateMonthDay(timestamp: Long): String {
    return DataFormatter.formatDate(timestamp)
}

private fun formatDayOfWeek(timestamp: Long): String {
    return DataFormatter.formatDayOfWeek(timestamp)
}
