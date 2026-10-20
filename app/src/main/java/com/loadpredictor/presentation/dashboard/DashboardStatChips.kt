package com.loadpredictor.presentation.dashboard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter

@Composable
fun DashboardStatChips(forecast:BurnForecast,dailyAvgBytes:Long,modifier:Modifier=Modifier) {
    val now=System.currentTimeMillis()
    val runway=ForecastCopy.runway(forecast.pace,forecast.estimatedDepletionTimestamp,now).removePrefix("About ").removeSuffix(" left")
    val rows=listOf("Daily avg" to if(forecast.pace==BurnPace.INSUFFICIENT_DATA) "—" else DataFormatter.formatBytes(dailyAvgBytes),
        "Validity" to if(forecast.promo.isNoExpiry) "No expiry" else if(forecast.pace==BurnPace.EXPIRED) "Expired" else validityDuration(forecast,now),
        "At this pace" to if(forecast.pace==BurnPace.INSUFFICIENT_DATA) "Learning" else runway)
    if(LocalDensity.current.fontScale>1.3f) Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label,value)-> StatTile(label,value,Modifier.fillMaxWidth()) }
    } else Row(modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label,value)-> StatTile(label,value,Modifier.weight(1f).fillMaxHeight()) }
    }
}
@Composable private fun StatTile(label:String,value:String,modifier:Modifier) {
    Surface(modifier,shape=RoundedCornerShape(16.dp),color=SurfaceLayer1) {
        Column(Modifier.padding(horizontal=8.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(value,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=TextHighEmphasis)
            Text(label,style=MaterialTheme.typography.labelSmall,color=TextMediumEmphasis)
        }
    }
}
