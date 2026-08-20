package com.loadpredictor.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.presentation.theme.BorderHighlight
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Segmented horizontal pill selector allowing the user to filter usage history
 * across 7D, 30D, and Lifetime ranges.
 */
@Composable
fun HistoryTimeRangePills(
    selectedRange: HistoryTimeRange,
    onRangeSelected: (HistoryTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceRecessed),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryTimeRange.values().forEach { range ->
                val isSelected = (range == selectedRange)
                Card(
                    onClick = { onRangeSelected(range) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SurfaceLayer1 else SurfaceRecessed
                    ),
                    border = if (isSelected) BorderStroke(1.dp, MintPrimary.copy(alpha = 0.6f)) else null
                ) {
                    Text(
                        text = range.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MintPrimary else TextMediumEmphasis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}
