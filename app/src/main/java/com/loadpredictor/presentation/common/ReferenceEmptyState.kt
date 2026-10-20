package com.loadpredictor.presentation.common
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loadpredictor.presentation.theme.*

@Composable
fun ReferenceEmptyState(permission:Boolean,onAction:()->Unit,modifier:Modifier=Modifier) {
    Column(modifier.fillMaxSize()) {
        ReferenceTopBar("DATRACK")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            val color=if(permission) PaceCritical else MintPrimary
            Box(Modifier.size(96.dp).background(color.copy(alpha=.08f),CircleShape),contentAlignment=Alignment.Center) {
                Icon(if(permission) Icons.Outlined.WarningAmber else Icons.Outlined.DataUsage,null,tint=color,modifier=Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(if(permission) "Usage access needed" else "No active promo",style=MaterialTheme.typography.titleLarge,color=TextHighEmphasis,textAlign=TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(if(permission) "DATRACK reads mobile-data totals from Android to forecast your usage. Your data stays on this phone." else "Set up your data promo to start tracking how long it will last.",style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis,textAlign=TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick=onAction,colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=MintOnPrimary),modifier=Modifier.heightIn(min=48.dp)) {Text(if(permission) "Grant access" else "Set up your promo")}
            if(permission) {Spacer(Modifier.height(16.dp));Text("We can’t read messages, calls, or app content.",style=MaterialTheme.typography.bodySmall,color=TextLowEmphasis,textAlign=TextAlign.Center)}
        }
    }
}
