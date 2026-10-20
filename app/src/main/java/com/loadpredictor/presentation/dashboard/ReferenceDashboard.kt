package com.loadpredictor.presentation.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.MainUiState
import com.loadpredictor.presentation.common.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import java.util.concurrent.TimeUnit

@Composable
fun ReferenceDashboard(state:MainUiState,onPromos:()->Unit,onHistory:()->Unit,onRefresh:()->Unit,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ReferenceTopBar("DATRACK",listOf("Refresh reading" to onRefresh,"Manage promos" to onPromos))
        val f=(state.forecastResult as? BurnForecastResult.Success)?.forecast
        if(f!=null) ReferenceForecast(f,state.dailyUsageBreakdown,onHistory)
        else Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.lastSuccessfulForecast?.let {Text("Last reading: ${DataFormatter.formatBytes(it.dataRemainingBytes)} · ${it.promo.name}",color=TextMediumEmphasis)}
            Text((state.forecastResult as? BurnForecastResult.Error)?.message ?: "Your forecast is unavailable.",color=PaceCritical)
            Button(onClick=onRefresh){Text("Try again")}
        }
    }
}

@Composable
fun ReferenceForecast(f:BurnForecast,buckets:List<UsageBucket>,onHistory:()->Unit) {
    val now=System.currentTimeMillis(); val visual=paceVisual(f)
    val isTerminal=f.pace==BurnPace.EXPIRED||f.pace==BurnPace.DEPLETED
    Column(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${f.promo.name} · ${if(f.promo.simSlot==SimSlot.SIM_1) "SIM 1" else "SIM 2"}",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                Text(ForecastCopy.updated(f.observedAt,now),style=MaterialTheme.typography.labelSmall,color=TextLowEmphasis)
            }
            ReferenceChip(if(f.promo.simSlot==SimSlot.SIM_1) "SIM 1" else "SIM 2",PaceOnTrack)
        }
        val (lead,highlight)=when(f.pace) {
            BurnPace.EXPIRED -> "Your promo has ended" to "Unused data is no longer available"
            BurnPace.DEPLETED -> "Your tracked allowance is" to "all used up"
            BurnPace.INSUFFICIENT_DATA -> "Getting to know" to "your data pace"
            else -> if(f.estimatedDepletionTimestamp!=null) "At current pace, you’ll run out" to DataFormatter.formatDepletionDateTime(f.estimatedDepletionTimestamp,now)
                else "At current pace, your data" to if(f.promo.isNoExpiry) "has no estimate yet" else "should last until expiry"
        }
        Text(buildAnnotatedString { append(lead);append(" ");withStyle(SpanStyle(color=visual.color)){append(highlight)} },
            style=MaterialTheme.typography.titleLarge.copy(fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold),color=TextHighEmphasis)
        Text(if(f.promo.expirationTimestamp==null) "No expiration · Data cap only" else if(f.pace==BurnPace.EXPIRED) "Promo validity has ended" else
            "Promo ends ${DataFormatter.formatDate(f.promo.expirationTimestamp,now)} · ${validityDuration(f,now)} left",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
        ReferenceChip(visual.label,visual.color)
        Box(Modifier.fillMaxWidth().padding(vertical=4.dp),contentAlignment=Alignment.Center) {
            ReferenceRing(f,visual.color)
        }
        DashboardStatChips(f,if(f.burnRateBytesPerHour>0) (f.burnRateBytesPerHour*24).toLong() else 0L)
        DailyUsageTeaserCard(buckets,onHistory)
        if(isTerminal && f.dataRemainingBytes>0) Text("${DataFormatter.formatBytes(f.dataRemainingBytes)} was unused at expiry.",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
        Spacer(Modifier.height(16.dp))
    }
}

fun validityDuration(f:BurnForecast,now:Long):String {
    val left=((f.promo.expirationTimestamp ?: return "No expiry")-now).coerceAtLeast(0)
    val days=TimeUnit.MILLISECONDS.toDays(left)
    return if(days>0) "$days ${if(days==1L) "day" else "days"}" else "< 1 day"
}

@Composable
fun ReferenceRing(f:BurnForecast,color:Color) {
    val ratio=(f.dataRemainingBytes.toFloat()/f.promo.totalAllowanceBytes.coerceAtLeast(1)).coerceIn(0f,1f)
    val scale=LocalDensity.current.fontScale
    val size=if(scale>1.3f) 232.dp else 200.dp
    val expired=f.pace==BurnPace.EXPIRED
    val pair=DataFormatter.formatDataPair(f.dataRemainingBytes,f.promo.totalAllowanceBytes)
    Box(Modifier.size(size).semantics(mergeDescendants=true) {contentDescription=if(expired) "Promo expired, ${DataFormatter.formatBytes(f.dataRemainingBytes)} unused" else "${DataFormatter.formatBytes(f.dataRemainingBytes)} remaining of ${DataFormatter.formatBytes(f.promo.totalAllowanceBytes)}"},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r=this.size.minDimension*.4f; val center=Offset(this.size.width/2,this.size.height/2)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha=.16f),Color.Transparent),center,r*1.3f),r*1.3f,center)
            val arc=Size(r*2,r*2);val off=Offset(center.x-r,center.y-r)
            drawArc(Color.White.copy(alpha=.07f),-90f,360f,false,off,arc,style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
            if(ratio>0 && !expired) {
                drawArc(color.copy(alpha=.1f),-90f,360f*ratio,false,off,arc,style=Stroke(23.dp.toPx(),cap=StrokeCap.Round))
                drawArc(color,-90f,360f*ratio,false,off,arc,style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
            }
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp),modifier=Modifier.width(size*.65f).clearAndSetSemantics {}) {
            Text(if(expired) "Expired" else pair.remainingFormatted,style=MaterialTheme.typography.headlineLarge.copy(fontSize=34.sp),fontWeight=FontWeight.Bold,color=color)
            if(!expired) {Text("of ${pair.totalFormatted}",style=MaterialTheme.typography.labelSmall,color=TextMediumEmphasis);Text("remaining",style=MaterialTheme.typography.labelSmall,color=TextLowEmphasis)}
        }
    }
}
