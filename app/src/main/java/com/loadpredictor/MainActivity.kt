package com.loadpredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loadpredictor.data.stats.UsageAccessHelper
import com.loadpredictor.domain.model.Promo
import com.loadpredictor.presentation.MainUiState
import com.loadpredictor.presentation.MainViewModel
import com.loadpredictor.presentation.common.UsagePermissionRequiredCard
import com.loadpredictor.presentation.theme.LoadPredictorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usageAccessHelper = UsageAccessHelper(this)

        setContent {
            LoadPredictorTheme {
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.provideFactory(context)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Re-check permission automatically when user returns from Settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.checkPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreenContent(
                        uiState = uiState,
                        onGrantPermissionClick = {
                            startActivity(usageAccessHelper.createUsageAccessSettingsIntent())
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        !uiState.isUsagePermissionGranted -> {
            UsagePermissionRequiredCard(
                onGrantPermissionClick = onGrantPermissionClick,
                modifier = modifier
            )
        }
        else -> {
            DashboardPlaceholderView(
                activePromo = uiState.activePromo,
                modifier = modifier
            )
        }
    }
}

@Composable
fun DashboardPlaceholderView(
    activePromo: Promo?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Load Predictor Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (activePromo != null) {
                Text(
                    text = "Active Promo: ${activePromo.name} (${activePromo.simSlot.displayName})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = "No active promo configured.\nConfigure a promo to begin forecasting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
