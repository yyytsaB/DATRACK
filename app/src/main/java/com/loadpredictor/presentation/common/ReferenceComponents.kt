package com.loadpredictor.presentation.common

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.loadpredictor.domain.model.*
import com.loadpredictor.presentation.theme.*

/** The supplied React reference is the visual authority; actual domain states drive these tokens. */
data class PaceVisual(val label: String, val color: Color)
fun paceVisual(f: BurnForecast): PaceVisual = when(f.pace) {
    BurnPace.EXPIRED -> PaceVisual("Promo expired", PaceCritical)
    BurnPace.DEPLETED -> PaceVisual("Data depleted", PaceCritical)
    BurnPace.INSUFFICIENT_DATA -> PaceVisual("Calibrating", PaceCalibrating)
    BurnPace.BURNING_FAST -> {
        val early = f.estimatedDepletionTimestamp?.let { t -> f.promo.expirationTimestamp?.minus(t) } ?: 0
        if (early > 86400000L) PaceVisual("Will run out early", PaceCritical)
        else PaceVisual("Cutting it close", Color(0xFFFB923C))
    }
    BurnPace.ON_TRACK -> PaceVisual(if(f.promo.isNoExpiry) "Steady pace" else "On track", PaceOnTrack)
    BurnPace.CONSERVATIVE -> PaceVisual(if(f.promo.isNoExpiry) "Light usage" else "On track", PaceOnTrack)
}

@Composable
fun ReferenceTopBar(title: String, actions: List<Pair<String, () -> Unit>> = emptyList()) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextHighEmphasis)
        if(actions.isNotEmpty()) Box {
            IconButton(onClick = {menu = true}) { Box(Modifier.size(32.dp).background(SurfaceLayer1, CircleShape), contentAlignment=Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, "More options", Modifier.size(18.dp), tint=TextMediumEmphasis)
            } }
            DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {actions.forEach { (label,action) ->
                DropdownMenuItem(text={Text(label)},onClick={menu=false;action()})
            }}
        }
    }
}

@Composable
fun ReferenceChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color=color.copy(alpha=.12f),shape=RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(color,CircleShape))
            Text(label,color=color,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
fun ReferenceProgress(fraction: Float, color: Color = MintPrimary, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha=.08f),RoundedCornerShape(50))) {
        if(fraction > 0) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f,1f)).background(color,RoundedCornerShape(50)))
    }
}

@Composable
fun ReferenceSection(title: String, actionLabel: String?=null, action:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,color=TextHighEmphasis)
        if(actionLabel!=null && action!=null) TextButton(onClick=action) {Text(actionLabel,style=MaterialTheme.typography.labelSmall,color=MintPrimary)}
    }
}
