package com.loadpredictor.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destinations for the 5-tab bottom navigation bar.
 */
enum class NavDestination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME(
        label = "Home",
        icon = Icons.Default.Home,
        contentDescription = "Home Dashboard"
    ),
    PROMOS(
        label = "Promos",
        icon = Icons.Default.SimCard,
        contentDescription = "Manage Promos"
    ),
    HISTORY(
        label = "History",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        contentDescription = "Usage History"
    ),
    ALERTS(
        label = "Alerts",
        icon = Icons.Default.Notifications,
        contentDescription = "Burn Alerts"
    ),
    WIDGETS(
        label = "Widgets",
        icon = Icons.Default.Widgets,
        contentDescription = "Widget Gallery"
    )
}
