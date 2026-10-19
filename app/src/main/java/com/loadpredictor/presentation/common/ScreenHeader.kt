package com.loadpredictor.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loadpredictor.presentation.theme.*

/** Natural height keeps titles and supporting text visible at larger Android font settings. */
@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Surface(color = DarkBackground) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextHighEmphasis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMediumEmphasis)
        }
    }
}
