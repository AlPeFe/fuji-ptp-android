package com.alpefe.fujiptp.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.components.filmTint
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.PastelGreen
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.Surface

/**
 * Read-only view of the recipes currently loaded in the camera. The user
 * cannot edit these here — only view them (and optionally save one to the
 * library). Editing/transferring happens in the "Activas" profile.
 */
@Composable
fun CameraRecipesScreen(
    viewModel: FujiViewModel,
    onBack: () -> Unit,
) {
    val recipes by viewModel.cameraRecipes.collectAsStateWithLifecycle()

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
                        Text(
                            "En la cámara",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "Recipes cargadas ahora mismo · solo lectura",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PastelGreen.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = com.alpefe.fujiptp.ui.theme.PastelGreenDeep,
                            modifier = Modifier.size(18.dp),
                        )
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
                            "Conecta la cámara y pulsa «Leer C1–C7»\npara ver sus recipes aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            items(recipes, key = { it.name }) { recipe ->
                CameraRecipeCard(
                    recipe = recipe,
                    slot = recipes.indexOf(recipe) + 1,
                    onSave = { viewModel.saveCameraRecipe(recipes.indexOf(recipe) + 1, recipe) },
                )
            }
        }
    }
}

@Composable
private fun CameraRecipeCard(
    recipe: com.alpefe.fujiptp.data.RecipeModel,
    slot: Int,
    onSave: () -> Unit,
) {
    val tint = filmTint(recipe.filmSimulation)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(Radius.card),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "C$slot",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    recipe.name.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                FilmSimulationChip(recipe.filmSimulation)
            }
            FilledTonalButton(
                onClick = onSave,
                shape = RoundedCornerShape(Radius.control),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = Peach,
                    contentColor = PeachDeep,
                ),
            ) {
                Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Guardar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
