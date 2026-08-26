package com.alpefe.fujiptp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.alpefe.fujiptp.ui.components.filmTint
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.PastelGreen
import com.alpefe.fujiptp.ui.theme.PastelGreenDeep
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.SoftBlue
import com.alpefe.fujiptp.ui.theme.SoftBlueDeep
import com.alpefe.fujiptp.ui.theme.Surface
import com.alpefe.fujiptp.ui.theme.SurfaceSoft

@Composable
fun HomeScreen(viewModel: FujiViewModel) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val devicePresent by viewModel.devicePresent.collectAsStateWithLifecycle()
    val cameraLabel by viewModel.cameraLabel.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val backlog by viewModel.backlog.collectAsStateWithLifecycle()

    var sendRecipe by remember { mutableStateOf<RecipeModel?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hola 👋",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tus recetas Fuji, en orden.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { viewModel.push(Screen.Diagnostics) }) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = "Diagnóstico",
                        tint = InkSoft,
                    )
                }
            }
        }
        item {
            CameraCard(
                connected = connected,
                devicePresent = devicePresent,
                cameraLabel = cameraLabel,
                busy = busy,
                onConnect = { viewModel.connectRequested() },
                onDisconnect = { viewModel.disconnect() },
                onRead = { viewModel.readFromCamera() },
                onSendAll = { viewModel.sendAllToCamera() },
            )
        }
        item {
            Column {
                Text(
                    "Recetas activas",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Tus 7 slots de la cámara, listos para disparar",
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
                // Asignar desde la biblioteca: navega ahí (donde podrás elegir
                // la recipe y asignarla a C1-C7 con la cámara conectada).
                onAssign = {
                    if (connected) viewModel.push(Screen.Backlog)
                    else viewModel.notifyUser("Conecta la cámara para poder asignar a un slot")
                },
                onClear = { viewModel.clearSlot(slot.index) },
            )
        }
    }

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
private fun CameraCard(
    connected: Boolean,
    devicePresent: Boolean,
    cameraLabel: String?,
    busy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRead: () -> Unit,
    onSendAll: () -> Unit,
) {
    val bg = when {
        connected -> PastelGreen
        devicePresent -> SoftBlue
        else -> SurfaceSoft
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.cardLarge),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = if (connected) PastelGreenDeep else SoftBlueDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            connected -> "Cámara conectada"
                            devicePresent -> "Cámara detectada"
                            else -> "Sin cámara"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    cameraLabel?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                connected -> PastelGreenDeep
                                devicePresent -> SoftBlueDeep
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        ),
                )
            }
            AnimatedVisibility(visible = connected) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onRead,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Radius.control),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Surface,
                                contentColor = Ink,
                            ),
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Leer C1–C7")
                        }
                        OutlinedButton(
                            onClick = onDisconnect,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Radius.control),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Surface.copy(alpha = 0.9f)),
                        ) {
                            Text("Desconectar")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onSendAll,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.control),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Peach,
                            contentColor = PeachDeep,
                        ),
                    ) {
                        Icon(Icons.Filled.Send, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enviar mis recipes a la cámara")
                    }
                }
            }
            AnimatedVisibility(visible = !connected && devicePresent) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = onConnect,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.control),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Surface,
                            contentColor = Ink,
                        ),
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Conectar cámara")
                    }
                }
            }
            AnimatedVisibility(visible = !connected && !devicePresent) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Conecta la cámara por USB en modo «RAW CONV./BACKUP RESTORE».",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
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
    val recipe = slot.recipe
    val tint = if (recipe != null) filmTint(recipe.filmSimulation) else SurfaceSoft
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (menuOpen) 0.97f else 1f,
        animationSpec = tween(140),
        label = "slotScale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(Radius.card))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (recipe != null) Surface else tint.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (recipe != null) tint else Surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "C${slot.index}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (recipe != null) MaterialTheme.colorScheme.primary else InkSoft,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (recipe != null) {
                    Text(
                        text = recipe.name.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    FilmSimulationChip(recipe.filmSimulation)
                } else {
                    Text(
                        "Vacío · toca para crear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Opciones de C${slot.index}",
                        tint = InkSoft,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (connected) {
                        DropdownMenuItem(
                            text = { Text("Enviar a la cámara") },
                            leadingIcon = { Icon(Icons.Filled.Send, null) },
                            enabled = recipe != null,
                            onClick = {
                                menuOpen = false
                                recipe?.let(onSend)
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
                    if (recipe != null) {
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

