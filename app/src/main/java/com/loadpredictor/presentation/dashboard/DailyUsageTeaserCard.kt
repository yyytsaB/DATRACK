package com.loadpredictor.presentation.dashboard
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.loadpredictor.domain.model.UsageBucket
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter

@Composable
fun DailyUsageTeaserCard(buckets:List<UsageBucket>,onViewHistoryClick:()->Unit,modifier:Modifier=Modifier) {
    val visible=buckets.takeLast(14)
    var selected by rememberSaveable {mutableStateOf<Long?>(null)}
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Daily usage",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,color=TextMediumEmphasis)
            TextButton(onClick=onViewHistoryClick) {Text("View history",style=MaterialTheme.typography.labelSmall)}
        }
        Surface(color=SurfaceLayer1,shape=RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(visible.isEmpty()) Text("Daily readings will appear here as you use mobile data.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                else {
                    Canvas(Modifier.fillMaxWidth().height(64.dp).pointerInput(visible) {
                        detectTapGestures { offset ->
                            val idx=(offset.x/size.width*visible.size).toInt().coerceIn(0,visible.lastIndex)
                            selected=visible[idx].startTimestamp
                        }
                    }.semantics {contentDescription="Daily mobile usage. Open history for individual readings.";onClick("Open usage history"){onViewHistoryClick();true}}) {
                        val max=(visible.maxOfOrNull {it.totalBytes} ?:1).coerceAtLeast(1048576).toFloat()
                        val slot=size.width/visible.size
                        visible.forEachIndexed {i,v ->
                            val h=(v.totalBytes/max*size.height).coerceIn(0f,size.height)
                            val w=minOf(14.dp.toPx(),slot*.68f)
                            drawRoundRect(if(v.startTimestamp==selected) androidx.compose.ui.graphics.Color(0xFFFB923C) else if(i==visible.lastIndex) MintPrimary else MintPrimary.copy(alpha=.5f),
                                Offset(i*slot+(slot-w)/2,size.height-h),Size(w,h),CornerRadius(3.dp.toPx()))
                        }
                    }
                    val chosen=visible.find {it.startTimestamp==selected}
                    if(chosen!=null) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(DataFormatter.formatDayLabel(chosen.startTimestamp),style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                        Text(DataFormatter.formatMeasuredBytes(chosen.totalBytes),style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.SemiBold,color=TextHighEmphasis)
                    }
                }
            }
        }
        if(visible.isNotEmpty()) Text(DataFormatter.formatDateRange(visible.first().startTimestamp,visible.last().startTimestamp),style=MaterialTheme.typography.labelSmall,color=TextLowEmphasis)
    }
}
