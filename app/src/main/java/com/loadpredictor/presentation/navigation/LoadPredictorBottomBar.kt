package com.loadpredictor.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.DarkOutline
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.MintPrimaryContainer
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis

@Composable
fun LoadPredictorBottomBar(
    currentDestination: NavDestination,
    onNavigateToDestination: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = DarkBackground,
            contentColor = TextHighEmphasis,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavDestination.values().forEach { destination ->
                val isSelected = currentDestination == destination
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigateToDestination(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.contentDescription,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = MintPrimary,
                        unselectedIconColor = TextLowEmphasis,
                        unselectedTextColor = TextLowEmphasis,
                        indicatorColor = MintPrimaryContainer.copy(alpha = 0.4f)
                    )
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = DarkOutline
        )
    }
}
