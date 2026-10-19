package com.loadpredictor.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.util.DataFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InteractiveUsageChart(buckets: List<UsageBucket>, selectedBucketTimestamp: Long?, onSelectBucketTimestamp: (Long) -> Unit,
    modifier: Modifier = Modifier) {
    val max = buckets.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily mobile usage", style = MaterialTheme.typography.titleMedium)
            Text("Scroll to explore all ${buckets.size} dates. Tap a day for its value.", style = MaterialTheme.typography.bodySmall)
            buckets.find { it.startTimestamp == selectedBucketTimestamp }?.let {
                Text("${DataFormatter.formatDate(it.startTimestamp)} · ${DataFormatter.formatBytes(it.totalBytes)}", color = MaterialTheme.colorScheme.primary)
            }
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(buckets, key = { it.startTimestamp }) { bucket ->
                    val label = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(bucket.startTimestamp))
                    val chosen = bucket.startTimestamp == selectedBucketTimestamp
                    Column(Modifier.width(64.dp).clickable { onSelectBucketTimestamp(bucket.startTimestamp) }
                        .semantics { contentDescription = "$label, ${DataFormatter.formatBytes(bucket.totalBytes)}"; selected = chosen }
                        .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(112.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.width(24.dp).height((112 * bucket.totalBytes.toFloat() / max).coerceAtLeast(2f).dp)
                                .background(if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (buckets.isEmpty()) Text("No measured usage to display.")
        }
    }
}
