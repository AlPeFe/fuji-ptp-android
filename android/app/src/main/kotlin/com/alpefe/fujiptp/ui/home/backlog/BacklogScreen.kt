package com.alpefe.fujiptp.ui.home.backlog

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.ui.CollectionUi
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.Screen
import com.alpefe.fujiptp.ui.theme.Danger
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface

val collectionTints = listOf(
    0xFF463A66, 0xFF35445C, 0xFF35453A, 0xFF5A3826, 0xFF4E442C, 0xFF55303C,
)
val collectionDeepTints = listOf(
    0xFFA99BE8, 0xFF8FB4E8, 0xFF8FC49C, 0xFFF0A878, 0xFFE0BC6E, 0xFFE89BB1,
)

private fun pickCollectionColor(index: Int): Long =
    collectionTints[index % collectionTints.size]

/**
 * Library root: shows the collections. Tap a collection to open it
 * (recipes live inside). The FAB creates a new collection.
 */
@Composable
fun BacklogScreen(viewModel: FujiViewModel) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<CollectionUi?>(null) }
    var menuFor by remember { mutableStateOf<CollectionUi?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column {
                Text(
                    "Biblioteca",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${collections.size} colección${if (collections.size == 1) "" else "es"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(collections, key = { it.id }) { collection ->
            CollectionCard(
                collection = collection,
                onOpen = { viewModel.push(Screen.Collection(collection.id, collection.name)) },
                onMenu = { menuFor = collection },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
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

    renaming?.let { collection ->
        CollectionNameDialog(
            title = "Renombrar colección",
            initialName = collection.name,
            onConfirm = { viewModel.renameCollection(collection.id, it) },
            onDismiss = { renaming = null },
        )
    }

    menuFor?.let { collection ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Radius.card),
            title = { Text(collection.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            menuFor = null
                            renaming = collection
                        },
                    ) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Renombrar")
                    }
                    TextButton(
                        onClick = {
                            menuFor = null
                            viewModel.deleteCollection(collection.id)
                        },
                        enabled = !collection.isDefault,
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (collection.isDefault) "La colección por defecto no se puede eliminar"
                            else "Eliminar colección"
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { menuFor = null }) { Text("Cerrar") }
            },
        )
    }
}

@Composable
private fun CollectionCard(
    collection: CollectionUi,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
) {
    val tint = Color(collection.colorHex)
    val deep = Color(collectionDeepTints[collection.id.toInt() % collectionDeepTints.size])
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
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
                    color = PeachDeep,
                )
            }
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
                Text(if (title.startsWith("Renombrar")) "Renombrar" else "Crear", color = PeachDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
