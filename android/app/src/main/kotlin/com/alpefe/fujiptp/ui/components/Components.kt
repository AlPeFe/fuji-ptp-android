package com.alpefe.fujiptp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpefe.fujiptp.data.FilmSimulation
import com.alpefe.fujiptp.ui.SlotUi
import com.alpefe.fujiptp.ui.theme.FilmClassicChrome
import com.alpefe.fujiptp.ui.theme.FilmClassicNeg
import com.alpefe.fujiptp.ui.theme.FilmEterna
import com.alpefe.fujiptp.ui.theme.FilmMono
import com.alpefe.fujiptp.ui.theme.FilmProvia
import com.alpefe.fujiptp.ui.theme.FilmVelvia
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.OnPastel
import com.alpefe.fujiptp.ui.theme.Radius

// Soft pastel tint per film simulation.
val filmTint: (FilmSimulation) -> Color = { film ->
    when (film) {
        FilmSimulation.Velvia -> FilmVelvia
        FilmSimulation.ClassicChrome -> FilmClassicChrome
        FilmSimulation.ClassicNegative -> FilmClassicNeg
        FilmSimulation.NostalgicNegative -> FilmClassicNeg
        FilmSimulation.Eterna -> FilmEterna
        FilmSimulation.EternaBleachBypass -> FilmEterna
        FilmSimulation.Provia -> FilmProvia
        FilmSimulation.RealaAce -> FilmProvia
        FilmSimulation.Astia -> FilmClassicNeg
        FilmSimulation.Monochrome,
        FilmSimulation.MonochromeYellow,
        FilmSimulation.MonochromeRed,
        FilmSimulation.MonochromeGreen,
        FilmSimulation.Sepia,
        FilmSimulation.Acros,
        FilmSimulation.AcrosYellow,
        FilmSimulation.AcrosRed,
        FilmSimulation.AcrosGreen -> FilmMono
        FilmSimulation.ProNegHigh -> FilmVelvia
        FilmSimulation.ProNegStandard -> FilmEterna
    }
}

/** Compact pill chip showing a film simulation in its pastel tone. */
@Composable
fun FilmSimulationChip(film: FilmSimulation, modifier: Modifier = Modifier) {
    val tint = filmTint(film)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.chip))
            .background(tint)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(OnPastel.copy(alpha = 0.45f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = film.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = OnPastel,
        )
    }
}

/** Picker of the 7 camera slots. Soft pastel rows. */
@Composable
fun SlotPickerDialog(
    title: String,
    slots: List<SlotUi>,
    busy: Boolean = false,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.card),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            if (busy) {
                Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column {
                    slots.forEach { slot ->
                        val tint = if (slot.recipe != null) {
                            filmTint(slot.recipe.filmSimulation)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tint.copy(alpha = 0.55f))
                                .clickable { onPick(slot.index) }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "C${slot.index}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(38.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = slot.recipe?.name?.ifBlank { "Sin nombre" } ?: "Vacío",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                slot.recipe?.let {
                                    Spacer(Modifier.height(3.dp))
                                    FilmSimulationChip(it.filmSimulation)
                                }
                            }
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
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

/** Generic dropdown anchored below a label, with soft pastel trigger. */
@Composable
fun <T> LabeledDropdown(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box {
            TextButton(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.control))
                    .background(tint),
            ) {
                Text(
                    text = optionLabel(selected),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "▾",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(18.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/** Pressable pill with a subtle press scale animation. */
@Composable
fun PressablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.96f else 1f,
        animationSpec = tween(160),
        label = "pillScale",
    )
    Text(
        text = text,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) tint else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
