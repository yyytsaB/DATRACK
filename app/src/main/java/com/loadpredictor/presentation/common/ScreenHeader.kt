package com.loadpredictor.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loadpredictor.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenHeader(title: String, subtitle: String) {
    TopAppBar(title = {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextHighEmphasis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMediumEmphasis)
        }
    }, expandedHeight = 104.dp, windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground))
}
