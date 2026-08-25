package com.alpefe.fujiptp.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.alpefe.fujiptp.ui.components.PressablePill
import com.alpefe.fujiptp.ui.components.SlotPickerDialog
import com.alpefe.fujiptp.ui.components.filmTint
import com.alpefe.fujiptp.ui.theme.Canvas
import com.alpefe.fujiptp.ui.theme.DustyPink
import com.alpefe.fujiptp.ui.theme.Ink
import com.alpefe.fujiptp.ui.theme.InkSoft
import com.alpefe.fujiptp.ui.theme.Lavender
import com.alpefe.fujiptp.ui.theme.LavenderDeep
import com.alpefe.fujiptp.ui.theme.Peach
import com.alpefe.fujiptp.ui.theme.PeachDeep
import com.alpefe.fujiptp.ui.theme.Radius
import com.alpefe.fujiptp.ui.theme.SoftBlue
import com.alpefe.fujiptp.ui.theme.SoftBlueDeep
import com.alpefe.fujiptp.ui.theme.SoftYellow
import com.alpefe.fujiptp.ui.theme.SoftYellowDeep
import com.alpefe.fujiptp.ui.theme.Surface
import com.alpefe.fujiptp.ui.theme.SurfaceSoft
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: FujiViewModel,
    recipeId: Long?,
    fromSlot: Int?,
    assignOnSave: Int?,
    collectionId: Long?,
    onBack: () -> Unit,
) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var recipe by remember(recipeId) {
        // New recipes render immediately (no blank flash); saved recipes
        // load async below.
        mutableStateOf(if (recipeId != null) null else RecipeModel.newDraft())
    }
    var pickFilm by remember { mutableStateOf(false) }
    var pickSlot by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            recipe = viewModel.getRecipe(recipeId) ?: RecipeModel.newDraft()
        }
    }

    val current = recipe ?: return

    Surface(Modifier.fillMaxSize(), color = Canvas) {
        Column(Modifier.fillMaxSize()) {
            // Top bar (below the status bar)
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Ink,
                    )
                }
                Text(
                    text = if (recipeId != null) current.name.ifBlank { "Recipe" } else "Nueva recipe",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        dirty = false
                        viewModel.saveRecipe(current, assignOnSave, collectionId)
                    },
                    enabled = !busy,
                ) {
                    Text(
                        "Guardar",
                        fontWeight = FontWeight.Bold,
                        color = PeachDeep,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.control),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface,
                            focusedBorderColor = PeachDeep.copy(alpha = 0.6f),
                            unfocusedBorderColor = HairlineColor,
                        ),
                    )
                }

                // Film simulation
                item {
                    SectionCard(tint = DustyPink) {
                        SectionTitle("Simulación de película")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.control))
                                .background(filmTint(current.filmSimulation).copy(alpha = 0.6f))
                                .clickable { pickFilm = true }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilmSimulationChip(current.filmSimulation)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Cambiar ▾",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSoft,
                            )
                        }
                    }
                }

                // Dynamic range
                item {
                    SectionCard(tint = SoftBlue) {
                        SectionTitle("Rango dinámico")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DynamicRange.entries.forEach { dr ->
                                PressablePill(
                                    text = dr.label,
                                    selected = current.dynamicRange == dr && current.dynamicRangePriority == 0,
                                    tint = SoftBlueDeep,
                                    onClick = {
                                        recipe = current.copy(dynamicRange = dr, dynamicRangePriority = 0)
                                        dirty = true
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Prioridad DR",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PressablePill(
                                "Off",
                                current.dynamicRangePriority == 0,
                                tint = SoftBlueDeep,
                                onClick = {
                                    recipe = current.copy(dynamicRangePriority = 0)
                                    dirty = true
                                },
                            )
                            PressablePill(
                                "Auto",
                                current.dynamicRangePriority == 1,
                                tint = SoftBlueDeep,
                                onClick = {
                                    recipe = current.copy(dynamicRangePriority = 1)
                                    dirty = true
                                },
                            )
                            PressablePill(
                                "Strong",
                                current.dynamicRangePriority == 2,
                                tint = SoftBlueDeep,
                                onClick = {
                                    recipe = current.copy(dynamicRangePriority = 2)
                                    dirty = true
                                },
                            )
                            PressablePill(
                                "Weak",
                                current.dynamicRangePriority == 32768,
                                tint = SoftBlueDeep,
                                onClick = {
                                    recipe = current.copy(dynamicRangePriority = 32768)
                                    dirty = true
                                },
                            )
                        }
                        AnimatedVisibility(visible = current.dynamicRangePriority != 0) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Con DR Priority activo, la cámara fija el DR en AUTO.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                    }
                }

                // Effects
                item {
                    SectionCard(tint = Lavender) {
                        SectionTitle("Efectos")
                        Spacer(Modifier.height(12.dp))
                        LabeledDropdown(
                            label = "Grain Effect",
                            options = GrainEffect.entries.toList(),
                            optionLabel = { it.label },
                            selected = current.grainEffect,
                            onSelect = {
                                recipe = current.copy(grainEffect = it)
                                dirty = true
                            },
                            tint = Surface,
                        )
                        AnimatedVisibility(visible = !current.isMonochrome) {
                            Column {
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
                                    tint = Surface,
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
                                    tint = Surface,
                                )
                            }
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
                            tint = Surface,
                        )
                    }
                }

                // White balance
                item {
                    SectionCard(tint = SoftYellow) {
                        SectionTitle("Balance de blancos")
                        Spacer(Modifier.height(12.dp))
                        LabeledDropdown(
                            label = "Modo",
                            options = WhiteBalanceMode.entries.toList(),
                            optionLabel = { it.label },
                            selected = current.whiteBalanceMode,
                            onSelect = {
                                recipe = current.copy(whiteBalanceMode = it)
                                dirty = true
                            },
                            tint = Surface,
                        )
                        Spacer(Modifier.height(12.dp))
                        DialSlider(
                            "Desplaz. R",
                            current.whiteBalanceShiftR.toFloat(),
                            -9f..9f,
                            1f,
                            enabled = !current.isMonochrome,
                        ) {
                            recipe = current.copy(whiteBalanceShiftR = it.toInt())
                            dirty = true
                        }
                        DialSlider(
                            "Desplaz. B",
                            current.whiteBalanceShiftB.toFloat(),
                            -9f..9f,
                            1f,
                            enabled = !current.isMonochrome,
                        ) {
                            recipe = current.copy(whiteBalanceShiftB = it.toInt())
                            dirty = true
                        }
                        AnimatedVisibility(visible = current.whiteBalanceMode == WhiteBalanceMode.ColorTemperature) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Modo K: fija la temperatura de color en Kelvin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
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
                    }
                }

                // Tones
                item {
                    SectionCard(tint = Peach) {
                        SectionTitle("Tonalidad")
                        Spacer(Modifier.height(8.dp))
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
                }

                // Noise reduction
                item {
                    SectionCard(tint = DustyPink) {
                        SectionTitle("Reducción de ruido")
                        Spacer(Modifier.height(8.dp))
                        DialSlider("High ISO NR", current.noiseReduction.toFloat(), -4f..4f, 1f) {
                            recipe = current.copy(noiseReduction = it.toInt())
                            dirty = true
                        }
                    }
                }

                // Monochrome adjustments
                item {
                    AnimatedVisibility(visible = current.isMonochrome) {
                        SectionCard(tint = SurfaceSoft) {
                            SectionTitle("Ajustes monocromo")
                            Spacer(Modifier.height(8.dp))
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
                }

                item {
                    Column {
                        Text(
                            if (connected) {
                                "La recipe se guarda en la biblioteca. Usa «Enviar a la cámara» para escribirla en un slot."
                            } else {
                                "Sin cámara conectada: la recipe se guardará en la biblioteca y podrás enviarla después."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { pickSlot = true },
                            enabled = connected && !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.control),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeachDeep,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Filled.Send, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar a la cámara…")
                        }
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

private val HairlineColor = Color(0xFFEFEBE4)

@Composable
private fun SectionCard(tint: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.card),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Ink,
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
                color = if (value == 0f) InkSoft else PeachDeep,
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = { onChange((it / step).roundToInt() * step) },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = PeachDeep,
                activeTrackColor = PeachDeep,
                inactiveTrackColor = PeachDeep.copy(alpha = 0.15f),
                disabledThumbColor = InkSoft.copy(alpha = 0.4f),
                disabledActiveTrackColor = InkSoft.copy(alpha = 0.2f),
                disabledInactiveTrackColor = InkSoft.copy(alpha = 0.1f),
            ),
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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.card),
        title = { Text("Simulación de película", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(FilmSimulation.entries.toList()) { film ->
                    val isSelected = film == selected
                    val tint = filmTint(film)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .background(if (isSelected) tint else tint.copy(alpha = 0.45f))
                            .clickable { onPick(film) }
                            .padding(12.dp),
                    ) {
                        Text(
                            text = film.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                        Text(
                            text = film.wire,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
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
