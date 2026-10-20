package com.loadpredictor.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextLowEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * 1-to-1 exact replica of the Usage Access permission screen matching preview (1).webp:
 * - TopAppBar: "Load Predictor" title + circular 3-dots overflow button
 * - Centered circular dark crimson badge with salmon warning icon
 * - "Usage Access needed" hero title
 * - Informative privacy explanation body
 * - Salmon/coral "Grant Access" primary button
 * - "We can't read messages, calls, or app content." reassurance caption
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsagePermissionRequiredCard(
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coralPrimary = Color(0xFFFF6F6F)
    val crimsonBadgeBg = Color(0xFF2B171C)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DATRACK",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = 1.sp,
                        color = TextHighEmphasis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextHighEmphasis
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Dark Crimson Warning Badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(crimsonBadgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Warning",
                    tint = coralPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Usage Access needed",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Body Description
            Text(
                text = "Datrack reads your network usage from Android's Usage Access — no personal data leaves your phone. This is the only way it can track your burn rate.",
                fontSize = 14.sp,
                color = TextMediumEmphasis,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Grant Access Button
            Button(
                onClick = onGrantPermissionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = coralPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Grant Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reassurance Caption
            Text(
                text = "We can't read messages, calls, or app content.",
                fontSize = 12.sp,
                color = TextLowEmphasis,
                textAlign = TextAlign.Center
            )
        }
    }
}
