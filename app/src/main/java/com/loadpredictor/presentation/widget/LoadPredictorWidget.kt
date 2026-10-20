package com.loadpredictor.presentation.widget
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.state.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.loadpredictor.MainActivity
import com.loadpredictor.data.local.AppDatabase
import com.loadpredictor.data.repository.*
import com.loadpredictor.data.stats.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.ForecastCopy
import com.loadpredictor.util.DataFormatter

/** Native Glance mirrors the supplied compact/wide designs, with real readings and theme-aware colors. */
class LoadPredictorWidget:GlanceAppWidget() {
    override val sizeMode=SizeMode.Exact
    override val stateDefinition:GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context:Context,id:GlanceId) {
        val promo=PromoRepositoryImpl(AppDatabase.getInstance(context).promoDao())
        val usage=UsageRepositoryImpl(UsageAccessHelper(context),NetworkStatsDataSource(context))
        WidgetStatePreferences.saveState(context,id,WidgetSyncHelper.computeWidgetState(promo,usage))
        provideContent {
            val state=WidgetStatePreferences.readState(currentState<Preferences>())
            ReferenceWidget(state,LocalSize.current.width>=260.dp)
        }
    }
}

internal fun widgetPaceColor(pace:BurnPace,light:Boolean=false):Color=when(pace) {
    BurnPace.EXPIRED,BurnPace.DEPLETED,BurnPace.BURNING_FAST -> if(light) Color(0xFFB3261E) else Color(0xFFF87171)
    BurnPace.INSUFFICIENT_DATA -> if(light) Color(0xFF1259A7) else Color(0xFF60A5FA)
    else -> if(light) Color(0xFF16713C) else Color(0xFF4ADE80)
}
internal fun widgetPaceLabel(pace:BurnPace,noExpiry:Boolean):String=when(pace) {
    BurnPace.EXPIRED -> "Promo expired"
    BurnPace.DEPLETED -> "Data depleted"
    BurnPace.BURNING_FAST -> if(noExpiry) "Higher usage" else "May run out early"
    BurnPace.INSUFFICIENT_DATA -> "Calibrating"
    else -> if(noExpiry) "Steady pace" else "On track"
}
internal fun referenceRingBitmap(fraction:Float,color:Color,isLight:Boolean=false):Bitmap {
    val bitmap=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.STROKE;strokeWidth=16f;strokeCap=Paint.Cap.ROUND}
    val rect=RectF(16f,16f,176f,176f)
    paint.color=if(isLight) 0x19000000 else 0x19FFFFFF
    canvas.drawArc(rect,-90f,360f,false,paint)
    paint.color=color.toArgb()
    if(fraction>0) canvas.drawArc(rect,-90f,360*fraction.coerceIn(0f,1f),false,paint)
    return bitmap
}
@Composable private fun ReferenceWidget(state:WidgetState,wide:Boolean) {
    val bg=DayNightColorProvider(day=Color(0xFFF0F4FF),night=Color(0xFF161B26))
    val text=DayNightColorProvider(day=Color(0xFF1A1D27),night=Color(0xFFF1F5F9))
    val muted=DayNightColorProvider(day=Color(0xFF586375),night=Color(0xFF9CA6B5))
    val track=DayNightColorProvider(day=Color(0xFFDAE0EA),night=Color(0xFF2C3340))
    val context=LocalContext.current
    val largeText=context.resources.configuration.fontScale>1.3f
    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(bg).cornerRadius(24.dp).padding(if(wide) 16.dp else 12.dp).clickable(actionStartActivity<MainActivity>())) {
        if(state is WidgetState.Success) {
            val color=widgetPaceColor(state.pace,(context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES)
            val expired=state.pace==BurnPace.EXPIRED
            val left=DataFormatter.formatDataPair(state.remainingBytes,state.totalAllowanceBytes)
            val ratio=if(expired) 0f else (state.remainingBytes.toFloat()/state.totalAllowanceBytes.coerceAtLeast(1)).coerceIn(0f,1f)
            val sim=if(state.simSlot==SimSlot.SIM_1) "SIM 1" else "SIM 2"
            Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.Vertical.CenterVertically) {
                Text(if(wide) "DATRACK · $sim" else sim,GlanceModifier.defaultWeight(),style=TextStyle(color=muted,fontSize=12.sp,fontWeight=FontWeight.Medium),maxLines=2)
                Image(ImageProvider(android.R.drawable.ic_popup_sync),"Refresh usage",modifier=GlanceModifier.size(48.dp).clickable(actionRunCallback<RefreshWidgetCallback>()).padding(12.dp))
            }
            Spacer(GlanceModifier.height(2.dp))
            Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.Vertical.CenterVertically) {
                if(!wide && !largeText) {
                    Image(ImageProvider(referenceRingBitmap(ratio,color)),null,modifier=GlanceModifier.size(56.dp))
                    Spacer(GlanceModifier.width(10.dp))
                }
                Column(GlanceModifier.defaultWeight()) {
                    Text(if(expired) "Expired" else left.remainingFormatted,style=TextStyle(color=text,fontSize=if(wide) 26.sp else 21.sp,fontWeight=FontWeight.Bold),maxLines=2)
                    Text(if(expired) "Validity ended" else if(wide) "of ${left.totalFormatted} remaining" else "remaining",style=TextStyle(color=muted,fontSize=11.sp),maxLines=2)
                }
                if(wide) {
                    Spacer(GlanceModifier.width(16.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text("${(ratio*100).toInt()}% left",style=TextStyle(color=muted,fontSize=11.sp))
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(ratio,GlanceModifier.fillMaxWidth().height(6.dp),ColorProvider(color),track)
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(if(wide) ForecastCopy.widgetSummary(state.pace,state.estimatedDepletionTimestamp,state.isNoExpiry,System.currentTimeMillis(),true) else widgetPaceLabel(state.pace,state.isNoExpiry),style=TextStyle(color=ColorProvider(color),fontSize=12.sp,fontWeight=FontWeight.Medium),maxLines=2)
            Spacer(GlanceModifier.height(4.dp))
            Text(ForecastCopy.updated(state.lastUpdatedMillis,System.currentTimeMillis()),style=TextStyle(color=muted,fontSize=11.sp),maxLines=2)
        } else {
            Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.Vertical.CenterVertically) {
                Text("DATRACK",GlanceModifier.defaultWeight(),style=TextStyle(color=text,fontWeight=FontWeight.Bold,fontSize=14.sp))
                Image(ImageProvider(android.R.drawable.ic_popup_sync),"Refresh usage",modifier=GlanceModifier.size(48.dp).clickable(actionRunCallback<RefreshWidgetCallback>()).padding(12.dp))
            }
            Text(when(state){WidgetState.NoActivePromo->"Set up your promo";WidgetState.PermissionRequired->"Usage access needed";else->"Reading unavailable"},style=TextStyle(color=text,fontSize=18.sp,fontWeight=FontWeight.Bold))
            Spacer(GlanceModifier.height(8.dp))
            Text(when(state){WidgetState.NoActivePromo->"Open DATRACK to start tracking.";WidgetState.PermissionRequired->"Open DATRACK to allow usage access.";is WidgetState.Error->if(state.lastSuccessfulAt>0) ForecastCopy.updated(state.lastSuccessfulAt,System.currentTimeMillis())+". Tap refresh to retry." else "Tap refresh to try again.";else->"Open DATRACK."},style=TextStyle(color=muted,fontSize=12.sp))
        }
    }
}
