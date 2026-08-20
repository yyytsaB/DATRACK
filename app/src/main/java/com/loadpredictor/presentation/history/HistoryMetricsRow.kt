package com.loadpredictor.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 3-card metrics summary row displaying Total Burnt, Daily Average, and Peak Day for the selected range.
 */
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
    val peakDaySublabel = if (peakDayTimestamp != null && peakDayBytes > 0) {
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        "Peak (${sdf.format(Date(peakDayTimestamp))})"
    } else {
        "Peak Day"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HistoryMetricCard(
            value = totalBurntText,
            label = "Total Burnt",
            modifier = Modifier.weight(1f)
        )
        HistoryMetricCard(
            value = dailyAvgText,
            label = "Daily Avg",
            modifier = Modifier.weight(1f)
        )
        HistoryMetricCard(
            value = peakDayText,
            label = peakDaySublabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoryMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLayer1),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMediumEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
