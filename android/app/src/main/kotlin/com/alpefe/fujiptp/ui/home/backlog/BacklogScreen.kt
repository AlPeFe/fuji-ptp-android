package com.alpefe.fujiptp.ui.home.backlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.RecipeModel
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.components.SlotPickerDialog
import com.alpefe.fujiptp.ui.components.filmTint
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface
import java.text.DateFormat
import java.util.Date

@Composable
fun BacklogScreen(viewModel: FujiViewModel) {
    val backlog by viewModel.backlog.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var sendRecipe by remember { mutableStateOf<RecipeModel?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
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
                            "${backlog.size} recipe${if (backlog.size == 1) "" else "s"} guardada${if (backlog.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(backlog, key = { it.id }) { recipe ->
                BacklogCard(
                    recipe = recipe,
                    slotLabel = slots
                        .filter { it.recipe?.id == recipe.id }
                        .joinToString { "C${it.index}" },
                    connected = connected,
                    onOpen = { viewModel.push(Screen.Editor(recipe.id, null)) },
                    onSend = { sendRecipe = recipe },
                    onDuplicate = { viewModel.duplicateRecipe(recipe.id) },
                    onDelete = { viewModel.deleteRecipe(recipe.id) },
                )
            }
        }
        FloatingActionButton(
            onClick = { viewModel.push(Screen.Editor(null, null)) },
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

@Composable
private fun BacklogCard(
    recipe: RecipeModel,
    slotLabel: String,
    connected: Boolean,
    onOpen: () -> Unit,
    onSend: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(120),
        label = "backlogScale",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.card))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
