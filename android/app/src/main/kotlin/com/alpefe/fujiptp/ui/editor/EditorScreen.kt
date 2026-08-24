package com.alpefe.fujiptp.ui.editor

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpefe.fujiptp.data.DynamicRange
import com.alpefe.fujiptp.data.EffectStrength
import com.alpefe.fujiptp.data.FilmSimulation
import com.alpefe.fujiptp.data.GrainEffect
import com.alpefe.fujiptp.data.RecipeModel
import com.alpefe.fujiptp.data.WhiteBalanceMode
import com.alpefe.fujiptp.ui.FujiViewModel
import com.alpefe.fujiptp.ui.components.FilmSimulationChip
import com.alpefe.fujiptp.ui.components.LabeledDropdown
import com.alpefe.fujiptp.ui.components.SlotPickerDialog
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: FujiViewModel,
    recipeId: Long?,
    fromSlot: Int?,
    assignOnSave: Int?,
    onBack: () -> Unit,
) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var recipe by remember { mutableStateOf<RecipeModel?>(null) }
    var pickFilm by remember { mutableStateOf(false) }
    var pickSlot by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        recipe = if (recipeId != null) {
            viewModel.getRecipe(recipeId) ?: RecipeModel.newDraft()
        } else {
            RecipeModel.newDraft()
        }
    }

    val current = recipe ?: return

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // Top bar (below the status bar; API 35+ draws edge-to-edge)
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = if (recipeId != null) current.name.ifBlank { "Recipe" } else "Nueva recipe",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        dirty = false
                        viewModel.saveRecipe(current, assignOnSave)
                    },
                    enabled = !busy,
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Name
                item {
                    OutlinedTextField(
                        value = current.name,
                        onValueChange = {
                            recipe = current.copy(name = it)
                            dirty = true
                        },
                        label = { Text("Nombre de la recipe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Film simulation
                item {
                    SectionTitle("Simulación de película")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pickFilm = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilmSimulationChip(current.filmSimulation)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Cambiar ▾",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Dynamic range (X100VI: DR + DR Priority)
                item {
                    SectionTitle("Rango dinámico")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DynamicRange.entries.forEach { dr ->
                            ChoiceChip(
                                label = dr.label,
                                selected = current.dynamicRange == dr && current.dynamicRangePriority == 0,
                                onClick = {
                                    recipe = current.copy(dynamicRange = dr, dynamicRangePriority = 0)
                                    dirty = true
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Prioridad DR:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ChoiceChip("Off", current.dynamicRangePriority == 0) {
                            recipe = current.copy(dynamicRangePriority = 0)
                            dirty = true
                        }
                        ChoiceChip("Auto", current.dynamicRangePriority == 1) {
                            recipe = current.copy(dynamicRangePriority = 1)
                            dirty = true
                        }
                        ChoiceChip("Strong", current.dynamicRangePriority == 2) {
                            recipe = current.copy(dynamicRangePriority = 2)
                            dirty = true
                        }
                        ChoiceChip("Weak", current.dynamicRangePriority == 32768) {
                            recipe = current.copy(dynamicRangePriority = 32768)
                            dirty = true
                        }
                    }
                    if (current.dynamicRangePriority != 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Con DR Priority activo, la cámara fija el DR en AUTO.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Effects
                item {
                    SectionTitle("Efectos")
                    LabeledDropdown(
                        label = "Grain Effect",
                        options = GrainEffect.entries.toList(),
                        optionLabel = { it.label },
                        selected = current.grainEffect,
                        onSelect = {
                            recipe = current.copy(grainEffect = it)
                            dirty = true
                        },
                    )
                    if (!current.isMonochrome) {
                        Spacer(Modifier.height(12.dp))
                        LabeledDropdown(
                            label = "Color Chrome Effect",
                            options = EffectStrength.entries.toList(),
                            optionLabel = { it.label },
                            selected = current.colorChrome,
                            onSelect = {
                                recipe = current.copy(colorChrome = it)
                                dirty = true
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        LabeledDropdown(
                            label = "Color Chrome FX Blue",
                            options = EffectStrength.entries.toList(),
                            optionLabel = { it.label },
                            selected = current.colorChromeFxBlue,
                            onSelect = {
                                recipe = current.copy(colorChromeFxBlue = it)
                                dirty = true
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledDropdown(
                        label = "Smooth Skin Effect",
                        options = EffectStrength.entries.toList(),
                        optionLabel = { it.label },
                        selected = current.smoothSkin,
                        onSelect = {
                            recipe = current.copy(smoothSkin = it)
                            dirty = true
                        },
                    )
                }

                // White balance
                item {
                    SectionTitle("Balance de blancos")
                    LabeledDropdown(
                        label = "Modo",
                        options = WhiteBalanceMode.entries.toList(),
                        optionLabel = { it.label },
                        selected = current.whiteBalanceMode,
                        onSelect = {
                            recipe = current.copy(whiteBalanceMode = it)
                            dirty = true
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    DialSlider("Desplaz. R", current.whiteBalanceShiftR.toFloat(), -9f..9f, 1f, enabled = !current.isMonochrome) {
                        recipe = current.copy(whiteBalanceShiftR = it.toInt())
                        dirty = true
                    }
                    DialSlider("Desplaz. B", current.whiteBalanceShiftB.toFloat(), -9f..9f, 1f, enabled = !current.isMonochrome) {
                        recipe = current.copy(whiteBalanceShiftB = it.toInt())
                        dirty = true
                    }
                    if (current.whiteBalanceMode == WhiteBalanceMode.ColorTemperature) {
                        DialSlider(
                            "Temperatura (K)",
                            (current.whiteBalanceTemperature ?: 5500).toFloat(),
                            2500f..10000f,
                            100f,
                        ) {
                            recipe = current.copy(whiteBalanceTemperature = it.toInt())
                            dirty = true
                        }
                    }
                }

                // Tones
                item {
                    SectionTitle("Tonalidad")
                    DialSlider("Highlight", current.highlight, -2f..2f, 0.5f) {
                        recipe = current.copy(highlight = it)
                        dirty = true
                    }
                    DialSlider("Shadow", current.shadow, -2f..2f, 0.5f) {
                        recipe = current.copy(shadow = it)
                        dirty = true
                    }
                    DialSlider("Color", current.color, -4f..4f, 0.5f) {
                        recipe = current.copy(color = it)
                        dirty = true
                    }
                    DialSlider("Sharpness", current.sharpness, -4f..4f, 0.5f) {
                        recipe = current.copy(sharpness = it)
                        dirty = true
                    }
                    DialSlider("Clarity", current.clarity, -5f..5f, 1f) {
                        recipe = current.copy(clarity = it)
                        dirty = true
                    }
                }

                // Noise reduction
                item {
                    SectionTitle("Reducción de ruido")
                    DialSlider("High ISO NR", current.noiseReduction.toFloat(), -4f..4f, 1f) {
                        recipe = current.copy(noiseReduction = it.toInt())
                        dirty = true
                    }
                }

                // Monochrome adjustments
                if (current.isMonochrome) {
                    item {
                        SectionTitle("Ajustes monocromo")
                        DialSlider("WC (Warm/Cool)", current.monochromeWc, -4f..4f, 0.5f) {
                            recipe = current.copy(monochromeWc = it)
                            dirty = true
                        }
                        DialSlider("MG (Magenta/Green)", current.monochromeMg, -4f..4f, 0.5f) {
                            recipe = current.copy(monochromeMg = it)
                            dirty = true
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (connected) {
                            "La recipe se guarda en la biblioteca. Usa «Enviar a la cámara» para escribirla en un slot."
                        } else {
                            "Sin cámara conectada: la recipe se guardará en la biblioteca y podrás enviarla después."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { pickSlot = true },
                        enabled = connected && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enviar a la cámara…")
                    }
                }
            }
        }
    }

    if (pickFilm) {
        FilmPickerDialog(
            selected = current.filmSimulation,
            onPick = {
                recipe = current.copy(filmSimulation = it)
                dirty = true
                pickFilm = false
            },
            onDismiss = { pickFilm = false },
        )
    }

    if (pickSlot) {
        SlotPickerDialog(
            title = "Enviar «${current.name.ifBlank { "recipe" }}» a…",
            slots = slots,
            busy = busy,
            onPick = { slot ->
                viewModel.sendToSlot(slot, current)
                pickSlot = false
                onBack()
            },
            onDismiss = { pickSlot = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = content,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DialSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    enabled: Boolean = true,
    onChange: (Float) -> Unit,
) {
    val steps = (((range.endInclusive - range.start) / step).toInt() - 1).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = fmtDial(value),
                style = MaterialTheme.typography.labelLarge,
                color = if (value == 0f) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = { onChange((it / step).roundToInt() * step) },
            valueRange = range,
            steps = steps,
            enabled = enabled,
        )
    }
}

private fun fmtDial(v: Float): String {
    val snapped = (v * 10).roundToInt() / 10f
    return if (snapped == snapped.toInt().toFloat()) {
        if (snapped > 0) "+${snapped.toInt()}" else "${snapped.toInt()}"
    } else {
        if (snapped > 0) "+$snapped" else "$snapped"
    }
}

@Composable
private fun FilmPickerDialog(
    selected: FilmSimulation,
    onPick: (FilmSimulation) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulación de película") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(380.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FilmSimulation.entries.toList()) { film ->
                    val isSelected = film == selected
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(film) }
                            .background(
                                if (isSelected) Color(film.hex) else Color(film.hex).copy(alpha = 0.14f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(10.dp),
                    ) {
                        Text(
                            text = film.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else Color(film.hex),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = film.wire,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
