package com.alpefe.fujiptp.ui.home.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.RecipeModel
import com.alpefe.fujiptp.ui.CollectionUi
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.components.SlotPickerDialog
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Danger
import com.alpefe.fujiptp.ui.theme.DustyPink
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface
import java.text.DateFormat
import java.util.Date

/**
 * Inside a collection: the recipes it contains, with create (FAB),
 * multi-select delete and per-recipe actions.
 */
@Composable
fun CollectionScreen(
    viewModel: FujiViewModel,
    collectionId: Long,
    collectionName: String,
    onBack: () -> Unit,
) {
    val recipes by viewModel.recipesInCollectionFlow(collectionId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    var sendRecipe by remember { mutableStateOf<RecipeModel?>(null) }
    var addTo by remember { mutableStateOf<RecipeModel?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val selecting = selection.isNotEmpty()
    val collection = collections.firstOrNull { it.id == collectionId }

    LaunchedEffect(collections) {
        // Nothing to do; collection data flows reactively.
    }

    Box(Modifier.fillMaxSize().background(Canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
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
                        Text(
                            collectionName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "${recipes.size} recipe${if (recipes.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selecting) {
                        IconButton(onClick = { selection = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar selección", tint = InkSoft)
                        }
                    } else {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de colección", tint = InkSoft)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Renombrar") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                    onClick = {
                                        menuOpen = false
                                        showRename = true
                                    },
                                )
                                if (collection?.isDefault != true) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar colección") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                        onClick = {
                                            menuOpen = false
                                            confirmDelete = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (recipes.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Esta colección está vacía.\nToca + para crear tu primera recipe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            items(recipes, key = { it.id }) { recipe ->
                val selected = recipe.id in selection
                CollectionRecipeCard(
                    recipe = recipe,
                    slotLabel = slots
                        .filter { it.recipe?.id == recipe.id }
                        .joinToString { "C${it.index}" },
                    connected = connected,
                    selecting = selecting,
                    selected = selected,
                    onToggleSelect = {
                        selection = if (selected) selection - recipe.id else selection + recipe.id
                    },
                    onOpen = { viewModel.push(Screen.Editor(recipe.id, null)) },
                    onSend = { sendRecipe = recipe },
                    onDuplicate = { viewModel.duplicateRecipe(recipe.id) },
                    onAddToCollection = { addTo = recipe },
                    onDelete = { viewModel.deleteRecipe(recipe.id) },
                )
            }
        }

        // Bulk delete bar.
        AnimatedVisibility(
            visible = selecting,
            enter = slideInVertically(androidx.compose.animation.core.tween(180)) { it } + fadeIn(androidx.compose.animation.core.tween(120)),
            exit = slideOutVertically(androidx.compose.animation.core.tween(150)) { it } + fadeOut(androidx.compose.animation.core.tween(120)),
        ) {
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
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = InkSoft)
                }
                Text(
                    "${selection.size} seleccionada${if (selection.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val all = recipes.map { it.id }.toSet()
                        selection = if (selection == all) emptySet() else all
                    },
                ) {
                    Text(if (selection.size == recipes.size) "Ninguna" else "Todo", color = InkSoft)
                }
                TextButton(onClick = { confirmDelete = true }, enabled = selecting) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(16.dp), tint = Danger)
                    Spacer(Modifier.width(4.dp))
                    Text("Borrar (${selection.size})", color = Danger, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!selecting) {
            FloatingActionButton(
                onClick = { viewModel.push(Screen.Editor(null, null, collectionId = collectionId)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(60.dp),
                shape = CircleShape,
                containerColor = Peach,
                contentColor = PeachDeep,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva recipe", modifier = Modifier.size(26.dp))
            }
        }
    }

    // Rename dialog.
    if (showRename) {
        var name by remember { mutableStateOf(collectionName) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text("Renombrar colección", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.control),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedBorderColor = PeachDeep.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color(0xFF362F28),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "La colección por defecto no se puede eliminar, pero sí renombrar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.renameCollection(collectionId, name.trim())
                        }
                        showRename = false
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Renombrar", color = PeachDeep, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancelar") }
            },
        )
    }

    // Confirm delete collection (only non-default).
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text("Eliminar colección", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "¿Eliminar «$collectionName» y sus recipes de la colección? Las recipes no se borran de la biblioteca.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteCollection(collectionId)
                        onBack()
                    },
                ) {
                    Text("Eliminar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
        )
    }

    // Confirm bulk delete recipes.
    if (selection.isNotEmpty() && !confirmDelete && !showRename) {
        var showBulkConfirm by remember { mutableStateOf(false) }
        if (showBulkConfirm) {
            AlertDialog(
                onDismissRequest = { showBulkConfirm = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(Radius.card),
                title = { Text("Borrar recipes", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Text(
                        "¿Borrar ${selection.size} recipe${if (selection.size == 1) "" else "s"}? Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val ids = selection.toList()
                            selection = emptySet()
                            showBulkConfirm = false
                            viewModel.deleteRecipes(ids)
                        },
                    ) {
                        Text("Borrar", color = Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkConfirm = false }) { Text("Cancelar") }
                },
            )
        }
    }

    addTo?.let { recipe ->
        AddToCollectionDialog(
            recipe = recipe,
            collections = collections,
            onPick = { collectionIdTarget ->
                viewModel.addToCollection(recipe.id, collectionIdTarget)
                addTo = null
            },
            onDismiss = { addTo = null },
        )
    }
    sendRecipe?.let { recipe ->
        SlotPickerDialog(
            title = "Enviar «${recipe.name}» a…",
            slots = slots,
            busy = busy,
            onPick = { slot ->
                viewModel.sendRecipeToSlot(recipe.id, slot)
                sendRecipe = null
            },
            onDismiss = { sendRecipe = null },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CollectionRecipeCard(
    recipe: RecipeModel,
    slotLabel: String,
    connected: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onSend: () -> Unit,
    onDuplicate: () -> Unit,
    onAddToCollection: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .combinedClickable(
                onClick = { if (selecting) onToggleSelect() else onOpen() },
                onLongClick = { if (!selecting) onToggleSelect() },
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DustyPink.copy(alpha = 0.5f) else Surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PeachDeep,
                        checkmarkColor = Color(0xFF171310),
                        uncheckedColor = InkSoft,
                    ),
                )
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = recipe.name.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilmSimulationChip(recipe.filmSimulation)
                    if (slotLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = slotLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = DateFormat.getDateInstance().format(Date(recipe.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!selecting) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = InkSoft)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (connected) {
                            DropdownMenuItem(
                                text = { Text("Enviar a la cámara…") },
                                leadingIcon = { Icon(Icons.Filled.Send, null) },
                                onClick = {
                                    menuOpen = false
                                    onSend()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Añadir a colección…") },
                            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                            onClick = {
                                menuOpen = false
                                onAddToCollection()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                menuOpen = false
                                onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToCollectionDialog(
                        recipe: RecipeModel,
                        collections: List<CollectionUi>,
                        onPick: (Long) -> Unit,
                        onDismiss: () -> Unit,
                        ) {
                        AlertDialog(
                        onDismissRequest = onDismiss,
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(Radius.card),
                        title = { Text("Añadir a colección", style = MaterialTheme.typography.titleLarge) },
                        text = {
                        Column {
                        Text(
                        "«${recipe.name}»",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        collections.filter { !it.isDefault }.forEach { collection ->
                        val tint = Color(collection.colorHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tint.copy(alpha = 0.55f))
                                .clickable { onPick(collection.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Folder, null, Modifier.size(16.dp), tint = Ink.copy(alpha = 0.6f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${collection.name} (${collection.count})",
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
                            TextButton(onClick = onDismiss) { Text("Cancelar") }
                        },
                    )
}