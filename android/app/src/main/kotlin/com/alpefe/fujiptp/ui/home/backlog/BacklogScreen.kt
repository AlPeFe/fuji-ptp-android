package com.alpefe.fujiptp.ui.home.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
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
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.CollectionUi
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Danger
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.PastelGreenDeep
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BacklogScreen(viewModel: FujiViewModel) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val backlog by viewModel.backlog.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<CollectionUi?>(null) }
    var deleting by remember { mutableStateOf<CollectionUi?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    // Multi-select for deletion.
    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val selecting = selection.isNotEmpty()

    // Search filter.
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(collections, searchQuery) {
        if (searchQuery.isBlank()) collections
        else collections.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    // Toggle selection mode: long-press a card to start selecting it.
    fun toggle(id: Long) {
        selection = if (id in selection) selection - id else selection + id
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Biblioteca",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (collections.isEmpty()) {
                                "Crea tu primera colección para empezar"
                            } else if (selecting) {
                                "${selection.size} seleccionada${if (selection.size == 1) "" else "s"}"
                            } else {
                                "${collections.size} colección${if (collections.size == 1) "" else "es"}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selecting) PeachDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Empty the whole library (with confirmation).
                    IconButton(
                        onClick = { confirmClearAll = true },
                        enabled = collections.isNotEmpty() || backlog.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "Vaciar biblioteca",
                            tint = InkSoft.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            if (collections.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aún no tienes colecciones.\nToca + para crear la primera.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            } else {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar colecciones…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.control),
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = InkSoft)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = InkSoft)
                                }
                            }
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = PeachDeep.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color(0xFFEDE7E0),
                        ),
                    )
                }
            }
            if (searchQuery.isNotBlank() && filtered.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No hay colecciones con «${searchQuery.trim()}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            items(filtered, key = { it.id }) { collection ->
                val selected = collection.id in selection
                CollectionCard(
                    collection = collection,
                    selected = selected,
                    onOpen = {
                        if (selecting) toggle(collection.id)
                        else viewModel.push(Screen.Collection(collection.id, collection.name))
                    },
                    onLongPress = { toggle(collection.id) },
                    onMenu = { menuFor = collection },
                )
            }
        }

        // Bottom action bar when selecting.
        AnimatedVisibility(
            visible = selecting,
            enter = slideInVertically(tween(180)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(160)) { it } + fadeOut(tween(100)),
        ) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selection = emptySet() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = InkSoft)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        selection = if (selection.size == collections.size) emptySet()
                        else collections.map { it.id }.toSet()
                    },
                ) {
                    Text(if (selection.size == collections.size) "Ninguna" else "Todo", color = Ink)
                }
                Spacer(Modifier.weight(1f))
                val deletable = collections.filter { it.id in selection && !it.isDefault }
                TextButton(
                    onClick = {
                        val ids = collections.filter { it.id in selection }.map { it.id }
                        if (ids.isNotEmpty()) {
                            viewModel.deleteCollections(ids)
                        }
                        selection = emptySet()
                    },
                    enabled = deletable.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = Danger)
                    Spacer(Modifier.width(4.dp))
                    Text("Borrar (${deletable.size})", color = Danger)
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp),
            shape = CircleShape,
            containerColor = Peach,
            contentColor = PeachDeep,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nueva colección", modifier = Modifier.size(26.dp))
        }
    }

    if (showCreate) {
        CollectionNameDialog(
            title = "Nueva colección",
            initialName = "",
            onConfirm = { viewModel.createCollection(it, pickCollectionColor(collections.size)) },
            onDismiss = { showCreate = false },
        )
    }

    menuFor?.let { collection ->
        CollectionMenuDialog(
            collection = collection,
            onDismiss = { menuFor = null },
            onRename = { menuFor = null },
            onDelete = {
                menuFor = null
                deleting = collection
            },
        )
    }

    deleting?.let { collection ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar colección") },
            text = {
                Text(
                    if (collection.isDefault) {
                        "La colección por defecto no se puede eliminar."
                    } else {
                        "¿Borrar «${collection.name}» y todas sus recipes? Esta acción no se puede deshacer."
                    }
                )
            },
            confirmButton = {
                if (!collection.isDefault) {
                    TextButton(
                        onClick = {
                            viewModel.deleteCollection(collection.id)
                            deleting = null
                        },
                    ) {
                        Text("Borrar", color = Danger)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancelar") }
            },
        )
    }

    // Confirm emptying the whole library.
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Vaciar biblioteca") },
            text = {
                Text(
                    "Se borrarán todas tus recipes y colecciones (excepto la por defecto). " +
                        "Podrás volver a importarlas desde Discover. ¿Continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        viewModel.clearLibrary()
                    },
                ) {
                    Text("Vaciar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionCard(
    collection: CollectionUi,
    selected: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onMenu: () -> Unit,
) {
    val tint = Color(collection.colorHex)
    val deep = Color(collectionDeepTints[collection.id.toInt() % collectionDeepTints.size])
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Peach.copy(alpha = 0.55f) else Surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = deep,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${collection.count} recipe${if (collection.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                if (collection.isDefault) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Por defecto",
                        style = MaterialTheme.typography.labelSmall,
                        color = PastelGreenDeep,
                    )
                }
            }
            // Selection checkbox overlay (top-right).
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Seleccionada",
                        tint = PeachDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionMenuDialog(
    collection: CollectionUi,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(collection.name) },
        text = {
            Column {
                TextButton(onClick = onRename) { Text("Renombrar") }
                TextButton(onClick = onDelete) {
                    Text(
                        if (collection.isDefault) "La colección por defecto no se puede eliminar"
                        else "Eliminar colección",
                        color = if (collection.isDefault) InkSoft else Danger,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

private fun pickCollectionColor(index: Int): Long {
    val palette = listOf(
        0xFFF9E0E4, 0xFFE4F0E5, 0xFFE8EEF7, 0xFFF3E8F5,
        0xFFF7EDD9, 0xFFE8F0E8, 0xFFF9E8E0, 0xFFEDE7F5,
    )
    return palette[index % palette.size]
}

private val collectionDeepTints = listOf(
    0xFFD982A0, 0xFF7FAF8B, 0xFF8FA8C9, 0xFFB48AC9,
    0xFFC9A24B, 0xFF7FB08A, 0xFFC98A6B, 0xFF9B8AC9,
)

@Composable
private fun CollectionNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.card),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.control),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface,
                    focusedBorderColor = PeachDeep.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color(0xFFEDE7E0),
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                    onDismiss()
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Crear", color = PeachDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
