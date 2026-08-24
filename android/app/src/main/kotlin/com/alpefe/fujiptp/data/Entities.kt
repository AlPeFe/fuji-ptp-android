package com.alpefe.fujiptp.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val filmSimulation: String,
    val dynamicRange: String,
    val grainEffect: String,
    val smoothSkin: String,
    val colorChrome: String,
    val colorChromeFxBlue: String,
    val whiteBalanceMode: String,
    val whiteBalanceShiftR: Int,
    val whiteBalanceShiftB: Int,
    val whiteBalanceTemperature: Int?,
    val highlight: Float,
    val shadow: Float,
    val color: Float,
    val sharpness: Float,
    val clarity: Float,
    val noiseReduction: Int,
    val exposure: Float,
    val dynamicRangePriority: Int,
    val monochromeWc: Float,
    val monochromeMg: Float,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toModel(): RecipeModel = RecipeModel(
        id = id,
        name = name,
        filmSimulation = enumWire(FilmSimulation.entries, filmSimulation, FilmSimulation.ClassicChrome),
        dynamicRange = enumWire(DynamicRange.entries, dynamicRange, DynamicRange.Dr100),
        grainEffect = enumWire(GrainEffect.entries, grainEffect, GrainEffect.Off),
        smoothSkin = enumWire(EffectStrength.entries, smoothSkin, EffectStrength.Off),
        colorChrome = enumWire(EffectStrength.entries, colorChrome, EffectStrength.Off),
        colorChromeFxBlue = enumWire(EffectStrength.entries, colorChromeFxBlue, EffectStrength.Off),
        whiteBalanceMode = enumWire(WhiteBalanceMode.entries, whiteBalanceMode, WhiteBalanceMode.Auto),
        whiteBalanceShiftR = whiteBalanceShiftR,
        whiteBalanceShiftB = whiteBalanceShiftB,
        whiteBalanceTemperature = whiteBalanceTemperature,
        highlight = highlight,
        shadow = shadow,
        color = color,
        sharpness = sharpness,
        clarity = clarity,
        noiseReduction = noiseReduction,
        exposure = exposure,
        dynamicRangePriority = dynamicRangePriority,
        monochromeWc = monochromeWc,
        monochromeMg = monochromeMg,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        private fun <T : Enum<T>> enumWire(values: List<T>, wire: String, fallback: T): T =
            values.firstOrNull { it.name == wire } ?: fallback

        fun fromModel(m: RecipeModel, now: Long): RecipeEntity = RecipeEntity(
            id = m.id,
            name = m.name,
            filmSimulation = m.filmSimulation.name,
            dynamicRange = m.dynamicRange.name,
            grainEffect = m.grainEffect.name,
            smoothSkin = m.smoothSkin.name,
            colorChrome = m.colorChrome.name,
            colorChromeFxBlue = m.colorChromeFxBlue.name,
            whiteBalanceMode = m.whiteBalanceMode.name,
            whiteBalanceShiftR = m.whiteBalanceShiftR,
            whiteBalanceShiftB = m.whiteBalanceShiftB,
            whiteBalanceTemperature = m.whiteBalanceTemperature,
            highlight = m.highlight,
            shadow = m.shadow,
            color = m.color,
            sharpness = m.sharpness,
            clarity = m.clarity,
            noiseReduction = m.noiseReduction,
            exposure = m.exposure,
            dynamicRangePriority = m.dynamicRangePriority,
            monochromeWc = m.monochromeWc,
            monochromeMg = m.monochromeMg,
            createdAt = if (m.createdAt > 0) m.createdAt else now,
            updatedAt = now,
        )
    }
}

/** One of the 7 physical camera slots (C1..C7). */
@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey val slotIndex: Int,
    val recipeId: Long?,
    val updatedAt: Long,
)

/** Slot joined with its assigned recipe (recipe may be null = empty slot). */
data class SlotWithRecipe(
    val slotIndex: Int,
    @Embedded val recipe: RecipeEntity?,
)

/**
 * A collection of recipes in the library. The app always keeps at least one
 * collection; the default "Todas" collection cannot be deleted.
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Many-to-many join between recipes and collections. */
@Entity(
    tableName = "recipe_collections",
    primaryKeys = ["recipeId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId")],
)
data class RecipeCollectionEntity(
    val recipeId: Long,
    val collectionId: Long,
)

/** Collection joined with its recipe count. */
data class CollectionWithCount(
    val id: Long,
    val name: String,
    val colorHex: Long,
    val isDefault: Boolean,
    val count: Int,
)

/** Recipe joined with its collection ids. */
data class RecipeWithCollections(
    @Embedded val recipe: RecipeEntity,
    val collectionIds: List<Long>,
)
