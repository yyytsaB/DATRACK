package com.loadpredictor.presentation.dashboard
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter

@Composable
fun DashboardStatChips(forecast: BurnForecast, dailyAvgBytes: Long, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val rows = listOf(
        "Daily average" to if (dailyAvgBytes > 0 && forecast.pace != BurnPace.INSUFFICIENT_DATA) DataFormatter.formatBytes(dailyAvgBytes) else "Learning",
        "Estimated runway" to ForecastCopy.runway(forecast.pace, forecast.estimatedDepletionTimestamp, now),
        "Estimate based on" to forecast.modelSource
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextMediumEmphasis)
                Text(value, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = TextHighEmphasis)
            }
            HorizontalDivider(color = DarkOutlineVariant)
        }
    }
}
