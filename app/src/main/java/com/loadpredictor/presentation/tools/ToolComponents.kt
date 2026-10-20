package com.loadpredictor.presentation.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.semantics.stateDescription
import com.loadpredictor.presentation.theme.*

/** Shared native presentation for secondary tools; leaves the app-wide theme untouched. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ToolSheet(
    title:String,subtitle:String?=null,busy:Boolean=false,canDismiss:Boolean=true,
    onDismiss:()->Unit,scrollable:Boolean=true,heightFraction:Float=.94f,feedback:String="",feedbackError:Boolean=false,
    footer:@Composable ColumnScope.()->Unit={},content:@Composable ColumnScope.()->Unit
) {
    val dismissAllowed by rememberUpdatedState(!busy && canDismiss)

    val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=true,
        confirmValueChange={it!=SheetValue.Hidden || dismissAllowed})
    ModalBottomSheet(onDismissRequest={if(!busy && canDismiss)onDismiss()},sheetState=sheet,
        containerColor=SurfaceLayer1,contentColor=TextHighEmphasis,tonalElevation=0.dp,
        dragHandle=null,sheetMaxWidth=640.dp) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(heightFraction).navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start=24.dp,end=12.dp,top=12.dp,bottom=12.dp),
                verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    subtitle?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)}
                }
                IconButton(onClick=onDismiss,enabled=!busy && canDismiss,modifier=Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close,contentDescription="Close $title",tint=if(!busy && canDismiss)TextMediumEmphasis else TextLowEmphasis)
                }
            }
            HorizontalDivider(color=DarkOutline)
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=MintPrimary,trackColor=DarkLinearTrack)
            if(feedback.isNotEmpty())Box(Modifier.padding(horizontal=24.dp,vertical=12.dp)){ToolNotice(feedback,feedbackError)}
            val body=Modifier.weight(1f).fillMaxWidth()
            Column((if(scrollable)body.verticalScroll(rememberScrollState()) else body)
                .padding(horizontal=24.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp),content=footer)
        }
    }
}

@Composable internal fun ToolSection(title:String,description:String?=null,content:@Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=TextHighEmphasis)
            description?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)}
        }
        content()
    }
}

@Composable internal fun ToolNotice(text:String,error:Boolean=false) {
    if(text.isEmpty())return
    Surface(color=if(error)PaceCriticalContainer else SurfaceRecessed,shape=RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(if(error)Icons.Default.ErrorOutline else Icons.Default.Info,contentDescription=null,
                tint=if(error)PaceCriticalText else MintPrimary,modifier=Modifier.size(20.dp))
            Text(text,style=MaterialTheme.typography.bodyMedium,color=if(error)PaceCriticalText else TextMediumEmphasis,modifier=Modifier.weight(1f))
        }
    }
}

@Composable internal fun ToolPrimary(label:String,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
        shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp),
        colors=ButtonDefaults.buttonColors(containerColor=if(danger)PaceCritical else MintPrimary,
            contentColor=MintOnPrimary,
            disabledContainerColor=SurfaceLayer2,disabledContentColor=TextMediumEmphasis)) {
        Text(label,style=MaterialTheme.typography.labelLarge)
    }
}

@Composable internal fun ToolSecondary(label:String,enabled:Boolean=true,onClick:()->Unit) {
    OutlinedButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
        shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp),
        border=BorderStroke(1.dp,TextLowEmphasis),colors=ButtonDefaults.outlinedButtonColors(
            contentColor=MintPrimary,disabledContentColor=TextMediumEmphasis)) {Text(label,style=MaterialTheme.typography.labelLarge)}
}

@Composable fun ToolActionRow(title:String,description:String?=null,icon:ImageVector=Icons.Default.Tune,
    enabled:Boolean=true,onClick:()->Unit) {
    Surface(onClick=onClick,enabled=enabled,color=androidx.compose.ui.graphics.Color.Transparent,
        shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min=56.dp).padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(icon,contentDescription=null,tint=if(enabled)MintPrimary else TextMediumEmphasis,modifier=Modifier.size(22.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleSmall,color=TextHighEmphasis)
                description?.let {Text(it,style=MaterialTheme.typography.bodySmall,color=TextMediumEmphasis)}
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,contentDescription=null,tint=TextMediumEmphasis,modifier=Modifier.size(24.dp))
        }
    }
}

@Composable internal fun ToolHelp(title:String,text:String) {
    var expanded by remember {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(vertical=10.dp),modifier=Modifier.heightIn(min=48.dp).semantics {stateDescription=if(expanded)"Expanded" else "Collapsed"}) {
            Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,contentDescription=null,modifier=Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp));Text(title,modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
        }
        if(expanded)Text(text,style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
    }
}

@Composable internal fun ToolValue(label:String,value:String,emphasize:Boolean=false) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.Top) {
        Text(label,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=TextMediumEmphasis)
        Text(value,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,
            color=if(emphasize)MintPrimary else TextHighEmphasis,fontWeight=FontWeight.SemiBold,
            textAlign=androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable internal fun ToolChoices(options:List<String>,selected:String,enabled:Boolean=true,onSelect:(String)->Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        options.forEach {value->FilterChip(selected=value==selected,onClick={onSelect(value)},enabled=enabled,
            label={Text(value)},leadingIcon=if(value==selected){{Icon(Icons.Default.Check,contentDescription=null,modifier=Modifier.size(16.dp))}}else null,
            modifier=Modifier.heightIn(min=48.dp),border=BorderStroke(1.dp,if(value==selected)MintPrimaryContainer else TextLowEmphasis),colors=FilterChipDefaults.filterChipColors(
                selectedContainerColor=MintPrimaryContainer,selectedLabelColor=MintOnPrimaryContainer,
                selectedLeadingIconColor=MintOnPrimaryContainer,labelColor=TextMediumEmphasis))}
    }
}

@Composable internal fun toolFieldColors()=OutlinedTextFieldDefaults.colors(
    focusedTextColor=TextHighEmphasis,unfocusedTextColor=TextHighEmphasis,
    focusedContainerColor=SurfaceRecessed,unfocusedContainerColor=SurfaceRecessed,
    focusedBorderColor=MintPrimary,unfocusedBorderColor=TextLowEmphasis,
    focusedLabelColor=MintPrimary,unfocusedLabelColor=TextMediumEmphasis,cursorColor=MintPrimary)
