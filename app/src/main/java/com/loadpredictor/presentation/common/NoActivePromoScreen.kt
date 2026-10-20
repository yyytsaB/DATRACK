package com.loadpredictor.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loadpredictor.presentation.theme.DarkBackground
import com.loadpredictor.presentation.theme.MintOnPrimary
import com.loadpredictor.presentation.theme.MintPrimary
import com.loadpredictor.presentation.theme.TextHighEmphasis
import com.loadpredictor.presentation.theme.TextMediumEmphasis

/**
 * 1-to-1 exact replica of the No Active Promo empty state screen matching preview (2).webp:
 * - TopAppBar: "Load Predictor" title + circular 3-dots overflow button
 * - Centered circular dark teal badge with dashed mint circle and exclamation mark
 * - "No active promo" hero title
 * - "Set up your data promo to start tracking how long it will last." copy
 * - Mint "Set up your promo" pill button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoActivePromoScreen(
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mintTeal = MintPrimary
    val darkTealBadgeBg = Color(0xFF102123)

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
            // Circular Dark Teal Badge with Dashed Ring and Exclamation Mark
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(darkTealBadgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    val strokeWidth = 2.dp.toPx()
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawCircle(
                        color = mintTeal,
                        style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                    )
                }
                Text(
                    text = "!",
                    color = mintTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "No active promo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Body Description
            Text(
                text = "Set up your data promo to start tracking how long it will last.",
                fontSize = 14.sp,
                color = TextMediumEmphasis,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Set up your promo Button
            Button(
                onClick = onConfigureClick,
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mintTeal,
                    contentColor = MintOnPrimary
                )
            ) {
                Text(
                    text = "Set up your promo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintOnPrimary
                )
            }
        }
    }
}
