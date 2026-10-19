package com.loadpredictor.presentation.history
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.HistoryTimeRange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryTimeRangePills(selectedRange: HistoryTimeRange, onRangeSelected: (HistoryTimeRange) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryTimeRange.entries.forEach { range ->
            FilterChip(selected = range == selectedRange, onClick = { onRangeSelected(range) },
                label = { Text(range.label) }, modifier = Modifier.heightIn(min = 48.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = com.loadpredictor.presentation.theme.MintPrimaryContainer,
                    selectedLabelColor = com.loadpredictor.presentation.theme.MintPrimary))
        }
    }
}
