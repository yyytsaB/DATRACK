package com.loadpredictor.presentation.widget
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.launch

@Composable
fun WidgetsScreen(viewModel:WidgetsViewModel,modifier:Modifier=Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context=LocalContext.current;val snack=remember {SnackbarHostState()};val scope=rememberCoroutineScope()
    val pin:(Boolean)->Unit={wide->val sent=viewModel.requestPinWidget(context,wide);scope.launch {snack.showSnackbar(if(sent) "Confirm placement on your home screen." else "Open your launcher’s widget picker and choose DATRACK Preview.")};Unit}
    Box(modifier.fillMaxSize()) {
        Column {
            ReferenceTopBar("Widgets",listOf("Add compact widget" to {pin(false)},"Add wide widget" to {pin(true)}))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Text(if(state.activeForecast==null) "Example previews · select an active promo for live values" else "Live previews · ${state.activeForecast!!.promo.name}",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                Text("2 × 2 Compact",color=TextMediumEmphasis,style=MaterialTheme.typography.titleSmall)
                val scale=LocalDensity.current.fontScale
                if(scale>1.3f) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    WidgetPreview(state.activeForecast,false,false,Modifier.fillMaxWidth());WidgetPreview(state.activeForecast,false,true,Modifier.fillMaxWidth())
                } else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    WidgetPreview(state.activeForecast,false,false,Modifier.weight(1f));WidgetPreview(state.activeForecast,false,true,Modifier.weight(1f))
                }
                OutlinedButton(onClick={pin(false)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)){Text("Add compact widget")}
                Text("4 × 2 Wide",color=TextMediumEmphasis,style=MaterialTheme.typography.titleSmall)
                WidgetPreview(state.activeForecast,true,false,Modifier.fillMaxWidth())
                WidgetPreview(state.activeForecast,true,true,Modifier.fillMaxWidth())
                Button(onClick={pin(true)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(12.dp)){Text("Add wide widget")}
                Text("Widgets follow your phone’s light or dark theme. Your launcher controls final size. Long-press to resize; tap the wide widget’s refresh icon for a new reading.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                Text("Background readings update about hourly. Tap the balance to open DATRACK Preview.",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)
            }
        }
        SnackbarHost(snack,Modifier.align(Alignment.BottomCenter))
    }
}
@Composable private fun WidgetPreview(f:BurnForecast?,wide:Boolean,light:Boolean,modifier:Modifier) {
    val bg=if(light) Color(0xFFF0F4FF) else Color(0xFF161B26)
    val text=if(light) Color(0xFF1A1D27) else Color(0xFFF1F5F9)
    val muted=if(light) Color(0xFF586375) else TextMediumEmphasis
    val pace=f?.pace?:BurnPace.ON_TRACK;val color=widgetPaceColor(pace,light)
    val ratio=if(pace==BurnPace.EXPIRED) 0f else f?.let {(it.dataRemainingBytes.toFloat()/it.promo.totalAllowanceBytes).coerceIn(0f,1f)}?:.35f
    val pair=f?.let {DataFormatter.formatDataPair(it.dataRemainingBytes,it.promo.totalAllowanceBytes)}
    val amount=pair?.remainingFormatted?:"2.8 GB"
    val large=LocalDensity.current.fontScale>1.3f
    Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(if(light) "Light" else "Dark",style=MaterialTheme.typography.labelSmall,color=TextLowEmphasis)
        Surface(color=bg,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,if(light) Color.Black.copy(alpha=.08f) else Color.White.copy(alpha=.07f))) {
            Column(Modifier.fillMaxWidth().heightIn(min=156.dp).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(if(wide) "DATRACK" else if(f?.promo?.simSlot==SimSlot.SIM_2) "SIM 2" else "SIM 1",Modifier.weight(1f),color=muted,style=MaterialTheme.typography.labelSmall)
                    Box(Modifier.size(7.dp).background(color,CircleShape))
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(!wide && !large) Image(referenceRingBitmap(ratio,color,light).asImageBitmap(),null,modifier=Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if(pace==BurnPace.EXPIRED) "Expired" else amount,color=text,style=MaterialTheme.typography.titleLarge.copy(fontSize=if(wide) 28.sp else 22.sp),fontWeight=FontWeight.Bold)
                        Text(if(wide) "of ${pair?.totalFormatted?:"8 GB"} remaining" else "remaining",color=muted,style=MaterialTheme.typography.labelSmall)
                    }
                    if(wide) Box(Modifier.weight(1f)) {ReferenceProgress(ratio,color)}
                }
                Text(widgetPaceLabel(pace,f?.promo?.isNoExpiry?:false),color=color,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}
