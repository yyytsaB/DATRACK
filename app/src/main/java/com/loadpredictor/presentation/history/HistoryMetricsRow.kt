package com.loadpredictor.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter

/**
 * Plain typography inline stats row (Total Burnt, Daily Avg, Peak Day) without any card frames/boxes.
 * Styled matching the reference design ("Daily avg 505 MB").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryMetricsRow(
    totalBurntBytes: Long,
    dailyAverageBytes: Long,
    peakDayBytes: Long,
    peakDayTimestamp: Long?,
    modifier: Modifier = Modifier
) {
    val totalBurntText = DataFormatter.formatBytes(totalBurntBytes)
    val dailyAvgText = if (totalBurntBytes <= 0 || dailyAverageBytes <= 0) "—" else DataFormatter.formatBytes(dailyAverageBytes)
    val peakDayText = if (peakDayBytes <= 0) "—" else DataFormatter.formatBytes(peakDayBytes)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Daily Avg (Primary highlight in Mint)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Daily avg  ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextMediumEmphasis
            )
            Text(
                text = dailyAvgText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MintPrimary
            )
        }

        // Total Burnt
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Total  ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextMediumEmphasis
            )
            Text(
                text = totalBurntText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )
        }

        // Peak Day
        if (peakDayBytes > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Peak  ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextMediumEmphasis
                )
                Text(
                    text = peakDayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
            }
        }
    }
}
