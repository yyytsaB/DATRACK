package com.loadpredictor.presentation.navigation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoadPredictorBottomBar(currentDestination: NavDestination, onNavigateToDestination: (NavDestination) -> Unit, modifier: Modifier = Modifier) {
    if (LocalDensity.current.fontScale >= 1.5f) {
        Surface(modifier, tonalElevation = 3.dp) {
            FlowRow(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavDestination.entries.forEach { destination -> FilterChip(selected = destination == currentDestination,
                    onClick = { onNavigateToDestination(destination) }, label = { Text(destination.label) }) }
            }
        }
    } else NavigationBar(modifier = modifier) {
        NavDestination.entries.forEach { destination -> NavigationBarItem(selected = destination == currentDestination,
            onClick = { onNavigateToDestination(destination) },
            icon = { Icon(destination.icon, destination.contentDescription) }, label = { Text(destination.label) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer)) }
    }
}
