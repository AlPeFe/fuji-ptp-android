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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.DiscoverData
import com.alpefe.fujiptp.data.DiscoverRecipe
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
 * Inside a predefined Discover collection. Read-only: recipes here cannot be
 * edited or deleted; the user can only import one (or all) into their own
 * collections.
 */
@Composable
fun DiscoverCollectionScreen(
    viewModel: FujiViewModel,
    collectionId: String,
    collectionName: String,
    onBack: () -> Unit,
) {
    val collection = DiscoverData.byId(collectionId)
    if (collection == null) {
        onBack()
        return
    }
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var importTarget by remember { mutableStateOf<DiscoverRecipe?>(null) }

    Box(Modifier.fillMaxSize().background(Canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(collection.colorHex).copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(collection.logo, style = MaterialTheme.typography.bodyLarge)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    collection.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    collection.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Colección pública · no editable. Importa las recipes que quieras a tu biblioteca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            items(collection.recipes) { recipe ->
                DiscoverRecipeCard(
                    recipe = recipe,
                    onImport = { importTarget = recipe },
                )
            }
        }
    }

    importTarget?.let { recipe ->
        val film = FilmSimulation.entries.firstOrNull { it.name == recipe.filmSimulation }
            ?: FilmSimulation.ClassicChrome
        AlertDialog(
            onDismissRequest = { importTarget = null },
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
                                    importTarget = null
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
                TextButton(onClick = { importTarget = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DiscoverRecipeCard(
    recipe: DiscoverRecipe,
    onImport: () -> Unit,
) {
    val film = FilmSimulation.entries.firstOrNull { it.name == recipe.filmSimulation }
        ?: FilmSimulation.ClassicChrome
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(Radius.card),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    FilmSimulationChip(film)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                recipe.description,
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.control),
            ) {
                Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Importar a mi biblioteca")
            }
        }
    }
}
