package com.alpefe.fujiptp.ui.home.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.LavenderDeep
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep

/**
 * Discover tab — placeholder. Soon it will host curated collections and
 * community recipes to bring into your library.
 */
@Composable
fun DiscoverScreen() {
    Box(Modifier.fillMaxSize().padding(PaddingValues(20.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Peach.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Explore,
                    contentDescription = null,
                    tint = PeachDeep,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Discover",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Pronto podrás descubrir colecciones y recipes\npara llevar a tu biblioteca.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Stay tuned ✨",
                style = MaterialTheme.typography.labelMedium,
                color = LavenderDeep,
            )
        }
    }
}
