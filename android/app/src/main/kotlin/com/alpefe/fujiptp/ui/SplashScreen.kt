package com.alpefe.fujiptp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import kotlinx.coroutines.delay

/**
 * Brief branded splash: the app logo scales+fades in, holds a moment, then
 * fades out to reveal the app.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        visible = false
        delay(350)
        onFinished()
    }
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(320)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Canvas),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(600),
                    label = "splashScale",
                )
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Peach.copy(alpha = 0.6f))
                        .scale(scale),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎞️", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Fuji Recipes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tus recetas, en orden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            }
        }
    }
}
