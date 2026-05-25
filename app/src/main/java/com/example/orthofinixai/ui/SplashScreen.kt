package com.example.orthofinixai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orthofinixai.R
import com.example.orthofinixai.ui.theme.BrandGreen
import com.example.orthofinixai.ui.theme.BrandNavy
import com.example.orthofinixai.ui.theme.BrandGray
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoggedIn: Boolean = false,
    onTimeout: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    LaunchedEffect(Unit) {
        for (i in 1..20) {
            delay(80)
            progress = i / 20f
        }
        delay(400)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFF0F7FF), Color(0xFFE8F5E9))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.orthofinix_logo),
                contentDescription = "OrthofinixAI Logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(3.2f)
                    .alpha(pulse)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "OrthofinixAI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )

            Text(
                text = stringResource(R.string.splash_tagline),
                fontSize = 14.sp,
                color = BrandGray,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp),
                color = BrandGreen,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    progress < 0.3f -> "Initializing clinical AI engine..."
                    progress < 0.6f -> "Loading orthodontic models..."
                    progress < 0.9f -> "Preparing analysis pipeline..."
                    else -> "Ready"
                },
                fontSize = 13.sp,
                color = BrandGray
            )
        }
    }
}
