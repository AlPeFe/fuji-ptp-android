package com.alpefe.fujiptp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alpefe.fujiptp.Diagnostics
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import kotlinx.coroutines.delay

/**
 * On-device diagnostics: shows the native/USB log buffer captured by
 * [Diagnostics], so camera I/O can be debugged without a PC.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    var snapshot by remember { mutableStateOf(Diagnostics.snapshot()) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        delay(700)
        snapshot = Diagnostics.snapshot()
        tick++
    }

    Column(Modifier.fillMaxSize().background(Canvas)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink)
            }
            Text(
                "Diagnóstico",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { Diagnostics.clear(); snapshot = Diagnostics.snapshot() }) {
                Icon(Icons.Filled.Delete, contentDescription = "Limpiar", tint = InkSoft)
            }
        }
        Text(
            "Conecta la cámara, envía recipes y mira los TX/RX USB aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(snapshot.reversed()) { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (line.contains("NATIVE")) Ink else InkSoft,
                )
            }
        }
        TextButton(
            onClick = { Diagnostics.clear(); snapshot = Diagnostics.snapshot() },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
        ) {
            Text("Limpiar log")
        }
    }
}
