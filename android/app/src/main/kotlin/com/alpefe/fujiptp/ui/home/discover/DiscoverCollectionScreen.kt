package com.alpefe.fujiptp.ui.home.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.DiscoverData
import com.alpefe.fujiptp.data.DiscoverRecipe
import com.alpefe.fujiptp.data.FilmSimulation
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.DustyPink
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface

/**
 * Inside a predefined Discover collection. Read-only: recipes here cannot be
 * edited or deleted; the user can import one, several (multi-select) or the
 * whole collection into their own collections.
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
    var importBatch by remember { mutableStateOf<Boolean>(false) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    val uriHandler = LocalUriHandler.current

    val selecting = selection.isNotEmpty()

    Box(Modifier.fillMaxSize().background(Canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
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
                    if (selecting) {
                        IconButton(onClick = { selection = emptySet() }) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Cancelar selección",
                                tint = InkSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Colección pública · no editable. Importa las recipes que quieras a tu biblioteca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                if (collection.source.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri(collection.source) }
                            .padding(vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            null,
                            Modifier.size(12.dp),
                            tint = PeachDeep,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Fuente: ${collection.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PeachDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item {
                // Integrated action pill: enter/exit selection mode and
                // import the whole collection.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.control))
                        .background(Surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            if (selecting) selection = emptySet()
                            else selection = collection.recipes.map { it.name }.toSet()
                        },
                    ) {
                        Icon(
                            if (selecting) Icons.Filled.RadioButtonUnchecked
                            else Icons.Filled.CheckCircle,
                            null,
                            Modifier.size(16.dp),
                            tint = PeachDeep,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selecting) "Cancelar" else "Seleccionar",
                            color = Ink,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { importBatch = true },
                        enabled = selection.isNotEmpty() || !selecting,
                        shape = RoundedCornerShape(Radius.control),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = Peach,
                            contentColor = PeachDeep,
                        ),
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selection.isNotEmpty()) "Importar (${selection.size})"
                            else "Importar todo",
                        )
                    }
                }
            }
            items(collection.recipes) { recipe ->
                val selected = recipe.name in selection
                DiscoverRecipeCard(
                    recipe = recipe,
                    selecting = selecting,
                    selected = selected,
                    onToggleSelect = {
                        selection = if (selected) selection - recipe.name else selection + recipe.name
                    },
                    onClick = {
                        if (selecting) {
                            selection = if (selected) selection - recipe.name else selection + recipe.name
                        } else {
                            viewModel.push(Screen.DiscoverRecipeDetail(collection.id, recipe.name))
                        }
                    },
                    onImport = { importTarget = recipe },
                )
            }
        }

        // Bulk import bar.
        AnimatedVisibility(
            visible = selecting,
            enter = slideInVertically(androidx.compose.animation.core.tween(180)) { it } + fadeIn(androidx.compose.animation.core.tween(120)),
            exit = slideOutVertically(androidx.compose.animation.core.tween(150)) { it } + fadeOut(androidx.compose.animation.core.tween(120)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 92.dp)
                        .clip(RoundedCornerShape(Radius.control))
                        .background(Surface)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selection = emptySet() }) {
                        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = "Cancelar", tint = InkSoft)
                    }
                    Text(
                        "${selection.size} seleccionada${if (selection.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { importBatch = true }, enabled = selecting) {
                        Icon(Icons.Filled.Download, null, Modifier.size(16.dp), tint = PeachDeep)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Importar (${selection.size})",
                            color = PeachDeep,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // Target collection dialog for the selected recipes.
    if (importBatch || importTarget != null) {
        val recipesToImport: List<DiscoverRecipe> = when {
            importTarget != null -> listOf(importTarget!!)
            selection.isNotEmpty() -> collection.recipes.filter { it.name in selection }
            else -> collection.recipes // "Importar todo"
        }
        AlertDialog(
            onDismissRequest = {
                importBatch = false
                importTarget = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text("Importar recipes", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "${recipesToImport.size} recipe${if (recipesToImport.size == 1) "" else "s"} seleccionada${if (recipesToImport.size == 1) "" else "s"} · ¿A qué colección?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(12.dp))
                    collections.forEach { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(collection.colorHex).copy(alpha = 0.5f))
                                .clickable {
                                    val pairs = recipesToImport.map { it.name to it.filmSimulation }
                                    viewModel.importDiscoverRecipes(pairs, collection.id)
                                    importBatch = false
                                    importTarget = null
                                    selection = emptySet()
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
                TextButton(onClick = {
                    importBatch = false
                    importTarget = null
                }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DiscoverRecipeCard(
    recipe: DiscoverRecipe,
    selecting: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onImport: () -> Unit,
) {
    val film = FilmSimulation.entries.firstOrNull { it.name == recipe.filmSimulation }
        ?: FilmSimulation.ClassicChrome
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DustyPink.copy(alpha = 0.55f) else Surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(Radius.card),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecting) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PeachDeep,
                            checkmarkColor = Color.White,
                            uncheckedColor = InkSoft,
                        ),
                    )
                    Spacer(Modifier.width(4.dp))
                }
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
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = Peach,
                    contentColor = PeachDeep,
                ),
            ) {
                Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Importar a mi biblioteca")
            }
            if (recipe.source.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { uriHandler.openUri(recipe.source) }
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        Icons.Filled.Link,
                        null,
                        Modifier.size(12.dp),
                        tint = PeachDeep,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Fuente: ${recipe.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PeachDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
