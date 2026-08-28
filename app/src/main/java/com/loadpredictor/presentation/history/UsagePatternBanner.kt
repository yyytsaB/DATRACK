package com.loadpredictor.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.domain.model.PatternType
import com.loadpredictor.domain.model.UsagePatternInsight
import com.loadpredictor.presentation.theme.DarkOutlineVariant
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.SurfaceLayer1
import com.loadpredictor.presentation.theme.SurfaceRecessed
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * Clean behavioral insight banner comparing weekday vs weekend data consumption patterns.
 *
 * Automatically stays hidden when [patternInsight] is [UsagePatternInsight.InsufficientData].
 */
@Composable
fun UsagePatternBanner(
    patternInsight: UsagePatternInsight,
    modifier: Modifier = Modifier
) {
    val pattern = patternInsight as? UsagePatternInsight.Pattern ?: return

    Surface(
        color = SurfaceRecessed,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkOutlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = SurfaceLayer1,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Usage Pattern",
                    tint = MintPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Weekly Pattern",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary,
                    fontSize = 11.sp
                )
                Text(
                    text = pattern.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHighEmphasis,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
