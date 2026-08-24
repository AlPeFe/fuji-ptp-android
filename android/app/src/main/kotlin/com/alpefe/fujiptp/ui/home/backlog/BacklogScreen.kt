package com.alpefe.fujiptp.ui.home.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.alpefe.fujiptp.ui.components.filmTint
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Danger
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface
import java.text.DateFormat
import java.util.Date

// Pastel palette for collections.
val collectionTints = listOf(
    0xFF463A66, // lavender (dark)
    0xFF35445C, // soft blue
    0xFF35453A, // pastel green
    0xFF5A3826, // peach
    0xFF4E442C, // soft yellow
    0xFF55303C, // dusty pink
)
val collectionDeepTints = listOf(
    0xFFA99BE8, 0xFF8FB4E8, 0xFF8FC49C, 0xFFF0A878, 0xFFE0BC6E, 0xFFE89BB1,
)

@Composable
fun BacklogScreen(viewModel: FujiViewModel) {
    val backlog by viewModel.libraryRecipes.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var sendRecipe by remember { mutableStateOf<RecipeModel?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<CollectionUi?>(null) }
    var addTo by remember { mutableStateOf<RecipeModel?>(null) }
    var collectionMenu by remember { mutableStateOf<CollectionUi?>(null) }
    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val selecting = selection.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    if (selecting) {
                        // Contextual header while selecting.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${selection.size} seleccionada${if (selection.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { selection = emptySet() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancelar selección",
                                    tint = InkSoft,
                                )
                            }
                        }
                    } else {
                        Text(
                            "Biblioteca",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (backlog.isEmpty()) {
                                "Tus recetas guardadas aparecerán aquí"
                            } else {
                                "${backlog.size} recipe${if (backlog.size == 1) "" else "s"} en esta colección"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                CollectionChips(
                    collections = collections,
                    onSelect = {
                        selection = emptySet()
                        viewModel.selectCollection(it.id)
                    },
                    onCreate = { showCreate = true },
                )
            }
            if (backlog.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Esta colección está vacía.\nAñade recipes desde «…» o crea una nueva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            items(backlog, key = { it.id }) { recipe ->
                val selected = recipe.id in selection
                BacklogCard(
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

        // Delete action bar (bottom, above the nav bar) while selecting.
        AnimatedVisibility(
            visible = selecting,
            enter = slideInVertically(tween(180)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(120)),
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
                        val all = backlog.map { it.id }.toSet()
                        selection = if (selection == all) emptySet() else all
                    },
                ) {
                    Text(
                        if (selection.size == backlog.size) "Ninguna" else "Todo",
                        color = InkSoft,
                    )
                }
                TextButton(
                    onClick = { confirmDelete = true },
                    enabled = selecting,
                ) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(16.dp), tint = Danger)
                    Spacer(Modifier.width(4.dp))
                    Text("Borrar (${selection.size})", color = Danger, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!selecting) {
            Box(Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                var fabMenu by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = { fabMenu = true },
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    containerColor = Peach,
                    contentColor = PeachDeep,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Crear", modifier = Modifier.size(26.dp))
                }
                DropdownMenu(
                    expanded = fabMenu,
                    onDismissRequest = { fabMenu = false },
                    shape = RoundedCornerShape(18.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("Nueva recipe") },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = {
                            fabMenu = false
                            viewModel.push(Screen.Editor(null, null))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Nueva colección") },
                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                        onClick = {
                            fabMenu = false
                            showCreate = true
                        },
                    )
                }
            }
        }
    }

    // Confirm delete dialog.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
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
                        confirmDelete = false
                        viewModel.deleteRecipes(ids)
                    },
                ) {
                    Text("Borrar", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
        )
    }

    when {
        showCreate -> CollectionNameDialog(
            title = "Nueva colección",
            initialName = "",
            onConfirm = { viewModel.createCollection(it, pickCollectionColor(collections.size)) },
            onDismiss = { showCreate = false },
        )
        renaming != null -> CollectionNameDialog(
            title = "Renombrar colección",
            initialName = renaming!!.name,
            onConfirm = { viewModel.renameCollection(renaming!!.id, it) },
            onDismiss = { renaming = null },
        )
    }

    collectionMenu?.let { collection ->
        AlertDialog(
            onDismissRequest = { collectionMenu = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text(collection.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    if (!collection.isDefault) {
                        TextButton(
                            onClick = {
                                collectionMenu = null
                                renaming = collection
                            },
                        ) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Renombrar")
                        }
                    }
                    TextButton(
                        onClick = {
                            collectionMenu = null
                            viewModel.deleteCollection(collection.id)
                        },
                        enabled = !collection.isDefault,
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (collection.isDefault) "«Todas» no se puede eliminar"
                            else "Eliminar colección"
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { collectionMenu = null }) { Text("Cerrar") }
            },
        )
    }

    addTo?.let { recipe ->
        AddToCollectionDialog(
            recipe = recipe,
            collections = collections,
            onPick = { collectionId ->
                viewModel.addToCollection(recipe.id, collectionId)
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

private fun pickCollectionColor(index: Int): Long =
    collectionTints[index % collectionTints.size]

@Composable
private fun CollectionChips(
    collections: List<CollectionUi>,
    onSelect: (CollectionUi) -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        collections.forEachIndexed { index, collection ->
            val tint = Color(collection.colorHex)
            val deep = Color(collectionDeepTints[index % collectionDeepTints.size])
            Box(
                Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(tint.copy(alpha = 0.7f))
                    .clickable { onSelect(collection) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Folder,
                        null,
                        Modifier.size(14.dp),
                        tint = deep,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${collection.name} (${collection.count})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(Peach.copy(alpha = 0.6f))
                .clickable(onClick = onCreate)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Icon(
                Icons.Filled.CreateNewFolder,
                null,
                Modifier.size(16.dp),
                tint = PeachDeep,
            )
        }
    }
}

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
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()); onDismiss() },
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BacklogCard(
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
                onClick = {
                    if (selecting) onToggleSelect() else onOpen()
                },
                onLongClick = {
                    if (!selecting) onToggleSelect()
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                com.alpefe.fujiptp.ui.theme.DustyPink.copy(alpha = 0.5f)
            } else {
                Surface
            },
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
