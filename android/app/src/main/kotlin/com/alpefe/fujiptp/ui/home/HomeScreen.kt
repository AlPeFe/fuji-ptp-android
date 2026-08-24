package com.alpefe.fujiptp.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.RecipeModel
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.SlotUi
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.components.SlotPickerDialog

@Composable
fun HomeScreen(viewModel: FujiViewModel) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val devicePresent by viewModel.devicePresent.collectAsStateWithLifecycle()
    val cameraLabel by viewModel.cameraLabel.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val backlog by viewModel.backlog.collectAsStateWithLifecycle()

    var assignTarget by remember { mutableStateOf<Int?>(null) }
    var sendRecipe by remember { mutableStateOf<RecipeModel?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            ConnectionCard(
                connected = connected,
                devicePresent = devicePresent,
                cameraLabel = cameraLabel,
                busy = busy,
                onConnect = { viewModel.connectRequested() },
                onDisconnect = { viewModel.disconnect() },
                onRead = { viewModel.readFromCamera() },
            )
        }
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    "Recetas activas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Los 7 slots de la cámara (C1–C7)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(slots, key = { it.index }) { slot ->
            SlotCard(
                slot = slot,
                connected = connected,
                onOpen = { viewModel.push(Screen.Editor(slot.recipe?.id, slot.index)) },
                onSend = { recipe -> viewModel.sendToSlot(slot.index, recipe) },
                onAssign = { assignTarget = slot.index },
                onClear = { viewModel.clearSlot(slot.index) },
            )
        }
    }

    // Assign dialog: pick a backlog recipe for the chosen slot.
    assignTarget?.let { slot ->
        AssignRecipeDialog(
            backlog = backlog,
            onPick = { recipe ->
                viewModel.assignToSlot(slot, recipe.id)
                assignTarget = null
            },
            onDismiss = { assignTarget = null },
        )
    }

    // Send dialog: pick a camera slot for a recipe (from the backlog screen).
    sendRecipe?.let { recipe ->
        SlotPickerDialog(
            title = "Enviar «${recipe.name}» a…",
            slots = slots,
            busy = busy,
            onPick = { slot ->
                viewModel.sendToSlot(slot, recipe)
                sendRecipe = null
            },
            onDismiss = { sendRecipe = null },
        )
    }
}

@Composable
private fun ConnectionCard(
    connected: Boolean,
    devicePresent: Boolean,
    cameraLabel: String?,
    busy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRead: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (connected) Color(0xFF4C9A5A)
                            else if (devicePresent) Color(0xFFD9A441)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        connected -> "Cámara conectada"
                        devicePresent -> "Cámara detectada"
                        else -> "Sin cámara"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            cameraLabel?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            when {
                connected -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onRead,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Leer C1–C7")
                        }
                        OutlinedButton(
                            onClick = onDisconnect,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Desconectar")
                        }
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = onConnect,
                        enabled = devicePresent && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Conectar cámara")
                    }
                    if (!devicePresent) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Conecta la cámara por USB en modo «RAW CONV./BACKUP RESTORE».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: SlotUi,
    connected: Boolean,
    onOpen: () -> Unit,
    onSend: (RecipeModel) -> Unit,
    onAssign: () -> Unit,
    onClear: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.recipe != null) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Column(Modifier.padding(end = 8.dp)) {
                Text(
                    text = "C${slot.index}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                val recipe = slot.recipe
                if (recipe != null) {
                    Text(
                        text = recipe.name.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    FilmSimulationChip(recipe.filmSimulation)
                } else {
                    Text(
                        text = "Vacío",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Toca para crear",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de C${slot.index}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (connected) {
                        DropdownMenuItem(
                            text = { Text("Enviar a la cámara") },
                            leadingIcon = { Icon(Icons.Filled.Send, null) },
                            enabled = slot.recipe != null,
                            onClick = {
                                menuOpen = false
                                slot.recipe?.let(onSend)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Asignar recipe…") },
                        leadingIcon = { Icon(Icons.Filled.SwapHoriz, null) },
                        onClick = {
                            menuOpen = false
                            onAssign()
                        },
                    )
                    if (slot.recipe != null) {
                        DropdownMenuItem(
                            text = { Text("Vaciar slot") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                menuOpen = false
                                onClear()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignRecipeDialog(
    backlog: List<RecipeModel>,
    onPick: (RecipeModel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar recipe") },
        text = {
            if (backlog.isEmpty()) {
                Text("Aún no hay recipes en la biblioteca. Crea una con el botón «+».")
            } else {
                Column {
                    Text(
                        "Toca una recipe para asignarla:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.height(300.dp)) {
                        lazyListItems(backlog) { recipe ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(recipe) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        recipe.name.ifBlank { "Sin nombre" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    FilmSimulationChip(recipe.filmSimulation)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
