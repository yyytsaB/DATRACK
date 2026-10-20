package com.loadpredictor.presentation.alerts
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import com.loadpredictor.presentation.common.*
import com.loadpredictor.presentation.dashboard.BurnAlertsOptInCard
import com.loadpredictor.presentation.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun AlertsScreen(viewModel:AlertsViewModel,modifier:Modifier=Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
    val settings={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_RESUME)viewModel.refreshPermissionState()}
        lifecycle.lifecycle.addObserver(observer);onDispose {lifecycle.lifecycle.removeObserver(observer)}
    }
    Column(modifier.fillMaxSize()) {
        ReferenceTopBar("Notifications",listOf("Android notification settings" to settings))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            if(!state.hasNotificationPermission) BurnAlertsOptInCard(onPermissionGranted={viewModel.refreshPermissionState()})
            AlertRow("50% used","When half your allowance is gone",state.is50Enabled,viewModel::onToggle50Alert)
            AlertRow("80% used","When one fifth of your data remains",state.is80Enabled,viewModel::onToggle80Alert)
            AlertRow("90% used","When your allowance is almost used up",state.is90Enabled,viewModel::onToggle90Alert)
            AlertRow("Early depletion warning","When data may run out before promo expiry",state.isPrematureEnabled,viewModel::onTogglePrematureAlert)
            TextButton(onClick=settings,modifier=Modifier.padding(top=16.dp)) {Text("Android notification settings")}
            Text("Alerts use Android notifications. Change sound, vibration, or delivery settings there.",style=MaterialTheme.typography.bodySmall,color=TextLowEmphasis)
        }
    }
}
@Composable private fun AlertRow(title:String,subtitle:String,on:Boolean,toggle:(Boolean)->Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.bodyMedium,color=TextHighEmphasis)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
            }
            Switch(on,toggle,colors=SwitchDefaults.colors(checkedThumbColor=MintOnPrimary,checkedTrackColor=MintPrimary,uncheckedThumbColor=TextMediumEmphasis,uncheckedTrackColor=SurfaceLayer2))
        }
        HorizontalDivider(color=DarkOutlineVariant)
    }
}
