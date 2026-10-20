package com.loadpredictor.presentation.navigation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import com.loadpredictor.presentation.theme.*

@Composable
fun LoadPredictorBottomBar(currentDestination:NavDestination,onNavigateToDestination:(NavDestination)->Unit,modifier:Modifier=Modifier) {
    Column(modifier) {
        HorizontalDivider(color=DarkOutlineVariant)
        NavigationBar(containerColor=DarkBackground,tonalElevation=0.dp) {
            NavDestination.entries.forEach { dest ->
                val selected=dest==currentDestination
                val icon=when(dest) {NavDestination.HOME->Icons.Outlined.Home;NavDestination.PROMOS->Icons.AutoMirrored.Outlined.FormatListBulleted;NavDestination.HISTORY->Icons.AutoMirrored.Outlined.ShowChart;NavDestination.ALERTS->Icons.Outlined.Notifications;NavDestination.WIDGETS->Icons.Outlined.GridView}
                NavigationBarItem(selected=selected,onClick={onNavigateToDestination(dest)},
                    icon={Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.width(28.dp).height(3.dp).background(if(selected) MintPrimary else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(50)))
                        Icon(icon,contentDescription=dest.contentDescription,modifier=Modifier.size(22.dp))
                    }},label={Text(dest.label,style=MaterialTheme.typography.labelSmall.copy(fontSize=11.sp))},
                    colors=NavigationBarItemDefaults.colors(selectedIconColor=MintPrimary,selectedTextColor=MintPrimary,unselectedIconColor=TextLowEmphasis,unselectedTextColor=TextLowEmphasis,indicatorColor=androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}
