package com.loadpredictor.presentation.history
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(viewModel:HistoryViewModel,modifier:Modifier=Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var allApps by rememberSaveable {mutableStateOf(false)}
    val context=LocalContext.current
    Column(modifier.fillMaxSize()) {
        ReferenceTopBar("Usage history",listOf("Refresh usage" to {viewModel.refresh()},"Usage access" to {context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))}))
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.errorMessage?.let {item {Text(it,color=PaceCritical,style=MaterialTheme.typography.bodySmall);TextButton(onClick={viewModel.refresh()}){Text("Retry")}}}
            item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(state.activePromo?.name ?: "Mobile data",color=TextHighEmphasis,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                if(state.dailyBuckets.isNotEmpty()) Text(DataFormatter.formatDateRange(state.dailyBuckets.first().startTimestamp,state.dailyBuckets.last().startTimestamp),color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                Text("Daily avg  ${DataFormatter.formatMeasuredBytes(state.dailyAverageBytes)}",color=MintPrimary,style=MaterialTheme.typography.bodySmall)
                HistoryTimeRangePills(state.selectedRange,{viewModel.setTimeRange(it)})
                if(state.selectedRange==HistoryTimeRange.LIFETIME) Text("Lifetime shows up to 90 days.",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)
            }}
            if(state.isLoading && state.dailyBuckets.isEmpty()) item {LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=MintPrimary)}
            item {ReferenceHistoryChart(state.dailyBuckets,state.selectedBucketTimestamp,{viewModel.selectBucket(it)})}
            state.resolvedSelectedBucket?.let {day -> item {
                Surface(shape=RoundedCornerShape(16.dp),color=SurfaceLayer1) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(DataFormatter.formatDayLabel(day.startTimestamp),style=MaterialTheme.typography.titleSmall,color=TextHighEmphasis)
                    Text(DataFormatter.formatMeasuredBytes(day.totalBytes),style=MaterialTheme.typography.headlineSmall,color=TextHighEmphasis,fontWeight=FontWeight.Bold)
                    val delta=day.totalBytes-state.dailyAverageBytes
                    Text("${DataFormatter.formatMeasuredBytes(kotlin.math.abs(delta))} ${if(delta>=0) "above" else "below"} daily avg${if(!day.isCompletedDay(System.currentTimeMillis())) " · partial day" else ""}",style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)
                }}
            }}
            item {WeeklyInsight(state.patternInsight)}
            item {ReferenceSection("Top data users",if(allApps) "Show less" else "View all",{allApps=!allApps})}
            when {
                state.isAppUsageLoading -> item {LinearProgressIndicator(Modifier.fillMaxWidth(),color=MintPrimary)}
                state.appUsageError!=null -> item {Text(state.appUsageError!!,color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall);TextButton(onClick={viewModel.refresh()}){Text("Retry app usage")}}
                state.appUsage?.apps.isNullOrEmpty() -> item {Text("No app mobile-data records in this period yet.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)}
                else -> {
                    val report=state.appUsage!!
                    items(if(allApps) report.apps else report.apps.take(4),key={"app-${it.uid}"}) {app ->AppUsageRow(app,report.share(app))}
                    item {Text("Percentages use app-attributed mobile data in this history period. Shared Android UIDs are grouped. Wi-Fi and any initial already-used amount are excluded.",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)}
                }
            }
            item {ReferenceSection("Daily breakdown")}
            items(state.dailyBuckets.reversed(),key={"day-${it.startTimestamp}"}) {bucket ->
                val max=state.dailyBuckets.maxOfOrNull {it.totalBytes}?.coerceAtLeast(1048576) ?: 1048576
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable{viewModel.selectBucket(bucket.startTimestamp)}.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(DataFormatter.formatDate(bucket.startTimestamp),Modifier.weight(1f),color=TextMediumEmphasis,style=MaterialTheme.typography.bodyMedium)
                    Box(Modifier.width(60.dp)){ReferenceProgress(bucket.totalBytes.toFloat()/max,MintPrimary.copy(alpha=.4f))}
                    Text(DataFormatter.formatMeasuredBytes(bucket.totalBytes),color=TextHighEmphasis,style=MaterialTheme.typography.bodySmall,fontFamily=DataFontFamily)
                }
                HorizontalDivider(color=DarkOutlineVariant)
            }
            if(state.activePromo==null) item {Text("Select a promo to view its history.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)}
        }
    }
}

@Composable private fun ReferenceHistoryChart(buckets:List<UsageBucket>,selected:Long?,onSelect:(Long)->Unit) {
    Surface(color=SurfaceLayer1,shape=RoundedCornerShape(16.dp)) {
        if(buckets.isEmpty()) Box(Modifier.fillMaxWidth().heightIn(min=140.dp).padding(20.dp),contentAlignment=Alignment.Center) {Text("No measured usage to plot yet.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)}
        else BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val chartWidth=maxOf(maxWidth,buckets.size*48.dp)
            Row(Modifier.horizontalScroll(rememberScrollState()).width(chartWidth).height(160.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.Bottom) {
                val max=buckets.maxOf {it.totalBytes}.coerceAtLeast(1048576)
                buckets.forEach {v ->
                    val active=v.startTimestamp==selected
                    Column(Modifier.weight(1f).fillMaxHeight().clickable {onSelect(v.startTimestamp)}.semantics {contentDescription="${DataFormatter.formatDayLabel(v.startTimestamp)}, ${DataFormatter.formatMeasuredBytes(v.totalBytes)}";this.selected=active},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
                            Box(Modifier.width(14.dp).fillMaxHeight((v.totalBytes.toFloat()/max).coerceIn(0f,1f)).background(if(active) androidx.compose.ui.graphics.Color(0xFFFB923C) else MintPrimary.copy(alpha=.55f),RoundedCornerShape(topStart=4.dp,topEnd=4.dp)))
                        }
                        Text(DataFormatter.formatShortDay(v.startTimestamp),Modifier.padding(top=8.dp),style=MaterialTheme.typography.labelSmall,color=TextLowEmphasis)
                    }
                }
            }
        }
    }
}

@Composable private fun WeeklyInsight(insight:UsagePatternInsight) {
    val p=insight as? UsagePatternInsight.Pattern
    val title=when(p?.patternType) {
        PatternType.WEEKEND_HEAVY -> "Weekend use is ${((p.ratio-1)*100).roundToInt()}% higher"
        PatternType.WEEKDAY_HEAVY -> "Weekday use is ${((p.ratio-1)*100).roundToInt()}% higher"
        PatternType.BALANCED -> "Your weekly usage is balanced"
        else -> "Learning your weekly pattern"
    }
    Surface(color=SurfaceLayer1,shape=RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).background(MintPrimary.copy(alpha=.1f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {Icon(Icons.Outlined.AutoAwesome,null,tint=MintPrimary,modifier=Modifier.size(18.dp))}
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("Weekly insight",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)
                Text(title,color=TextHighEmphasis,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                Text(if(p==null) "Needs 3 completed weekdays and 2 weekend days. Partial days don’t count." else
                    "Weekends average ${DataFormatter.formatMeasuredBytes(p.weekendAvgBytes)}/day; weekdays ${DataFormatter.formatMeasuredBytes(p.weekdayAvgBytes)}/day.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable private fun AppUsageRow(app:AppMobileUsage,share:Float) {
    val context=LocalContext.current
    val icon by produceState<ImageBitmap?>(null,app.uid,app.packageName) {
        value=withContext(Dispatchers.IO) {try {app.packageName?.let {context.packageManager.getApplicationIcon(it).toBitmap(72,72).asImageBitmap()}} catch(_:android.content.pm.PackageManager.NameNotFoundException){null}}
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(SurfaceLayer1,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                icon?.let {Image(it,null,modifier=Modifier.size(24.dp))} ?: Icon(Icons.Outlined.Android,null,tint=MintPrimary,modifier=Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(app.label,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,color=TextHighEmphasis)
                    Column(horizontalAlignment=Alignment.End) {
                        Text(DataFormatter.formatMeasuredBytes(app.bytes),color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall,fontFamily=DataFontFamily)
                        Text("${(share*100).roundToInt()}%",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)
                    }
                }
                ReferenceProgress(share,MintPrimary.copy(alpha=.5f))
            }
        }
        HorizontalDivider(color=DarkOutlineVariant)
    }
}
