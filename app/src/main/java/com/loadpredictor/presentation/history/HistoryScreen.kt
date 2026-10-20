package com.loadpredictor.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen presenting device mobile data consumption history.
 *
 * Implements the reference-inspired frameless visual direction:
 * - Exactly ONE framed container (the Chart)
 * - Plain typography header & inline stats
 * - Tab-style range selector
 * - Frameless daily breakdown list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Usage History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextHighEmphasis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextHighEmphasis
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.dailyBuckets.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MintPrimary)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                item {
                // 1. Context Title, Inline Stats, and Time-Range Selector
                val promoTitle = uiState.activePromo?.name ?: "All Mobile Data"
                val dateRangeStr = if (uiState.dailyBuckets.isNotEmpty()) {
                    com.loadpredictor.util.DataFormatter.formatDateRange(
                        startTimestamp = uiState.dailyBuckets.first().startTimestamp,
                        endTimestamp = uiState.dailyBuckets.last().startTimestamp
                    )
                } else {
                    "Recent"
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$promoTitle • $dateRangeStr",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )

                    // Plain inline typography stats (Daily Avg, Total, Peak)
                    val peakBucket = uiState.peakDayBucket
                    HistoryMetricsRow(
                        totalBurntBytes = uiState.totalBurntBytes,
                        dailyAverageBytes = uiState.dailyAverageBytes,
                        peakDayBytes = peakBucket?.totalBytes ?: 0L,
                        peakDayTimestamp = peakBucket?.startTimestamp
                    )

                    // Tab-Style Time-Range Selector (7D, 30D, Lifetime)
                    HistoryTimeRangePills(
                        selectedRange = uiState.selectedRange,
                        onRangeSelected = { viewModel.setTimeRange(it) }
                    )
                }

                if (uiState.selectedRange == com.loadpredictor.domain.model.HistoryTimeRange.LIFETIME) {
                    Text("Lifetime view is limited to the last 90 days.", color = TextMediumEmphasis, style = MaterialTheme.typography.bodySmall)
                }
                }
                // 2. Behavioral Usage Pattern Banner (automatically stays hidden if InsufficientData)
                item { UsagePatternBanner(patternInsight = uiState.patternInsight) }

                // 3. Interactive Bar Chart (The single boxed container on this screen)
                item { InteractiveUsageChart(
                    buckets = uiState.dailyBuckets,
                    selectedBucketTimestamp = uiState.selectedBucketTimestamp,
                    onSelectBucketTimestamp = { viewModel.selectBucket(it) }
                ) }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 4. Frameless Daily Breakdown List
                item {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Breakdown", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextHighEmphasis)
                        Text("${uiState.dailyBuckets.size} days", style = MaterialTheme.typography.bodySmall, color = TextMediumEmphasis)
                    }
                }
                val maximum = uiState.dailyBuckets.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1048576L) ?: 1048576L
                items(uiState.dailyBuckets.reversed(), key = { it.startTimestamp }) { bucket ->
                    DailyBreakdownRow(bucket, maximum,
                        bucket.startTimestamp == (uiState.selectedBucketTimestamp ?: uiState.dailyBuckets.lastOrNull()?.startTimestamp),
                        { viewModel.selectBucket(bucket.startTimestamp) })
                }

                // 5. Minimal Registration-Day Explainer Callout (at bottom)
                if (uiState.isNewlyRegisteredPromo) {
                    item { RegistrationExplainerCard() }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
