package com.loadpredictor.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.HistoryTimeRange
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Tab-style time range selector (7D, 30D, Lifetime) in a compact left-aligned horizontal row.
 * Active range indicated via mint typography and underline bar; inactive ranges are muted and tappable.
 */
@Composable
fun HistoryTimeRangePills(
    selectedRange: HistoryTimeRange,
    onRangeSelected: (HistoryTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryTimeRange.entries.forEach { range ->
            val isSelected = (range == selectedRange)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = range.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MintPrimary else TextMediumEmphasis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Subtle underline bar for the active tab only
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(if (range == HistoryTimeRange.LIFETIME) 48.dp else 24.dp)
                        .background(
                            color = if (isSelected) MintPrimary else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
