package com.alpefe.fujiptp.ui.home.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.DiscoverData
import com.alpefe.fujiptp.data.FilmSimulation
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface

/**
 * Detail view of a Discover recipe: shows the recipe info and lets the
 * user import it into one of their collections.
 */
@Composable
fun DiscoverRecipeDetailScreen(
    viewModel: FujiViewModel,
    collectionId: String,
    recipeId: String,
    onBack: () -> Unit,
) {
    val collection = DiscoverData.byId(collectionId)
    val recipe = collection?.recipes?.firstOrNull { it.name == recipeId }
    if (recipe == null) {
        onBack()
        return
    }
    val film = FilmSimulation.entries.firstOrNull { it.name == recipe.filmSimulation }
        ?: FilmSimulation.ClassicChrome
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var showTargets by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Ink)
                    }
                    Text(
                        "Detalle de recipe",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.card))
                        .background(Surface)
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(collection.colorHex).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(collection.logo, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                recipe.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${collection.name} · colección pública",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    FilmSimulationChip(film)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (recipe.source.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Link,
                                null,
                                Modifier.size(12.dp),
                                tint = InkSoft,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Fuente: ${recipe.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.card))
                        .background(Surface)
                        .padding(20.dp),
                ) {
                    Text(
                        "Valores de la recipe",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailRow("Simulación", film.label)
                    DetailRow("Highlight", "+1")
                    DetailRow("Shadow", "-1")
                    DetailRow("Color", "+2")
                    DetailRow("Grain", "Strong · Small")
                    DetailRow("DR", "DR200")
                }
            }
            item {
                Button(
                    onClick = { showTargets = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeachDeep,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Importar a mi biblioteca")
                }
            }
        }
    }

    if (showTargets) {
        AlertDialog(
            onDismissRequest = { showTargets = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text("Importar recipe", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "«${recipe.name}» · ${film.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "¿A qué colección quieres importarla?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    collections.forEach { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(collection.colorHex).copy(alpha = 0.5f))
                                .clickable {
                                    viewModel.importDiscoverRecipe(recipe.name, recipe.filmSimulation, collection.id)
                                    showTargets = false
                                    onBack()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Folder, null, Modifier.size(16.dp), tint = Ink.copy(alpha = 0.6f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${collection.name} (${collection.count})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargets = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
