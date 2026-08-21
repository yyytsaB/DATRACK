package com.loadpredictor.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Plain callout text banner displayed at the bottom when an active promo is ≤ 48 hours old.
 * De-emphasized without bulky card frames.
 */
@Composable
fun RegistrationExplainerCard(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = TextLowEmphasis,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Text(
            text = "Registration window active: Showing data tracked since promo registration. Prior device traffic is excluded.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMediumEmphasis,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
