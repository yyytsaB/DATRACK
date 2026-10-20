package com.loadpredictor.presentation.promo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.common.*
import com.loadpredictor.presentation.theme.*
import com.loadpredictor.util.DataFormatter
import java.math.BigDecimal

@Composable
fun PromoManagementScreen(viewModel:PromoViewModel,onNavigateBack:(()->Unit)?=null) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sim by rememberSaveable {mutableStateOf(SimSlot.SIM_1)}
    var adding by rememberSaveable {mutableStateOf(false)}
    var editId by rememberSaveable {mutableStateOf<Long?>(null)}
    var detailId by rememberSaveable {mutableStateOf<Long?>(null)}
    var deleteId by rememberSaveable {mutableStateOf<Long?>(null)}
    var seeAll by rememberSaveable {mutableStateOf(false)}
    val now=System.currentTimeMillis()
    val promos=state.promos.filter {it.simSlot==sim}
    val active=promos.firstOrNull {it.isActive}
    val inactive=promos.filterNot {it.isActive}
    val past=inactive.filter {it.isExpired(now)||it.remainingBytes(it.lastSyncDataUsedBytes)==0L}
    val saved=inactive.filterNot {it in past}
    Column(Modifier.fillMaxSize()) {
        ReferenceTopBar("Promos",listOf("Add promo" to {adding=true},"Show all load history" to {seeAll=true}))
        Box(Modifier.weight(1f)) {
            LazyColumn(contentPadding=PaddingValues(start=20.dp,end=20.dp,top=12.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                state.errorMessage?.let {item {Text(it,color=PaceCritical,style=MaterialTheme.typography.bodySmall)}}
                item {
                    Row(Modifier.fillMaxWidth().background(SurfaceLayer1,RoundedCornerShape(12.dp)).padding(3.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        SimSlot.entries.forEach {slot ->
                            val selected=slot==sim
                            Surface(onClick={sim=slot},modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(10.dp),color=if(selected) MintPrimary else Color.Transparent) {
                                Box(Modifier.padding(vertical=12.dp),contentAlignment=Alignment.Center) {Text(if(slot==SimSlot.SIM_1) "SIM 1" else "SIM 2",color=if(selected) MintOnPrimary else TextMediumEmphasis,style=MaterialTheme.typography.labelLarge)}
                            }
                        }
                    }
                }
                if(active!=null) item {ActiveLoadCard(active,state.activeForecast){detailId=active.id}}
                else item {
                    Surface(shape=RoundedCornerShape(16.dp),color=SurfaceLayer1) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("No active promo for this SIM",color=TextHighEmphasis,style=MaterialTheme.typography.titleSmall)
                        Text("Choose a saved promo or add a new one. Tracking uses device-total mobile data.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={adding=true}) {Text("Add promo")}
                    }}
                }
                if(saved.isNotEmpty()) {
                    item {ReferenceSection("Saved promos")}
                    items(saved,key={"saved-${it.id}"}) {p ->
                        Surface(shape=RoundedCornerShape(16.dp),color=SurfaceLayer1) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment=Alignment.Top) {
                                Column(Modifier.weight(1f)) {Text(p.name,color=TextHighEmphasis,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(DataFormatter.formatDateRange(p.startTimestamp,p.expirationTimestamp),color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)}
                                IconButton(onClick={detailId=p.id}){Icon(Icons.Outlined.MoreHoriz,"Promo details",tint=TextMediumEmphasis)}
                            }
                            ReferenceProgress(p.remainingBytes(p.lastSyncDataUsedBytes).toFloat()/p.totalAllowanceBytes)
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                Text("${DataFormatter.formatBytes(p.remainingBytes(p.lastSyncDataUsedBytes))} · saved balance",Modifier.weight(1f),color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                                TextButton(onClick={viewModel.selectActivePromo(p.id)}){Text("Track")}
                            }
                        }}
                    }
                }
                item {ReferenceSection("Load history",if(seeAll) "Show less" else "See all",{seeAll=!seeAll})}
                if(past.isEmpty()) item {Text("Your past promos will appear here when they expire or their data is used up.",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)}
                items(if(seeAll) past else past.take(3),key={"past-${it.id}"}) {p -> PastLoadRow(p){detailId=p.id}}
                item {Text("SIM labels identify your selected promo context. Usage is measured across this device’s mobile connections.",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)}
            }
            FloatingActionButton(onClick={adding=true},containerColor=MintPrimary,contentColor=MintOnPrimary,shape=RoundedCornerShape(16.dp),modifier=Modifier.align(Alignment.BottomEnd).padding(end=20.dp,bottom=20.dp)) {
                Icon(Icons.Outlined.Add,"Add promo")
            }
        }
    }
    val editing=state.promos.firstOrNull {it.id==editId}
    if(adding || editing!=null) PromoEntryDialog(promoToEdit=editing,saveError=state.errorMessage,isSaving=state.isSaving,
        onDismissRequest={adding=false;editId=null},onSavePromo={id,name,allowance,start,expiry,offset,slot,isActive,existing,carrier,price ->
            viewModel.savePromo(id=id,name=name,allowanceBytes=allowance,startTimestamp=start,expirationTimestamp=expiry,initialUsageOffsetBytes=offset,simSlot=slot,isActive=isActive,existingPromo=existing,carrier=carrier,priceCentavos=price,
                onSuccess={adding=false;editId=null})
        })
    state.promos.firstOrNull {it.id==detailId}?.let {p ->
        AlertDialog(onDismissRequest={detailId=null},title={Text(p.name)},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("${if(p.simSlot==SimSlot.SIM_1) "SIM 1" else "SIM 2"}${p.carrier?.let {" · $it"}.orEmpty()}")
            Text("Allowance: ${DataFormatter.formatBytes(p.totalAllowanceBytes)}")
            Text(DataFormatter.formatDateRange(p.startTimestamp,p.expirationTimestamp))
            Text(if(p.isExpired(now)) "Expired · unused balance is unavailable" else if(p.isActive) "Currently tracking" else "Saved context · balance is a snapshot")
            p.priceCentavos?.let {Text("Price paid: ${loadPrice(it)}")}
            Row {TextButton(onClick={detailId=null;editId=p.id}){Text("Edit")};TextButton(onClick={detailId=null;deleteId=p.id}){Text("Delete",color=PaceCritical)}}
        }},confirmButton={TextButton(onClick={detailId=null}){Text("Done")}})
    }
    state.promos.firstOrNull {it.id==deleteId}?.let {p ->AlertDialog(onDismissRequest={deleteId=null},title={Text("Delete ${p.name}?")},text={Text("This removes the saved promo and its load-history record.")},confirmButton={TextButton(onClick={viewModel.deletePromo(p);deleteId=null}){Text("Delete",color=PaceCritical)}},dismissButton={TextButton(onClick={deleteId=null}){Text("Keep promo")}})}
}

@Composable
private fun ActiveLoadCard(p:Promo,f:BurnForecast?,onClick:()->Unit) {
    val expired=p.isExpired(System.currentTimeMillis())
    val left=f?.dataRemainingBytes?:p.remainingBytes(p.lastSyncDataUsedBytes)
    Surface(onClick=onClick,shape=RoundedCornerShape(16.dp),color=SurfaceLayer1,border=BorderStroke(1.dp,MintPrimary.copy(alpha=.25f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    ReferenceChip(if(expired) "Expired" else "Active now",if(expired) PaceCritical else PaceOnTrack)
                    Text(p.name,color=TextHighEmphasis,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(listOfNotNull(p.carrier,if(p.expirationTimestamp==null) "No expiry" else "${if(expired) "Expired" else "Expires"} ${DataFormatter.formatDate(p.expirationTimestamp)}").joinToString(" · "),color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment=Alignment.End) {
                    Text(if(expired) "Ended" else DataFormatter.formatBytes(left),color=MintPrimary,style=MaterialTheme.typography.headlineSmall,fontFamily=DataFontFamily)
                    Text(if(expired) "not usable" else "remaining",color=TextMediumEmphasis,style=MaterialTheme.typography.labelSmall)
                }
            }
            ReferenceProgress(if(expired) 0f else left.toFloat()/p.totalAllowanceBytes)
            Text("${DataFormatter.formatBytes(p.totalAllowanceBytes-left)} of ${DataFormatter.formatBytes(p.totalAllowanceBytes)} used",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable private fun PastLoadRow(p:Promo,onClick:()->Unit) {
    val used=p.totalAllowanceBytes-p.remainingBytes(p.lastSyncDataUsedBytes)
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(MintPrimary.copy(alpha=.08f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Icon(Icons.Outlined.SimCard,null,Modifier.size(20.dp),tint=MintPrimary)}
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(p.name,color=TextHighEmphasis,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                Text("${DataFormatter.formatBytes(p.totalAllowanceBytes)} · ${DataFormatter.formatDateRange(p.startTimestamp,p.expirationTimestamp)}",color=TextMediumEmphasis,style=MaterialTheme.typography.bodySmall)
                Text("Used ${((used.toDouble()/p.totalAllowanceBytes)*100).toInt()}% · saved snapshot",color=TextLowEmphasis,style=MaterialTheme.typography.labelSmall)
            }
            p.priceCentavos?.let {Text(loadPrice(it),color=TextMediumEmphasis,fontFamily=DataFontFamily,style=MaterialTheme.typography.bodySmall)}
            Icon(Icons.Outlined.ChevronRight,null,tint=TextLowEmphasis,modifier=Modifier.size(18.dp))
        }
        HorizontalDivider(color=DarkOutlineVariant)
    }
}
private fun loadPrice(value:Long):String="₱"+BigDecimal(value).movePointLeft(2).stripTrailingZeros().toPlainString()
