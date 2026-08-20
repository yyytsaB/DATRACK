package com.loadpredictor.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Screen presenting device mobile data consumption history, range filtering,
 * interactive Canvas bar inspection, aggregate metrics, and grouped daily records.
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = SurfaceLayer1,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Usage History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextHighEmphasis
                        )
                    }
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Time-Range Segmented Pill Bar
                HistoryTimeRangePills(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )

                // 2. Metrics Summary Row
                val peakBucket = uiState.peakDayBucket
                HistoryMetricsRow(
                    totalBurntBytes = uiState.totalBurntBytes,
                    dailyAverageBytes = uiState.dailyAverageBytes,
                    peakDayBytes = peakBucket?.totalBytes ?: 0L,
                    peakDayTimestamp = peakBucket?.startTimestamp
                )

                // 3. Interactive Bar Chart with Tap-to-Inspect Tooltip
                InteractiveUsageChart(
                    buckets = uiState.dailyBuckets,
                    selectedBucketTimestamp = uiState.selectedBucketTimestamp,
                    onSelectBucketTimestamp = { viewModel.selectBucket(it) }
                )

                // 4. Registration-Day Explainer Card (shown for promos ≤ 48h old)
                if (uiState.isNewlyRegisteredPromo) {
                    RegistrationExplainerCard()
                }

                // 5. Grouped Daily Consumption List
                DailyBreakdownList(
                    buckets = uiState.dailyBuckets,
                    selectedBucketTimestamp = uiState.selectedBucketTimestamp,
                    onSelectBucketTimestamp = { viewModel.selectBucket(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
