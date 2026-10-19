package com.loadpredictor.presentation.navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable
fun LoadPredictorBottomBar(currentDestination: NavDestination, onNavigateToDestination: (NavDestination) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        NavDestination.entries.forEach { destination -> NavigationBarItem(selected = destination == currentDestination,
            onClick = { onNavigateToDestination(destination) },
            icon = { Icon(destination.icon, destination.contentDescription) }, label = { Text(destination.label) }) }
    }
}
