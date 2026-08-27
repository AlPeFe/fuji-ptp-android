package com.alpefe.fujiptp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    // --- recipes ------------------------------------------------------------

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): RecipeEntity?

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<RecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    // --- slots ----------------------------------------------------------------

    @Query(
        """SELECT s.slotIndex AS slotIndex, r.* FROM slots s
           LEFT JOIN recipes r ON s.recipeId = r.id
           ORDER BY s.slotIndex"""
    )
    fun observeSlots(): Flow<List<SlotWithRecipe>>

    @Query("SELECT * FROM slots WHERE slotIndex = :slot")
    suspend fun getSlot(slot: Int): SlotEntity?

    @Query("SELECT * FROM slots")
    suspend fun getAllSlots(): List<SlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: SlotEntity)

    @Query("SELECT slotIndex, recipeId FROM slots WHERE recipeId = :recipeId")
    suspend fun slotForRecipe(recipeId: Long): List<SlotIndexRow>

    @Query("DELETE FROM slots WHERE recipeId = :recipeId")
    suspend fun unassignRecipe(recipeId: Long)

    // --- collections -----------------------------------------------------------

    @Query(
        """SELECT c.id AS id, c.name AS name, c.colorHex AS colorHex,
                   c.isDefault AS isDefault,
                   (SELECT COUNT(*) FROM recipe_collections rc WHERE rc.collectionId = c.id) AS count
           FROM collections c
           ORDER BY c.isDefault DESC, c.name COLLATE NOCASE"""
    )
    fun observeCollections(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollection(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections ORDER BY id ASC")
    suspend fun getAllCollections(): List<CollectionEntity>

    @Query("SELECT id FROM collections WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCollectionId(): Long?

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun collectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id AND isDefault = 0")
    suspend fun deleteCollection(id: Long)

    @Query("INSERT OR IGNORE INTO recipe_collections (recipeId, collectionId) VALUES (:recipeId, :collectionId)")
    suspend fun addRecipeToCollection(recipeId: Long, collectionId: Long)

    @Query("DELETE FROM recipe_collections WHERE recipeId = :recipeId AND collectionId = :collectionId")
    suspend fun removeRecipeFromCollection(recipeId: Long, collectionId: Long)

    @Query("SELECT collectionId FROM recipe_collections WHERE recipeId = :recipeId")
    suspend fun collectionIdsForRecipe(recipeId: Long): List<Long>

    @Query("DELETE FROM recipe_collections WHERE collectionId = :collectionId")
    suspend fun clearCollection(collectionId: Long)

    // --- clear library ------------------------------------------------------

    @Query("DELETE FROM recipe_collections")
    suspend fun clearAllMemberships()

    @Query("DELETE FROM recipes")
    suspend fun clearAllRecipes()

    @Query("DELETE FROM collections WHERE isDefault = 0")
    suspend fun clearAllNonDefaultCollections()

    @Query("DELETE FROM slots")
    suspend fun clearAllSlots()

    // --- recipes in a collection ------------------------------------------------

    @Query(
        """SELECT r.* FROM recipes r
           INNER JOIN recipe_collections rc ON r.id = rc.recipeId
           WHERE rc.collectionId = :collectionId
           ORDER BY r.updatedAt DESC"""
    )
    fun observeRecipesInCollection(collectionId: Long): Flow<List<RecipeEntity>>

    data class SlotIndexRow(val slotIndex: Int, val recipeId: Long?)
}
