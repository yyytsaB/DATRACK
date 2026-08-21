package com.loadpredictor.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.loadpredictor.domain.model.BurnForecast
import com.loadpredictor.domain.model.BurnPace
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import com.loadpredictor.util.DataFormatter
import java.util.concurrent.TimeUnit

/**
 * 3-chip status row displayed directly below the radial progress ring.
 */
@Composable
fun DashboardStatChips(
    forecast: BurnForecast,
    dailyAvgBytes: Long, // 0 if calibrating or not enough data
    modifier: Modifier = Modifier
) {
    val promo = forecast.promo
    val now = System.currentTimeMillis()

    // 1. Daily Average Chip
    val dailyAvgText = if (forecast.pace == BurnPace.INSUFFICIENT_DATA || dailyAvgBytes <= 0) {
        "—"
    } else {
        DataFormatter.formatBytes(dailyAvgBytes)
    }

    // 2. Days Left / Validity Chip
    val (validityValue, validityLabel) = if (promo.isNoExpiry || promo.expirationTimestamp == null) {
        Pair("No Expiry", "Validity")
    } else {
        val daysLeft = ((promo.expirationTimestamp - now).coerceAtLeast(0L) / TimeUnit.DAYS.toMillis(1)).toInt()
        val daysText = if (daysLeft == 0) "< 1 day" else "$daysLeft ${if (daysLeft == 1) "day" else "days"}"
        Pair(daysText, "Days left")
    }

    // 3. At This Pace / Projection Chip
    val (paceValue, paceLabel) = when (forecast.pace) {
        BurnPace.INSUFFICIENT_DATA -> Pair("Calibrating", "At this pace")
        BurnPace.DEPLETED -> Pair("0 days", "At this pace")
        else -> {
            if (forecast.estimatedDepletionTimestamp != null) {
                val msUntilDepletion = (forecast.estimatedDepletionTimestamp - now).coerceAtLeast(0L)
                val daysUntilDepletion = (msUntilDepletion / TimeUnit.DAYS.toMillis(1)).toInt()
                val paceText = when {
                    daysUntilDepletion > 60 -> "> 60 days"
                    daysUntilDepletion > 0 -> "$daysUntilDepletion ${if (daysUntilDepletion == 1) "day" else "days"}"
                    else -> "< 1 day"
                }
                Pair(paceText, "At this pace")
            } else {
                Pair("Steady", "At this pace")
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            value = dailyAvgText,
            label = "Daily avg",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            value = validityValue,
            label = validityLabel,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            value = paceValue,
            label = paceLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF141824)),
        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF222B3D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(top = 2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = androidx.compose.ui.graphics.Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
