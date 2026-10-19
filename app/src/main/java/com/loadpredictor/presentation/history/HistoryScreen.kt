package com.loadpredictor.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.*
import com.loadpredictor.util.DataFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Usage history") }) }) { padding ->
        LazyColumn(modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(state.activePromo?.name ?: "No active promo", style = MaterialTheme.typography.headlineSmall)
                Text("Measured mobile usage only. Starting balance corrections are not daily traffic.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryTimeRange.entries.forEach { range -> FilterChip(selected = range == state.selectedRange,
                        onClick = { viewModel.setTimeRange(range) }, label = { Text(when(range) {
                            HistoryTimeRange.LAST_7_DAYS -> "7 days"
                            HistoryTimeRange.LAST_30_DAYS -> "30 days"
                            HistoryTimeRange.LIFETIME -> "Up to 90 days"
                        }) }) }
                }
            }
            state.errorMessage?.let { message -> item {
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.refresh() }) { Text("Try again") }
            } }
            item {
                Text("${DataFormatter.formatBytes(state.totalBurntBytes)} measured", style = MaterialTheme.typography.headlineMedium)
                Text("${DataFormatter.formatBytes(state.dailyAverageBytes)} per completed day", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.patternInsight is UsagePatternInsight.Pattern) item { Text((state.patternInsight as UsagePatternInsight.Pattern).summary) }
            item { InteractiveUsageChart(state.dailyBuckets, state.selectedBucketTimestamp, { viewModel.selectBucket(it) }) }
            item { Text("Daily breakdown", style = MaterialTheme.typography.titleLarge) }
            if (state.dailyBuckets.isEmpty()) item { Text(if (state.isLoading) "Loading usage…" else "No readings yet. Check Usage Access and your active promo.") }
            items(state.dailyBuckets.reversed(), key = { it.startTimestamp }) { bucket ->
                Column(Modifier.fillMaxWidth().clickable { viewModel.selectBucket(bucket.startTimestamp) }.padding(vertical = 12.dp)) {
                    Text(DataFormatter.formatDate(bucket.startTimestamp), style = MaterialTheme.typography.titleMedium)
                    Text(DataFormatter.formatBytes(bucket.totalBytes), color = MaterialTheme.colorScheme.primary)
                    if (!bucket.isCompletedDay(System.currentTimeMillis())) Text("Partial day", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}
